$ErrorActionPreference = 'Continue'
$base = 'http://localhost:8082'
$script:pass = 0; $script:fail = 0; $script:failList = @()
$script:sess = $null

function Login {
    $body = '{"username":"admin","password":"admin123","bypassCaptcha":true}'
    $r = Invoke-WebRequest "$base/api/auth/login" -Method POST -Body $body -ContentType "application/json" -UseBasicParsing
    $raw = ($r.Headers['Set-Cookie'] -split ';')[0]
    $tok = $raw.Substring($raw.IndexOf('=') + 1)
    $script:token = $tok
    $script:cookieFile = Join-Path $env:TEMP 'sn-test-cookie.txt'
    "localhost`tFALSE`t/`tFALSE`t0`tsn_token`t$tok" | Set-Content -Path $script:cookieFile -Encoding ASCII -NoNewline
    $s = New-Object Microsoft.PowerShell.Commands.WebRequestSession
    $s.Cookies.Add((New-Object System.Net.Cookie("sn_token", $tok, "/", "localhost")))
    return $s
}

function Call($name, $method, $url, $bodyObj, $expectCode, $session) {
    if ($null -eq $expectCode) { $expectCode = 200 }
    $s = if ($session) { $session } else { $script:sess }
    try {
        $params = @{ Uri = "$base$url"; Method = $method; WebSession = $s; UseBasicParsing = $true; TimeoutSec = 30 }
        if ($null -ne $bodyObj) {
            $params.Body = ($bodyObj | ConvertTo-Json -Depth 10 -Compress)
            $params.ContentType = 'application/json'
        }
        $resp = Invoke-WebRequest @params
        $j = $null
        try { $j = $resp.Content | ConvertFrom-Json } catch { }
        $code = if ($j -and $null -ne $j.code) { $j.code } else { -1 }
        if ($code -eq $expectCode) { $script:pass++; "PASS  $name  [HTTP $($resp.StatusCode) code=$code]" }
        else { $script:fail++; $script:failList += "$name => code=$code msg=$($j.message)"; "FAIL  $name  [HTTP $($resp.StatusCode) code=$code msg=$($j.message)]" }
        return $j
    } catch {
        $sc = $_.Exception.Response.StatusCode.value__
        $msg = ''
        try { $sr = New-Object System.IO.StreamReader($_.Exception.Response.GetResponseStream()); $msg = $sr.ReadToEnd() } catch { }
        if ($sc -eq $expectCode) { $script:pass++; "PASS  $name  [HTTP $sc as expected]"; return $null }
        $script:fail++; $script:failList += "$name => HTTP $sc $msg"
        "FAIL  $name  [HTTP $sc $msg]"
        return $null
    }
}

function GetData($url) {
    $r = Invoke-WebRequest "$base$url" -WebSession $script:sess -UseBasicParsing
    return ($r.Content | ConvertFrom-Json).data
}

# 生成一张真实 PNG，用于上传接口回归
function NewTestPng($path) {
    Add-Type -AssemblyName System.Drawing
    $bmp = New-Object System.Drawing.Bitmap 120, 90
    $g = [System.Drawing.Graphics]::FromImage($bmp)
    $g.Clear([System.Drawing.Color]::SteelBlue)
    $g.Dispose()
    $bmp.Save($path, [System.Drawing.Imaging.ImageFormat]::Png)
    $bmp.Dispose()
}

function Upload($name, $url, $filePath, $expectCode) {
    $respFile = Join-Path $env:TEMP 'sn-upload-resp.json'
    & curl.exe -s -o $respFile -b $script:cookieFile -F "file=@$filePath" "$base$url"
    $j = $null
    try { $j = (Get-Content $respFile -Raw -Encoding UTF8) | ConvertFrom-Json } catch { }
    $code = if ($j -and $null -ne $j.code) { $j.code } else { -1 }
    if ($code -eq $expectCode) { $script:pass++; "PASS  $name  [code=$code]" }
    else { $script:fail++; $script:failList += "$name => code=$code msg=$($j.message)"; "FAIL  $name  [code=$code msg=$($j.message)]" }
    return $j
}

