$ErrorActionPreference = 'Stop'
Add-Type -AssemblyName System.Net.Http
Add-Type -ReferencedAssemblies 'System.Net.Http' -TypeDefinition @'
using System;
using System.Collections.Generic;
using System.Net;
using System.Net.Http;
using System.Text;
using System.Threading;
using System.Threading.Tasks;

public class HttpEvidence {
    public string Method, Url, RequestBody, RequestTimestamp, ResponseTimestamp, ResponseBody, Error;
    public int? StatusCode;
    public Dictionary<string, string> RequestHeaders = new Dictionary<string, string>();
    public Dictionary<string, string> ResponseHeaders = new Dictionary<string, string>();
}

public static class LiveHttp {
    private static readonly HttpClient client = new HttpClient(new HttpClientHandler {
        AllowAutoRedirect = false,
        UseProxy = false
    }) { Timeout = TimeSpan.FromSeconds(15) };

    public static async Task<HttpEvidence> Send(string method, string url, string body) {
        var result = new HttpEvidence { Method = method, Url = url, RequestBody = body };
        try {
            using (var request = new HttpRequestMessage(new HttpMethod(method), url)) {
                request.Headers.Host = new Uri(url).Authority;
                request.Headers.TryAddWithoutValidation("Accept", "application/json, application/problem+json");
                request.Headers.TryAddWithoutValidation("User-Agent", "course-waitlist-live-evidence/1.0");
                if (body != null && method != "GET" && method != "DELETE") {
                    request.Content = new StringContent(body, Encoding.UTF8, "application/json");
                    request.Content.Headers.ContentLength = Encoding.UTF8.GetByteCount(body);
                }
                foreach (var header in request.Headers)
                    result.RequestHeaders[header.Key] = String.Join(", ", header.Value);
                if (request.Content != null)
                    foreach (var header in request.Content.Headers)
                        result.RequestHeaders[header.Key] = String.Join(", ", header.Value);
                result.RequestTimestamp = DateTimeOffset.Now.ToString("o");
                using (var response = await client.SendAsync(request)) {
                    result.StatusCode = (int)response.StatusCode;
                    foreach (var header in response.Headers)
                        result.ResponseHeaders[header.Key] = String.Join(", ", header.Value);
                    foreach (var header in response.Content.Headers)
                        result.ResponseHeaders[header.Key] = String.Join(", ", header.Value);
                    result.ResponseBody = await response.Content.ReadAsStringAsync();
                    result.ResponseTimestamp = DateTimeOffset.Now.ToString("o");
                }
            }
        } catch (Exception ex) {
            result.ResponseTimestamp = DateTimeOffset.Now.ToString("o");
            result.Error = ex.ToString();
        }
        return result;
    }

    public static HttpEvidence[] Concurrent(string[] methods, string[] urls, string[] bodies) {
        int n = methods.Length;
        using (var barrier = new Barrier(n)) {
            int worker, io;
            ThreadPool.GetMinThreads(out worker, out io);
            ThreadPool.SetMinThreads(Math.Max(worker, n), io);
            var tasks = new Task<HttpEvidence>[n];
            for (int i = 0; i < n; i++) {
                int index = i;
                tasks[i] = Task.Run(async () => {
                    if (!barrier.SignalAndWait(TimeSpan.FromSeconds(15)))
                        throw new TimeoutException("Concurrent request start barrier timed out");
                    return await Send(methods[index], urls[index], bodies[index]);
                });
            }
            return Task.WhenAll(tasks).GetAwaiter().GetResult();
        }
    }
}
'@

$root = Split-Path -Parent $PSScriptRoot
$base = 'http://127.0.0.1:8080/api/v1/offerings/CS101-FALL'
$ids = @('learner-1001', 'learner-1002', 'learner-1003', 'learner-1042')
$script:allRequests = New-Object System.Collections.ArrayList
$script:allResponses = New-Object System.Collections.ArrayList
$script:cases = New-Object System.Collections.ArrayList
$script:sequence = 0
$script:case = $null
$script:phase = ''
$journal = Join-Path $PSScriptRoot 'complete-execution.log'

