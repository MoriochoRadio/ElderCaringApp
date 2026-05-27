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

#define ssid "<YOUR_WIFI_SSID>"
#define password "<YOUR_WIFI_PASSWORD>"
#define firebaseHost "wearable-e1302-default-rtdb.firebaseio.com"
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
int step = 0;

unsigned long preMillis = 0;
const long interval = 3600000;

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
}

void loop() {
  unsigned long currentMillis = millis();

  Serial.println(ESP.getFreeHeap());
  int16_t ax, ay, az;
  mpu.getAcceleration(&ax, &ay, &az);
  getFormattedTime();

  // MPU6050 연결 확인 및 가속도 데이터 가져오기
  if (!mpu.testConnection()) {
    Serial.println("MPU6050 연결 끊김. 재연결 시도 중...");
    connectToMPU6050();
  } else {
    mpu.getAcceleration(&ax, &ay, &az);
  }

  // 새로운 날짜로 전환되면 stepCount 초기화
  if (currentDay != day) {
    currentDay = day; // 현재 날짜 업데이트
    stepCount = 0;    // stepCount 초기화
  }

  // 가속도 데이터 기반 스텝 카운트 계산
  int val = ((ax * ax) + (ay * ay) + (az * az)) / 1000;

  if ((val > 100) && (prev_val <= 100) && (millis() - last_millis > 300)) {
    stepCount++;
    last_millis = millis();
  }
  prev_val = val;

  // 스텝 수 계산 및 출력
  step = stepCount / 2;
  Serial.println("Step count : " + String(stepCount));

  // 일정 시간 간격으로 Firebase에 데이터 전송
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

void getFormattedTime() {
  timeClient.update();  // NTP 시간을 업데이트합니다.
  String formattedTime = timeClient.getFormattedTime();  // 시간을 HH:MM:SS 형식으로 가져옵니다.
  
  // 현재 날짜를 계산하기 위해 epoch time을 가져옵니다.
  unsigned long epochTime = timeClient.getEpochTime();
  time_t rawTime = (time_t)epochTime;
  struct tm *timeInfo = gmtime(&rawTime);

  char dayBuffer[11];
  strftime(dayBuffer, sizeof(dayBuffer), "%Y-%m-%d", timeInfo);
  day = String(dayBuffer);

  Serial.println("현재 날짜: " + day);
  Serial.println("현재 시간: " + formattedTime);
}


void sendStatsDataToFirebase() {
  String path = "/USER/LiveData";
  FirebaseJson liveJson;

  liveJson.set("steps", step);  // 스텝 데이터 추가

  if (Firebase.updateNode(firebaseData, path, liveJson)) {
    Serial.println("Live data updated successfully!");
  } else {
    Serial.print("Error updating live data: ");
    Serial.println(firebaseData.errorReason());
  }
}
