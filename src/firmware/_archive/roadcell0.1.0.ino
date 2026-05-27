#include <FirebaseESP32.h>
#include "HX711.h"
#include <WiFi.h>
#include <NTPClient.h>
#include <WiFiUdp.h>

// WiFi 및 Firebase 설정
#define WIFI_SSID ""
#define WIFI_PASSWORD ""
#define FIREBASE_HOST ""
#define FIREBASE_AUTH ""

// Firebase 객체 선언
FirebaseData firebaseData;
FirebaseConfig config;
FirebaseAuth auth;
FirebaseJson json;

// HX711 설정
#define LOADCELL_DOUT_PIN1 18
#define LOADCELL_SCK_PIN1 19
#define LOADCELL_DOUT_PIN2 21
#define LOADCELL_SCK_PIN2 22

HX711 scale1;
HX711 scale2;

// NTP 클라이언트 설정
WiFiUDP ntpUDP;
NTPClient timeClient(ntpUDP, "pool.ntp.org", 9 * 3600, 60000); // 한국 표준시 (KST), UTC+9

// 무게 임계값 및 앉아있는 상태 변수
const float weightThreshold = 5.0; // 앉아있다고 판단하는 무게 변화 임계값 (kg)
bool isUserSitting = false; // 현재 사용자가 앉아있는지 여부
unsigned long sittingStartTime = 0; // 앉기 시작 시간 (밀리초)
String sittingStartTimeStr = ""; // 앉기 시작 시간 (형식화된 문자열)
String sittingEndTimeStr = ""; // 앉기 종료 시간 (형식화된 문자열)

// 데이터 업로드 간격
unsigned long lastUploadTime = 0;
const unsigned long uploadInterval = 60000; // 60초마다 업로드

void setup() {
  Serial.begin(115200);

  // WiFi 연결
  WiFi.begin(WIFI_SSID, WIFI_PASSWORD);
  while (WiFi.status() != WL_CONNECTED) {
    delay(1000);
    Serial.println("Connecting to WiFi...");
  }
  Serial.println("Connected to WiFi");

  // NTP 클라이언트 시작
  timeClient.begin();
  timeClient.update();

  // Firebase 연결
  config.host = FIREBASE_HOST;
  config.signer.tokens.legacy_token = FIREBASE_AUTH;
  Firebase.begin(&config, &auth);
  Firebase.reconnectWiFi(true);
  Serial.println("Connected to Firebase");

  // 로드셀 초기화
  scale1.begin(LOADCELL_DOUT_PIN1, LOADCELL_SCK_PIN1);
  scale2.begin(LOADCELL_DOUT_PIN2, LOADCELL_SCK_PIN2);

  // 로드셀 영점 조정
  scale1.set_scale(); // 보정 계수 설정 필요
  scale1.tare(); // 영점 설정
  scale2.set_scale(); // 보정 계수 설정 필요
  scale2.tare(); // 영점 설정
}

void loop() {
  // NTP 시간 업데이트
  timeClient.update();

  // 두 로드셀에서 무게 읽기
  float weight1 = scale1.get_units(10);
  float weight2 = scale2.get_units(10);
  float averageWeight = (weight1 + weight2) / 2.0;

  // 사용자가 앉았는지 여부 판단
  if (averageWeight > weightThreshold && !isUserSitting) {
    isUserSitting = true;
    sittingStartTime = millis();
    sittingStartTimeStr = timeClient.getFormattedTime(); // 시작 시간 저장
    Serial.println("User is now sitting.");
    Serial.print("Start Time: ");
    Serial.println(sittingStartTimeStr);
  } 
  else if (averageWeight <= weightThreshold && isUserSitting) {
    isUserSitting = false;
    sittingEndTimeStr = timeClient.getFormattedTime(); // 종료 시간 저장
    unsigned long sittingDuration = (millis() - sittingStartTime) / 1000; // 초 단위로 변환
    Serial.print("User finished sitting. Duration: ");
    Serial.print(sittingDuration);
    Serial.println(" seconds");

    // Firebase에 데이터 업로드
    uploadDataToFirebase(sittingDuration, sittingStartTimeStr, sittingEndTimeStr);
  }

  // 주기적으로 Firebase에 상태 업데이트
  if (millis() - lastUploadTime >= uploadInterval) {
    uploadDataToFirebase(0, "", ""); // 빈 데이터로 주기적 업로드
    lastUploadTime = millis();
  }

  delay(500);
}

// Firebase에 데이터 업로드 함수
void uploadDataToFirebase(unsigned long duration, String startTime, String endTime) {
  if (WiFi.status() == WL_CONNECTED) {
    String path = "/toiletUsage";
    json.clear();
    json.add("isSitting", isUserSitting);
    json.add("duration", duration);
    json.add("startTime", startTime);
    json.add("endTime", endTime);

    if (Firebase.setJSON(firebaseData, path, json)) {
      Serial.println("Data uploaded successfully");
    } 
    else {
      Serial.println("Failed to upload data");
      Serial.println(firebaseData.errorReason());
    }
  }
}
