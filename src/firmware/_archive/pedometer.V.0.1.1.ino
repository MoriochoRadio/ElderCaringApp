#include <Wire.h>
#include <MPU6050.h>

MPU6050 mpu;

unsigned long count = 0; // 걸음 수는 count/2로 계산됨.
int prev_val;
unsigned long last_millis;

void setup() {
    Serial.begin(115200);
    Wire.begin(21, 22);  // ESP32의 기본 I2C 핀 설정 (SDA: GPIO 21, SCL: GPIO 22)
    mpu.initialize();

    // 센서 연결 확인
    if (!mpu.testConnection()) {
        Serial.println("MPU6050 연결 실패!");
        while (1);  // 연결 실패 시 멈춤
    } else {
        Serial.println("MPU6050 연결 성공!");
    }
}

void loop() {
    int16_t ax, ay, az;
    mpu.getAcceleration(&ax, &ay, &az);

    // 가속도 값 계산
    int val = (ax / 1000) * (ax / 1000) + (ay / 1000) * (ay / 1000) + (az / 1000) * (az / 1000);

    // 걸음 감지 로직
    if ((val > 500) && (prev_val <= 500)) {
        if ((millis() - last_millis) > 300) { // 0.3초 미만 간격은 무시 (노이즈 방지)
            count++;
            last_millis = millis();
        }
    }
    prev_val = val;

    // 걸음 수 출력 (count/2)
    Serial.print("걸음 수: ");
    Serial.println(count / 2);

    delay(100);  // 주기적으로 센서 데이터를 읽기 위해 대기
}
