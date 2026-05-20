# Final reviewer QA audit - IssueFlow
$ErrorActionPreference = "Continue"
$base = "http://localhost:8080"
$issues = [System.Collections.Generic.List[object]]::new()
$passed = 0
$failed = 0

function Pass($msg) { $script:passed++; Write-Host "[PASS] $msg" -ForegroundColor Green }
function Fail($msg, $severity = "medium", $detail = "") {
    $script:failed++
    Write-Host "[FAIL][$severity] $msg $(if($detail){"- $detail"})" -ForegroundColor Red
    $script:issues.Add([pscustomobject]@{ Issue = $msg; Severity = $severity; Detail = $detail })
}
function ExpectStatus($name, $method, $uri, $expected, $headers = $null, $body = $null, $contentType = "application/json") {
    try {
        $params = @{ Uri = "$base$uri"; Method = $method; UseBasicParsing = $true }
        if ($headers) { $params.Headers = $headers }
        if ($body -ne $null) {
            $params.Body = $body
            $params.ContentType = $contentType
        }
        $r = Invoke-WebRequest @params
        $code = $r.StatusCode
    } catch {
        $code = $_.Exception.Response.StatusCode.value__
        if (-not $code) { Fail $name "critical" "No response: $($_.Exception.Message)"; return $null }
    }
    if ($code -eq $expected) { Pass "$name ($code)"; return $code }
    else { Fail $name "medium" "Expected $expected got $code"; return $code }
}

Write-Host "=== IssueFlow Final QA Audit ===" -ForegroundColor Cyan

# OpenAPI
try {
    $docs = (Invoke-WebRequest "$base/v3/api-docs" -UseBasicParsing).Content
    if ($docs -match 'bearerAuth' -and $docs -match 'components\.schemas|"schemas":\{' -and $docs -notmatch '#/schemas/') {
        Pass "OpenAPI: bearerAuth + schemas present, no broken refs"
    } else { Fail "OpenAPI document invalid" "critical" }
} catch { Fail "OpenAPI unreachable" "critical" $_.Exception.Message }

# 401 without auth
ExpectStatus "401 GET /users no auth" GET "/users" 401 | Out-Null

# Login
try {
    $adminLogin = Invoke-RestMethod "$base/auth/login" -Method Post -ContentType "application/json" `
        -Body '{"username":"admin","password":"secret"}'
    $devLogin = Invoke-RestMethod "$base/auth/login" -Method Post -ContentType "application/json" `
        -Body '{"username":"jdoe","password":"secret"}'
    $adminH = @{ Authorization = "Bearer $($adminLogin.accessToken)" }
    $devH = @{ Authorization = "Bearer $($devLogin.accessToken)" }
    Pass "Login admin + jdoe"
} catch { Fail "Login seeded users" "critical" $_.Exception.Message; exit 1 }

# Logout revoke
$revokeToken = $adminLogin.accessToken
Invoke-RestMethod "$base/auth/logout" -Method Post -Headers @{ Authorization = "Bearer $revokeToken" } | Out-Null
try {
    Invoke-RestMethod "$base/auth/me" -Headers @{ Authorization = "Bearer $revokeToken" }
    Fail "Revoked token should fail" "medium"
} catch {
    if ($_.Exception.Response.StatusCode.value__ -in 401,403) { Pass "Revoked token rejected" }
    else { Fail "Revoked token unexpected status" "medium" $_.Exception.Response.StatusCode }
}
# Re-login admin for rest
$adminLogin = Invoke-RestMethod "$base/auth/login" -Method Post -ContentType "application/json" `
    -Body '{"username":"admin","password":"secret"}'
$adminH = @{ Authorization = "Bearer $($adminLogin.accessToken)" }

# 403 developer on admin endpoint
ExpectStatus "403 dev GET /tickets/deleted" GET "/tickets/deleted?projectId=1" 403 $devH | Out-Null

# Malformed JSON
ExpectStatus "400 malformed JSON" POST "/tickets" 400 $adminH "{bad json" | Out-Null

# Invalid enum
$badTicket = '{"title":"x","status":"INVALID","priority":"LOW","type":"BUG","projectId":1}'
ExpectStatus "400 invalid enum status" POST "/tickets" 400 $adminH $badTicket | Out-Null

# Missing fields
ExpectStatus "400 missing required fields" POST "/tickets" 400 $adminH '{}' | Out-Null

# Invalid ID
ExpectStatus "404 invalid ticket id" GET "/tickets/999999" 404 $adminH | Out-Null

$me = Invoke-RestMethod "$base/auth/me" -Headers $adminH
$projectBody = @{ name = "QA Audit $(Get-Date -Format 'HHmmss')"; description = "audit"; ownerId = $me.id } | ConvertTo-Json
$project = Invoke-RestMethod "$base/projects" -Method Post -Headers $adminH -ContentType "application/json" -Body $projectBody
$pid = $project.id

# Dependency edge cases
$t1 = Invoke-RestMethod "$base/tickets" -Method Post -Headers $adminH -ContentType "application/json" -Body (@{
    title="T1"; status="TODO"; priority="LOW"; type="BUG"; projectId=$pid } | ConvertTo-Json)
$t2 = Invoke-RestMethod "$base/tickets" -Method Post -Headers $adminH -ContentType "application/json" -Body (@{
    title="T2"; status="TODO"; priority="LOW"; type="BUG"; projectId=$pid } | ConvertTo-Json)

function TryDep($name, $ticketId, $blockedBy, $expectCode) {
    try {
        Invoke-RestMethod "$base/tickets/$ticketId/dependencies" -Method Post -Headers $adminH -ContentType "application/json" `
            -Body (@{ blockedBy = $blockedBy } | ConvertTo-Json) | Out-Null
        if ($expectCode -ge 200 -and $expectCode -lt 300) { Pass $name } else { Fail $name "medium" "Unexpected success" }
    } catch {
        $c = $_.Exception.Response.StatusCode.value__
        if ($c -eq $expectCode) { Pass $name } else { Fail $name "medium" "Expected $expectCode got $c" }
    }
}
TryDep "Block self-dependency" $t1.id $t1.id 400
TryDep "Block duplicate dependency" $t1.id $t2.id 200
TryDep "Block duplicate again" $t1.id $t2.id 400

