// ============================================================
// ★ 보안 안내 (Phase C 정리 시 추가):
//   본 .ino 의 WiFi/Firebase 자격증명은 학습용 더미입니다.
//   실제 사용 시 secrets.h 또는 환경 변수로 분리하세요.
//   원본 시크릿은 GitHub push 전 마스킹되었으며,
//   Firebase 콘솔에서 무효화 완료되었습니다.
// ============================================================

#include <FirebaseESP32.h>
#include <Wire.h>
#include <WiFiUdp.h>
#include <NTPClient.h>
#include "MAX30105.h"
#include "spo2_algorithm.h"

#define ssid "<YOUR_WIFI_SSID>"
#define password "<YOUR_WIFI_PASSWORD>"
#define firebaseHost "wearable-e1302-default-rtdb.firebaseio.com"
#define firebaseAuth "<FIREBASE_AUTH_SECRET>"
#define buttonPin 26 // 적절한 핀 번호로 설정
#define buzerPin 32
#define C 262 // 도
#define D 296 // 레
#define E 330 // 미
#define G 392 // 솔
#define A 440 // 라

int notes[12] = {G, G, A, A, G, G, E, G, E, D, E, C};

FirebaseData firebaseData;
FirebaseConfig config;
FirebaseAuth auth;

WiFiUDP ntpUDP;
NTPClient timeClient(ntpUDP, "pool.ntp.org", 32400);  // UTC +9 시간대 설정

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

unsigned long startTime = 0;
bool buttonPressed = false;
bool measuring = false;
int validMeasurements = 0;
int totalHeartRate = 0;

void setup() {
  Serial.begin(115200);
  connectToWiFi();
  timeClient.begin();
  config.host = firebaseHost;
  config.signer.tokens.legacy_token = firebaseAuth;
  connectToFirebase();
  connectToMAX30102();
  pinMode(buttonPin, INPUT_PULLUP);
  pinMode(buzerPin, OUTPUT);
  attachInterrupt(digitalPinToInterrupt(buttonPin), onButtonPress, FALLING);
  ledcSetup(0, 5000, 8); // 채널 0, 5kHz 주파수, 8비트 해상도
  ledcAttachPin(buzerPin, 0); // 채널 0에 buzerPin 연결
}

void loop() {
  if (buttonPressed && !measuring) {
    startMeasurement();
  }

  if (measuring) {
    if (millis() - startTime < 60000) { // 60초 측정
      measureHeartRateAndSPO2();
    } else {
      finishMeasurement();
    }
  }
}

void onButtonPress() {
  buttonPressed = true;
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

void startMeasurement() {
  if (!particleSensor.check()) {
    Serial.println("MAX30102 센서 연결 끊김. 재연결 시도 중...");
    connectToMAX30102();
  }
  buttonPressed = false;
  measuring = true;
  validMeasurements = 0;
  totalHeartRate = 0;
  startTime = millis();
  Serial.println("측정을 시작합니다.");
}

void measureHeartRateAndSPO2() {
  bufferLength = BUFFER_SIZE;
  for (int i = 0; i < bufferLength; i++) {
    while (particleSensor.available() == false)
      particleSensor.check();

    redBuffer[i] = particleSensor.getRed();
    irBuffer[i] = particleSensor.getIR();
    particleSensor.nextSample();
  }

  maxim_heart_rate_and_oxygen_saturation(irBuffer, bufferLength, redBuffer, &spo2, &validSPO2, &heartRate, &validHeartRate);
  if (heartRate >= 20 && heartRate <= 200) {
    validMeasurements++;
    totalHeartRate += heartRate;
    Serial.print("측정 중: 산소포화도(SPO2): ");
    Serial.print(spo2);
    Serial.print("%, 심박수: ");
    Serial.print(heartRate);
    Serial.println(" bpm");
  } else {
    Serial.println("유효한 측정값을 얻지 못했습니다. 다시 측정 중...");
  }
}

void finishMeasurement() {
  measuring = false;
  if (validMeasurements > 0) {
    int averageHeartRate = totalHeartRate / validMeasurements;
    sendStatsDataToFirebase(averageHeartRate);
    playMelody(notes, 12, 500);
  } else {
    Serial.println("유효한 측정값이 없어 데이터를 전송하지 않습니다.");
    playMelody(notes, 12, 500);
  }
}

void sendStatsDataToFirebase(int averageHeartRate) {
  String path = "/USER/LiveData";
  FirebaseJson liveJson;

  liveJson.set("heartRate", averageHeartRate);

  if (Firebase.updateNode(firebaseData, path, liveJson)) {
    Serial.println("Live data updated successfully!");
  } else {
    Serial.print("Error updating live data: ");
    Serial.println(firebaseData.errorReason());
  }
}

void playMelody(int* melody, int length, int noteDelay) {
  for (int i = 0; i < length; i++) {
    ledcWriteTone(0, melody[i]); 
    delay(noteDelay);          
    ledcWriteTone(0, 0);        
    delay(100);                
  }
}
