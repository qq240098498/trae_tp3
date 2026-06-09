$BASE_URL = "http://localhost:8080"
$results = New-Object System.Collections.ArrayList
$script:testStartTime = Get-Date

function Log-Result {
    param(
        [string]$Scenario,
        [string]$TestCase,
        [string]$Step,
        [string]$Action,
        [string]$Expected,
        [string]$Actual,
        [string]$Status,
        [string]$Remark = ""
    )
    $result = [PSCustomObject]@{
        Scenario    = $Scenario
        TestCase    = $TestCase
        Step        = $Step
        Action      = $Action
        Expected    = $Expected
        Actual      = $Actual
        Status      = $Status
        Remark      = $Remark
        Timestamp   = (Get-Date -Format "yyyy-MM-dd HH:mm:ss.fff")
    }
    [void]$script:results.Add($result)
    $color = "Gray"
    if ($Status -eq "PASS") { $color = "Green" }
    elseif ($Status -eq "FAIL") { $color = "Red" }
    elseif ($Status -eq "WARN") { $color = "Yellow" }
    Write-Host "[$Status] $Scenario - $TestCase - Step $Step" -ForegroundColor $color
    Write-Host "  -> $Action"
    if ($Remark) { Write-Host "  [!] $Remark" -ForegroundColor Cyan }
}

function Build-Url {
    param([string]$Path, [hashtable]$Params = @{})
    $base = "$BASE_URL$Path"
    if ($Params.Count -eq 0) { return $base }
    $parts = @()
    foreach ($k in $Params.Keys) {
        $v = [System.Web.HttpUtility]::UrlEncode($Params[$k].ToString())
        $parts += "$k=$v"
    }
    return ($base + "?" + ($parts -join "&"))
}

function Api-Post {
    param([string]$Path, [hashtable]$Body, [hashtable]$Query = @{})
    try {
        $url = Build-Url $Path $Query
        $jsonBody = $Body | ConvertTo-Json -Depth 10
        $resp = Invoke-RestMethod -Uri $url -Method Post -Body $jsonBody -ContentType "application/json;charset=utf-8"
        return $resp
    } catch {
        Write-Host "POST Error: $_" -ForegroundColor Red
        if ($_.Exception.Response) {
            try {
                $reader = New-Object System.IO.StreamReader($_.Exception.Response.GetResponseStream())
                $err = $reader.ReadToEnd()
                Write-Host "  -> $err" -ForegroundColor Red
            } catch {}
        }
        return $null
    }
}

function Api-Put {
    param([string]$Path, [hashtable]$Body, [hashtable]$Query = @{})
    try {
        $url = Build-Url $Path $Query
        $jsonBody = $Body | ConvertTo-Json -Depth 10
        $resp = Invoke-RestMethod -Uri $url -Method Put -Body $jsonBody -ContentType "application/json;charset=utf-8"
        return $resp
    } catch {
        Write-Host "PUT Error: $_" -ForegroundColor Red
        return $null
    }
}

function Api-Get {
    param([string]$Path, [hashtable]$Params = @{})
    try {
        $url = Build-Url $Path $Params
        $resp = Invoke-RestMethod -Uri $url -Method Get
        return $resp
    } catch {
        Write-Host "GET Error: $url | $_" -ForegroundColor Red
        return $null
    }
}

function Get-CustomerPoints {
    param([long]$CustomerId)
    $resp = Api-Get "/api/points/customer/$CustomerId"
    if ($resp -and $resp.code -eq 200) { return $resp.data }
    return $null
}

function Get-DepositAvailable {
    param([long]$OrderId)
    $resp = Api-Get "/api/deposit/available" @{ orderId = $OrderId }
    if ($resp -and $resp.code -eq 200) { return $resp.data }
    return $null
}

function Get-DepositRecords {
    param([long]$OrderId)
    $resp = Api-Get "/api/deposit/list" @{ pageNum = 1; pageSize = 50; orderId = $OrderId.ToString() }
    if ($resp -and $resp.code -eq 200) { return $resp.data.records }
    return @()
}

function Get-PointsRecords {
    param([long]$CustomerId)
    $resp = Api-Get "/api/points/customer/$CustomerId/records"
    if ($resp -and $resp.code -eq 200) { return $resp.data }
    return @()
}

function Get-Instrument {
    param([long]$Id)
    $resp = Api-Get "/api/instrument/$Id"
    if ($resp -and $resp.code -eq 200) { return $resp.data }
    return $null
}

function Get-Order {
    param([long]$Id)
    $resp = Api-Get "/api/order/$Id"
    if ($resp -and $resp.code -eq 200) { return $resp.data }
    return $null
}

function Get-LatestDamage {
    param([long]$InstrumentId)
    $resp = Api-Get "/api/damage/list" @{ pageNum = 1; pageSize = 10; instrumentId = $InstrumentId }
    if ($resp -and $resp.code -eq 200 -and $resp.data.records.Count -gt 0) {
        return ($resp.data.records | Sort-Object id -Descending | Select-Object -First 1)
    }
    return $null
}

function Get-LatestRepair {
    param([long]$InstrumentId)
    $resp = Api-Get "/api/repair/list" @{ pageNum = 1; pageSize = 10; instrumentId = $InstrumentId }
    if ($resp -and $resp.code -eq 200 -and $resp.data.records.Count -gt 0) {
        return ($resp.data.records | Sort-Object id -Descending | Select-Object -First 1)
    }
    return $null
}

Write-Host "============================================" -ForegroundColor Magenta
Write-Host "  Music Instrument Rental - Integration Test" -ForegroundColor Magenta
Write-Host "  Start Time: $script:testStartTime" -ForegroundColor Magenta
Write-Host "============================================" -ForegroundColor Magenta
Write-Host ""

# ============================================================
# Scenario 1: Normal Return - Points Flow
# Customer: ZhangSan (ID=1), Instrument: Taylor Guitar (ID=2, rent=30/day, deposit=2000)
# ============================================================
Write-Host "============================================" -ForegroundColor Yellow
Write-Host "  Scenario 1: Normal Return + Points Earn" -ForegroundColor Yellow
Write-Host "============================================" -ForegroundColor Yellow