# Cross-project: create other project ticket
$p2 = Invoke-RestMethod "$base/projects" -Method Post -Headers $adminH -ContentType "application/json" -Body (@{
    name="Other"; description="x"; ownerId=$me.id } | ConvertTo-Json)
$tOther = Invoke-RestMethod "$base/tickets" -Method Post -Headers $adminH -ContentType "application/json" -Body (@{
    title="Other"; status="TODO"; priority="LOW"; type="BUG"; projectId=$p2.id } | ConvertTo-Json)
TryDep "Block cross-project dependency" $t1.id $tOther.id 400

# Cycle A->B, B->A
$tA = Invoke-RestMethod "$base/tickets" -Method Post -Headers $adminH -ContentType "application/json" -Body (@{
    title="A"; status="TODO"; priority="LOW"; type="BUG"; projectId=$pid } | ConvertTo-Json)
$tB = Invoke-RestMethod "$base/tickets" -Method Post -Headers $adminH -ContentType "application/json" -Body (@{
    title="B"; status="TODO"; priority="LOW"; type="BUG"; projectId=$pid } | ConvertTo-Json)
Invoke-RestMethod "$base/tickets/$($tA.id)/dependencies" -Method Post -Headers $adminH -ContentType "application/json" `
    -Body (@{ blockedBy = $tB.id } | ConvertTo-Json) | Out-Null
TryDep "Block cyclic dependency" $tB.id $tA.id 400

# Mentions: case insensitive + invalid user safe
$c1 = Invoke-RestMethod "$base/tickets/$($t1.id)/comments" -Method Post -Headers $adminH -ContentType "application/json" `
    -Body (@{ authorId=$me.id; content="Hi @ADMIN and @notauser" } | ConvertTo-Json)
if ($c1.mentionedUsers | Where-Object { $_.username -eq "admin" }) { Pass "Case-insensitive @ADMIN mention" }
else { Fail "Case-insensitive mention" "medium" }
if (-not ($c1.mentionedUsers | Where-Object { $_.username -eq "notauser" })) { Pass "Invalid @username ignored safely" }
else { Fail "Invalid mention created" "medium" }

