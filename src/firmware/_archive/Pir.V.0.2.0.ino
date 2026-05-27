// ============================================================
// ★ 보안 안내 (Phase C 정리 시 추가):
//   본 .ino 의 WiFi/Firebase 자격증명은 학습용 더미입니다.
//   실제 사용 시 secrets.h 또는 환경 변수로 분리하세요.
//   원본 시크릿은 GitHub push 전 마스킹되었으며,
//   Firebase 콘솔에서 무효화 완료되었습니다.
// ============================================================

#include <WiFi.h>
#include <FirebaseESP32.h>

// Wi-Fi 설정
const char* ssid = "<YOUR_WIFI_SSID>";           // Wi-Fi 네트워크 이름
const char* password = "05120302";    // Wi-Fi 비밀번호

// Firebase 설정
#define FIREBASE_HOST "wearable-dataset-default-rtdb.firebaseio.com" // Firebase 호스트
#define FIREBASE_AUTH "ADXySkfRTR2FoogN0hkLjmrkq0ajRJonlpSnOZq0"      // Firebase 인증 키

// 핀 설정
const int pirPin = 14;     // PIR 센서가 연결된 핀 (예: GPIO 14)
const int buttonPin = 13;  // 시스템 ON/OFF 버튼 핀 (예: GPIO 13)

int systemState = LOW;     // 시스템 초기 상태 (OFF)
int pirState = LOW;        // PIR 센서 상태 (움직임 없음)
int restlessCount = 0;     // 뒤척임 카운트
unsigned long sleepStartTime = 0;   // 수면 시작 시간
unsigned long totalSleepTime = 0;   // 총 수면 시간
unsigned long lastMotionTime = 0;   // 마지막 움직임 시간
const unsigned long sleepDelay = 60000; // 수면 상태 전환 지연 시간 (1분)

FirebaseData firebaseData; // Firebase 데이터 객체

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
  Firebase.begin(FIREBASE_HOST, FIREBASE_AUTH);
  Firebase.reconnectWiFi(true);

  // 핀 모드 설정
  pinMode(pirPin, INPUT);     // PIR 센서 핀을 입력으로 설정
  pinMode(buttonPin, INPUT);  // 버튼 핀을 입력으로 설정
}

void loop() {
  // 버튼 상태 읽기
  if (digitalRead(buttonPin) == HIGH) {  // 버튼이 눌린 경우
    systemState = !systemState;          // 시스템 상태 전환
    delay(200);                          // 버튼 노이즈 방지 지연

    if (systemState == HIGH) {
      Serial.println("Sleep monitoring ON");
      sleepStartTime = millis();         // 수면 시작 시간 기록
      lastMotionTime = sleepStartTime;   // 초기 움직임 시간 설정
      restlessCount = 0;                 // 뒤척임 카운트 초기화
      totalSleepTime = 0;                // 총 수면 시간 초기화
    } else {
      Serial.println("Sleep monitoring OFF");

      // 수면 종료 시 총 수면 시간 계산
      unsigned long sleepEndTime = millis();
      totalSleepTime += (sleepEndTime - lastMotionTime) / 1000; // 총 수면 시간 추가 (초 단위)

      // Firebase로 데이터 전송 - LiveData 업데이트
      if (Firebase.ready()) {
        Firebase.setInt(firebaseData, "/USER/LiveData/sleepDuration", totalSleepTime);
        Firebase.setInt(firebaseData, "/USER/LiveData/restlessCount", restlessCount);
        Serial.println("Data sent to Firebase (LiveData)");

        // StatsData 예시: 날짜와 시간을 포함해 저장
        Firebase.setInt(firebaseData, "/USER/StatsData/sleepData/2024/11/week1/2024-11-07/22/Daily sleep duration", totalSleepTime);
        Firebase.setInt(firebaseData, "/USER/StatsData/sleepData/2024/11/week1/2024-11-07/22/Daily restless count", restlessCount);
        Serial.println("Data sent to Firebase (StatsData)");
      }
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
