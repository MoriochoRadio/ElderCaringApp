// ============================================================
// ★ 보안 안내 (Phase C 정리 시 추가):
//   본 .ino 의 WiFi/Firebase 자격증명은 학습용 더미입니다.
//   실제 사용 시 secrets.h 또는 환경 변수로 분리하세요.
//   원본 시크릿은 GitHub push 전 마스킹되었으며,
//   Firebase 콘솔에서 무효화 완료되었습니다.
// ============================================================

#include <FirebaseESP32.h>         // Firebase 라이브러리 포함
#include <WiFiUdp.h>               // Wi-Fi UDP 라이브러리 포함
#include <NTPClient.h>             // NTP 클라이언트 라이브러리 포함
#include <Wire.h>
#include <MPU6050.h>

// Wi-Fi 및 Firebase 설정
#define ssid "<YOUR_WIFI_SSID>"
#define password "<YOUR_WIFI_PASSWORD>"
#define firebaseHost "monitoring-a6b20-default-rtdb.firebaseio.com"
#define firebaseAuth "<FIREBASE_AUTH_SECRET>"

// Firebase 데이터 객체 및 구성 설정
FirebaseData firebaseData;
FirebaseConfig config;
FirebaseAuth auth;
FirebaseJson stepsJson;
FirebaseJson heartRateJson;

// NTP 클라이언트 객체 및 구성 설정
WiFiUDP ntpUDP;
NTPClient timeClient(ntpUDP);

// 시간 및 날짜 관련 변수
String formattedDate;
String dayStamp;
String timeStamp;

// MPU6050 객체 및 구성 설정
MPU6050 mpu;

// 걸음 수를 저장하는 변수
unsigned long stepsCount = 0;  // 측정된 걸음 수 (실제 걸음 수 = 측정된 걸음 수 / 2)
int prev_val;  // 마지막으로 측정된 걸음
unsigned long last_millis;  // 마지막으로 측정된 시간 (밀리초 단위)
int steps = 0;  // 현재 걸음 수
int dailySteps = 0;  // 총 걸음 수
int hourlySteps = 0;  // 시간별 걸음 수
int weeklySteps = 0;  // 주간별 걸음 수
int monthlySteps = 0;  // 월간별 걸음 수
int yearlySteps = 0;  // 연간별 걸음 수

// 심박수 관련 통계 변수
int heartRate = 0;  // 현재 심박수
int totalHeartRate = 0;  // 누적된 심박수
int minHeartRate = 1000;  // 최소 심박수 (초기값은 매우 높은 값으로 설정)
int maxHeartRate = 0;  // 최대 심박수 (초기값은 0)
int aveHeartRate = 0;  // 평균 심박수
int heartRateCount = 0;  // 심박수 측정 횟수
int hourlyMinHeartRate = 0;  // 매시간의 최소 심박수
int hourlyMaxHeartRate = 0;  // 매시간의 최대 심박수
int hourlyAveHeartRate = 0;  // 매시간의 평균 심박수
int dailyAveHeartRate = 0;
int dailyMinHeartRate = 0;
int dailyMaxHeartRate = 0;
int weeklyMinHeartRate = 0;  // 주간 최소 심박수
int weeklyMaxHeartRate = 0;  // 주간 최대 심박수
int weeklyAveHeartRate = 0;  // 주간 평균 심박수
int monthlyMinHeartRate = 0;  // 월간 최소 심박수
int monthlyMaxHeartRate = 0;  // 월간 최대 심박수
int monthlyAveHeartRate = 0;  // 월간 평균 심박수0
int yearlyMinHeartRate = 0;  // 월간 최소 심박수
int yearlyMaxHeartRate = 0;  // 월간 최대 심박수
int yearlyAveHeartRate = 0;  // 월간 평균 심박수

void setup() {
  Serial.begin(115200);
  connectToWiFi();
  timeClient.begin();
  Wire.begin(21, 22);
  timeClient.setTimeOffset(32400);
  config.host = firebaseHost;
  config.signer.tokens.legacy_token = firebaseAuth;
  connectToFirebase();
  connectToMPU6050();
}

void loop() {
  sendLiveDataToFirebase();
  measureSteps();
  measureHeartRate();
  sendStatsDataToFirebase();
  delay(60000);  // 1분 간격으로 데이터 전송
}

// Wi-Fi 연결 함수
void connectToWiFi() {
  WiFi.begin(ssid, password);
  while (WiFi.status() != WL_CONNECTED) {
    delay(1000);
    Serial.println("Connecting to WiFi...");
  }
  Serial.println("~~~~~Connected to WiFi~~~~~");
}

// Firebase 연결 함수
void connectToFirebase() {
  Firebase.begin(&config, &auth);
  Firebase.reconnectWiFi(true);
  firebaseData.setResponseSize(1024);
  Serial.println("~~~~~Connected to Firebase~~~~~");
}

// MPU6050 연결 함수
void connectToMPU6050(){
  if (!mpu.testConnection()) {
    Serial.println("MPU6050 연결 실패!");
    while (1);
  } else {
    Serial.println("MPU6050 연결 성공!");
  }
}

