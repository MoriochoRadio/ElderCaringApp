// ============================================================
// ★ 보안 안내 (Phase C 정리 시 추가):
//   본 .ino 의 WiFi/Firebase 자격증명은 학습용 더미입니다.
//   실제 사용 시 secrets.h 또는 환경 변수로 분리하세요.
//   원본 시크릿은 GitHub push 전 마스킹되었으며,
//   Firebase 콘솔에서 무효화 완료되었습니다.
// ============================================================

#include <FirebaseESP32.h>
#include "HX711.h"
#include <WiFi.h>
#include <time.h>

// WiFi 설정
#define WIFI_SSID "<YOUR_WIFI_SSID>"
#define WIFI_PASSWORD "<YOUR_WIFI_PASSWORD>"

// Firebase 설정
#define FIREBASE_HOST "monitoring-a6b20-default-rtdb.firebaseio.com"
#define FIREBASE_AUTH "<FIREBASE_AUTH_SECRET>"

// 로드셀 및 Firebase 객체 선언
HX711 scale;
FirebaseData firebaseData;
FirebaseConfig config;
FirebaseAuth auth;
FirebaseJson json;

// 로드셀 핀 설정
#define LOADCELL_DOUT_PIN 18
#define LOADCELL_SCK_PIN 19

// 무게 변화 감지 임계값 및 시간 설정
const float weightThreshold = 200.0;  // 앉아있다고 판단하는 무게 변화 임계값
const unsigned long minSitDuration = 15000; // 최소 15초 동안 앉아있어야 함 (밀리초)

// 전역 변수 선언
bool isUserSitting = false; // 현재 사용자가 앉아있는지 여부
unsigned long sittingStartTime = 0; // 앉기 시작 시간
unsigned long sittingEndTime = 0; // 일어난 시간
unsigned long sittingDuration = 0; // 앉아있던 총 시간

String startTime = ""; // 화장실 사용 시작 시간
String startDate = ""; // 화장실 사용 시작 날짜
String endTime = ""; // 화장실 사용 종료 시간
int count = 0;

unsigned long preLiveMillis = 0;
unsigned long preStatsMillis = 0;
const long intervalLive = 1000;

// WiFi 연결 함수
void connectToWiFi() {
  WiFi.begin(WIFI_SSID, WIFI_PASSWORD);
  while (WiFi.status() != WL_CONNECTED) {
    delay(1000);
    Serial.println("WiFi 연결 중...");
  }
  Serial.println("WiFi 연결 완료");
}

// Firebase 연결 함수
void connectToFirebase() {
  config.host = FIREBASE_HOST;
  config.signer.tokens.legacy_token = FIREBASE_AUTH;
  Firebase.begin(&config, &auth);
  Firebase.reconnectWiFi(true);
  Serial.println("Firebase 연결 완료");
}

// 시간 설정 (NTP 서버를 사용하여 대한민국 표준 시간으로 동기화)
void setupTime() {
  configTime(9 * 3600, 0, "pool.ntp.org", "time.nist.gov");
  struct tm timeInfo;
  while (!getLocalTime(&timeInfo)) {
    Serial.println("시간 동기화 대기 중...");
    delay(1000);
  }
  Serial.println("시간 동기화 완료");
}

// 시간 및 날짜를 문자열로 포맷하는 함수
void getFormattedTime(String &date, String &time) {
  struct tm timeInfo;
  if (getLocalTime(&timeInfo)) {
    char dateBuffer[11]; // YYYY/MM/DD 형식
    char timeBuffer[9];  // HH:MM:SS 형식
    strftime(dateBuffer, sizeof(dateBuffer), "%Y/%m/%d", &timeInfo);
    strftime(timeBuffer, sizeof(timeBuffer), "%H:%M:%S", &timeInfo);
    date = String(dateBuffer);
    time = String(timeBuffer);
  }
}

void setup() {
  Serial.begin(115200);
  connectToWiFi();
  connectToFirebase();
  scale.begin(LOADCELL_DOUT_PIN, LOADCELL_SCK_PIN);
  scale.tare();  // 로드셀 영점 설정

  setupTime();
}

