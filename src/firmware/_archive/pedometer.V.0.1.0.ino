#include <Wire.h>
#include <MPU6050.h>

MPU6050 mpu;

const int STEP_THRESHOLD = 12000;  // 걸음으로 인식할 가속도 변화 임계값
int steps = 0;  // 걸음 수 카운트
bool stepDetected = false;

void setup() {
  Serial.begin(115200);
  Wire.begin();
  mpu.initialize();

  if (!mpu.testConnection()) {
    Serial.println("MPU6050 연결 실패!");
    while (1);
  } else {
    Serial.println("MPU6050 연결 성공!");
  }
}

void loop() {
  int16_t ax, ay, az;
  mpu.getAcceleration(&ax, &ay, &az);

  // 가속도 값의 크기를 계산하여 걸음 여부 확인
  int accelerationMagnitude = sqrt(sq(ax) + sq(ay) + sq(az));

  // 특정 가속도 임계값을 초과하면 걸음으로 인식
  if (accelerationMagnitude > STEP_THRESHOLD && !stepDetected) {
    steps++;
    stepDetected = true;
    Serial.print("걸음 수: ");
    Serial.println(steps);
  }

  // 가속도가 다시 낮아지면 stepDetected 플래그 해제
  if (accelerationMagnitude < STEP_THRESHOLD) {
    stepDetected = false;
  }

  delay(100);  // 센서 데이터 읽기 간격
}