// 걸음 측정 함수
void measureSteps() {
  int16_t ax, ay, az;
  mpu.getAcceleration(&ax, &ay, &az);

  int val = (ax / 1000) * (ax / 1000) + (ay / 1000) * (ay / 1000) + (az / 1000) * (az / 1000);

  if ((val > 500) && (prev_val <= 500)) {
    if ((millis() - last_millis) > 300) {
      stepsCount++;
      last_millis = millis();
    }
  }
  prev_val = val;

  steps = stepsCount / 2;
  dailySteps += steps;
  weeklySteps += steps;
  monthlySteps += steps;
  yearlySteps += steps;
  Serial.println("Steps: " + String(steps) + " Total Steps: " + String(dailySteps));
}

// 심박수 측정 함수
void measureHeartRate() {
  heartRate = random(60, 100);  // 예제용 랜덤 심박수 생성
  
  // 심박수 통계 업데이트
  totalHeartRate += heartRate;
  heartRateCount++;
  aveHeartRate = totalHeartRate / heartRateCount;
  if (heartRate < minHeartRate) minHeartRate = heartRate;
  if (heartRate > maxHeartRate) maxHeartRate = heartRate;
}

// 실시간 데이터 전송 함수
void sendLiveDataToFirebase(){
  String path = "/USER/LiveData";
  FirebaseJson liveJson;
  liveJson.set("steps", steps);
  liveJson.set("heartRate", totalHeartRate);

  if (Firebase.updateNode(firebaseData, path, liveJson)) {
    Serial.println("Live data updated successfully!");
  } else {
    Serial.print("Error updating live data: ");
    Serial.println(firebaseData.errorReason());
  }
}

// 통계 데이터 전송 함수
void sendStatsDataToFirebase() {
  while (!timeClient.update()) {         
    timeClient.forceUpdate();            
  }

  formattedDate = timeClient.getFormattedDate(); 
  int splitT = formattedDate.indexOf("T"); 
  dayStamp = formattedDate.substring(0, splitT);
  timeStamp = formattedDate.substring(splitT + 1, formattedDate.length() - 1);

  String year = dayStamp.substring(0, 4);
  String month = dayStamp.substring(5, 7);
  String day = dayStamp;
  String hour = timeStamp.substring(0, 2);
  String week = "week1";

  // 걸음 수 데이터 구조 설정
  stepsJson.set("/" + month + "/Monthly steps", monthlySteps);
  stepsJson.set("/" + month  + "/" + week + "/Weekly steps ", weeklySteps);
  stepsJson.set("/" + month + "/" + week + "/" + day + "/Daily steps", dailySteps);
  stepsJson.set("/" + month + "/" + week + "/" + day + "/" + hour  + "/Hourly steps", hourlySteps);

  // 심박수 데이터 구조 설정
  //월
  heartRateJson.set("/" + month + "/Monthly average", monthlyAveHeartRate);
  heartRateJson.set("/" + month + "/Monthly min", monthlyMinHeartRate);
  heartRateJson.set("/" + month + "/Monthly max", monthlyMaxHeartRate);
  //주
  heartRateJson.set("/" + month + "/" + week + "/Weekly average", weeklyAveHeartRate);
  heartRateJson.set("/" + month + "/" + week + "/Weekly min", weeklyMinHeartRate);
  heartRateJson.set("/" + month + "/" + week + "/Weekly max", weeklyMaxHeartRate);
  //일
  heartRateJson.set("/" + month + "/" + week + "/" + day + "/Daily average", dailyAveHeartRate);
  heartRateJson.set("/" + month + "/" + week + "/" + day + "/Daily min", dailyMinHeartRate);
  heartRateJson.set("/" + month + "/" + week + "/" + day + "/Daily max", dailyMaxHeartRate);
  //시간
  heartRateJson.set("/" + month + "/" + week + "/" + day + "/" + hour + "/Hourly average", hourlyAveHeartRate);
  heartRateJson.set("/" + month + "/" + week + "/" + day + "/" + hour + "/Hourly min", hourlyMinHeartRate);
  heartRateJson.set("/" + month + "/" + week + "/" + day + "/" + hour + "/Hourly max", hourlyMaxHeartRate);

  // Firebase에 걸음 수 데이터 업데이트
  String stepsPath = "/USER/StatsData/steps/" + year;
  if (Firebase.updateNode(firebaseData, stepsPath, stepsJson)) {
    Serial.println("Steps data updated in Firebase successfully!");
  } else {
    Serial.print("Error updating steps data in Firebase: ");
    Serial.println(firebaseData.errorReason());
  }

  // Firebase에 심박수 데이터 업데이트
  String heartRatePath = "/USER/StatsData/heartRate/" + year;
  if (Firebase.updateNode(firebaseData, heartRatePath, heartRateJson)) {
    Serial.println("Heart rate data updated in Firebase successfully!");
  } else {
    Serial.print("Error updating heart rate data in Firebase: ");
    Serial.println(firebaseData.errorReason());
  }
}