void loop() {
  float weight = scale.get_units(10);  // 로드셀 무게 값 읽기

  String currentDate, currentTime;
  getFormattedTime(currentDate, currentTime);

  // 날짜 변경 감지
  static String previousDate = currentDate;
  if (currentDate != previousDate) {
    previousDate = currentDate;
    count = 0; // 날짜 변경 시 count 초기화
  }

  if (weight > weightThreshold && !isUserSitting) {
    // 앉기 시작
    isUserSitting = true;
    sittingStartTime = millis();

    getFormattedTime(startDate, startTime);

    Serial.println("사용자가 앉았습니다.");
    Serial.print("시작 시간: ");
    Serial.println(startTime);

  } else if (weight < weightThreshold && isUserSitting) {
    // 일어나기 시작
    sittingEndTime = millis();
    sittingDuration = sittingEndTime - sittingStartTime;

    if (sittingDuration >= minSitDuration) {
      int sdSeconds = sittingDuration / 1000; // 초 단위로 변환
      getFormattedTime(startDate, endTime);

      Serial.println("사용자가 일어났습니다.");
      Serial.print("종료 시간: ");
      Serial.println(endTime);
      Serial.print("총 사용 시간: ");
      Serial.println(sdSeconds);

      if (sittingEndTime - preLiveMillis >= intervalLive) {
        preLiveMillis = sittingEndTime;
        sendLiveDataToFirebase(sdSeconds);
        sendStatsDataToFirebase(sdSeconds);
      }
    } else {
      Serial.println("오류 감지: 짧은 시간 동안의 무게 변화가 감지되었습니다. (15초 이하)");
    }
    isUserSitting = false;  // 상태 초기화
  }
}

// Firebase에 실시간 데이터 전송 함수
void sendLiveDataToFirebase(int sdSeconds) {
  String path = "/USER/LiveData";
  FirebaseJson liveJson;

  liveJson.set("toiletUsage", sdSeconds);

  if (Firebase.updateNode(firebaseData, path, liveJson)) {
    Serial.println("Live data updated successfully!");
  } else {
    Serial.print("Error updating live data: ");
    Serial.println(firebaseData.errorReason());
  }
}

void sendStatsDataToFirebase(int sdSeconds) {
  String statsPath = "/USER/StatsData/toiletUsage";
  FirebaseJson statsJson;
  struct tm timeInfo;
  getLocalTime(&timeInfo);

  // 날짜 및 주 계산
  char yearBuffer[5];
  char monthBuffer[3];
  char dayBuffer[11]; // YYYY-MM-DD 형식에 맞는 버퍼 크기

  strftime(yearBuffer, sizeof(yearBuffer), "%Y", &timeInfo);
  strftime(monthBuffer, sizeof(monthBuffer), "%m", &timeInfo);
  strftime(dayBuffer, sizeof(dayBuffer), "%Y-%m-%d", &timeInfo); // 날짜 형식을 "YYYY-MM-DD"로 설정

  String year = String(yearBuffer); // 2024
  String month = String(monthBuffer); // 11
  String day = String(dayBuffer); // 2024-11-26

  // 주 계산 (해당 월의 첫 번째 날부터 몇 번째 주인지 계산)
  int dayOfMonth = timeInfo.tm_mday;
  int weekNumber = ((dayOfMonth - 1) / 7) + 1; // 1주차는 1~7일, 2주차는 8~14일 ...
  String week = "week" + String(weekNumber);

  // 날짜별 경로 생성
  String dayPath = statsPath + "/" + year + "/" + month + "/" + week + "/" + day;

  // 현재 날짜에 저장된 Daily count 값 불러오기
  int count = 0;
  if (Firebase.getInt(firebaseData, dayPath + "/Daily count")) {
    if (firebaseData.dataType() == "int") {
      count = firebaseData.intData(); // 현재 저장된 Daily count 값
    }
  } else {
    Serial.print("Error getting Daily count: ");
    Serial.println(firebaseData.errorReason());
  }

  // 측정 횟수 증가
  count++;

  // JSON 데이터 생성
  statsJson.set("startTime", startTime);
  statsJson.set("endTime", endTime);
  statsJson.set("duration", sdSeconds);

  // 고유 경로 생성
  String measurePath = dayPath + "/" + String(count);

  // 데이터를 해당 경로에 추가
  if (Firebase.setJSON(firebaseData, measurePath, statsJson)) {
    Serial.println("Stats data added successfully!");
  } else {
    Serial.print("Error adding stats data: ");
    Serial.println(firebaseData.errorReason());
  }

  // 증가된 Daily count 값을 업데이트
  if (Firebase.setInt(firebaseData, dayPath + "/Daily count", count)) {
    Serial.println("Daily count updated successfully!");
  } else {
    Serial.print("Error updating Daily count: ");
    Serial.println(firebaseData.errorReason());
  }
}







