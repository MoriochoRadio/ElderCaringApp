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
#define firebaseHost "wearable-e1302-default-rtdb.firebaseio.com"
#define firebaseAuth "<FIREBASE_AUTH_SECRET>"
//#define firebaseHost "wearable-dataset-default-rtdb.firebaseio.com"
//#define firebaseAuth "ADXySkfRTR2FoogN0hkLjmrkq0ajRJonlpSnOZq0"

FirebaseData firebaseData;
FirebaseConfig config;
FirebaseAuth auth;

WiFiUDP ntpUDP;
NTPClient timeClient(ntpUDP, "pool.ntp.org", 32400);  // UTC +9 시간대 설정

MPU6050 mpu;
int prev_val = 0;
unsigned long last_millis = 0;
int step;
int stepCount = 0;

MAX30105 particleSensor;
// SPO2 계산을 위한 변수 설정
#define BUFFER_SIZE 100  // 데이터 버퍼 크기
uint32_t irBuffer[BUFFER_SIZE]; // 적외선 신호 버퍼
uint32_t redBuffer[BUFFER_SIZE]; // 적색 신호 버퍼
int32_t bufferLength; // 버퍼 길이
int32_t spo2; // 산소포화도 값
int8_t validSPO2; // SPO2 유효성
int32_t heartRate; // 심박수 값
int8_t validHeartRate; // 심박수 유효성

unsigned long preMillis = 0;
const long interval = 1000;

String day = "";
String currentDay = ""; 

void setup() {
  Serial.begin(115200);
  connectToWiFi();
  timeClient.begin();
  Wire.begin(21, 22);
  config.host = firebaseHost;
  config.signer.tokens.legacy_token = firebaseAuth;
  connectToFirebase();
  connectToMPU6050();
  connectToMAX30102();

}

void loop() {
  unsigned long currentMillis = millis();

  Serial.println(ESP.getFreeHeap());
  int16_t ax, ay, az;
  mpu.getAcceleration(&ax, &ay, &az);
  getFormattedTime();

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


  if (currentDay != day) {
    currentDay = day; // 현재 날짜 업데이트
    stepCount = 0;    // stepCount 초기화
  }

  int val = ((ax * ax) + (ay * ay) + (az * az)) / 1000;

  if ((val > 100) && (prev_val <= 100) && (millis() - last_millis > 300)) {
    stepCount++;
    last_millis = millis();
  }
  prev_val = val;
  Serial.println("Step count : " + String(stepCount) );
  step = stepCount / 2;
  
  bufferLength = BUFFER_SIZE;
  for (int i = 0; i < bufferLength; i++) {
    while (particleSensor.available() == false)
      particleSensor.check();
      
    redBuffer[i] = particleSensor.getRed();
    irBuffer[i] = particleSensor.getIR();
    particleSensor.nextSample();
  }

  maxim_heart_rate_and_oxygen_saturation(irBuffer, bufferLength, redBuffer, &spo2, &validSPO2, &heartRate, &validHeartRate);
  if (validSPO2 && validHeartRate) {
    Serial.print("산소포화도(SPO2): ");
    Serial.print(spo2);
    Serial.print("%, 심박수: ");
    Serial.print(heartRate);
    Serial.println(" bpm");
  } else {
    Serial.println("유효한 측정값을 얻지 못했습니다. 다시 측정 중...");
  }
  if (currentMillis - preMillis >= interval) {
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

void getFormattedTime() {
  timeClient.update();
  String formattedDate = timeClient.getFormattedDate();
  int splitT = formattedDate.indexOf("T");
  day = formattedDate.substring(0, splitT);
}

void sendStatsDataToFirebase() {
  String path = "/USER/LiveData";
  FirebaseJson liveJson;
  FirebaseJson stepsJson;

  liveJson.set("steps", step);
  liveJson.set("spO2", spo2);
  liveJson.set("heartRate", heartRate);

  if (Firebase.updateNode(firebaseData, path, liveJson)) {
    Serial.println("Live data updated successfully!");
  } else {
    Serial.print("Error updating live data: ");
    Serial.println(firebaseData.errorReason());
  }
}