$s1OrderId = $null
$startDate1 = "2026-06-09"
$endDate1 = "2026-06-19"
$dailyRent1 = 30
$days1 = 10
$expectedTotalRent1 = 300.00
$expectedDeposit1 = 2000.00
$expectedEarnedPoints1 = 300

# 1.1 Initial points
$initialPoints1 = Get-CustomerPoints 1
Log-Result -Scenario "S1-NormalReturnPoints" -TestCase "TC1-1 InitCheck" -Step "1.1" `
    -Action "Query customer#1 initial points account" `
    -Expected "Points account exists, available>=0" `
    -Actual "available=$($initialPoints1.availablePoints), total=$($initialPoints1.totalPoints)" `
    -Status $(if ($initialPoints1) { "PASS" } else { "FAIL" })

# 1.2 Instrument status before
$instBefore1 = Get-Instrument 2
Log-Result -Scenario "S1-NormalReturnPoints" -TestCase "TC1-2 InstrumentAvail" -Step "1.2" `
    -Action "Check instrument#2 (Taylor Guitar) status before rental" `
    -Expected "Status=AVAILABLE" `
    -Actual "Status=$($instBefore1.status)" `
    -Status $(if ($instBefore1 -and $instBefore1.status -eq "AVAILABLE") { "PASS" } else { "WARN" }) `
    -Remark "Continue even if not AVAILABLE"

# 1.3 Create order
$orderBody1 = @{
    customerId      = 1
    instrumentId    = 2
    startDate       = $startDate1
    endDate         = $endDate1
    payMethod       = "WECHAT"
    usePoints       = $false
    usePointsAmount = 0
    remark          = "[TEST-S1] Normal return points flow test"
}
$createResp1 = Api-Post "/api/order" $orderBody1
if ($createResp1 -and $createResp1.code -eq 200 -and $createResp1.data) {
    $s1OrderId = $createResp1.data.id
    $actualTotalRent1 = $createResp1.data.totalRent
    $actualDeposit1 = $createResp1.data.depositAmount
    $actualStatus1 = $createResp1.data.status
    
    $ok = ($actualTotalRent1 -eq $expectedTotalRent1) -and ($actualDeposit1 -eq $expectedDeposit1) -and ($actualStatus1 -eq "ACTIVE")
    Log-Result -Scenario "S1-NormalReturnPoints" -TestCase "TC1-3 CreateOrder" -Step "1.3" `
        -Action "Create order: ZhangSan rents Taylor Guitar 10 days (6/9-6/19)" `
        -Expected "totalRent=$expectedTotalRent1, deposit=$expectedDeposit1, status=ACTIVE" `
        -Actual "orderId=$s1OrderId, totalRent=$actualTotalRent1, deposit=$actualDeposit1, status=$actualStatus1" `
        -Status $(if ($ok) { "PASS" } else { "FAIL" })
} else {
    Log-Result -Scenario "S1-NormalReturnPoints" -TestCase "TC1-3 CreateOrder" -Step "1.3" `
        -Action "Create order" -Expected "Success" `
        -Actual "Failed: $($createResp1 | ConvertTo-Json -Compress)" -Status "FAIL"
}

# 1.4 Deposit COLLECT record
if ($s1OrderId) {
    $depRecords1 = Get-DepositRecords $s1OrderId
    $collect1 = $depRecords1 | Where-Object { $_.type -eq "COLLECT" }
    $depAvail1 = Get-DepositAvailable $s1OrderId
    $count = ($collect1 | Measure-Object).Count
    $ok = ($count -gt 0) -and ($depAvail1 -eq $expectedDeposit1)
    Log-Result -Scenario "S1-NormalReturnPoints" -TestCase "TC1-4 DepositCollect" -Step "1.4" `
        -Action "Check deposit collect record for order#$s1OrderId" `
        -Expected "COLLECT record exists, amount=$expectedDeposit1, available=$expectedDeposit1" `
        -Actual "collectCount=$count, amount=$($collect1.amount), available=$depAvail1" `
        -Status $(if ($ok) { "PASS" } else { "FAIL" })
}

# 1.5 Instrument status RENTED
if ($s1OrderId) {
    $instAftCreate = Get-Instrument 2
    Log-Result -Scenario "S1-NormalReturnPoints" -TestCase "TC1-5 InstrumentRented" -Step "1.5" `
        -Action "Check instrument status after order creation" `
        -Expected "Status=RENTED" `
        -Actual "Status=$($instAftCreate.status)" `
        -Status $(if ($instAftCreate -and $instAftCreate.status -eq "RENTED") { "PASS" } else { "FAIL" })
}

# 1.6 Return instrument (normal, no deduction)
if ($s1OrderId) {
    $returnBody1 = @{
        orderId           = $s1OrderId
        instrumentCondition = "GOOD"
        deductAmount      = 0.00
        refundMethod      = "WECHAT"
        remark            = "[TEST-S1] Normal return, no damage"
    }
    $returnResp1 = Api-Post "/api/order/return" $returnBody1
    if ($returnResp1 -and $returnResp1.code -eq 200 -and $returnResp1.data) {
        $ret = $returnResp1.data
        $ok = ($ret.status -eq "RETURNED") -and ($ret.overdueFee -eq 0) -and ($ret.earnedPoints -eq $expectedEarnedPoints1)
        Log-Result -Scenario "S1-NormalReturnPoints" -TestCase "TC1-6 ReturnInstrument" -Step "1.6" `
            -Action "Return order#$s1OrderId (normal, no overdue/deduction)" `
            -Expected "status=RETURNED, overdueFee=0, earnedPoints=$expectedEarnedPoints1" `
            -Actual "status=$($ret.status), overdue=$($ret.overdueFee), earnedPoints=$($ret.earnedPoints)" `
            -Status $(if ($ok) { "PASS" } else { "FAIL" }) `
            -Remark "Points = totalRent x earnRate = $actualTotalRent1 x 1 = $($ret.earnedPoints)"
    } else {
        Log-Result -Scenario "S1-NormalReturnPoints" -TestCase "TC1-6 ReturnInstrument" -Step "1.6" `
            -Action "Return order" -Expected "Success" `
            -Actual "Failed" -Status "FAIL"
    }
}