function Write-Json($path, $value) {
    ConvertTo-Json -InputObject $value -Depth 40 | Set-Content -LiteralPath $path -Encoding UTF8
}

function Note($message) {
    $line = "$(Get-Date -Format o) $message"
    Add-Content -LiteralPath $journal -Value $line -Encoding UTF8
    Write-Host $line
}

function Check($condition, $message) {
    if (-not $condition) {
        $script:case.failures.Add($message) | Out-Null
        Note "FAIL $($script:case.id): $message"
    }
}

function Record($exchange, $step, $expectedStatuses, $expectedFields) {
    $script:sequence++
    $response = $null
    if ($exchange.ResponseBody) {
        try { $response = $exchange.ResponseBody | ConvertFrom-Json } catch { $response = $null }
    }
    $problems = New-Object System.Collections.ArrayList
    if ($exchange.Error) { $problems.Add("Transport error: $($exchange.Error)") | Out-Null }
    if ($expectedStatuses -notcontains $exchange.StatusCode) {
        $problems.Add("Expected HTTP $($expectedStatuses -join ' or '), received $($exchange.StatusCode)") | Out-Null
    }
    foreach ($key in $expectedFields.Keys) {
        $property = if ($null -ne $response) { $response.PSObject.Properties[$key] } else { $null }
        if ($null -eq $property -or $property.Value -ne $expectedFields[$key]) {
            $actual = if ($null -eq $property) { '<missing>' } else { $property.Value }
            $problems.Add("Expected $key=$($expectedFields[$key]); received $actual") | Out-Null
        }
    }
    if ($exchange.StatusCode -ge 400 -and $exchange.StatusCode -lt 500) {
        if ($exchange.ResponseHeaders['Content-Type'] -notmatch '^application/problem\+json') {
            $problems.Add('Expected application/problem+json error response') | Out-Null
        }
        foreach ($key in @('type', 'title', 'status', 'detail', 'instance')) {
            if ($null -eq $response -or $null -eq $response.PSObject.Properties[$key]) {
                $problems.Add("Problem response missing $key") | Out-Null
            }
        }
        if ($null -ne $response -and $response.status -ne $exchange.StatusCode) {
            $problems.Add('Problem response status differs from HTTP status') | Out-Null
        }
    }
    $requestEntry = [ordered]@{
        sequence = $script:sequence; testCaseId = $script:case.id; scenario = $script:case.name
        phase = $script:phase; step = $step; method = $exchange.Method; url = $exchange.Url
        headers = $exchange.RequestHeaders; body = $exchange.RequestBody
        timestamp = $exchange.RequestTimestamp
        expected = [ordered]@{ httpStatus = @($expectedStatuses); responseFields = $expectedFields }
    }
    $responseEntry = [ordered]@{
        sequence = $script:sequence; testCaseId = $script:case.id; scenario = $script:case.name
        phase = $script:phase; step = $step; httpStatus = $exchange.StatusCode
        headers = $exchange.ResponseHeaders; body = $exchange.ResponseBody
        timestamp = $exchange.ResponseTimestamp; transportError = $exchange.Error
        actual = if ($problems.Count) { $problems -join '; ' } else { 'HTTP status and specified fields matched' }
        result = if ($problems.Count) { 'FAIL' } else { 'PASS' }
    }
    $script:allRequests.Add($requestEntry) | Out-Null
    $script:allResponses.Add($responseEntry) | Out-Null
    $script:case.requests.Add($requestEntry) | Out-Null
    $script:case.responses.Add($responseEntry) | Out-Null
    foreach ($problem in $problems) { $script:case.failures.Add("$step : $problem") | Out-Null }
    Note "$($script:case.id) $($script:phase) $step $($exchange.Method) $($exchange.Url) -> $($exchange.StatusCode) $($responseEntry.result)"
    return [pscustomobject]@{ exchange = $exchange; body = $response; result = $responseEntry.result }
}

function Send($step, $method, $url, $body, $statuses, $fields) {
    $exchange = [LiveHttp]::Send($method, $url, $body).GetAwaiter().GetResult()
    return Record $exchange $step $statuses $fields
}

