const { onValueWritten } = require("firebase-functions/v2/database");
const admin = require("firebase-admin");

if (!admin.apps.length) {
  admin.initializeApp();
}

// 심박수 데이터가 업데이트될 때 통계 데이터를 계산하는 Firebase 함수
exports.stepsFunctions = onValueWritten({ ref: "/USER/LiveData/steps" }, async (event) => {
    console.log("Triggered: Steps data change detected.");

    // 변경된 데이터 확인
    const change = event.data;
    if (!change.after.exists()) {
        console.log("No data available after change.");
        return null; // 데이터가 없으면 종료
    }

    const currentSteps = change.after.val();
    console.log("Current steps:", currentSteps);

    // 날짜 정보 설정

    const getKoreanTime = () => {
      const now = new Date();
      const koreanTime = new Date(now.toLocaleString("en-US", { timeZone: "Asia/Seoul" }));
      return koreanTime;
    };
  
    const koreanTime = getKoreanTime();

    const year = koreanTime.getFullYear().toString();
    const month = (koreanTime.getMonth() + 1).toString().padStart(2, '0');
    const day = koreanTime.getDate().toString().padStart(2, '0');
    const hour = koreanTime.getHours().toString();
    const week = "week"+Math.ceil(koreanTime.getDate() / 7).toString();

    console.log("Date info:", { year, month, day, hour, week });

    // 통계를 저장할 경로 지정
    const statsRef = admin.database().ref(`/USER/StatsData`);

    const dailyStatsRef = statsRef.child(`steps/${year}/${month}/${week}/${year}-${month}-${day}`);
    const dailySnapshot = await dailyStatsRef.once('value');
    const previousDailyData = dailySnapshot.val() || { "Daily steps": 0 };
    const previousDailySteps = Number(previousDailyData["Daily steps"] || 0); // 숫자로 변환
    const differenceSteps = currentSteps - previousDailySteps;

    await dailyStatsRef.update({ "Daily steps": currentSteps });
    console.log("Daily stats updated:", { "Daily steps": currentSteps });

    // 시간별 통계 업데이트
    const hourlyStatsRef = statsRef.child(`steps/${year}/${month}/${week}/${year}-${month}-${day}/${hour}`);
    const hourlySnapshot = await hourlyStatsRef.once('value');
    const previousHourlyData = hourlySnapshot.val() || { "Hourly steps": 0 };
    const previousHourlySteps = Number(previousHourlyData["Hourly steps"] || 0); // 숫자로 변환

    const updatedHourlySteps = previousHourlySteps + differenceSteps;
    await hourlyStatsRef.update({ "Hourly steps": updatedHourlySteps });
    console.log("Hourly stats updated:", { "Hourly steps": updatedHourlySteps });

    // 주간 통계 업데이트
    const weeklyStatsRef = statsRef.child(`steps/${year}/${month}/${week}`);
    const weeklySnapshot = await weeklyStatsRef.once('value');
    const previousWeeklyData = weeklySnapshot.val() || { "Weekly steps": 0 };
    const previousWeeklySteps = Number(previousWeeklyData["Weekly steps"] || 0); // 숫자로 변환

    const updatedWeeklySteps = previousWeeklySteps + differenceSteps;
    await weeklyStatsRef.update({ "Weekly steps": updatedWeeklySteps });
    console.log("Weekly stats updated:", { "Weekly steps": updatedWeeklySteps });

    // 월간 통계 업데이트
    const monthlyStatsRef = statsRef.child(`steps/${year}/${month}`);
    const monthlySnapshot = await monthlyStatsRef.once('value');
    const previousMonthlyData = monthlySnapshot.val() || { "Monthly steps": 0 };
    const previousMonthlySteps = Number(previousMonthlyData["Monthly steps"] || 0); // 숫자로 변환

    const updatedMonthlySteps = previousMonthlySteps + differenceSteps;
    await monthlyStatsRef.update({ "Monthly steps": updatedMonthlySteps });
    console.log("Monthly stats updated:", { "Monthly steps": updatedMonthlySteps });
});