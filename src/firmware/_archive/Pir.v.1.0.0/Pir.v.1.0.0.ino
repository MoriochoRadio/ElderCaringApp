// ============================================================
// ★ 보안 안내 (Phase C 정리 시 추가):
//   본 .ino 의 WiFi/Firebase 자격증명은 학습용 더미입니다.
//   실제 사용 시 secrets.h 또는 환경 변수로 분리하세요.
//   원본 시크릿은 GitHub push 전 마스킹되었으며,
//   Firebase 콘솔에서 무효화 완료되었습니다.
// ============================================================

#include <WiFi.h>
#include <FirebaseESP32.h>
#include <time.h> // 시간 라이브러리 추가

// Wi-Fi 설정
const char* ssid = "<YOUR_WIFI_SSID>";           // Wi-Fi 네트워크 이름
const char* password = "<YOUR_WIFI_PASSWORD>";    // Wi-Fi 비밀번호

// Firebase 설정
FirebaseConfig config;
FirebaseAuth auth;

#define FIREBASE_HOST "monitoring-a6b20-default-rtdb.firebaseio.com" // Firebase 호스트
#define FIREBASE_AUTH "<FIREBASE_AUTH_SECRET>"      // Firebase 인증 키

// 핀 설정
const int pirPin = 14;     // PIR 센서의 출력 핀 (예: GPIO 14)
const int buttonPin = 13;  // 시스템 ON/OFF 버튼 핀 (예: GPIO 13)

int systemState = LOW;     // 시스템 초기 상태 (OFF)
int pirState = LOW;        // PIR 센서 상태 (움직임 없음)
int restlessCount = 0;     // 뒤척임 카운트
unsigned long totalSleepTime = 0;   // 총 수면 시간 (초 단위)
unsigned long lastMotionTime = 0;   // 마지막 움직임 시간
const unsigned long sleepDelay = 60000; // 수면 상태 전환 지연 시간 (1분)

String sleepStartTimeFormatted;    // 수면 시작 시간 (포맷된 문자열)
String sleepEndTimeFormatted;      // 수면 종료 시간 (포맷된 문자열)
String formattedSleepDuration;     // 수면 시간 (HH:MM:SS 형식)

FirebaseData firebaseData; // Firebase 데이터 객체

// NTP 서버를 통한 시간 동기화 설정 (대한민국 표준 시간대)
void setupTime() {
  configTime(9 * 3600, 0, "pool.ntp.org", "time.nist.gov");
  struct tm timeInfo;
  while (!getLocalTime(&timeInfo)) {
    Serial.println("시간 동기화 대기 중...");
    delay(1000);
  }
  Serial.println("시간 동기화 완료");
}

// 현재 날짜와 시간을 포맷하는 함수
void getFormattedTime(String &date, String &time) {
  struct tm timeInfo;
  if (getLocalTime(&timeInfo)) {
    char dateBuffer[11];  // YYYY/MM/DD 형식
    char timeBuffer[9];   // HH:MM:SS 형식
    strftime(dateBuffer, sizeof(dateBuffer), "%Y/%m/%d", &timeInfo);
    strftime(timeBuffer, sizeof(timeBuffer), "%H:%M:%S", &timeInfo);
    date = String(dateBuffer);
    time = String(timeBuffer);
  }
}

// 초를 HH:MM:SS 형식으로 변환하는 함수
String formatSecondsToTime(unsigned long seconds) {
  int hours = seconds / 3600;
  seconds %= 3600;
  int minutes = seconds / 60;
  seconds %= 60;
  char buffer[9];
  sprintf(buffer, "%02d:%02d:%02d", hours, minutes, seconds);
  return String(buffer);
}

void setup() {
  Serial.begin(115200);       // 시리얼 통신 시작

  // Wi-Fi 연결
  WiFi.begin(ssid, password);
  Serial.print("Connecting to Wi-Fi");
  while (WiFi.status() != WL_CONNECTED) {
    delay(500);
    Serial.print(".");
  }
  Serial.println();
  Serial.println("Connected to Wi-Fi");

  // Firebase 초기화
  config.host = FIREBASE_HOST;
  config.signer.tokens.legacy_token = FIREBASE_AUTH;
  Firebase.begin(&config, &auth);
  Firebase.reconnectWiFi(true);

  if (Firebase.ready()) {
    Serial.println("Firebase 연결 완료");
  } else {
    Serial.println("Firebase 연결 실패");
  }

  setupTime(); // NTP 시간 동기화

  // 핀 모드 설정
  pinMode(pirPin, INPUT);     // PIR 센서 핀을 입력으로 설정
  pinMode(buttonPin, INPUT_PULLUP);  // 버튼 핀을 내부 풀업으로 설정
}

