// ============================================================
// ★ 보안 안내 (Phase C 정리 시 추가):
//   본 .ino 의 WiFi/Firebase 자격증명은 학습용 더미입니다.
//   실제 사용 시 secrets.h 또는 환경 변수로 분리하세요.
//   원본 시크릿은 GitHub push 전 마스킹되었으며,
//   Firebase 콘솔에서 무효화 완료되었습니다.
// ============================================================

#include <FirebaseESP32.h>
#include <Wire.h>
#include <MPU6050.h> 
#include <WiFiUdp.h>
#include <NTPClient.h>
#include "MAX30105.h"
#include "spo2_algorithm.h"

#define ssid "<YOUR_WIFI_SSID>"
#define password "<YOUR_WIFI_PASSWORD>"
#define firebaseHost "wearable-dataset-default-rtdb.firebaseio.com"
#define firebaseAuth "<FIREBASE_AUTH_SECRET>"

FirebaseData firebaseData;
FirebaseConfig config;
FirebaseAuth auth;

WiFiUDP ntpUDP;
NTPClient timeClient(ntpUDP, "pool.ntp.org", 32400);  // UTC +9 시간대 설정

MPU6050 mpu;
int prev_val = 0;
unsigned long last_millis = 0;
int stepCount = 0;
int step;
int dailySteps;
int hourlySteps;
int weeklySteps;
int monthlySteps;

MAX30105 particleSensor;
// SPO2 계산을 위한 변수 설정
#define BUFFER_SIZE 25  // 데이터 버퍼 크기
uint32_t irBuffer[BUFFER_SIZE]; // 적외선 신호 버퍼
uint32_t redBuffer[BUFFER_SIZE]; // 적색 신호 버퍼
int32_t bufferLength; // 버퍼 길이
int32_t spo2; // 산소포화도 값
int8_t validSPO2; // SPO2 유효성
int32_t heartRate; // 심박수 값
int8_t validHeartRate; // 심박수 유효성

unsigned long preMillis = 0;
const long interval = 5000;

int dayCount = 0;
String year, month, day, hour, week;

void setup() {
  Serial.begin(115200);
  connectToWiFi();
  timeClient.begin();
  Wire.begin(21, 22);
  config.host = firebaseHost;
  config.signer.tokens.legacy_token = firebaseAuth;
  connectToFirebase();
}

void loop() {
  int16_t ax, ay, az;
  unsigned long currentMillis = millis();
  getFormattedTime(year, month, day, hour, week);
  //updateAndRetrieveTime();

  if (!mpu.testConnection()) {
    Serial.println("MPU6050 연결 끊김. 재연결 시도 중...");
    connectToMPU6050();
  } else {
    mpu.getAcceleration(&ax, &ay, &az);
  }

  if (!particleSensor.check()) {
    Serial.println("MAX30102 센서 연결 끊김. 재연결 시도 중...");
    connectToMAX30102();
  } else {
    bufferLength = BUFFER_SIZE;
    for (int i = 0; i < bufferLength; i++) {
      while (particleSensor.available() == false)
        particleSensor.check();
      
      redBuffer[i] = particleSensor.getRed();
      irBuffer[i] = particleSensor.getIR();
      particleSensor.nextSample();
    }
  }

  int val = ((ax * ax) + (ay * ay) + (az * az)) / 1000;

  if ((val > 100) && (prev_val <= 100) && (millis() - last_millis > 300)) {
    stepCount++;
    last_millis = millis();
  }

  // 산소포화도와 심박수 계산
  maxim_heart_rate_and_oxygen_saturation(irBuffer, bufferLength, redBuffer, &spo2, &validSPO2, &heartRate, &validHeartRate);

  prev_val = val;
  Serial.println("Step count : " + String(stepCount) );
  step = stepCount / 2;

  if (currentMillis - preMillis >= interval) {
    preMillis = currentMillis;
    sendStatsDataToFirebase();
  }
}

void connectToWiFi() {
  WiFi.begin(ssid, password);
  while (WiFi.status() != WL_CONNECTED) {
    delay(1000);
    Serial.println("Connecting to WiFi...");
  }
  Serial.println("~~~~~Connected to WiFi~~~~~");
}

