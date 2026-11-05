# Milestone: Resolve Oracle tablespace quota error and verify with Postman

**Status:** Oracle granted the application user quota on `USERS`; retrying the create request returned `201 Created`.

### Problem

Postman sent a valid create request, but the API returned HTTP 500. The database reported `ORA-01950`, meaning the `SERVICEDESK` user did not have quota to allocate space in the `USERS` tablespace. Oracle could not create or extend the table/segment needed to persist the request. The API's internal server error was the application-level result of that database write failure.

### Root cause

The application user lacked a tablespace quota. A database user can authenticate successfully and still lack permission to allocate storage for inserts or schema objects.

### Solution

Connect to the Oracle container as an administrator, confirm the current container is the application PDB, grant a bounded quota to the application account, and exit SQL*Plus:

```bash
docker exec -it oracle-free sqlplus sys@FREEPDB1 as sysdba
```

At the SQL*Plus password prompt, enter the administrator password. Then run:

```sql
SHOW CON_NAME;
ALTER USER SERVICEDESK QUOTA 100M ON USERS;
EXIT
```

Confirm `SHOW CON_NAME` reports `FREEPDB1` before altering the user. `EXIT` closes SQL*Plus and returns to the terminal. The quota statement grants up to 100 MB in the `USERS` tablespace for this account.

### Verify in Postman

Retry the same request after the API is running:

- Method: `POST`
- URL: `http://localhost:8080/api/requests`
- Header: `Content-Type: application/json`
- Body: choose **raw** and **JSON**, then send:

  ```json
  {
    "title": "Laptop won't connect to Wi-Fi",
    "description": "The laptop cannot connect to the office network.",
    "priority": "HIGH"
  }
  ```

Expected result: `201 Created` and a response containing the generated `id`, `status` of `OPEN`, `createdAt` timestamp, plus the submitted title, description, and priority. The successful response observed was:

```json
{
  "createdAt": "2026-10-26T17:16:25.571153",
  "description": "The laptop cannot connect to the office network.",
  "id": 2,
  "priority": "HIGH",
  "status": "OPEN",
  "title": "Laptop won't connect to Wi-Fi"
}
```

IDs and timestamps vary between runs; the important checks are the 201 status, a generated ID, and `OPEN` status.

### Key takeaway

When a request reaches the database but fails during persistence, inspect the Oracle error in the application logs. Authentication, schema privileges, and tablespace quota are separate concerns; a successful login does not guarantee that the account can allocate storage.
