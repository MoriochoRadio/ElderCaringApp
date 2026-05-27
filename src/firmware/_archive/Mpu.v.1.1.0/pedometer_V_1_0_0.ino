// ============================================================
// ★ 보안 안내 (Phase C 정리 시 추가):
//   본 .ino 의 WiFi/Firebase 자격증명은 학습용 더미입니다.
//   실제 사용 시 secrets.h 또는 환경 변수로 분리하세요.
//   원본 시크릿은 GitHub push 전 마스킹되었으며,
//   Firebase 콘솔에서 무효화 완료되었습니다.
// ============================================================

#include <Wire.h>
#include <MPU6050.h>
#include <WiFi.h>
#include <FirebaseESP32.h>
#include <time.h> // 시간 라이브러리 추가

// Wi-Fi 설정
const char* ssid = "<YOUR_WIFI_SSID>";           // Wi-Fi 네트워크 이름
const char* password = "<YOUR_WIFI_PASSWORD>";   // Wi-Fi 비밀번호

// Firebase 설정
FirebaseConfig config;
FirebaseAuth auth;

#define FIREBASE_HOST "monitoring-a6b20-default-rtdb.firebaseio.com" // Firebase 호스트
#define FIREBASE_AUTH "<FIREBASE_AUTH_SECRET>"      // Firebase 인증 키

// MPU6050 객체
MPU6050 mpu;

// 걸음수 데이터
unsigned long stepCount = 0;        // 현재 걸음수
unsigned long dailySteps = 0;       // 일일 걸음수
unsigned long weeklySteps = 0;      // 주별 걸음수
unsigned long monthlySteps = 0;     // 월별 걸음수
unsigned long yearlyTotalSteps = 0; // 연간 걸음수
unsigned long lastStepMillis = 0;   // 마지막 걸음 기록 시간
int prevAccelValue = 0;             // 이전 가속도 값

// Firebase 데이터 객체
FirebaseData firebaseData;

// 현재 날짜 및 시간 정보 저장 변수
String year, month, day, week, hour;

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

// 현재 날짜 및 주차를 계산하는 함수
void getFormattedDateTime() {
  struct tm timeInfo;
  if (getLocalTime(&timeInfo)) {
    char yearBuffer[5], monthBuffer[3], dayBuffer[3], weekBuffer[7], hourBuffer[3];
    strftime(yearBuffer, sizeof(yearBuffer), "%Y", &timeInfo);
    strftime(monthBuffer, sizeof(monthBuffer), "%m", &timeInfo);
    strftime(dayBuffer, sizeof(dayBuffer), "%d", &timeInfo);
    strftime(weekBuffer, sizeof(weekBuffer), "week%U", &timeInfo); // 주차 계산
    strftime(hourBuffer, sizeof(hourBuffer), "%H", &timeInfo); // 시간 계산
    year = String(yearBuffer);
    month = String(monthBuffer);
    day = String(dayBuffer);
    week = String(weekBuffer);
    hour = String(hourBuffer);
  }
}

void setup() {
  Serial.begin(115200);

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

  // MPU6050 초기화
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
  // 가속도 데이터 읽기
  int16_t ax, ay, az;
  mpu.getAcceleration(&ax, &ay, &az);

  // 가속도 값 계산
  int accelValue = (ax / 1000) * (ax / 1000) + (ay / 1000) * (ay / 1000) + (az / 1000) * (az / 1000);

  // 걸음 감지 로직
  if ((accelValue > 500) && (prevAccelValue <= 500)) {
    if ((millis() - lastStepMillis) > 300) { // 0.3초 미만 간격은 무시
      stepCount++;
      dailySteps++;
      weeklySteps++;
      monthlySteps++;
      yearlyTotalSteps++;
      lastStepMillis = millis();
    }
  }
  prevAccelValue = accelValue;

  // 매 1시간마다 Firebase로 데이터 전송
  static unsigned long lastUploadMillis = 0;
  if (millis() - lastUploadMillis > 3600000) { // 1시간 = 3,600,000ms
    getFormattedDateTime();
    sendStatsDataToFirebase();
    lastUploadMillis = millis();
  }

  delay(100); // 센서 데이터 읽기 주기
}

// StatsData의 steps 데이터 전송
void sendStatsDataToFirebase() {
  String path = "/USER/StatsData/steps/" + year + "/" + month;

  // 시간별 데이터
  String dailyPath = path + "/" + day;
  FirebaseJson hourJson;
  hourJson.set(hour, dailySteps);

  if (Firebase.updateNode(firebaseData, dailyPath, hourJson)) {
    Serial.println("시간별 데이터 전송 성공!");
  } else {
    Serial.print("시간별 데이터 전송 실패: ");
    Serial.println(firebaseData.errorReason());
  }

  // 주차별 데이터
  String weeklyPath = path + "/" + week;
  Firebase.setInt(firebaseData, weeklyPath + "/Weekly steps", weeklySteps);

  // 월별 및 연간 데이터 업데이트
  Firebase.setInt(firebaseData, path + "/Monthly steps", monthlySteps);
  Firebase.setInt(firebaseData, "/USER/StatsData/yearlyTotal", yearlyTotalSteps);
}