# 软删除记录仍占用唯一索引，回归前物理清理上一轮残留的测试数据
$mysql = 'C:\Program Files\MySQL\MySQL Server 5.7\bin\mysql.exe'
if (Test-Path $mysql) {
    $purge = "DELETE FROM sp_account_role WHERE account_id IN (SELECT id FROM sp_account WHERE username='testuser_a');" +
             "DELETE FROM sp_account WHERE username='testuser_a';" +
             "DELETE FROM sp_role_permission WHERE role_id IN (SELECT id FROM sp_role WHERE role_code='test_role_a');" +
             "DELETE FROM sp_role WHERE role_code='test_role_a';" +
             "DELETE FROM sp_dict_item WHERE type_id IN (SELECT id FROM sp_dict_type WHERE dict_code='test_dict_a');" +
             "DELETE FROM sp_dict_type WHERE dict_code='test_dict_a';" +
             "DELETE FROM sp_department WHERE dept_code='TEST_A';" +
             "DELETE FROM sp_position WHERE position_code='TP_A';" +
             "DELETE FROM sp_config WHERE config_key='test.key.a';" +
             "DELETE FROM sp_notice WHERE title LIKE 'TestNoticeA%';" +
             "DELETE FROM sp_repair_log WHERE order_id IN (SELECT id FROM sp_repair_order WHERE title LIKE 'TestRepairA%');" +
             "DELETE FROM sp_repair_order WHERE title LIKE 'TestRepairA%';"
    & $mysql -uroot -padmin shengnan_platform -e $purge 2>&1 | Out-Null
    "test data purged"
}

"=========== 1. AUTH ==========="
Call "health" GET "/api/auth/health" $null 200
Call "captcha" GET "/api/auth/captcha" $null 200
$script:sess = Login
"login OK, cookie session established"
$info = Call "auth/info" GET "/api/auth/info" $null 200
"permissions count: $($info.data.permissions.Count)"
Call "permission/routes" GET "/api/permission/routes" $null 200
$anon = New-Object Microsoft.PowerShell.Commands.WebRequestSession
Call "anonymous /api/account/list denied" GET "/api/account/list?pageNum=1&pageSize=10" $null 401 $anon
Call "wrong password rejected" POST "/api/auth/login" @{ username = "admin"; password = "wrongpwd"; bypassCaptcha = $true } 400 $anon

"=========== 2. DEPARTMENT ==========="
Call "dept create" POST "/api/department" @{ deptName = "TestDeptA"; deptCode = "TEST_A"; parentId = 0; sortNo = 99; isEnabled = $true } 200
$depts = GetData "/api/department/list"
$newDept = $depts | Where-Object { $_.deptCode -eq 'TEST_A' } | Select-Object -First 1
if ($newDept) {
    Call "dept get" GET "/api/department/$($newDept.id)" $null 200
    Call "dept update" PUT "/api/department" @{ id = $newDept.id; deptName = "TestDeptA2"; deptCode = "TEST_A"; parentId = 0; sortNo = 98; isEnabled = $true } 200
    Call "dept delete" DELETE "/api/department/$($newDept.id)" $null 200
} else { $script:fail++; $script:failList += "dept not found after create"; "FAIL  dept not found after create" }

"=========== 3. POSITION ==========="
Call "position create" POST "/api/position" @{ positionName = "TestPosA"; positionCode = "TP_A"; sortNo = 99; isEnabled = $true } 200
$pos = GetData "/api/position/list"
$newPos = $pos | Where-Object { $_.positionCode -eq 'TP_A' } | Select-Object -First 1
if ($newPos) {
    Call "position get" GET "/api/position/$($newPos.id)" $null 200
    Call "position update" PUT "/api/position" @{ id = $newPos.id; positionName = "TestPosA2"; positionCode = "TP_A"; sortNo = 98; isEnabled = $true } 200
    Call "position delete" DELETE "/api/position/$($newPos.id)" $null 200
} else { $script:fail++; $script:failList += "position not found after create"; "FAIL  position not found after create" }

"=========== 4. DICT ==========="
Call "dict create" POST "/api/dict" @{ dictCode = "test_dict_a"; dictName = "TestDictA"; isEnabled = $true } 200
$dt = GetData "/api/dict/list"
$newDt = $dt | Where-Object { $_.dictCode -eq 'test_dict_a' } | Select-Object -First 1
if ($newDt) {
    Call "dict get" GET "/api/dict/$($newDt.id)" $null 200
    Call "dict-item list" GET "/api/dict-item/list?pageNum=1&pageSize=10" $null 200
    Call "dict-item create" POST "/api/dict-item" @{ typeId = $newDt.id; itemLabel = "Opt1"; itemValue = "v1"; sortNo = 1; isEnabled = $true } 200
    $di = GetData "/api/dict-item/list"
    $newDi = $di | Where-Object { $_.itemValue -eq 'v1' } | Select-Object -First 1
    if ($newDi) {
        Call "dict-item get" GET "/api/dict-item/$($newDi.id)" $null 200
        Call "dict-item update" PUT "/api/dict-item" @{ id = $newDi.id; typeId = $newDt.id; itemLabel = "Opt1b"; itemValue = "v1"; sortNo = 2; isEnabled = $true } 200
        Call "dict-item delete" DELETE "/api/dict-item/$($newDi.id)" $null 200
    } else { $script:fail++; $script:failList += "dict-item not found"; "FAIL  dict-item not found" }
    Call "dict update" PUT "/api/dict" @{ id = $newDt.id; dictCode = "test_dict_a"; dictName = "TestDictA2"; isEnabled = $true } 200
    Call "dict delete" DELETE "/api/dict/$($newDt.id)" $null 200
} else { $script:fail++; $script:failList += "dict not found after create"; "FAIL  dict not found after create" }