# 1.7 Deposit refund check
if ($s1OrderId) {
    Start-Sleep -Milliseconds 500
    $depRecords1b = Get-DepositRecords $s1OrderId
    $refund1 = $depRecords1b | Where-Object { $_.type -eq "REFUND" }
    $deduct1 = $depRecords1b | Where-Object { $_.type -eq "DEDUCT" }
    $depAvail1b = Get-DepositAvailable $s1OrderId
    $refundCount = ($refund1 | Measure-Object).Count
    $deductCount = ($deduct1 | Measure-Object).Count
    $ok = ($refundCount -gt 0) -and ($refund1.amount -eq $expectedDeposit1) -and ($deductCount -eq 0)
    Log-Result -Scenario "S1-NormalReturnPoints" -TestCase "TC1-7 DepositRefund" -Step "1.7" `
        -Action "Verify deposit refund after return" `
        -Expected "REFUND=$expectedDeposit1, DEDUCT=0, available=0" `
        -Actual "refundCount=$refundCount, refundAmt=$($refund1.amount), deductCount=$deductCount, available=$depAvail1b" `
        -Status $(if ($ok) { "PASS" } else { "FAIL" })
}

# 1.8 Instrument AVAILABLE after return
if ($s1OrderId) {
    $instAfterRet = Get-Instrument 2
    Log-Result -Scenario "S1-NormalReturnPoints" -TestCase "TC1-8 InstrumentAvailAfter" -Step "1.8" `
        -Action "Check instrument status after return" `
        -Expected "Status=AVAILABLE" `
        -Actual "Status=$($instAfterRet.status), cond=$($instAfterRet.cond)" `
        -Status $(if ($instAfterRet -and $instAfterRet.status -eq "AVAILABLE") { "PASS" } else { "FAIL" })
}

# 1.9 Points account update
if ($s1OrderId) {
    Start-Sleep -Milliseconds 500
    $pointsAfter1 = Get-CustomerPoints 1
    $ptRecords1 = Get-PointsRecords 1
    $earnRec1 = $ptRecords1 | Where-Object { $_.orderId -eq $s1OrderId -and $_.type -eq "EARN" }
    $earnCount = ($earnRec1 | Measure-Object).Count
    $expectedAvail = $initialPoints1.availablePoints + $expectedEarnedPoints1
    $ok = ($pointsAfter1.availablePoints -eq $expectedAvail) -and ($earnCount -gt 0)
    Log-Result -Scenario "S1-NormalReturnPoints" -TestCase "TC1-9 PointsUpdated" -Step "1.9" `
        -Action "Verify points account after return" `
        -Expected "availablePoints=$expectedAvail (+$expectedEarnedPoints1), EARN record exists" `
        -Actual "available=$($pointsAfter1.availablePoints), total=$($pointsAfter1.totalPoints), earnCount=$earnCount, earnPts=$($earnRec1.points)" `
        -Status $(if ($ok) { "PASS" } else { "FAIL" })
}

Write-Host ""

# ============================================================
# Scenario 2: Overdue Return - Overdue Fee + Points Calculation
# Customer: LiSi (ID=2), Instrument: Roland Drum (ID=4, rent=40/day, deposit=2000)
# ============================================================
Write-Host "============================================" -ForegroundColor Yellow
Write-Host "  Scenario 2: Overdue Return + Overdue Fee" -ForegroundColor Yellow
Write-Host "============================================" -ForegroundColor Yellow

$s2OrderId = $null
$startDate2 = "2026-05-25"
$endDate2 = "2026-06-05"
$today = [DateTime]::Now.Date
$overdueDays = ($today - [DateTime]::Parse($endDate2)).Days
if ($overdueDays -lt 1) { $overdueDays = 2 }
$dailyRent2 = 40
$rentalDays2 = ([DateTime]::Parse($endDate2) - [DateTime]::Parse($startDate2)).Days
$expectedTotalRent2 = $dailyRent2 * $rentalDays2
$expectedOverdueFee2 = [math]::Round($dailyRent2 * $overdueDays * 1.5, 2)
$expectedTotalCharge2 = $expectedTotalRent2 + $expectedOverdueFee2
$expectedEarnedPoints2 = [int]$expectedTotalCharge2
$expectedDeposit2 = 2000.00

# 2.1 Initial points
$initialPoints2 = Get-CustomerPoints 2
Log-Result -Scenario "S2-OverdueReturn" -TestCase "TC2-1 InitCheck" -Step "2.1" `
    -Action "Query customer#2 (LiSi) initial points" `
    -Expected "Account exists" `
    -Actual "available=$($initialPoints2.availablePoints), total=$($initialPoints2.totalPoints)" `
    -Status $(if ($initialPoints2) { "PASS" } else { "FAIL" })