function Post($id, $expectedStatus, $expectedFields) {
    $body = '{"learnerId":"' + $id + '"}'
    $result = Send "register $id" 'POST' "$base/registrations" $body @($expectedStatus) $expectedFields
    if ($script:phase -eq 'scenario') { $null = Snapshot "after register $id" }
    return $result
}

function Drop($id, $expectedStatus, $expectedFields) {
    $result = Send "drop $id" 'DELETE' "$base/enrollments/$id" $null @($expectedStatus) $expectedFields
    if ($script:phase -eq 'scenario') { $null = Snapshot "after drop $id" }
    return $result
}

function Snapshot($label) {
    $entries = @()
    foreach ($id in $ids) {
        $r = Send "snapshot $label $id" 'GET' "$base/registrations/$id" $null @(200,404) @{}
        if ($r.exchange.StatusCode -eq 200) {
            Check ($r.body.learnerId -eq $id) "$label learner ID mismatch for $id"
            $entries += [pscustomobject]@{ learnerId = $id; status = $r.body.status; waitlistPosition = $r.body.waitlistPosition }
        } else {
            $entries += [pscustomobject]@{ learnerId = $id; status = 'NOT_FOUND'; waitlistPosition = $null }
        }
    }
    $enrolled = @($entries | Where-Object { $_.status -eq 'ENROLLED' })
    $waiting = @($entries | Where-Object { $_.status -eq 'WAITLISTED' } | Sort-Object waitlistPosition)
    Check ($enrolled.Count -le 2) "$label exceeded configured capacity"
    for ($i = 0; $i -lt $waiting.Count; $i++) {
        Check ($waiting[$i].waitlistPosition -eq ($i + 1)) "$label queue position mismatch"
    }
    $snapshot = [ordered]@{
        label = $label; timestamp = (Get-Date -Format o); configuredCapacity = 2
        enrolledCount = $enrolled.Count; remainingCapacity = 2 - $enrolled.Count
        waitlistOrder = @($waiting | ForEach-Object { $_.learnerId }); learners = $entries
        limitation = 'HTTP-observable fixture registrations; not a direct internal-store or database query.'
    }
    $script:case.snapshots.Add($snapshot) | Out-Null
    return $snapshot
}

function Reset-State {
    $script:phase = 'cleanup'
    for ($i = 0; $i -lt 5; $i++) {
        $state = Snapshot "cleanup-$i"
        $enrolled = @($state.learners | Where-Object { $_.status -eq 'ENROLLED' })
        if ($enrolled.Count -eq 0) {
            Check ($state.waitlistOrder.Count -eq 0) 'Cleanup left waiting learners without an enrolled learner'
            return
        }
        $null = Drop $enrolled[0].learnerId 200 @{}
    }
    Check $false 'Cleanup could not clear active registrations'
}

function Concurrent($methods, $urls, $bodies, $statuses, $fields) {
    $results = [LiveHttp]::Concurrent([string[]]$methods, [string[]]$urls, [string[]]$bodies)
    $recorded = @()
    for ($i = 0; $i -lt $results.Length; $i++) {
        $recorded += Record $results[$i] "concurrent request $($i + 1)" $statuses $fields
    }
    return $recorded
}

function Case($id, $name, $folder, $operation) {
    $script:case = [pscustomobject]@{
        id=$id; name=$name; folder=$folder; status='PASS'; reason=''
        failures=(New-Object System.Collections.ArrayList)
        requests=(New-Object System.Collections.ArrayList)
        responses=(New-Object System.Collections.ArrayList)
        snapshots=(New-Object System.Collections.ArrayList)
    }
    Note "BEGIN $id $name"
    try {
        Reset-State
        $script:phase = 'before'
        $null = Snapshot 'before'
        $script:phase = 'scenario'
        & $operation
        $script:phase = 'after'
        $null = Snapshot 'after'
    } catch {
        $script:case.failures.Add($_.ToString()) | Out-Null
        Note "ERROR $id $($_.ToString())"
    }
    if ($script:case.failures.Count) { $script:case.status = 'FAIL' }
    $caseDir = Join-Path $root $folder
    Write-Json (Join-Path $caseDir 'requests.json') @($script:case.requests)
    Write-Json (Join-Path $caseDir 'responses.json') @($script:case.responses)
    Write-Json (Join-Path $caseDir 'execution.json') $script:case
    $script:cases.Add($script:case) | Out-Null
    Note "END $id $($script:case.status)"
}