void loop() {
  // 버튼 상태 읽기
  if (digitalRead(buttonPin) == LOW) {  // 버튼이 눌린 경우 (내부 풀업으로 설정했으므로 LOW)
    systemState = !systemState;          // 시스템 상태 전환
    delay(200);                          // 버튼 노이즈 방지 지연

    if (systemState == HIGH) {
      Serial.println("Sleep monitoring ON");

      // 수면 시작 시간을 포맷하여 저장
      String currentDate, currentTime;
      getFormattedTime(currentDate, currentTime);
      sleepStartTimeFormatted = currentDate + " " + currentTime;  // 수면 시작 시간 저장

      lastMotionTime = millis();   // 초기 움직임 시간 설정
      restlessCount = 0;           // 뒤척임 카운트 초기화
      totalSleepTime = 0;          // 총 수면 시간 초기화
    } else {
      Serial.println("Sleep monitoring OFF");

      // 수면 종료 시간을 포맷하여 저장
      String currentDate, currentTime;
      getFormattedTime(currentDate, currentTime);
      sleepEndTimeFormatted = currentDate + " " + currentTime;  // 수면 종료 시간 저장

      // 수면 종료 시 총 수면 시간 계산
      unsigned long sleepEndTime = millis();
      totalSleepTime += (sleepEndTime - lastMotionTime) / 1000; // 총 수면 시간 추가 (초 단위)

      // 수면 시간을 HH:MM:SS 형식으로 변환
      formattedSleepDuration = formatSecondsToTime(totalSleepTime);

      // Firebase로 데이터 전송 - LiveData 업데이트
      sendLiveDataToFirebase();
      sendStatsDataToFirebase();
    }
  }

  // 시스템이 ON 상태일 때만 동작
  if (systemState == HIGH) {
    int val = digitalRead(pirPin);       // PIR 센서 입력값 읽기

    if (val == HIGH) {                   // 움직임이 감지된 경우
      if (pirState == LOW) {             // 새로운 움직임 감지
        pirState = HIGH;
        restlessCount++;                 // 뒤척임 카운트 증가
        Serial.println("Movement detected - restless count incremented");

        // 총 수면 시간에 현재 수면 구간 추가
        totalSleepTime += (millis() - lastMotionTime) / 1000; // 수면 시간 추가
        lastMotionTime = millis();       // 마지막 움직임 시간 기록
      }
    } else if (pirState == HIGH) {       // 움직임이 멈춘 경우
      pirState = LOW;                    // PIR 상태를 LOW로 변경
    }

    // 움직임이 없고 1분이 지나면 수면 상태로 돌아감
    if (pirState == LOW && (millis() - lastMotionTime >= sleepDelay)) {
      lastMotionTime = millis();         // 수면 상태로 복귀
      Serial.println("Back to sleep state");
    }
  }
}

// Firebase에 실시간 데이터 전송 함수
void sendLiveDataToFirebase() {
  String path = "/TEST/LiveData/sleep";
  FirebaseJson liveJson;
  liveJson.set("start_time", sleepStartTimeFormatted);  // 수면 시작 시간 추가
  liveJson.set("end_time", sleepEndTimeFormatted);      // 수면 종료 시간 추가
  liveJson.set("sleepDuration", formattedSleepDuration);    // HH:MM:SS 형식 수면 시간
  liveJson.set("restlessCount", restlessCount);

  if (Firebase.updateNode(firebaseData, path, liveJson)) {
    Serial.println("Live data updated successfully!");
  } else {
    Serial.print("Error updating live data: ");
    Serial.println(firebaseData.errorReason());
  }
}

// Firebase에 통계 데이터 전송 함수
void sendStatsDataToFirebase() {
  struct tm timeInfo;
  getLocalTime(&timeInfo);

  char yearBuffer[5];
  char monthBuffer[3];
  char dayBuffer[3];
  
  strftime(yearBuffer, sizeof(yearBuffer), "%Y", &timeInfo);
  strftime(monthBuffer, sizeof(monthBuffer), "%m", &timeInfo);
  strftime(dayBuffer, sizeof(dayBuffer), "%d", &timeInfo);

  String year = String(yearBuffer);
  String month = String(monthBuffer);
  String day = String(dayBuffer);

  // 주차 계산
  int weekNumber = (day.toInt() - 1) / 7 + 1;
  String week = "week" + String(weekNumber);

  // 경로를 "2024-11-19" 형식으로 변경
  String formattedDay = year + "-" + month + "-" + day;

  // Firebase 경로 설정
  String path = "/TEST/StatsData/sleepData/" + year + "/" + month + "/" + week + "/" + formattedDay;

  FirebaseJson statsJson;
  statsJson.set("sleepStartTime", sleepStartTimeFormatted); // 수면 시작 시간 추가
  statsJson.set("sleepEndTime", sleepEndTimeFormatted);     // 수면 종료 시간 추가
  statsJson.set("Daily sleep duration", formattedSleepDuration); // HH:MM:SS 형식 수면 시간
  statsJson.set("Daily restless count", restlessCount);

  if (Firebase.updateNode(firebaseData, path, statsJson)) {
    Serial.println("Stats data updated in Firebase successfully!");
  } else {
    Serial.print("Error updating stats data in Firebase: ");
    Serial.println(firebaseData.errorReason());
  }
}
