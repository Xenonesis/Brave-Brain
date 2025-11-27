$ErrorActionPreference = "Stop"

$API_KEY = "AIzaSyBc4HKleZITdt161mjf-vOpJyqEhWAqSF4"
$PROJECT_ID = "bravebrain-59cdc"

Write-Host "Initializing Firestore Collections for Brave Brain" -ForegroundColor Cyan
Write-Host ""

# Step 1: Sign in anonymously to get an ID token
Write-Host "Creating anonymous user session..." -ForegroundColor Yellow

$authUrl = "https://identitytoolkit.googleapis.com/v1/accounts:signUp?key=$API_KEY"
$authBody = @{ returnSecureToken = $true } | ConvertTo-Json

$authResponse = $null
try {
    $authResponse = Invoke-RestMethod -Uri $authUrl -Method Post -Body $authBody -ContentType "application/json"
}
catch {
    Write-Host "Failed to create anonymous session: $_" -ForegroundColor Red
    exit 1
}

$idToken = $authResponse.idToken
$userId = $authResponse.localId
Write-Host "Anonymous session created (User ID: $userId)" -ForegroundColor Green

# Firestore REST API base URL
$firestoreUrl = "https://firestore.googleapis.com/v1/projects/$PROJECT_ID/databases/(default)/documents"

$headers = @{
    "Authorization" = "Bearer $idToken"
    "Content-Type" = "application/json"
}

$today = (Get-Date).ToString("yyyy-MM-dd")
$timestamp = (Get-Date).ToUniversalTime().ToString("yyyy-MM-ddTHH:mm:ssZ")

function Create-Document {
    param(
        [string]$collection,
        [string]$docId,
        [hashtable]$fields
    )
    
    if ($docId -and $docId -ne "") {
        $url = "$firestoreUrl/${collection}/${docId}"
    }
    else {
        $url = "$firestoreUrl/${collection}"
    }
    
    $body = @{ fields = $fields } | ConvertTo-Json -Depth 10
    
    try {
        if ($docId -and $docId -ne "") {
            $response = Invoke-RestMethod -Uri $url -Method Patch -Headers $headers -Body $body
        }
        else {
            $response = Invoke-RestMethod -Uri $url -Method Post -Headers $headers -Body $body
        }
        return $true
    }
    catch {
        Write-Host "  Error: $($_.Exception.Message)" -ForegroundColor Red
        return $false
    }
}

Write-Host ""
Write-Host "Creating Firestore collections..." -ForegroundColor Yellow
Write-Host ""

# 1. Users collection
Write-Host "  Creating 'users' collection..." -NoNewline
$usersFields = @{
    userId = @{ stringValue = $userId }
    email = @{ stringValue = "" }
    displayName = @{ stringValue = "Anonymous User" }
    createdAt = @{ timestampValue = $timestamp }
    lastSyncAt = @{ timestampValue = $timestamp }
    preferences = @{ 
        mapValue = @{ 
            fields = @{
                theme = @{ stringValue = "system" }
                notifications = @{ booleanValue = $true }
            }
        }
    }
}
$result = Create-Document -collection "users" -docId $userId -fields $usersFields
if ($result) { Write-Host " OK" -ForegroundColor Green } else { Write-Host " FAILED" -ForegroundColor Red }

# 2. Analytics collection
Write-Host "  Creating 'analytics' collection..." -NoNewline
$analyticsFields = @{
    userId = @{ stringValue = $userId }
    date = @{ stringValue = $today }
    totalScreenTimeMs = @{ integerValue = "0" }
    productivityScore = @{ integerValue = "0" }
    blockedAttempts = @{ integerValue = "0" }
    challengesCompleted = @{ integerValue = "0" }
    challengesFailed = @{ integerValue = "0" }
    timestamp = @{ timestampValue = $timestamp }
    usagePatterns = @{
        mapValue = @{
            fields = @{
                initialized = @{ booleanValue = $true }
            }
        }
    }
}
$result = Create-Document -collection "analytics" -docId "${userId}_${today}" -fields $analyticsFields
if ($result) { Write-Host " OK" -ForegroundColor Green } else { Write-Host " FAILED" -ForegroundColor Red }

