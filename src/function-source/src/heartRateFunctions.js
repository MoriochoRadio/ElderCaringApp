const { onValueWritten } = require("firebase-functions/v2/database");
const admin = require("firebase-admin");

if (!admin.apps.length) {
  admin.initializeApp();
}

// 심박수 데이터가 업데이트될 때 통계 데이터를 계산하는 Firebase 함수
exports.heartRateFunctions = onValueWritten({ ref: "/USER/LiveData/heartRate" }, async (event) => {
    console.log("Triggered: Heart rate data change detected.");

    // change.after가 존재하는지 확인 후 처리
    const change = event.data;
    console.log("Full change after data:", change.after);
    if (!change.after) {
        console.log("No data available after change.");
        return null; // 데이터가 없을 경우 함수 종료
    }

    // 심박수 데이터 가져오기
    const heartRate = change.after.val(); // 심박수 값을 가져옴
    if (typeof heartRate !== "number" || heartRate <= 0) {
        console.log("Invalid heart rate value:", heartRate);
        return null; // 유효하지 않은 데이터가 있을 경우 함수 종료
    }

    console.log("Heart Rate Value:", heartRate);

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

    // 1. 시간별 통계 업데이트
    const hourlyStatsRef = statsRef.child(
        `heartRate/${year}/${month}/${week}/${year}-${month}-${day}/${hour}`
    );
    const hourlySnapshot = await hourlyStatsRef.once('value');
    const hourlyData = hourlySnapshot.val() || {
        "Hourly average": 0, "Hourly max": 0, "Hourly min": 0, "count": 0, "total": 0
    };

    hourlyData["Hourly max"] = Math.max(hourlyData["Hourly max"] || 0, heartRate);
    hourlyData["Hourly min"] = hourlyData["Hourly min"] === 0 ? heartRate : Math.min(hourlyData["Hourly min"], heartRate);
    hourlyData.total += heartRate;
    hourlyData.count += 1;
    hourlyData["Hourly average"] = hourlyData.total / hourlyData.count;

    await hourlyStatsRef.update(hourlyData);
    console.log("Hourly stats updated:", hourlyData);

    // 2. 일별 통계 업데이트
    const dailyStatsRef = statsRef.child(
        `heartRate/${year}/${month}/${week}/${year}-${month}-${day}`
    );
    const dailySnapshot = await dailyStatsRef.once('value');
    const dailyData = dailySnapshot.val() || { "Daily average": 0, "Daily max": 0, "Daily min": 0, "count": 0, "total": 0 };

    dailyData["Daily max"] = Math.max(dailyData["Daily max"] || 0, heartRate);
    dailyData["Daily min"] = dailyData["Daily min"] === 0 ? heartRate : Math.min(dailyData["Daily min"] , heartRate);
    dailyData.total += heartRate;
    dailyData.count += 1;
    dailyData["Daily average"] = dailyData.total / dailyData.count;

    await dailyStatsRef.set(dailyData);
    console.log("Daily stats updated:", dailyData);

    // 3. 주간 통계 업데이트
    const weeklyStatsRef = statsRef.child(
        `heartRate/${year}/${month}/${week}`
    );
    const weeklySnapshot = await weeklyStatsRef.once('value');
    const weeklyData = weeklySnapshot.val() || {
        "Weekly average": 0, "Weekly max": 0, "Weekly min": 0, "count": 0, "total": 0
    };

    weeklyData["Weekly max"] = Math.max(weeklyData["Weekly max"] || 0, heartRate);
    weeklyData["Weekly min"] = weeklyData["Weekly min"] === 0 ? heartRate : Math.min(weeklyData["Weekly min"], heartRate);
    weeklyData.total += heartRate;
    weeklyData.count += 1;
    weeklyData["Weekly average"] = weeklyData.total / weeklyData.count;

    await weeklyStatsRef.set(weeklyData);
    console.log("Weekly stats updated:", weeklyData);

    // 4. 월간 통계 업데이트
    const monthlyStatsRef = statsRef.child(
        `heartRate/${year}/${month}`
    );
    const monthlySnapshot = await monthlyStatsRef.once('value');
    const monthlyData = monthlySnapshot.val() || {
        "Monthly average": 0, "Monthly max": 0, "Monthly min": 0, "count": 0, "total": 0
    };

    monthlyData["Monthly max"] = Math.max(monthlyData["Monthly max"] || 0, heartRate);
    monthlyData["Monthly min"] = monthlyData["Monthly min"] === 0 ? heartRate : Math.min(monthlyData["Monthly min"], heartRate);
    monthlyData.total += heartRate;
    monthlyData.count += 1;
    monthlyData["Monthly average"] = monthlyData.total / monthlyData.count;

    await monthlyStatsRef.set(monthlyData);
    console.log("Monthly stats updated:", monthlyData);

    return null;
});
