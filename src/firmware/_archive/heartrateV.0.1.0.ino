#include <Wire.h>
#include "MAX30105.h"
#include "spo2_algorithm.h"

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

void setup() {
  Serial.begin(115200);

  // 센서 초기화
  if (!particleSensor.begin(Wire, I2C_SPEED_FAST)) {
    Serial.println("MAX30102 센서 초기화 실패");
    while (1);
  }
  Serial.println("MAX30102 센서 초기화 성공");

  particleSensor.setup();  // 기본 설정으로 센서 작동
  particleSensor.setPulseAmplitudeRed(0x0A); // 적색 LED 설정
  particleSensor.setPulseAmplitudeIR(0x0A);  // 적외선 LED 설정
}

void loop() {
  // 버퍼에 데이터 저장
  bufferLength = BUFFER_SIZE;
  for (int i = 0; i < bufferLength; i++) {
    while (particleSensor.available() == false) // 센서 데이터가 준비될 때까지 대기
      particleSensor.check(); 

    redBuffer[i] = particleSensor.getRed();
    irBuffer[i] = particleSensor.getIR();
    particleSensor.nextSample(); // 다음 샘플로 이동
  }

  // 산소포화도와 심박수 계산
  maxim_heart_rate_and_oxygen_saturation(irBuffer, bufferLength, redBuffer, &spo2, &validSPO2, &heartRate, &validHeartRate);

  // 결과 출력
  if (validSPO2 && validHeartRate) {
    Serial.print("산소포화도(SPO2): ");
    Serial.print(spo2);
    Serial.print("%, 심박수: ");
    Serial.print(heartRate);
    Serial.println(" bpm");
  } else {
    Serial.println("유효한 측정값을 얻지 못했습니다. 다시 측정 중...");
  }
  
  delay(1000); // 1초 간격으로 측정
}