Case 'TC01' 'Successful enrollment' '01-enrollment\TC01-successful-enrollment' {
    $null = Post $ids[0] 201 @{ status='ENROLLED'; created=$true }
}
Case 'TC02' 'Course full waitlist' '01-enrollment\TC02-course-full-waitlist' {
    $null = Post $ids[0] 201 @{ status='ENROLLED' }
    $null = Post $ids[1] 201 @{ status='ENROLLED' }
    $null = Post $ids[2] 201 @{ status='WAITLISTED'; waitlistPosition=1 }
}
Case 'TC03' 'Duplicate enrollment and waitlisting' '01-enrollment\TC03-duplicate-enrollment' {
    $null = Post $ids[0] 201 @{ status='ENROLLED'; created=$true }
    $null = Post $ids[0] 200 @{ status='ENROLLED'; created=$false }
    $null = Post $ids[1] 201 @{ status='ENROLLED' }
    $null = Post $ids[2] 201 @{ status='WAITLISTED'; waitlistPosition=1 }
    $null = Post $ids[2] 200 @{ status='WAITLISTED'; waitlistPosition=1; created=$false }
}
Case 'TC04' 'Ineligible learner' '01-enrollment\TC04-ineligible-learner' {
    $null = Post 'not-eligible' 422 @{ status=422; type='urn:course-waitlist:ineligible' }
    $null = Send 'lookup rejected learner' 'GET' "$base/registrations/not-eligible" $null @(404) @{ status=404 }
}
Case 'TC05' 'Unknown offering' '01-enrollment\TC05-invalid-course' {
    $url = 'http://127.0.0.1:8080/api/v1/offerings/unknown/registrations'
    $null = Send 'unknown offering' 'POST' $url '{"learnerId":"learner-1001"}' @(404) @{ status=404 }
}
Case 'TC06' 'Invalid learner ID' '01-enrollment\TC06-invalid-learner' {
    $null = Send 'blank learner' 'POST' "$base/registrations" '{"learnerId":" "}' @(400) @{ status=400 }
}
Case 'TC07' 'Successful drop' '02-course-dropping\TC07-successful-drop' {
    $null = Post $ids[0] 201 @{ status='ENROLLED' }
    $null = Drop $ids[0] 200 @{ droppedLearnerId=$ids[0]; remainingCapacity=2; promotedLearnerId=$null }
}
Case 'TC08' 'Drop when not enrolled' '02-course-dropping\TC08-invalid-drop' {
    $null = Drop $ids[0] 409 @{ status=409 }
}
Case 'TC09' 'Repeated drop' '02-course-dropping\TC09-repeated-drop' {
    $null = Post $ids[0] 201 @{ status='ENROLLED' }
    $null = Drop $ids[0] 200 @{ droppedLearnerId=$ids[0] }
    $null = Drop $ids[0] 409 @{ status=409 }
}
Case 'TC10' 'Drop with empty waitlist' '02-course-dropping\TC10-drop-with-empty-waitlist' {
    $null = Post $ids[0] 201 @{ status='ENROLLED' }
    $null = Post $ids[1] 201 @{ status='ENROLLED' }
    $null = Drop $ids[0] 200 @{ promotedLearnerId=$null; skippedIneligibleCount=0; remainingCapacity=1 }
}
Case 'TC11' 'FIFO promotion' '03-waitlist-promotion\TC11-fifo-promotion' {
    $null = Post $ids[0] 201 @{ status='ENROLLED' }
    $null = Post $ids[1] 201 @{ status='ENROLLED' }
    $null = Post $ids[2] 201 @{ status='WAITLISTED'; waitlistPosition=1 }
    $null = Post $ids[3] 201 @{ status='WAITLISTED'; waitlistPosition=2 }
    $null = Snapshot 'queue-before-drop'
    $null = Drop $ids[0] 200 @{ promotedLearnerId=$ids[2]; remainingCapacity=0 }
    $state = Snapshot 'first-promotion'
    Check ($state.waitlistOrder.Count -eq 1 -and $state.waitlistOrder[0] -eq $ids[3]) 'FIFO remaining queue mismatch'
}