# 3. Gamification collection
Write-Host "  Creating 'gamification' collection..." -NoNewline
$gamificationFields = @{
    userId = @{ stringValue = $userId }
    points = @{ integerValue = "0" }
    level = @{ integerValue = "1" }
    badges = @{ arrayValue = @{ values = @() } }
    lastUpdated = @{ timestampValue = $timestamp }
    challenges = @{
        mapValue = @{
            fields = @{
                dailyStreak = @{ integerValue = "0" }
                initialized = @{ booleanValue = $true }
            }
        }
    }
}
$result = Create-Document -collection "gamification" -docId $userId -fields $gamificationFields
if ($result) { Write-Host " OK" -ForegroundColor Green } else { Write-Host " FAILED" -ForegroundColor Red }

# 4. Notifications collection
Write-Host "  Creating 'notifications' collection..." -NoNewline
$notificationFields = @{
    userId = @{ stringValue = $userId }
    type = @{ stringValue = "welcome" }
    title = @{ stringValue = "Welcome to Brave Brain!" }
    message = @{ stringValue = "Your database has been initialized successfully." }
    sentAt = @{ timestampValue = $timestamp }
    wasClicked = @{ booleanValue = $false }
    wasDismissed = @{ booleanValue = $false }
    effectiveness = @{ doubleValue = 0.0 }
    context = @{
        mapValue = @{
            fields = @{
                source = @{ stringValue = "init_script" }
            }
        }
    }
}
$result = Create-Document -collection "notifications" -docId "" -fields $notificationFields
if ($result) { Write-Host " OK" -ForegroundColor Green } else { Write-Host " FAILED" -ForegroundColor Red }

# 5. AppUsage collection
Write-Host "  Creating 'appUsage' collection..." -NoNewline
$appUsageFields = @{
    userId = @{ stringValue = $userId }
    packageName = @{ stringValue = "com.bravebrain" }
    appName = @{ stringValue = "Brave Brain" }
    usageTimeMs = @{ integerValue = "0" }
    dailyLimitMs = @{ integerValue = "0" }
    category = @{ stringValue = "productivity" }
    date = @{ stringValue = $today }
    timestamp = @{ timestampValue = $timestamp }
}
$result = Create-Document -collection "appUsage" -docId "${userId}_com.bravebrain_${today}" -fields $appUsageFields
if ($result) { Write-Host " OK" -ForegroundColor Green } else { Write-Host " FAILED" -ForegroundColor Red }

# 6. Feedback collection
Write-Host "  Creating 'feedback' collection..." -NoNewline
$feedbackFields = @{
    userId = @{ stringValue = $userId }
    feedbackType = @{ stringValue = "system_init" }
    rating = @{ integerValue = "5" }
    comment = @{ stringValue = "Database initialized via PowerShell script" }
    timestamp = @{ timestampValue = $timestamp }
    context = @{
        mapValue = @{
            fields = @{
                source = @{ stringValue = "powershell_init" }
            }
        }
    }
}
$result = Create-Document -collection "feedback" -docId "" -fields $feedbackFields
if ($result) { Write-Host " OK" -ForegroundColor Green } else { Write-Host " FAILED" -ForegroundColor Red }

# 7. UserEngagement collection
Write-Host "  Creating 'userEngagement' collection..." -NoNewline
$engagementFields = @{
    userId = @{ stringValue = $userId }
    totalSessions = @{ integerValue = "0" }
    lastActiveAt = @{ timestampValue = $timestamp }
}
$result = Create-Document -collection "userEngagement" -docId $userId -fields $engagementFields
if ($result) { Write-Host " OK" -ForegroundColor Green } else { Write-Host " FAILED" -ForegroundColor Red }

Write-Host ""
Write-Host "Firestore collections initialized!" -ForegroundColor Green
Write-Host ""
Write-Host "Collections created:" -ForegroundColor Cyan
Write-Host "   - users"
Write-Host "   - analytics"
Write-Host "   - gamification"
Write-Host "   - notifications"
Write-Host "   - appUsage"
Write-Host "   - feedback"
Write-Host "   - userEngagement"
Write-Host ""
Write-Host "View in Firebase Console:" -ForegroundColor Cyan
Write-Host "   https://console.firebase.google.com/project/bravebrain-59cdc/firestore/data" -ForegroundColor Blue
Write-Host ""