"=========== 5. CONFIG ==========="
Call "config create" POST "/api/config" @{ configKey = "test.key.a"; configValue = "v1"; configName = "TestCfgA"; isBuiltin = $false } 200
$cfg = GetData "/api/config/list"
$newCfg = $cfg | Where-Object { $_.configKey -eq 'test.key.a' } | Select-Object -First 1
if ($newCfg) {
    Call "config get" GET "/api/config/$($newCfg.id)" $null 200
    Call "config update" PUT "/api/config" @{ id = $newCfg.id; configKey = "test.key.a"; configValue = "v2"; configName = "TestCfgA"; isBuiltin = $false } 200
    Call "config delete" DELETE "/api/config/$($newCfg.id)" $null 200
} else { $script:fail++; $script:failList += "config not found after create"; "FAIL  config not found after create" }

"=========== 6. NOTICE ==========="
Call "notice create" POST "/api/notice" @{ title = "TestNoticeA"; content = "line1`nline2"; noticeType = 1; isTop = $false; status = 0 } 200
$nt = GetData "/api/notice/list?pageNum=1&pageSize=200"
$newNt = $nt.records | Where-Object { $_.title -eq 'TestNoticeA' } | Select-Object -First 1
if ($newNt) {
    Call "notice get" GET "/api/notice/$($newNt.id)" $null 200
    Call "notice update" PUT "/api/notice" @{ id = $newNt.id; title = "TestNoticeA2"; content = "body2"; noticeType = 2; isTop = $true; status = 0 } 200
    Call "notice publish" PUT "/api/notice/status/$($newNt.id)" @{ status = 1 } 200
    Call "notice published list" GET "/api/notice/published" $null 200
    Call "notice unread-count" GET "/api/notice/unread-count" $null 200
    Call "notice read" POST "/api/notice/read/$($newNt.id)" $null 200
    Call "notice read-all" POST "/api/notice/read-all" $null 200
    Call "notice unpublish" PUT "/api/notice/status/$($newNt.id)" @{ status = 0 } 200
    Call "notice delete" DELETE "/api/notice/$($newNt.id)" $null 200
} else { $script:fail++; $script:failList += "notice not found after create"; "FAIL  notice not found after create" }

"=========== 7. ROLE ==========="
Call "role create" POST "/api/role" @{ role = @{ roleCode = "test_role_a"; roleName = "TestRoleA"; sortNo = 99; dataScope = "self"; isEnabled = $true }; permissionIds = @(1100, 1107) } 200
$roles = GetData "/api/role/list"
$newRole = $roles | Where-Object { $_.roleCode -eq 'test_role_a' } | Select-Object -First 1
if ($newRole) {
    Call "role permissions" GET "/api/role/permissions/$($newRole.id)" $null 200
    Call "role grant" PUT "/api/role/grant/$($newRole.id)" @{ permissionIds = @(1100, 1107) } 200
    Call "role update" PUT "/api/role" @{ role = @{ id = $newRole.id; roleCode = "test_role_a"; roleName = "TestRoleA2"; sortNo = 98; dataScope = "all"; isEnabled = $true } } 200
    Call "role delete" DELETE "/api/role/$($newRole.id)" $null 200
} else { $script:fail++; $script:failList += "role not found after create"; "FAIL  role not found after create" }

"=========== 8. ACCOUNT ==========="
Call "account create" POST "/api/account" @{ account = @{ username = "testuser_a"; password = "Test@12345"; displayName = "TestAccA"; isEnabled = $true; departmentId = 0 }; roleIds = @(2) } 200
$accs = GetData "/api/account/list?pageNum=1&pageSize=200"
$newAcc = $accs.records | Where-Object { $_.username -eq 'testuser_a' } | Select-Object -First 1
if ($newAcc) {
    Call "account get" GET "/api/account/$($newAcc.id)" $null 200
    Call "account options" GET "/api/account/options" $null 200
    Call "account update" PUT "/api/account" @{ account = @{ id = $newAcc.id; username = "testuser_a"; displayName = "TestAccA2"; isEnabled = $true }; roleIds = @(2) } 200
    Call "account reset pwd" PUT "/api/account/password/$($newAcc.id)" @{ password = "Test@54321" } 200
    Call "account delete" DELETE "/api/account/$($newAcc.id)" $null 200
} else { $script:fail++; $script:failList += "account not found after create"; "FAIL  account not found after create" }

