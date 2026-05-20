# Run Instructions

## Prerequisites
- **Java 21 JDK** (Spring Boot 3.4)
- **Docker Desktop** (PostgreSQL via `compose.yml`)
- **Maven** (wrapper included)

### JAVA_HOME (Windows PowerShell)
```powershell
$env:JAVA_HOME = "C:\Program Files\Microsoft\jdk-21.0.11.10-hotspot"
$env:Path = "$env:JAVA_HOME\bin;$env:Path"
java -version
.\mvnw.cmd -version
```

## 1) Start PostgreSQL
From `issueflow-java`:
```powershell
docker compose up -d
docker compose ps
```

### Reset database (if Flyway/schema errors from old volume)
```powershell
docker compose down -v
docker compose up -d
```

## 2) Build
```powershell
.\mvnw.cmd clean package -DskipTests
```

## 3) Run application (PostgreSQL + Flyway)
```powershell
.\mvnw.cmd spring-boot:run
```

- API: `http://localhost:8080`
- Swagger UI: `http://localhost:8080/swagger-ui/index.html`
- OpenAPI: `http://localhost:8080/v3/api-docs`

## 4) Run tests (H2 in-memory)
```powershell
.\mvnw.cmd test
```

Includes unit tests and `IssueFlowApiIntegrationTest` (MockMvc).

## Seeded users (first startup on empty PostgreSQL DB)
| Username | Password | Role |
|----------|----------|------|
| admin | secret | ADMIN |
| jdoe | secret | DEVELOPER |
| asmith | secret | DEVELOPER |

## Example API flow (PowerShell)
```powershell
# Login
$login = Invoke-RestMethod -Method Post -Uri "http://localhost:8080/auth/login" `
  -ContentType "application/json" `
  -Body '{"username":"admin","password":"secret"}'
$token = $login.accessToken

# Me
Invoke-RestMethod -Uri "http://localhost:8080/auth/me" -Headers @{ Authorization = "Bearer $token" }

# Create user (password required)
Invoke-RestMethod -Method Post -Uri "http://localhost:8080/users" `
  -Headers @{ Authorization = "Bearer $token" } `
  -ContentType "application/json" `
  -Body '{"username":"newdev","email":"new@test.local","fullName":"New Dev","role":"DEVELOPER","password":"secret"}'

# List projects
Invoke-RestMethod -Uri "http://localhost:8080/projects" -Headers @{ Authorization = "Bearer $token" }

# Create ticket
Invoke-RestMethod -Method Post -Uri "http://localhost:8080/tickets" `
  -Headers @{ Authorization = "Bearer $token" } `
  -ContentType "application/json" `
  -Body '{"title":"Bug","description":"desc","status":"TODO","priority":"LOW","type":"BUG","projectId":1}'

# Audit logs (README fields: performedBy, actor, oldValue, newValue)
Invoke-RestMethod -Uri "http://localhost:8080/audit-logs?entityType=TICKET&page=1&pageSize=20" `
  -Headers @{ Authorization = "Bearer $token" }
```

## Attachments
README contract: **upload** (`POST /tickets/{id}/attachments`) and **delete** only.  
List/download endpoints are not in the README and are not implemented.

## Swagger Authorize
1. `POST /auth/login` with `admin` / `secret`
2. Click **Authorize**, paste JWT (or `Bearer <token>`)
3. Call protected endpoints

## Troubleshooting
| Issue | Fix |
|-------|-----|
| `JAVA_HOME` error | Set JDK 21 path (see above) |
| Flyway migration failed | `docker compose down -v` then `docker compose up -d` |
| Port 5432 in use | Stop other PostgreSQL or change `compose.yml` port |
| 401 on APIs | Login first; use Bearer token in Swagger Authorize |