# Optimistic locking 409
$one = Invoke-RestMethod "$base/tickets/$($t1.id)" -Headers $adminH
try {
    Invoke-RestMethod "$base/tickets/$($t1.id)" -Method Patch -Headers $adminH -ContentType "application/json" `
        -Body (@{ title="Conflict"; version=99999 } | ConvertTo-Json) | Out-Null
    Fail "Optimistic lock should 409" "medium"
} catch {
    if ($_.Exception.Response.StatusCode.value__ -eq 409) { Pass "Optimistic locking 409" }
    else { Fail "Optimistic lock status" "medium" $_.Exception.Response.StatusCode }
}

# CSV export/import
$export = Invoke-WebRequest "$base/tickets/export?projectId=$pid" -Headers $adminH -UseBasicParsing
if ($export.Content -match "title" -and $export.StatusCode -eq 200) { Pass "CSV export" }
else { Fail "CSV export" "medium" }

$csv = "title,description,status,priority,type,assigneeId`nImported,Desc,TODO,LOW,BUG,"
$boundary = [guid]::NewGuid().ToString()
$bodyLines = @(
    "--$boundary",
    "Content-Disposition: form-data; name=`"file`"; filename=`"import.csv`"",
    "Content-Type: text/csv",
    "",
    $csv,
    "--$boundary",
    "Content-Disposition: form-data; name=`"projectId`"",
    "",
    "$pid",
    "--$boundary--"
) -join "`r`n"
try {
    $import = Invoke-RestMethod "$base/tickets/import?projectId=$pid" -Method Post -Headers $adminH `
        -ContentType "multipart/form-data; boundary=$boundary" -Body $bodyLines
    if ($import.created -ge 1) { Pass "CSV import valid row" } else { Fail "CSV import" "medium" "created=$($import.created)" }
} catch { Fail "CSV import" "medium" $_.Exception.Message }

# Bad CSV graceful
$badCsv = "not,a,valid,csv`n1,2"
$boundary2 = [guid]::NewGuid().ToString()
$body2 = @(
    "--$boundary2","Content-Disposition: form-data; name=`"file`"; filename=`"bad.csv`"","Content-Type: text/csv","","$badCsv",
    "--$boundary2","Content-Disposition: form-data; name=`"projectId`"","","$pid","--$boundary2--"
) -join "`r`n"
try {
    $badImport = Invoke-RestMethod "$base/tickets/import?projectId=$pid" -Method Post -Headers $adminH `
        -ContentType "multipart/form-data; boundary=$boundary2" -Body $body2
    Pass "Malformed CSV handled without crash (failed=$($badImport.failed))"
} catch {
    if ($_.Exception.Response.StatusCode.value__ -in 400,200) { Pass "Malformed CSV no 500" }
    else { Fail "Malformed CSV caused error" "medium" $_.Exception.Response.StatusCode }
}

# Attachment MIME (skip if no file - use byte upload simulation via curl alternative)
# Use .NET HttpClient for multipart
Add-Type -AssemblyName System.Net.Http
$client = [System.Net.Http.HttpClient]::new()
$client.DefaultRequestHeaders.Authorization = [System.Net.Http.Headers.AuthenticationHeaderValue]::new("Bearer", $adminLogin.accessToken)
$mp = [System.Net.Http.MultipartFormDataContent]::new()
$bytes = [byte[]](0x89,0x50,0x4E,0x47) # fake header
$sc = [System.Net.Http.ByteArrayContent]::new($bytes)
$sc.Headers.ContentType = [System.Net.Http.Headers.MediaTypeHeaderValue]::new("image/png")
$mp.Add($sc, "file", "test.png")
$r = $client.PostAsync("$base/tickets/$($t1.id)/attachments", $mp).Result
if ([int]$r.StatusCode -eq 200) { Pass "Attachment PNG upload" } else { Fail "Attachment PNG" "medium" $r.StatusCode }

$mp2 = [System.Net.Http.MultipartFormDataContent]::new()
$sc2 = [System.Net.Http.ByteArrayContent]::new([byte[]](1,2,3))
$sc2.Headers.ContentType = [System.Net.Http.Headers.MediaTypeHeaderValue]::new("application/exe")
$mp2.Add($sc2, "file", "bad.exe")
$r2 = $client.PostAsync("$base/tickets/$($t1.id)/attachments", $mp2).Result
if ([int]$r2.StatusCode -eq 400) { Pass "Attachment invalid MIME rejected" }
else { Fail "Attachment invalid MIME" "medium" $r2.StatusCode }

# Audit filter + pagination
$logs = Invoke-RestMethod "$base/audit-logs?entityType=TICKET&page=1&pageSize=5" -Headers $adminH
if ($logs.Count -le 5) { Pass "Audit pagination pageSize" } else { Fail "Audit pagination" "cosmetic" }
$sys = Invoke-RestMethod "$base/audit-logs?action=AUTO_ASSIGN" -Headers $adminH -ErrorAction SilentlyContinue
# AUTO_ASSIGN may exist from auto-assign tickets

# Audit actor filter mismatch (document)
$byUser = Invoke-RestMethod "$base/audit-logs?actor=admin&pageSize=3" -Headers $adminH
$byLabel = Invoke-RestMethod "$base/audit-logs?actor=USER&pageSize=3" -Headers $adminH -ErrorAction SilentlyContinue
if ($byLabel.Count -eq 0) {
    $issues.Add([pscustomobject]@{
        Issue = "Audit query param actor=USER/SYSTEM does not match README; filter uses stored username"
        Severity = "medium"
        Detail = "Use actor=admin not actor=USER"
    })
    Write-Host "[NOTE][medium] Audit actor filter uses username not USER/SYSTEM label" -ForegroundColor Yellow
}

Write-Host "`n=== Summary: $passed passed, $failed failed checks ===" -ForegroundColor Cyan
$issues | Format-Table -AutoSize
