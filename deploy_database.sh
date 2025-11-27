#!/bin/bash
# Firestore Database & Index Deployment Script
# This script deploys indexes and security rules after database creation

set -e  # Exit on any error

echo ""
echo "============================================================"
echo "  Firestore Database Setup & Index Deployment"
echo "============================================================"
echo ""

# Colors for output
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
RED='\033[0;31m'
CYAN='\033[0;36m'
NC='\033[0m' # No Color

# Check if Firebase CLI is installed
if ! command -v firebase &> /dev/null; then
    echo -e "${RED}❌ Firebase CLI not found!${NC}"
    echo "Install it with: npm install -g firebase-tools"
    exit 1
fi

echo -e "${GREEN}✓ Firebase CLI found${NC}"
echo ""

# Check if user is logged in
echo -e "${CYAN}Checking Firebase authentication...${NC}"
if ! firebase login:list &> /dev/null; then
    echo -e "${YELLOW}Please login to Firebase...${NC}"
    firebase login
fi

echo -e "${GREEN}✓ Authenticated${NC}"
echo ""

# Check if database exists
echo -e "${CYAN}Checking if Firestore database exists...${NC}"
if firebase firestore:indexes 2>&1 | grep -q "does not exist"; then
    echo -e "${RED}❌ Database does not exist yet!${NC}"
    echo ""
    echo -e "${YELLOW}Please create the database first:${NC}"
    echo "1. Visit: https://console.firebase.google.com/project/bravebrain-59cdc/firestore"
    echo "2. Click 'Create database'"
    echo "3. Choose 'Production mode'"
    echo "4. Select location (e.g., us-central1)"
    echo "5. Click 'Enable' and wait for provisioning"
    echo ""
    echo "After database is created, run this script again."
    exit 1
fi

echo -e "${GREEN}✓ Database exists${NC}"
echo ""

# Deploy indexes
echo -e "${CYAN}Step 1: Deploying Firestore indexes...${NC}"
echo "This will deploy 6 composite indexes:"
echo "  1. appUsage (userId + date)"
echo "  2. analytics (userId + date)"
echo "  3. notifications (userId + sentAt)"
echo "  4. feedback (userId + timestamp)"
echo "  5. gamification (userId + lastUpdated)"
echo "  6. appUsage (userId + packageName + date)"
echo ""

if firebase deploy --only firestore:indexes; then
    echo ""
    echo -e "${GREEN}✓ Indexes deployed successfully!${NC}"
    echo ""
else
    echo -e "${RED}❌ Index deployment failed${NC}"
    exit 1
fi

# Deploy security rules
echo -e "${CYAN}Step 2: Deploying Firestore security rules...${NC}"
echo ""

if firebase deploy --only firestore:rules; then
    echo ""
    echo -e "${GREEN}✓ Security rules deployed successfully!${NC}"
    echo ""
else
    echo -e "${RED}❌ Security rules deployment failed${NC}"
    exit 1
fi

# Check index status
echo -e "${CYAN}Step 3: Checking index build status...${NC}"
echo ""

firebase firestore:indexes

echo ""
echo -e "${YELLOW}Note: Indexes may show 'BUILDING' status initially.${NC}"
echo -e "${YELLOW}They will change to 'READY' within a few minutes.${NC}"
echo ""

# Success summary
echo "============================================================"
echo -e "${GREEN}✅ DEPLOYMENT COMPLETE!${NC}"
echo "============================================================"
echo ""
echo "What was deployed:"
echo "  ✓ 6 composite indexes"
echo "  ✓ Security rules for 7 collections"
echo ""
echo "Next steps:"
echo "  1. Wait for indexes to reach 'READY' status (check with: firebase firestore:indexes)"
echo "  2. Test your app to verify data sync works"
echo "  3. Check Firebase Console to see synced data"
echo ""
echo "Firebase Console: https://console.firebase.google.com/project/bravebrain-59cdc/firestore"
echo ""
