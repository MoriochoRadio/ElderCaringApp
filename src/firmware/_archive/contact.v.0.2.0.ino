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

// 움직임 및 심박수 관련 변수
unsigned long stepsCount = 0;
int prev_val;
unsigned long last_millis;
int steps = 0;
int totalSteps = 0;
int weeklySteps = 0;
int monthlySteps = 0;
int yearlySteps = 0;

// 심박수 통계 변수
int heartRate = 0;
int totalHeartRate = 0;
int minHeartRate = 1000;
int maxHeartRate = 0;
int averageHeartRate = 0;
int heartRateCount = 0;

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
  totalSteps += steps;
  weeklySteps += steps;
  monthlySteps += steps;
  yearlySteps += steps;
  Serial.println("Steps: " + String(steps) + " Total Steps: " + String(totalSteps));
}

// 심박수 측정 함수
void measureHeartRate() {
  heartRate = random(60, 100);  // 예제용 랜덤 심박수 생성
  
  // 심박수 통계 업데이트
  totalHeartRate += heartRate;
  heartRateCount++;
  averageHeartRate = totalHeartRate / heartRateCount;
  if (heartRate < minHeartRate) minHeartRate = heartRate;
  if (heartRate > maxHeartRate) maxHeartRate = heartRate;
}

// 실시간 데이터 전송 함수
void sendLiveDataToFirebase(){
  String path = "/USER/LiveData";
  FirebaseJson liveJson;
  liveJson.set("steps", steps);
  liveJson.set("heartRate", heartRate);

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
  stepsJson.clear();
  stepsJson.set("monthlyData/" + month + "/monthlyTotal", monthlySteps);
  stepsJson.set("monthlyData/" + month + "/weeklyData/" + week + "/weeklyTotal", weeklySteps);
  stepsJson.set("monthlyData/" + month + "/weeklyData/" + week + "/dailyData/" + day + "/steps", totalSteps);
  stepsJson.set("yearlyTotal", yearlySteps);

  // 심박수 데이터 구조 설정
  heartRateJson.clear();
  heartRateJson.set("monthlyData/" + month + "/monthlyAverage", averageHeartRate);
  heartRateJson.set("monthlyData/" + month + "/weeklyData/" + week + "/weeklyAverage", averageHeartRate);
  heartRateJson.set("monthlyData/" + month + "/weeklyData/" + week + "/dailyData/" + day + "/average", averageHeartRate);
  heartRateJson.set("monthlyData/" + month + "/weeklyData/" + week + "/dailyData/" + day + "/max", maxHeartRate);
  heartRateJson.set("monthlyData/" + month + "/weeklyData/" + week + "/dailyData/" + day + "/min", minHeartRate);
  heartRateJson.set("yearlyAverage", averageHeartRate);

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