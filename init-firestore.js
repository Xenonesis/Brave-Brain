/**
 * Firebase Firestore Collection Initializer
 * 
 * This script creates all required collections in Firestore with sample data.
 * Run with: node init-firestore.js
 */

const { initializeApp, cert, getApps } = require('firebase-admin/app');
const { getFirestore, Timestamp } = require('firebase-admin/firestore');

// Initialize Firebase Admin with application default credentials
// This uses the credentials from `firebase login`
async function initializeCollections() {
    try {
        // Check if already initialized
        if (getApps().length === 0) {
            initializeApp({
                projectId: 'bravebrain-59cdc'
            });
        }

        const db = getFirestore();
        const now = Timestamp.now();
        const today = new Date().toISOString().split('T')[0]; // yyyy-MM-dd format
        const testUserId = 'init_test_user_' + Date.now();

        console.log('🚀 Initializing Firestore collections...\n');

        // 1. Create 'users' collection
        console.log('Creating users collection...');
        await db.collection('users').doc(testUserId).set({
            userId: testUserId,
            email: 'test@bravebrain.app',
            displayName: 'Test User',
            createdAt: now,
            lastSyncAt: now,
            preferences: {
                theme: 'system',
                notifications: true,
                initialized: true
            }
        });
        console.log('✓ users collection created');

        // 2. Create 'analytics' collection
        console.log('Creating analytics collection...');
        await db.collection('analytics').doc(`${testUserId}_${today}`).set({
            userId: testUserId,
            date: today,
            totalScreenTimeMs: 0,
            productivityScore: 0,
            blockedAttempts: 0,
            challengesCompleted: 0,
            challengesFailed: 0,
            usagePatterns: {
                initialized: true,
                lastSyncTime: Date.now()
            },
            timestamp: now
        });
        console.log('✓ analytics collection created');

        // 3. Create 'gamification' collection
        console.log('Creating gamification collection...');
        await db.collection('gamification').doc(testUserId).set({
            userId: testUserId,
            points: 0,
            level: 1,
            badges: [],
            challenges: {
                dailyStreak: 0,
                challengeStreak: 0,
                productivityStreak: 0,
                initialized: true
            },
            achievements: [],
            lastUpdated: now
        });
        console.log('✓ gamification collection created');

        // 4. Create 'notifications' collection
        console.log('Creating notifications collection...');
        await db.collection('notifications').add({
            userId: testUserId,
            type: 'system',
            title: 'Database Initialized',
            message: 'Firestore collections have been set up successfully!',
            sentAt: now,
            wasClicked: false,
            wasDismissed: false,
            effectiveness: 0.0,
            context: { source: 'init_script' }
        });
        console.log('✓ notifications collection created');

        // 5. Create 'appUsage' collection
        console.log('Creating appUsage collection...');
        await db.collection('appUsage').doc(`${testUserId}_com.bravebrain_${today}`).set({
            userId: testUserId,
            packageName: 'com.bravebrain',
            appName: 'Brave Brain',
            usageTimeMs: 0,
            dailyLimitMs: 0,
            category: 'productivity',
            date: today,
            timestamp: now
        });
        console.log('✓ appUsage collection created');

        // 6. Create 'feedback' collection
        console.log('Creating feedback collection...');
        await db.collection('feedback').add({
            userId: testUserId,
            feedbackType: 'system_init',
            rating: 5,
            comment: 'Database initialized via script',
            context: { source: 'init_script', version: '1.0' },
            timestamp: now
        });
        console.log('✓ feedback collection created');

        // 7. Create 'userEngagement' collection (mentioned in security rules)
        console.log('Creating userEngagement collection...');
        await db.collection('userEngagement').doc(testUserId).set({
            userId: testUserId,
            totalSessions: 0,
            totalScreenTime: 0,
            lastActiveAt: now,
            initialized: true
        });
        console.log('✓ userEngagement collection created');

        console.log('\n✅ All Firestore collections initialized successfully!');
        console.log('\n📌 Collections created:');
        console.log('   • users');
        console.log('   • analytics');
        console.log('   • gamification');
        console.log('   • notifications');
        console.log('   • appUsage');
        console.log('   • feedback');
        console.log('   • userEngagement');
        console.log('\n🔗 View in Firebase Console:');
        console.log('   https://console.firebase.google.com/project/bravebrain-59cdc/firestore/data\n');

    } catch (error) {
        console.error('❌ Error initializing collections:', error.message);
        console.error('\nMake sure you have run: firebase login');
        process.exit(1);
    }
}

initializeCollections();