$script:case = [pscustomobject]@{
    id='TC12'; name='First waiter becomes ineligible'; folder='03-waitlist-promotion\TC12-ineligible-first-learner'
    status='NOT EXECUTED'
    reason='The running application uses an immutable eligibility fixture and exposes no API to change eligibility after waitlisting. No mock or business-logic change was used.'
    failures=@(); requests=@(); responses=@(); snapshots=@()
}
Write-Json (Join-Path (Join-Path $root $script:case.folder) 'execution.json') $script:case
Write-Json (Join-Path (Join-Path $root $script:case.folder) 'requests.json') @()
Write-Json (Join-Path (Join-Path $root $script:case.folder) 'responses.json') @()
$script:cases.Add($script:case) | Out-Null
Note "END TC12 NOT EXECUTED: $($script:case.reason)"

Case 'TC13' 'Multiple waitlisted learners' '03-waitlist-promotion\TC13-multiple-waitlisted-learners' {
    $null = Post $ids[0] 201 @{ status='ENROLLED' }
    $null = Post $ids[1] 201 @{ status='ENROLLED' }
    $null = Post $ids[2] 201 @{ status='WAITLISTED'; waitlistPosition=1 }
    $null = Post $ids[3] 201 @{ status='WAITLISTED'; waitlistPosition=2 }
    $state = Snapshot 'multiple-waiters'
    Check (($state.waitlistOrder -join ',') -eq "$($ids[2]),$($ids[3])") 'Multiple-waiter order mismatch'
}
Case 'TC14' 'Multiple sequential promotions' '03-waitlist-promotion\TC14-promotion-after-drop' {
    $null = Post $ids[0] 201 @{ status='ENROLLED' }
    $null = Post $ids[1] 201 @{ status='ENROLLED' }
    $null = Post $ids[2] 201 @{ status='WAITLISTED'; waitlistPosition=1 }
    $null = Post $ids[3] 201 @{ status='WAITLISTED'; waitlistPosition=2 }
    $null = Drop $ids[0] 200 @{ promotedLearnerId=$ids[2] }
    $null = Snapshot 'after-first-promotion'
    $null = Drop $ids[1] 200 @{ promotedLearnerId=$ids[3] }
}
Case 'TC15' 'Simultaneous enrollment for final seat' '04-concurrency\TC15-simultaneous-enrollment' {
    $null = Post $ids[0] 201 @{ status='ENROLLED' }
    $methods=@('POST','POST','POST'); $urls=@("$base/registrations","$base/registrations","$base/registrations")
    $bodies=@('{"learnerId":"learner-1002"}','{"learnerId":"learner-1003"}','{"learnerId":"learner-1042"}')
    $results = Concurrent $methods $urls $bodies @(201) @{ created=$true }
    Check (@($results | Where-Object { $_.body.status -eq 'ENROLLED' }).Count -eq 1) 'Expected exactly one last-seat enrollment'
    Check (@($results | Where-Object { $_.body.status -eq 'WAITLISTED' }).Count -eq 2) 'Expected exactly two waitlisted learners'
}
Case 'TC16' 'Duplicate concurrent enrollment' '04-concurrency\TC16-duplicate-concurrent-enrollment' {
    $null = Post $ids[0] 201 @{ status='ENROLLED' }
    $methods=@(); $urls=@(); $bodies=@()
    for ($i=0; $i -lt 8; $i++) { $methods += 'POST'; $urls += "$base/registrations"; $bodies += '{"learnerId":"learner-1002"}' }
    $results = Concurrent $methods $urls $bodies @(200,201) @{ status='ENROLLED' }
    Check (@($results | Where-Object { $_.exchange.StatusCode -eq 201 -and $_.body.created -eq $true }).Count -eq 1) 'Expected one created registration'
    Check (@($results | Where-Object { $_.exchange.StatusCode -eq 200 -and $_.body.created -eq $false }).Count -eq 7) 'Expected seven idempotent responses'
}
Case 'TC17' 'Simultaneous drops' '04-concurrency\TC17-simultaneous-drops' {
    $null = Post $ids[0] 201 @{ status='ENROLLED' }
    $null = Post $ids[1] 201 @{ status='ENROLLED' }
    $results = Concurrent @('DELETE','DELETE') @("$base/enrollments/$($ids[0])","$base/enrollments/$($ids[1])") @($null,$null) @(200) @{ promotedLearnerId=$null }
    Check (@($results | Where-Object { $_.body.droppedLearnerId -eq $ids[0] }).Count -eq 1) 'First drop missing'
    Check (@($results | Where-Object { $_.body.droppedLearnerId -eq $ids[1] }).Count -eq 1) 'Second drop missing'
}
Case 'TC18' 'Concurrent promotion opportunities' '04-concurrency\TC18-concurrent-promotion' {
    $null = Post $ids[0] 201 @{ status='ENROLLED' }
    $null = Post $ids[1] 201 @{ status='ENROLLED' }
    $null = Post $ids[2] 201 @{ status='WAITLISTED'; waitlistPosition=1 }
    $null = Post $ids[3] 201 @{ status='WAITLISTED'; waitlistPosition=2 }
    $null = Snapshot 'queue-before-concurrent-drops'
    $results = Concurrent @('DELETE','DELETE') @("$base/enrollments/$($ids[0])","$base/enrollments/$($ids[1])") @($null,$null) @(200) @{ remainingCapacity=0 }
    Check (@($results | Where-Object { $_.body.promotedLearnerId -eq $ids[2] }).Count -eq 1) 'First waiter not promoted exactly once'
    Check (@($results | Where-Object { $_.body.promotedLearnerId -eq $ids[3] }).Count -eq 1) 'Second waiter not promoted exactly once'
}
Case 'TC19' 'Missing mandatory fields' '05-validation-and-errors\TC19-missing-fields' {
    $null = Send 'missing learner ID' 'POST' "$base/registrations" '{}' @(400) @{ status=400 }
}
Case 'TC20' 'Malformed request' '05-validation-and-errors\TC20-invalid-request' {
    $null = Send 'malformed JSON' 'POST' "$base/registrations" '{' @(400) @{ status=400 }
    $null = Send 'numeric learner ID' 'POST' "$base/registrations" '{"learnerId":123}' @(400) @{ status=400 }
}
Case 'TC21' 'Invalid identifiers' '05-validation-and-errors\TC21-invalid-identifiers' {
    $null = Send 'surrounding whitespace' 'POST' "$base/registrations" '{"learnerId":" learner-1001"}' @(400) @{ status=400 }
    $long = '{"learnerId":"' + ('x' * 101) + '"}'
    $null = Send 'identifier longer than 100' 'POST' "$base/registrations" $long @(400) @{ status=400 }
}
Case 'TC22' 'Unsupported method' '05-validation-and-errors\TC22-unsupported-method' {
    $r = Send 'delete registration lookup endpoint' 'DELETE' "$base/registrations/$($ids[0])" $null @(405) @{ status=405 }
    Check ($r.exchange.ResponseHeaders['Allow'] -match 'GET') '405 response did not advertise GET'
}

Write-Json (Join-Path $PSScriptRoot 'all-api-requests.json') @($script:allRequests)
Write-Json (Join-Path $PSScriptRoot 'all-api-responses.json') @($script:allResponses)
Write-Json (Join-Path $PSScriptRoot 'scenario-summary.json') @($script:cases | ForEach-Object {
    [ordered]@{ id=$_.id; name=$_.name; status=$_.status; reason=$_.reason; failures=@($_.failures); requestCount=$_.requests.Count; folder=$_.folder }
})
$pass = @($script:cases | Where-Object { $_.status -eq 'PASS' }).Count
$fail = @($script:cases | Where-Object { $_.status -eq 'FAIL' }).Count
$notExecuted = @($script:cases | Where-Object { $_.status -eq 'NOT EXECUTED' }).Count
Note "TOTAL scenarios=$($script:cases.Count) pass=$pass fail=$fail not-executed=$notExecuted requests=$($script:allRequests.Count)"
if ($fail -gt 0) { exit 1 }