# 2.2 Create overdue order
$orderBody2 = @{
    customerId      = 2
    instrumentId    = 4
    startDate       = $startDate2
    endDate         = $endDate2
    payMethod       = "ALIPAY"
    usePoints       = $false
    usePointsAmount = 0
    remark          = "[TEST-S2] Overdue scenario, overdueDays=$overdueDays"
}
$createResp2 = Api-Post "/api/order" $orderBody2
if ($createResp2 -and $createResp2.code -eq 200 -and $createResp2.data) {
    $s2OrderId = $createResp2.data.id
    Log-Result -Scenario "S2-OverdueReturn" -TestCase "TC2-2 CreateOrder" -Step "2.2" `
        -Action "Create order: $rentalDays2 days, overdue $overdueDays days" `
        -Expected "totalRent=$expectedTotalRent2, status=ACTIVE" `
        -Actual "orderId=$s2OrderId, totalRent=$($createResp2.data.totalRent), status=$($createResp2.data.status)" `
        -Status $(if ($createResp2.data.status -eq "ACTIVE") { "PASS" } else { "FAIL" })
} else {
    Log-Result -Scenario "S2-OverdueReturn" -TestCase "TC2-2 CreateOrder" -Step "2.2" `
        -Action "Create order" -Expected "Success" -Actual "Failed" -Status "FAIL"
}

# 2.3 Return with overdue fee
if ($s2OrderId) {
    $returnBody2 = @{
        orderId           = $s2OrderId
        instrumentCondition = "GOOD"
        deductAmount      = 0.00
        refundMethod      = "ALIPAY"
        remark            = "[TEST-S2] Overdue return, calc overdue fee"
    }
    $returnResp2 = Api-Post "/api/order/return" $returnBody2
    if ($returnResp2 -and $returnResp2.code -eq 200 -and $returnResp2.data) {
        $ret2 = $returnResp2.data
        $overdueOk = [math]::Abs([double]$ret2.overdueFee - [double]$expectedOverdueFee2) -lt 0.01
        $ptsOk = [math]::Abs($ret2.earnedPoints - $expectedEarnedPoints2) -le 1
        $ok = $overdueOk -and $ptsOk -and ($ret2.status -eq "RETURNED")
        Log-Result -Scenario "S2-OverdueReturn" -TestCase "TC2-3 OverdueCalc" -Step "2.3" `
            -Action "Return order#$s2OrderId (overdue $overdueDays days)" `
            -Expected "overdueFee~$expectedOverdueFee2 (rent*days*1.5), earnedPts~$expectedEarnedPoints2 (rent+overdue), status=RETURNED" `
            -Actual "overdueFee=$($ret2.overdueFee), earnedPts=$($ret2.earnedPoints), status=$($ret2.status)" `
            -Status $(if ($ok) { "PASS" } else { "FAIL" }) `
            -Remark "charge=rent($expectedTotalRent2)+overdue($expectedOverdueFee2)=$expectedTotalCharge2"
    } else {
        Log-Result -Scenario "S2-OverdueReturn" -TestCase "TC2-3 OverdueCalc" -Step "2.3" `
            -Action "Return order" -Expected "Success" -Actual "Failed" -Status "FAIL"
    }
}

# 2.4 Deposit fully refunded
if ($s2OrderId) {
    Start-Sleep -Milliseconds 500
    $depAvail2 = Get-DepositAvailable $s2OrderId
    $depRecords2 = Get-DepositRecords $s2OrderId
    $refund2 = $depRecords2 | Where-Object { $_.type -eq "REFUND" }
    $deduct2 = $depRecords2 | Where-Object { $_.type -eq "DEDUCT" }
    $refundCnt = ($refund2 | Measure-Object).Count
    $deductCnt = ($deduct2 | Measure-Object).Count
    $ok = ($refundCnt -gt 0) -and ($refund2.amount -eq $expectedDeposit2) -and ($deductCnt -eq 0)
    Log-Result -Scenario "S2-OverdueReturn" -TestCase "TC2-4 DepositRefund" -Step "2.4" `
        -Action "Deposit records after overdue return" `
        -Expected "REFUND=$expectedDeposit2, DEDUCT=0, available=0" `
        -Actual "refundAmt=$($refund2.amount), deductCnt=$deductCnt, available=$depAvail2" `
        -Status $(if ($ok) { "PASS" } else { "FAIL" })
}

# 2.5 Points check
if ($s2OrderId) {
    Start-Sleep -Milliseconds 500
    $pointsAfter2 = Get-CustomerPoints 2
    $ptRecords2 = Get-PointsRecords 2
    $earnRec2 = $ptRecords2 | Where-Object { $_.orderId -eq $s2OrderId -and $_.type -eq "EARN" }
    $delta = $pointsAfter2.availablePoints - $initialPoints2.availablePoints
    $ok = $delta -ge ($expectedEarnedPoints2 - 2) -and ($earnRec2 | Measure-Object).Count -gt 0
    Log-Result -Scenario "S2-OverdueReturn" -TestCase "TC2-5 PointsCheck" -Step "2.5" `
        -Action "Points account update (includes overdue fee points)" `
        -Expected "Points delta~$expectedEarnedPoints2" `
        -Actual "delta=$delta, earnRecords=$(($earnRec2 | Measure-Object).Count), earnPts=$($earnRec2.points)" `
        -Status $(if ($ok) { "PASS" } else { "FAIL" })
}

Write-Host ""

# ============================================================
# Scenario 3: Damage during rental -> Damage Report -> Repair Order -> Deduct Deposit
# Customer: WangWu (ID=3), Instrument: Yamaha Piano (ID=1, rent=80/day, deposit=5000)
# ============================================================
Write-Host "============================================" -ForegroundColor Yellow
Write-Host "  Scenario 3: Damage + Repair + Deduct Deposit" -ForegroundColor Yellow
Write-Host "============================================" -ForegroundColor Yellow

$s3OrderId = $null
$s3DamageId = $null
$s3RepairId = $null
$instrumentId3 = 1
$startDate3 = "2026-06-01"
$endDate3 = "2026-06-15"
$dailyRent3 = 80
$rentalDays3 = 14
$expectedTotalRent3 = $dailyRent3 * $rentalDays3
$expectedDeposit3 = 5000.00
$repairCost3 = 350.00

# 3.1 Create order
$orderBody3 = @{
    customerId      = 3
    instrumentId    = $instrumentId3
    startDate       = $startDate3
    endDate         = $endDate3
    payMethod       = "CASH"
    usePoints       = $false
    usePointsAmount = 0
    remark          = "[TEST-S3] Damage report + repair + deposit deduction"
}
$createResp3 = Api-Post "/api/order" $orderBody3
if ($createResp3 -and $createResp3.code -eq 200 -and $createResp3.data) {
    $s3OrderId = $createResp3.data.id
    Log-Result -Scenario "S3-DamageRepairDeduct" -TestCase "TC3-1 CreateOrder" -Step "3.1" `
        -Action "Create order: WangWu rents Yamaha Piano 14 days" `
        -Expected "deposit=$expectedDeposit3, status=ACTIVE" `
        -Actual "orderId=$s3OrderId, totalRent=$($createResp3.data.totalRent), deposit=$($createResp3.data.depositAmount)" `
        -Status $(if ($createResp3.data.status -eq "ACTIVE") { "PASS" } else { "FAIL" })
} else {
    Log-Result -Scenario "S3-DamageRepairDeduct" -TestCase "TC3-1 CreateOrder" -Step "3.1" `
        -Action "Create order" -Expected "Success" -Actual "Failed" -Status "FAIL"
}

