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

// 로드셀 및 파이어베이스 객체 선언
HX711 scale;
FirebaseData firebaseData;
FirebaseConfig config;
FirebaseAuth auth;
FirebaseJson json;

// 로드셀 핀 설정
#define LOADCELL_DOUT_PIN 18
#define LOADCELL_SCK_PIN 19

// 무게 변화 감지 임계값 및 시간 설정
const float weightThreshold = 70.0; // 앉아있다고 판단하는 무게 변화 임계값 (예: 70kg으로 높임)
const unsigned long minSitDuration = 15000; // 최소 15초 동안 앉아있어야 함 (밀리초)

bool isUserSitting = false; // 현재 사용자가 앉아있는지 여부
unsigned long sittingStartTime = 0; // 앉기 시작 시간
unsigned long sittingEndTime = 0; // 일어난 시간
unsigned long sittingDuration = 0; // 앉아있던 총 시간

String startTime = ""; // 화장실 사용 시작 시간 저장 변수
String startDate = ""; // 화장실 사용 시작 날짜 저장 변수

// WiFi 연결
void connectToWiFi() {
  WiFi.begin(WIFI_SSID, WIFI_PASSWORD);
  while (WiFi.status() != WL_CONNECTED) {
    delay(1000);
    Serial.println("WiFi 연결 중...");
  }
  Serial.println("WiFi 연결 완료");
}

// Firebase 연결
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

void setup() {
  Serial.begin(115200);

  // 로드셀 초기화
  scale.begin(LOADCELL_DOUT_PIN, LOADCELL_SCK_PIN);
  scale.tare(); // 영점 설정

  // WiFi 및 Firebase 연결
  connectToWiFi();
  connectToFirebase();

  // 시간 동기화
  setupTime();
}

void loop() {
  // 로드셀에서 무게 값 읽기
  float weight = scale.get_units(10);

  // 무게 변화 감지
  if (weight > weightThreshold && !isUserSitting) {
    // 앉기 시작
    isUserSitting = true;
    sittingStartTime = millis();

    struct tm timeInfo;
    if (getLocalTime(&timeInfo)) {
      char dateBuffer[11]; // YYYY/MM/DD 형식
      char timeBuffer[9]; // HH:MM:SS 형식
      strftime(dateBuffer, sizeof(dateBuffer), "%Y/%m/%d", &timeInfo);
      strftime(timeBuffer, sizeof(timeBuffer), "%H:%M:%S", &timeInfo);
      startDate = String(dateBuffer);
      startTime = String(timeBuffer);
    }

    Serial.println("사용자가 앉았습니다.");
    Serial.print("시작 시간: ");
    Serial.println(startTime);

  } else if (weight < weightThreshold && isUserSitting) {
    // 일어나기 시작
    sittingEndTime = millis();
    sittingDuration = sittingEndTime - sittingStartTime;

    // 15초 이상 앉아 있었다면 파이어베이스에 업로드
    if (sittingDuration >= minSitDuration) {
      struct tm timeInfo;
      String endTime;
      if (getLocalTime(&timeInfo)) {
        char timeBuffer[9]; // HH:MM:SS 형식
        strftime(timeBuffer, sizeof(timeBuffer), "%H:%M:%S", &timeInfo);
        endTime = String(timeBuffer);
      }
      String durationStr = String(sittingDuration / 60000) + "분 " + String((sittingDuration % 60000) / 1000) + "초"; // 총 앉은 시간 (분, 초)

      Serial.println("사용자가 일어났습니다.");
      Serial.print("종료 시간: ");
      Serial.println(endTime);
      Serial.print("총 사용 시간: ");
      Serial.println(durationStr);

      // Firebase에 데이터 업로드
      json.clear();
      json.add("startTime", startTime);
      json.add("endTime", endTime);
      json.add("duration", durationStr); // 예: 1분 30초

      // 날짜를 기준으로 카테고리 생성 후 데이터 저장
      String path = "/toiletUsage/" + startDate + "/" + startTime;
      if (Firebase.setJSON(firebaseData, path, json)) {
        Serial.println("데이터 업로드 성공");
      } else {
        Serial.print("데이터 업로드 실패: ");
        Serial.println(firebaseData.errorReason());
      }
    } else {
      // 15초 이하로 앉은 경우 오류로 간주하여 시리얼 모니터에 표시
      Serial.println("오류 감지: 짧은 시간 동안의 무게 변화가 감지되었습니다. (15초 이하)");
    }
    isUserSitting = false; // 상태 초기화
  }

  delay(1000); // 1초마다 확인
}
