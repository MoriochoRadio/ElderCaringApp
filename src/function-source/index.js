const functions = require("firebase-functions");
const admin = require("firebase-admin");

if (!admin.apps.length) {
    // 로컬 환경과 배포 환경을 분리
    if (process.env.FUNCTIONS_EMULATOR) {
        // 로컬 환경 (serviceAccountKey.json 사용)
        const serviceAccount = require("./serviceAccountKey.json");
        admin.initializeApp({
            credential: admin.credential.cert(serviceAccount),
            databaseURL: "https://wearable-e1302-default-rtdb.firebaseio.com"
        });
    } else {
        // 배포 환경 (applicationDefault 사용)
        admin.initializeApp({
            credential: admin.credential.applicationDefault(),
            databaseURL: "https://wearable-e1302-default-rtdb.firebaseio.com"
        });
    }
}

// 함수 가져오기
const heartRateFunctions = require('./src/heartRateFunctions');
const stepsFunctions = require('./src/stepsFunctions');

exports.heartRateFunctions = heartRateFunctions.heartRateFunctions;
exports.stepsFunctions = stepsFunctions.stepsFunctions;