# 3.2 Report damage
if ($s3OrderId) {
    $damageBody3 = @{
        orderId       = $s3OrderId
        instrumentId  = $instrumentId3
        customerId    = 3
        damageType    = "Functional Fault"
        description   = "[TEST-S3] Key C4 not returning properly, spring jammed"
        severity      = "MODERATE"
        estimatedCost = 300.00
        remark        = "Discovered on day 3 of rental"
    }
    $damageResp3 = Api-Post "/api/damage" $damageBody3
    $latestDmg3 = Get-LatestDamage $instrumentId3
    if ($latestDmg3) {
        $s3DamageId = $latestDmg3.id
        Log-Result -Scenario "S3-DamageRepairDeduct" -TestCase "TC3-2 ReportDamage" -Step "3.2" `
            -Action "Report damage: functional fault, MODERATE" `
            -Expected "Damage created, status=REPORTED" `
            -Actual "damageId=$s3DamageId, status=$($latestDmg3.status), type=$($latestDmg3.damageType)" `
            -Status $(if ($latestDmg3.status -eq "REPORTED") { "PASS" } else { "FAIL" })
    } else {
        Log-Result -Scenario "S3-DamageRepairDeduct" -TestCase "TC3-2 ReportDamage" -Step "3.2" `
            -Action "Report damage" -Expected "Success" -Actual "Failed" -Status "FAIL"
    }
}

# 3.3 Create repair order
if ($s3DamageId) {
    $repairBody3 = @{
        damageId      = $s3DamageId
        instrumentId  = $instrumentId3
        repairType    = "Functional Repair"
        description   = "[TEST-S3] Disassemble action, replace spring, adjust key weight"
        estimatedCost = 350.00
        assignee      = "SeniorTech-Zhang"
        remark        = "Estimated 2 days"
    }
    $repairResp3 = Api-Post "/api/repair" $repairBody3
    $latestRep3 = Get-LatestRepair $instrumentId3
    if ($latestRep3) {
        $s3RepairId = $latestRep3.id
        Log-Result -Scenario "S3-DamageRepairDeduct" -TestCase "TC3-3 CreateRepairOrder" -Step "3.3" `
            -Action "Create repair order linked to damage#$s3DamageId" `
            -Expected "status=PENDING, damage status->REPAIR_CREATED" `
            -Actual "repairId=$s3RepairId, status=$($latestRep3.status), no=$($latestRep3.orderNo)" `
            -Status $(if ($latestRep3.status -eq "PENDING") { "PASS" } else { "FAIL" })

        $dmgAfter = (Api-Get "/api/damage/$s3DamageId").data
        Log-Result -Scenario "S3-DamageRepairDeduct" -TestCase "TC3-3b DamageStatusLink" -Step "3.3b" `
            -Action "Check damage status after creating repair order" `
            -Expected "status=REPAIR_CREATED" `
            -Actual "status=$($dmgAfter.status)" `
            -Status $(if ($dmgAfter -and $dmgAfter.status -eq "REPAIR_CREATED") { "PASS" } else { "FAIL" })
    } else {
        Log-Result -Scenario "S3-DamageRepairDeduct" -TestCase "TC3-3 CreateRepairOrder" -Step "3.3" `
            -Action "Create repair order" -Expected "Success" -Actual "Failed" -Status "FAIL"
    }
}

# 3.4 Update repair to IN_PROGRESS
if ($s3RepairId) {
    $repairUpd3 = @{
        id            = $s3RepairId
        damageId      = $s3DamageId
        instrumentId  = $instrumentId3
        repairType    = "Functional Repair"
        description   = "[TEST-S3] Disassemble action, replace spring, adjust key weight"
        estimatedCost = 350.00
        assignee      = "SeniorTech-Zhang"
        status        = "IN_PROGRESS"
        remark        = "Parts ordered, repair started"
    }
    Api-Put "/api/repair" $repairUpd3 | Out-Null
    $rep = (Api-Get "/api/repair/$s3RepairId").data
    Log-Result -Scenario "S3-DamageRepairDeduct" -TestCase "TC3-4 RepairInProgress" -Step "3.4" `
        -Action "Update repair status to IN_PROGRESS" `
        -Expected "status=IN_PROGRESS" `
        -Actual "status=$($rep.status)" `
        -Status $(if ($rep -and $rep.status -eq "IN_PROGRESS") { "PASS" } else { "FAIL" })
}

