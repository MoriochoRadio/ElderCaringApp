// 핀 설정
const int pirPin = 14;     // PIR 센서가 연결된 핀 (예: GPIO 14)
const int buttonPin = 13;  // 시스템 ON/OFF 버튼 핀 (예: GPIO 13)

int pirState = LOW;        // PIR 센서 초기 상태 (움직임 없음)
int systemState = LOW;     // 시스템 초기 상태 (OFF)
int val = 0;               // PIR 센서 입력값
unsigned long lastMotionTime = 0;  // 마지막 움직임이 감지된 시간
const unsigned long sleepDelay = 60000; // 수면 상태 전환 지연 시간 (1분)

// 설정 함수
void setup() {
  Serial.begin(115200);       // 시리얼 통신 시작
  pinMode(pirPin, INPUT);     // PIR 센서 핀을 입력으로 설정
  pinMode(buttonPin, INPUT);  // 버튼 핀을 입력으로 설정
}

// 메인 루프 함수
void loop() {
  // 버튼 상태 읽기
  if (digitalRead(buttonPin) == HIGH) {  // 버튼이 눌린 경우
    systemState = !systemState;          // 시스템 상태 전환
    delay(200);                          // 버튼 노이즈 방지 지연
    if (systemState == HIGH) {
      Serial.println("System ON");       // 시스템 ON 메시지 출력
    } else {
      Serial.println("System OFF");      // 시스템 OFF 메시지 출력
    }
  }

  // 시스템이 ON 상태일 때만 동작
  if (systemState == HIGH) {
    val = digitalRead(pirPin);           // PIR 센서 입력값 읽기
    
    if (val == HIGH) {                   // 움직임이 감지된 경우
      if (pirState == LOW) {             // 새로운 움직임이 감지된 경우
        Serial.println("Motion detected!");  // 시리얼 모니터에 메시지 출력
        pirState = HIGH;                 // PIR 상태를 HIGH로 변경
        lastMotionTime = millis();       // 마지막 움직임 시간 기록
      }
    } else if (pirState == HIGH) {       // 움직임이 끝난 경우
      pirState = LOW;                    // PIR 상태를 LOW로 변경
    }
    
    // 마지막 움직임 이후 일정 시간이 지나면 수면 상태로 전환
    if (pirState == LOW && (millis() - lastMotionTime >= sleepDelay)) {
      Serial.println("Entering sleep state"); // 수면 상태 메시지 출력
      lastMotionTime = millis();              // 시간 초기화
    }
  }
}