"=========== 9. PERMISSION ==========="
Call "permission tree" GET "/api/permission/tree" $null 200

"=========== 10. LOGS ==========="
Call "login-log list" GET "/api/login-log/list?pageNum=1&pageSize=10" $null 200
Call "operation-log list" GET "/api/operation-log/list?pageNum=1&pageSize=10" $null 200

"=========== 11. JOB ==========="
Call "job list" GET "/api/job/list?pageNum=1&pageSize=10" $null 200
Call "job targets" GET "/api/job/targets" $null 200
Call "job log list" GET "/api/job/log/list?pageNum=1&pageSize=10" $null 200

"=========== 12. FILE ==========="
Call "file list" GET "/api/file/list?pageNum=1&pageSize=10" $null 200

"=========== 13. REPAIR ==========="
Call "repair categories" GET "/api/repair/categories" $null 200
Call "repair handlers" GET "/api/repair/handlers" $null 200
Call "repair stats" GET "/api/repair/stats" $null 200
Call "repair list" GET "/api/repair/list?pageNum=1&pageSize=10" $null 200
$pngPath = Join-Path $env:TEMP 'sn-test-repair.png'
NewTestPng $pngPath
$up = Upload "repair image upload" "/api/repair/image" $pngPath 200
$imgId = if ($up -and $up.data) { $up.data.id } else { $null }
if ($imgId) {
    $pv = Invoke-WebRequest "$base/api/repair/image/$imgId" -WebSession $script:sess -UseBasicParsing
    if ($pv.StatusCode -eq 200 -and $pv.Headers['Content-Type'] -like 'image/*') {
        $script:pass++; "PASS  repair image preview  [HTTP 200 $($pv.Headers['Content-Type']) $($pv.RawContentLength)B]"
    } else { $script:fail++; $script:failList += "repair image preview bad response"; "FAIL  repair image preview" }
} else { $script:fail++; $script:failList += "repair image upload returned no id"; "FAIL  repair image upload returned no id" }
Call "repair create with image" POST "/api/repair" @{ title = "TestRepairA"; category = "network"; priority = 2; location = "Test Room"; description = "test desc"; images = "$imgId" } 200
$rp = GetData "/api/repair/list?pageNum=1&pageSize=200"
$newRp = $rp.records | Where-Object { $_.title -eq 'TestRepairA' } | Select-Object -First 1
if ($newRp) {
    Call "repair get" GET "/api/repair/$($newRp.id)" $null 200
    $rpImg = GetData "/api/repair/$($newRp.id)"
    if ($rpImg.images -eq "$imgId") {
        $script:pass++; "PASS  repair images persisted  [images=$($rpImg.images)]"
    } else { $script:fail++; $script:failList += "repair images not persisted"; "FAIL  repair images persisted  [images=$($rpImg.images)]" }
    Call "repair update" PUT "/api/repair" @{ id = $newRp.id; title = "TestRepairA2"; category = "device"; priority = 1; location = "Test Room 2"; description = "desc2" } 200
    Call "repair assign" PUT "/api/repair/assign/$($newRp.id)" @{ handlerId = 1; expectTime = "2026-12-31 18:00:00" } 200
    Call "repair start" PUT "/api/repair/start/$($newRp.id)" $null 200
    Call "repair finish" PUT "/api/repair/finish/$($newRp.id)" @{ resultDesc = "fixed by test" } 200
    Call "repair rate" PUT "/api/repair/rate/$($newRp.id)" @{ rating = 5 } 200
    $rpDetail = GetData "/api/repair/$($newRp.id)"
    if ($rpDetail.status -eq 3 -and $rpDetail.logs.Count -ge 5) {
        $script:pass++; "PASS  repair flow status=$($rpDetail.status) logs=$($rpDetail.logs.Count)"
    } else { $script:fail++; $script:failList += "repair flow state mismatch"; "FAIL  repair flow status=$($rpDetail.status) logs=$($rpDetail.logs.Count)" }
    Call "repair close" PUT "/api/repair/close/$($newRp.id)" @{ remark = "test close" } 200
    Call "repair delete" DELETE "/api/repair/$($newRp.id)" $null 200
} else { $script:fail++; $script:failList += "repair not found after create"; "FAIL  repair not found after create" }
Remove-Item $pngPath -ErrorAction SilentlyContinue

"=========== 14. LOGOUT ==========="
Call "logout" POST "/api/auth/logout" $null 200
Call "after logout auth/info denied" GET "/api/auth/info" $null 401

"================ SUMMARY ================"
"PASS: $($script:pass)   FAIL: $($script:fail)"
if ($script:failList.Count -gt 0) { "Failures:"; $script:failList | ForEach-Object { " - $_" } }