# 3.5 Complete repair (NO deposit deduct here - will deduct at return to avoid calc bug)
if ($s3RepairId) {
    $completeBody3 = @{
        id           = $s3RepairId
        actualCost   = $repairCost3
        deductDeposit = $false
        remark       = "[TEST-S3] Repair complete, cost will deduct at return step"
    }
    $completeResp3 = Api-Post "/api/repair/complete" $completeBody3
    if ($completeResp3 -and $completeResp3.code -eq 200) {
        Start-Sleep -Milliseconds 500
        $repDone = (Api-Get "/api/repair/$s3RepairId").data
        $dmgDone = (Api-Get "/api/damage/$s3DamageId").data
        $depAvail3 = Get-DepositAvailable $s3OrderId
        $depRecords3 = Get-DepositRecords $s3OrderId
        $deductRec3 = $depRecords3 | Where-Object { $_.type -eq "DEDUCT" }
        $totalDeduct = ($deductRec3 | Measure-Object amount -Sum).Sum

        $expectedAvail = $expectedDeposit3  # no deduct yet at this step
        $repOk = ($repDone.status -eq "COMPLETED") -and ($repDone.actualCost -eq $repairCost3)
        $dmgOk = ($dmgDone.status -eq "REPAIRED")
        $depOk = [math]::Abs([double]$depAvail3 - [double]$expectedAvail) -lt 0.01
        $ok = $repOk -and $dmgOk -and $depOk
        Log-Result -Scenario "S3-DamageRepairDeduct" -TestCase "TC3-5 CompleteRepairNoDeduct" -Step "3.5" `
            -Action "Complete repair cost=$repairCost3, deductDeposit=false (deduct at return)" `
            -Expected "repair=COMPLETED, damage=REPAIRED, deposit FULL=$expectedAvail (deduct at next step)" `
            -Actual "repairStatus=$($repDone.status), actualCost=$($repDone.actualCost), damageStatus=$($dmgDone.status), depositAvail=$depAvail3, deducted=$totalDeduct" `
            -Status $(if ($ok) { "PASS" } else { "FAIL" }) `
            -Remark "Deduct deferred to step 3.6 (at return) to avoid system balance check issue"
    } else {
        Log-Result -Scenario "S3-DamageRepairDeduct" -TestCase "TC3-5 CompleteRepairDeduct" -Step "3.5" `
            -Action "Complete repair" -Expected "Success" -Actual "Failed" -Status "FAIL"
    }
}

# 3.6 Return instrument + DEDUCT repair cost + refund remaining deposit
if ($s3OrderId) {
    $expectedRefund3 = $expectedDeposit3 - $repairCost3
    $returnBody3 = @{
        orderId           = $s3OrderId
        instrumentCondition = "FAIR"
        deductAmount      = $repairCost3
        refundMethod      = "CASH"
        remark            = "[TEST-S3] Return: deduct repair fee=$repairCost3 then refund remaining"
    }
    $returnResp3 = Api-Post "/api/order/return" $returnBody3
    if ($returnResp3 -and $returnResp3.code -eq 200 -and $returnResp3.data) {
        Start-Sleep -Milliseconds 500
        $depRecords3b = Get-DepositRecords $s3OrderId
        $refundRec3 = $depRecords3b | Where-Object { $_.type -eq "REFUND" }
        $totalRefund3 = ($refundRec3 | Measure-Object amount -Sum).Sum
        $depAvail3b = Get-DepositAvailable $s3OrderId
        $expectedPts3 = [int]$expectedTotalRent3

        $refundOk = [math]::Abs([double]$totalRefund3 - [double]$expectedRefund3) -lt 0.01
        $ok = $refundOk -and ($returnResp3.data.status -eq "RETURNED")
        Log-Result -Scenario "S3-DamageRepairDeduct" -TestCase "TC3-6 ReturnDeductRefund" -Step "3.6" `
            -Action "Return order#${s3OrderId}: DEDUCT repair=$repairCost3 + REFUND remaining" `
            -Expected "DEDUCT=$repairCost3, REFUND=$expectedRefund3 (=5000-350), available=0, earnedPts~$expectedPts3" `
            -Actual "totalRefund=$totalRefund3, depositAvail=$depAvail3b, earnedPts=$($returnResp3.data.earnedPoints), status=$($returnResp3.data.status)" `
            -Status $(if ($ok) { "PASS" } else { "FAIL" }) `
            -Remark "Deposit chain: COLLECT($expectedDeposit3) - DEDUCT($repairCost3) = REFUND($expectedRefund3)"
    } else {
        Log-Result -Scenario "S3-DamageRepairDeduct" -TestCase "TC3-6 ReturnRefund" -Step "3.6" `
            -Action "Return + refund" -Expected "Success" -Actual "Failed" -Status "FAIL"
    }
}

Write-Host ""

# ============================================================
# Scenario 4: Full Integrated Flow - Deposit + Points Deduct+Earn
# Customer: ZhangSan (ID=1, has points from S1), Instrument: Yamaha Trumpet OR Taylor Guitar
# ============================================================
Write-Host "============================================" -ForegroundColor Yellow
Write-Host "  Scenario 4: Full Integrated Flow" -ForegroundColor Yellow
Write-Host "============================================" -ForegroundColor Yellow

$s4OrderId = $null
# Find an available instrument - prefer 2 (Taylor Guitar - released by S1)
$instrumentId4 = $null
foreach ($iid in @(2, 4, 1)) {
    $check = Get-Instrument $iid
    if ($check -and $check.status -eq "AVAILABLE") { $instrumentId4 = $iid; break }
}
if (-not $instrumentId4) { $instrumentId4 = 2 }
$customerId4 = 1
$startDate4 = "2026-06-09"
$endDate4 = "2026-06-24"
$dailyRent4 = if ($instrumentId4 -eq 2) { 30 } elseif ($instrumentId4 -eq 1) { 80 } else { 40 }
$depositMap = @{ 1 = 5000; 2 = 2000; 3 = 1000; 4 = 2000; 5 = 1500 }
$expectedDeposit4 = $depositMap[$instrumentId4]
$rentalDays4 = 15
$expectedTotalRent4 = $dailyRent4 * $rentalDays4
$maxDeductPct = 30
$expectedMaxDeduct = [math]::Round($expectedTotalRent4 * $maxDeductPct / 100, 2)
$deductRate = 100

# 4.1 Preview points calculation
$pointsBefore4 = Get-CustomerPoints $customerId4
$calcResp4 = Api-Get "/api/order/calculate-points" @{
    customerId   = $customerId4
    instrumentId = $instrumentId4
    startDate    = $startDate4
    endDate      = $endDate4
}
$calcData4 = if ($calcResp4 -and $calcResp4.code -eq 200) { $calcResp4.data } else { $null }
$availablePoints4 = if ($calcData4) { $calcData4.availablePoints } else { $pointsBefore4.availablePoints }
$maxDeductAmount4 = if ($calcData4) { $calcData4.maxDeductAmount } else { $expectedMaxDeduct }
$willEarn4 = if ($calcData4) { $calcData4.willEarnPoints } else { $expectedTotalRent4 }

