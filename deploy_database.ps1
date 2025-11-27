# Firestore Database & Index Deployment Script (PowerShell)
# This script deploys indexes and security rules after database creation

$ErrorActionPreference = "Stop"

Write-Host "`n============================================================" -ForegroundColor Cyan
Write-Host "  Firestore Database Setup & Index Deployment" -ForegroundColor Cyan
Write-Host "============================================================`n" -ForegroundColor Cyan

# Check if Firebase CLI is installed
Write-Host "Checking Firebase CLI..." -ForegroundColor Yellow
$firebaseCmd = Get-Command firebase -ErrorAction SilentlyContinue

if (-not $firebaseCmd) {
    Write-Host "❌ Firebase CLI not found!" -ForegroundColor Red
    Write-Host "Install it with: npm install -g firebase-tools" -ForegroundColor Yellow
    exit 1
}

Write-Host "✓ Firebase CLI found" -ForegroundColor Green
Write-Host ""

# Check if user is logged in
Write-Host "Checking Firebase authentication..." -ForegroundColor Yellow
try {
    $loginStatus = firebase login:list 2>&1
    Write-Host "✓ Authenticated as: $($loginStatus | Select-String 'Logged in as')" -ForegroundColor Green
} catch {
    Write-Host "Please login to Firebase..." -ForegroundColor Yellow
    firebase login
}
Write-Host ""

# Check if database exists
Write-Host "Checking if Firestore database exists..." -ForegroundColor Yellow
$dbCheck = firebase firestore:indexes 2>&1

if ($dbCheck -match "does not exist") {
    Write-Host "❌ Database does not exist yet!" -ForegroundColor Red
    Write-Host ""
    Write-Host "Please create the database first:" -ForegroundColor Yellow
    Write-Host "1. Visit: https://console.firebase.google.com/project/bravebrain-59cdc/firestore" -ForegroundColor White
    Write-Host "2. Click 'Create database'" -ForegroundColor White
    Write-Host "3. Choose 'Production mode'" -ForegroundColor White
    Write-Host "4. Select location (e.g., us-central1)" -ForegroundColor White
    Write-Host "5. Click 'Enable' and wait for provisioning" -ForegroundColor White
    Write-Host ""
    Write-Host "After database is created, run this script again." -ForegroundColor Cyan
    exit 1
}

Write-Host "✓ Database exists" -ForegroundColor Green
Write-Host ""

# Deploy indexes
Write-Host "Step 1: Deploying Firestore indexes..." -ForegroundColor Cyan
Write-Host "This will deploy 6 composite indexes:" -ForegroundColor Yellow
Write-Host "  1. appUsage (userId + date)" -ForegroundColor White
Write-Host "  2. analytics (userId + date)" -ForegroundColor White
Write-Host "  3. notifications (userId + sentAt)" -ForegroundColor White
Write-Host "  4. feedback (userId + timestamp)" -ForegroundColor White
Write-Host "  5. gamification (userId + lastUpdated)" -ForegroundColor White
Write-Host "  6. appUsage (userId + packageName + date)" -ForegroundColor White
Write-Host ""

try {
    firebase deploy --only firestore:indexes
    Write-Host ""
    Write-Host "✓ Indexes deployed successfully!" -ForegroundColor Green
    Write-Host ""
} catch {
    Write-Host "❌ Index deployment failed: $_" -ForegroundColor Red
    exit 1
}

# Deploy security rules
Write-Host "Step 2: Deploying Firestore security rules..." -ForegroundColor Cyan
Write-Host ""

try {
    firebase deploy --only firestore:rules
    Write-Host ""
    Write-Host "✓ Security rules deployed successfully!" -ForegroundColor Green
    Write-Host ""
} catch {
    Write-Host "❌ Security rules deployment failed: $_" -ForegroundColor Red
    exit 1
}

# Check index status
Write-Host "Step 3: Checking index build status..." -ForegroundColor Cyan
Write-Host ""

firebase firestore:indexes

Write-Host ""
Write-Host "Note: Indexes may show 'BUILDING' status initially." -ForegroundColor Yellow
Write-Host "They will change to 'READY' within a few minutes." -ForegroundColor Yellow
Write-Host ""

# Success summary
Write-Host "============================================================" -ForegroundColor Green
Write-Host "✅ DEPLOYMENT COMPLETE!" -ForegroundColor Green
Write-Host "============================================================" -ForegroundColor Green
Write-Host ""
Write-Host "What was deployed:" -ForegroundColor Yellow
Write-Host "  ✓ 6 composite indexes" -ForegroundColor Green
Write-Host "  ✓ Security rules for 7 collections" -ForegroundColor Green
Write-Host ""
Write-Host "Next steps:" -ForegroundColor Yellow
Write-Host "  1. Wait for indexes to reach 'READY' status (check with: firebase firestore:indexes)" -ForegroundColor White
Write-Host "  2. Test your app to verify data sync works" -ForegroundColor White
Write-Host "  3. Check Firebase Console to see synced data" -ForegroundColor White
Write-Host ""
Write-Host "Firebase Console: https://console.firebase.google.com/project/bravebrain-59cdc/firestore" -ForegroundColor Cyan
Write-Host ""
