# IssueFlow API smoke test (Swagger-equivalent flow)
$ErrorActionPreference = "Stop"
$base = "http://localhost:8080"
$results = [System.Collections.Generic.List[object]]::new()

function Step($name, $script) {
    try {
        & $script
        $results.Add([pscustomobject]@{ Step = $name; Result = "PASS" })
        Write-Host "[PASS] $name" -ForegroundColor Green
    } catch {
        $results.Add([pscustomobject]@{ Step = $name; Result = "FAIL"; Detail = $_.Exception.Message })
        Write-Host "[FAIL] $name : $($_.Exception.Message)" -ForegroundColor Red
        throw
    }
}

Step "1. POST /auth/login" {
    $script:login = Invoke-RestMethod -Uri "$base/auth/login" -Method Post -ContentType "application/json" `
        -Body '{"username":"admin","password":"secret"}'
    if (-not $script:login.accessToken) { throw "No accessToken in response" }
    $script:headers = @{ Authorization = "Bearer $($script:login.accessToken)" }
}

Step "2. Authorize (Bearer header)" {
    if ($script:headers.Authorization -notmatch "^Bearer .+") { throw "Bearer token not set" }
}

Step "3. GET /auth/me" {
    $me = Invoke-RestMethod -Uri "$base/auth/me" -Headers $script:headers
    if ($me.username -ne "admin") { throw "Expected admin, got $($me.username)" }
    $script:adminId = $me.id
}

Step "4. GET /users" {
    $users = Invoke-RestMethod -Uri "$base/users" -Headers $script:headers
    if ($users.Count -lt 1) { throw "Expected seeded users" }
}

Step "5. POST /projects" {
    $body = @{ name = "Swagger Test Project"; description = "API smoke test"; ownerId = $script:adminId } | ConvertTo-Json
    $script:project = Invoke-RestMethod -Uri "$base/projects" -Method Post -Headers $script:headers -ContentType "application/json" -Body $body
    if (-not $script:project.id) { throw "No project id" }
    $script:projectId = $script:project.id
}

Step "6. GET /projects" {
    $projects = Invoke-RestMethod -Uri "$base/projects" -Headers $script:headers
    if (-not ($projects | Where-Object { $_.id -eq $script:projectId })) { throw "Created project not in list" }
}

Step "7. POST /tickets" {
    $body = @{
        title = "Swagger ticket 1"; description = "First ticket"
        status = "TODO"; priority = "LOW"; type = "BUG"; projectId = $script:projectId
    } | ConvertTo-Json
    $script:ticket = Invoke-RestMethod -Uri "$base/tickets" -Method Post -Headers $script:headers -ContentType "application/json" -Body $body
    $script:ticketId = $script:ticket.id
    if ($script:ticket.status -ne "TODO") { throw "Expected TODO, got $($script:ticket.status)" }
}

Step "8. GET /tickets/{id}" {
    $one = Invoke-RestMethod -Uri "$base/tickets/$($script:ticketId)" -Headers $script:headers
    if ($one.status -ne "TODO") { throw "Expected TODO" }
    $script:version = $one.version
}

Step "9. PATCH invalid TODO->DONE" {
    $body = @{ status = "DONE"; version = $script:version } | ConvertTo-Json
    try {
        Invoke-RestMethod -Uri "$base/tickets/$($script:ticketId)" -Method Patch -Headers $script:headers -ContentType "application/json" -Body $body
        throw "Expected 400 for TODO->DONE"
    } catch {
        if ($_.Exception.Response.StatusCode.value__ -ne 400) { throw "Expected 400, got $($_.Exception.Response.StatusCode)" }
    }
}

Step "10. PATCH valid TODO->IN_PROGRESS" {
    $one = Invoke-RestMethod -Uri "$base/tickets/$($script:ticketId)" -Headers $script:headers
    $body = @{ status = "IN_PROGRESS"; version = $one.version } | ConvertTo-Json
    Invoke-RestMethod -Uri "$base/tickets/$($script:ticketId)" -Method Patch -Headers $script:headers -ContentType "application/json" -Body $body | Out-Null
}

Step "11. POST comment with @admin" {
    $body = @{ authorId = $script:adminId; content = "Hello @admin from smoke test" } | ConvertTo-Json
    $script:comment = Invoke-RestMethod -Uri "$base/tickets/$($script:ticketId)/comments" -Method Post -Headers $script:headers -ContentType "application/json" -Body $body
    if (-not ($script:comment.mentionedUsers | Where-Object { $_.username -eq "admin" })) {
        throw "mentionedUsers should include admin"
    }
}

Step "12. GET /users/{adminId}/mentions" {
    $mentions = Invoke-RestMethod -Uri "$base/users/$($script:adminId)/mentions" -Headers $script:headers
    if (-not ($mentions.data | Where-Object { $_.id -eq $script:comment.id })) {
        throw "Comment not in admin mentions"
    }
}

Step "13. POST second ticket + dependency" {
    $body = @{
        title = "Blocker ticket"; description = "Blocks ticket 1"
        status = "TODO"; priority = "LOW"; type = "BUG"; projectId = $script:projectId
    } | ConvertTo-Json
    $script:ticket2 = Invoke-RestMethod -Uri "$base/tickets" -Method Post -Headers $script:headers -ContentType "application/json" -Body $body
    $depBody = @{ blockedBy = $script:ticket2.id } | ConvertTo-Json
    Invoke-RestMethod -Uri "$base/tickets/$($script:ticketId)/dependencies" -Method Post -Headers $script:headers -ContentType "application/json" -Body $depBody | Out-Null
}

Step "14. Dependency blocks DONE" {
    $one = Invoke-RestMethod -Uri "$base/tickets/$($script:ticketId)" -Headers $script:headers
    # Advance to IN_REVIEW (IN_PROGRESS -> IN_REVIEW)
    $body = @{ status = "IN_REVIEW"; version = $one.version } | ConvertTo-Json
    Invoke-RestMethod -Uri "$base/tickets/$($script:ticketId)" -Method Patch -Headers $script:headers -ContentType "application/json" -Body $body | Out-Null
    $one = Invoke-RestMethod -Uri "$base/tickets/$($script:ticketId)" -Headers $script:headers
    $body = @{ status = "DONE"; version = $one.version } | ConvertTo-Json
    try {
        Invoke-RestMethod -Uri "$base/tickets/$($script:ticketId)" -Method Patch -Headers $script:headers -ContentType "application/json" -Body $body
        throw "Expected 400 when blocker not DONE"
    } catch {
        if ($_.Exception.Response.StatusCode.value__ -ne 400) { throw "Expected 400, got $($_.Exception.Response.StatusCode)" }
    }
}

Step "15. DELETE /tickets/{id}" {
    Invoke-RestMethod -Uri "$base/tickets/$($script:ticketId)" -Method Delete -Headers $script:headers | Out-Null
}

Step "16. GET /tickets hidden deleted" {
    $list = Invoke-RestMethod -Uri "$base/tickets?projectId=$($script:projectId)" -Headers $script:headers
    if ($list | Where-Object { $_.id -eq $script:ticketId }) { throw "Deleted ticket still in list" }
}

Step "17. GET /tickets/deleted" {
    $deleted = Invoke-RestMethod -Uri "$base/tickets/deleted?projectId=$($script:projectId)" -Headers $script:headers
    if (-not ($deleted | Where-Object { $_.id -eq $script:ticketId })) { throw "Deleted ticket not in /deleted" }
}

Step "18. POST /tickets/{id}/restore" {
    Invoke-RestMethod -Uri "$base/tickets/$($script:ticketId)/restore" -Method Post -Headers $script:headers | Out-Null
}

Step "19. GET /audit-logs" {
    $logs = Invoke-RestMethod -Uri "$base/audit-logs" -Headers $script:headers
    if ($logs.Count -lt 1) { throw "Expected audit logs" }
    $actions = $logs.action | Select-Object -Unique
    Write-Host "  Audit actions: $($actions -join ', ')"
}

Write-Host "`n=== All API smoke steps passed ===" -ForegroundColor Cyan
$results | Format-Table -AutoSize