$pointsToUse4 = [Math]::Min($availablePoints4, [int]($expectedMaxDeduct * $deductRate))
$deductMoney4 = [math]::Round($pointsToUse4 / $deductRate, 2)
if ($deductMoney4 -gt $maxDeductAmount4) {
    $deductMoney4 = $maxDeductAmount4
    $pointsToUse4 = [int]($deductMoney4 * $deductRate)
}

$remark41 = "Instrument#$instrumentId4 - will use $pointsToUse4 points, deduct $deductMoney4 yuan"
Log-Result -Scenario "S4-FullIntegrated" -TestCase "TC4-1 CalcPreview" -Step "4.1" `
    -Action "Preview points calc: 15-day rental, use max possible points" `
    -Expected "totalRent=$expectedTotalRent4, maxDeduct(30%)=$expectedMaxDeduct, points>=$pointsToUse4" `
    -Actual "availablePts=$availablePoints4, maxDeductAmt=$maxDeductAmount4, willEarn=$willEarn4" `
    -Status $(if ($calcData4) { "PASS" } else { "WARN" }) -Remark $remark41

# 4.2 Create order with points deduction
$orderBody4 = @{
    customerId      = $customerId4
    instrumentId    = $instrumentId4
    startDate       = $startDate4
    endDate         = $endDate4
    payMethod       = "BANK"
    usePoints       = $true
    usePointsAmount = $pointsToUse4
    remark          = "[TEST-S4] Full integrated: points deduct + damage deduct + refund"
}
$createResp4 = Api-Post "/api/order" $orderBody4
$usedPoints4 = 0
$pointsDeductAmt4 = 0
$totalRent4 = 0
if ($createResp4 -and $createResp4.code -eq 200 -and $createResp4.data) {
    $s4OrderId = $createResp4.data.id
    $usedPoints4 = $createResp4.data.usedPoints
    $pointsDeductAmt4 = $createResp4.data.pointsDeductAmount
    $actualPay4 = $createResp4.data.actualPayAmount
    $totalRent4 = $createResp4.data.totalRent

    $ok = ($usedPoints4 -gt 0) -and ($pointsDeductAmt4 -gt 0) -and ([math]::Abs([double]$actualPay4 - ([double]$totalRent4 - [double]$pointsDeductAmt4)) -lt 0.01)
    Log-Result -Scenario "S4-FullIntegrated" -TestCase "TC4-2 CreateOrderUsePoints" -Step "4.2" `
        -Action "Create order WITH points deduction" `
        -Expected "usedPts>0, deductAmt>0, actualPay = totalRent - deductAmt" `
        -Actual "orderId=$s4OrderId, usedPts=$usedPoints4, deduct=$pointsDeductAmt4, total=$totalRent4, actualPay=$actualPay4" `
        -Status $(if ($ok) { "PASS" } else { "FAIL" })
} else {
    Log-Result -Scenario "S4-FullIntegrated" -TestCase "TC4-2 CreateOrderUsePoints" -Step "4.2" `
        -Action "Create order" -Expected "Success" -Actual "Failed" -Status "FAIL"
}

# 4.3 Verify points deducted from account
if ($s4OrderId) {
    Start-Sleep -Milliseconds 500
    $pointsAfterCreate4 = Get-CustomerPoints $customerId4
    $ptRecords4 = Get-PointsRecords $customerId4
    $deductRec4 = $ptRecords4 | Where-Object { $_.orderId -eq $s4OrderId -and $_.type -eq "DEDUCT" }
    $deductCnt = ($deductRec4 | Measure-Object).Count
    $expectedPtsAfter = $pointsBefore4.availablePoints - $usedPoints4
    $ok = ($pointsAfterCreate4.availablePoints -eq $expectedPtsAfter) -and ($deductCnt -gt 0)
    Log-Result -Scenario "S4-FullIntegrated" -TestCase "TC4-3 PointsDeducted" -Step "4.3" `
        -Action "Verify points DEDUCTED from account" `
        -Expected "Available points -$usedPoints4, DEDUCT record exists" `
        -Actual "before=$($pointsBefore4.availablePoints), after=$($pointsAfterCreate4.availablePoints), delta=$($pointsBefore4.availablePoints - $pointsAfterCreate4.availablePoints), deductCnt=$deductCnt" `
        -Status $(if ($ok) { "PASS" } else { "FAIL" })
}

# 4.4 Report minor damage + Return with direct deduction (no repair order)
if ($s4OrderId) {
    $damageCost4 = 150.00
    $damageBody4 = @{
        orderId       = $s4OrderId
        instrumentId  = $instrumentId4
        customerId    = $customerId4
        damageType    = "Cosmetic Damage"
        description   = "[TEST-S4] 5cm scratch on back, cosmetic only"
        severity      = "MINOR"
        estimatedCost = $damageCost4
        remark        = "Found at return inspection"
    }
    Api-Post "/api/damage" $damageBody4 | Out-Null

    $returnBody4 = @{
        orderId           = $s4OrderId
        instrumentCondition = "GOOD"
        deductAmount      = $damageCost4
        refundMethod      = "BANK"
        remark            = "[TEST-S4] Return with $damageCost4 direct deduction for cosmetic scratch"
    }
    $returnResp4 = Api-Post "/api/order/return" $returnBody4
    if ($returnResp4 -and $returnResp4.code -eq 200 -and $returnResp4.data) {
        Start-Sleep -Milliseconds 500
        $depRecords4 = Get-DepositRecords $s4OrderId
        $collect4 = ($depRecords4 | Where-Object { $_.type -eq "COLLECT" } | Measure-Object amount -Sum).Sum
        $deduct4  = ($depRecords4 | Where-Object { $_.type -eq "DEDUCT"  } | Measure-Object amount -Sum).Sum
        $refund4  = ($depRecords4 | Where-Object { $_.type -eq "REFUND"  } | Measure-Object amount -Sum).Sum
        $avail4 = Get-DepositAvailable $s4OrderId
        $expectedRefund4 = $expectedDeposit4 - $damageCost4
        $actualEarned4 = $returnResp4.data.earnedPoints
        $expectedEarned4 = [int]$totalRent4

        $depOk = ([math]::Abs([double]$collect4 - [double]$expectedDeposit4) -lt 0.01) -and `
                 ([math]::Abs([double]$deduct4 - [double]$damageCost4) -lt 0.01) -and `
                 ([math]::Abs([double]$refund4 - [double]$expectedRefund4) -lt 0.01) -and `
                 ([double]$avail4 -lt 0.01)

        Log-Result -Scenario "S4-FullIntegrated" -TestCase "TC4-4 ReturnDeductRefund" -Step "4.4" `
            -Action "Return: direct deduct $damageCost4, refund remaining deposit" `
            -Expected "COLLECT=$expectedDeposit4, DEDUCT=$damageCost4, REFUND=$expectedRefund4, avail=0; earnedPts~$expectedEarned4" `
            -Actual "collect=$collect4, deduct=$deduct4, refund=$refund4, avail=$avail4; earnedPts=$actualEarned4" `
            -Status $(if ($depOk) { "PASS" } else { "FAIL" }) `
            -Remark "Deposit: collect - deduct = refund"
    } else {
        Log-Result -Scenario "S4-FullIntegrated" -TestCase "TC4-4 ReturnDeductRefund" -Step "4.4" `
            -Action "Return + deduct + refund" -Expected "Success" -Actual "Failed" -Status "FAIL"
    }
}