void connectToFirebase() {
  Firebase.begin(&config, &auth);
  Firebase.reconnectWiFi(true);
  firebaseData.setResponseSize(1024);
  Serial.println("~~~~~Connected to Firebase~~~~~");
}

void connectToMPU6050() {
  while (true) {
    mpu.initialize();
    if (mpu.testConnection()) {
      Serial.println("MPU6050 연결 성공!");
      break;
    } else {
      Serial.println("MPU6050 연결 실패! 재시도 중...");
      delay(1000);
    }
  }
}

void connectToMAX30102() {
  if (!particleSensor.begin(Wire, I2C_SPEED_FAST)) {
    Serial.println("MAX30102 센서 초기화 실패");
    while (1);
  }
  Serial.println("MAX30102 센서 초기화 성공");

  particleSensor.setup();  // 기본 설정으로 센서 작동
  particleSensor.setPulseAmplitudeRed(0x0A); // 적색 LED 설정
  particleSensor.setPulseAmplitudeIR(0x0A);  // 적외선 LED 설정
}

void getFormattedTime(String &year, String &month, String &day, String &hour, String &week) {
  timeClient.update();
  String formattedDate = timeClient.getFormattedDate();
  int splitT = formattedDate.indexOf("T");
  String dayStamp = formattedDate.substring(0, splitT);
  String timeStamp = formattedDate.substring(splitT + 1, formattedDate.length() - 1);

  year = dayStamp.substring(0, 4);
  month = dayStamp.substring(5, 7);
  day = dayStamp.substring(8, 10);
  hour = timeStamp.substring(0, 2);
  int w = ((day.toInt() - 1) / 7) + 1; 
  week = "week" + String(w);
}

void sendStatsDataToFirebase() {
  String stepsPath = "/USER/StatsData/steps/" + year;
  String path = "/USER/LiveData";
  FirebaseJson liveJson;
  FirebaseJson stepsJson;

  // Firebase에서 기존 데이터 가져오기
  int currentMonthlySteps = Firebase.getInt(firebaseData, stepsPath + "/" + month + "/Monthly steps") ? firebaseData.intData() : 0;
  int currentWeeklySteps = Firebase.getInt(firebaseData, stepsPath + "/" + month + "/" + week + "/Weekly steps") ? firebaseData.intData() : 0;
  int currentDailySteps = Firebase.getInt(firebaseData, stepsPath + "/" + month + "/" + week + "/" + day + "/Daily steps") ? firebaseData.intData() : 0;
  int currentHourlySteps = Firebase.getInt(firebaseData, stepsPath + "/" + month + "/" + week + "/" + day + "/" + hour + "/Hourly steps") ? firebaseData.intData() : 0;
  
  liveJson.set("steps", currentDailySteps + step);
  liveJson.set("spo2", spo2);
  liveJson.set("Heart rate", heartRate);

  if (Firebase.updateNode(firebaseData, path, liveJson)) {
    Serial.println("Live data updated successfully!");
  } else {
    Serial.print("Error updating live data: ");
    Serial.println(firebaseData.errorReason());
  }
  // 새로운 데이터를 기존 데이터에 추가하여 병합
  stepsJson.add("/" + month + "/Monthly steps", currentMonthlySteps + step);
  stepsJson.add("/" + month + "/" + week + "/Weekly steps", currentWeeklySteps + step);
  stepsJson.add("/" + month + "/" + week + "/" + day + "/Daily steps", currentDailySteps + step);
  stepsJson.add("/" + month + "/" + week + "/" + day + "/" + hour + "/Hourly steps", currentHourlySteps + step);

  // 병합된 데이터를 Firebase에 업로드
  if (Firebase.updateNode(firebaseData, stepsPath, stepsJson)) {
    Serial.println("Steps data updated in Firebase successfully!");
  } else {
    Serial.print("Error updating steps data in Firebase: ");
    Serial.println(firebaseData.errorReason());
  }
}