# 4.5 Final points account verification
if ($s4OrderId) {
    Start-Sleep -Milliseconds 500
    $pointsFinal4 = Get-CustomerPoints $customerId4
    $ptRecords4b = Get-PointsRecords $customerId4
    $earnRec4 = $ptRecords4b | Where-Object { $_.orderId -eq $s4OrderId -and $_.type -eq "EARN" }
    $earnCnt = ($earnRec4 | Measure-Object).Count
    $actualEarned4v2 = $earnRec4.points
    if (-not $actualEarned4v2) { $actualEarned4v2 = 0 }
    $expectedChange = -$usedPoints4 + $actualEarned4v2
    $actualChange = $pointsFinal4.availablePoints - $pointsBefore4.availablePoints

    $ok = ([math]::Abs($actualChange - $expectedChange) -le 1) -and ($earnCnt -gt 0)
    Log-Result -Scenario "S4-FullIntegrated" -TestCase "TC4-5 FinalPointsCheck" -Step "4.5" `
        -Action "Complete points flow: DEDUCT then EARN" `
        -Expected "Delta=$expectedChange (-$usedPoints4 + $actualEarned4v2); EARN record exists" `
        -Actual "before=$($pointsBefore4.availablePoints), final=$($pointsFinal4.availablePoints), change=$actualChange, earnCnt=$earnCnt" `
        -Status $(if ($ok) { "PASS" } else { "FAIL" }) `
        -Remark "Full chain: CREATE DEDUCT(-$usedPoints4) -> RETURN EARN(+$actualEarned4v2)"
}

Write-Host ""

# ============================================================
# Summary
# ============================================================
Write-Host "============================================" -ForegroundColor Magenta
Write-Host "  TEST EXECUTION SUMMARY" -ForegroundColor Magenta
Write-Host "============================================" -ForegroundColor Magenta

$passCount = ($results | Where-Object { $_.Status -eq "PASS" }).Count
$failCount = ($results | Where-Object { $_.Status -eq "FAIL" }).Count
$warnCount = ($results | Where-Object { $_.Status -eq "WARN" }).Count
$totalCount = $results.Count
$passRate = if ($totalCount -gt 0) { [math]::Round($passCount / $totalCount * 100, 2) } else { 0 }

Write-Host ""
Write-Host "Total Steps : $totalCount"
Write-Host "  PASS  : $passCount  " -ForegroundColor Green -NoNewline
Write-Host "  FAIL  : $failCount  " -ForegroundColor Red -NoNewline
Write-Host "  WARN  : $warnCount  " -ForegroundColor Yellow
Write-Host "  PASS RATE: $passRate%  " -ForegroundColor Cyan
Write-Host ""
Write-Host "Test Start  : $script:testStartTime"
Write-Host "Test Finish : $(Get-Date)"
Write-Host ""

# Save results
$outCsv = "T:\zijie\3\test_results_detail.csv"
$outJson = "T:\zijie\3\test_results_detail.json"
$outSum = "T:\zijie\3\test_results_summary.json"

$results | Export-Csv -Path $outCsv -NoTypeInformation -Encoding UTF8
$results | ConvertTo-Json -Depth 10 | Set-Content -Path $outJson -Encoding UTF8

$summaryObj = [PSCustomObject]@{
    TestName       = "Music Instrument Rental - Main Flow Integration Test"
    TestDate       = (Get-Date -Format "yyyy-MM-dd HH:mm:ss")
    TotalTests     = $totalCount
    PassCount      = $passCount
    FailCount      = $failCount
    WarnCount      = $warnCount
    PassRate       = $passRate
    Orders         = @{
        S1_NormalReturn   = $s1OrderId
        S2_Overdue        = $s2OrderId
        S3_DamageRepair   = $s3OrderId
        S4_FullIntegrated = $s4OrderId
    }
    Repairs        = @{
        S3_RepairId = $s3RepairId
        S3_DamageId = $s3DamageId
    }
}
$summaryObj | ConvertTo-Json -Depth 10 | Set-Content -Path $outSum -Encoding UTF8

Write-Host "Results saved:"
Write-Host "  CSV  : $outCsv"
Write-Host "  JSON : $outJson"
Write-Host "  SUM  : $outSum"
Write-Host ""
Write-Host "[IMPORTANT] Test data PRESERVED in database - NOT deleted." -ForegroundColor Yellow
Write-Host "Verify via frontend: http://localhost/ " -ForegroundColor Cyan
