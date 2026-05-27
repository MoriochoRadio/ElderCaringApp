// ============================================================
// ★ 보안 안내 (Phase C 정리 시 추가):
//   본 .ino 의 WiFi/Firebase 자격증명은 학습용 더미입니다.
//   실제 사용 시 secrets.h 또는 환경 변수로 분리하세요.
//   원본 시크릿은 GitHub push 전 마스킹되었으며,
//   Firebase 콘솔에서 무효화 완료되었습니다.
// ============================================================

#include <FirebaseESP32.h>         // Firebase 라이브러리 포함
#include <WiFiUdp.h>               // Wi-Fi UDP 라이브러리 포함
#include <NTPClient.h>             // NTP 클라이언트 라이브러리 포함
#include "MAX30105.h"             // MAX30105 센서 라이브러리 포함

// Wi-Fi 및 Firebase 설정
#define ssid "<YOUR_WIFI_SSID>"                 // 연결할 Wi-Fi 네트워크의 SSID
#define password "<YOUR_WIFI_PASSWORD>"            // Wi-Fi 비밀번호
#define firebaseHost "monitoring-a6b20-default-rtdb.firebaseio.com" // Firebase 데이터베이스의 URL
#define firebaseAuth "<FIREBASE_AUTH_SECRET>"     // Firebase 인증 토큰

// Firebase 데이터 객체 및 구성 설정
FirebaseData firebaseData;               // Firebase 데이터 객체 생성
FirebaseConfig config;                   // Firebase 설정 객체 생성
FirebaseAuth auth;                       // Firebase 인증 객체 생성
FirebaseJson json;                       // JSON 형식의 데이터를 Firebase에 전송하기 위한 객체
FirebaseJson monthJson;                  // 월 데이터를 Firebase에 저장할 때 사용할 JSON 객체
FirebaseJson dayJson;                    // 일 데이터를 Firebase에 저장할 때 사용할 JSON 객체
FirebaseJson hourJson;                   // 시간 데이터를 저장할 JSON 객체
FirebaseJson minuteJson;                 // 분 데이터를 저장할 JSON 객체

WiFiUDP ntpUDP;                          // NTP 클라이언트를 위한 UDP 객체 생성
NTPClient timeClient(ntpUDP);            // NTP 클라이언트 초기화

MAX30105 particleSensor;                 // MAX30105 센서 객체 생성
const byte RATE_SIZE = 4;                // 심박수 평균 계산을 위한 배열 크기 설정
byte rates[RATE_SIZE];                   // 심박수 저장을 위한 배열
byte rateSpot = 0;                       // 배열 인덱스 초기화
long lastBeat = 0;                       // 마지막 심박 감지 시간 저장
float beatsPerMinute;                    // BPM (분당 심박수) 저장
int beatAvg;                             // 평균 BPM 저장
float spo2;                              // 산소포화도 값 저장

int i = 0;                               // 루프 카운터
int bpmSum = 0;                          // BPM 합계 계산을 위한 변수
int spo2Sum = 0;                         // SpO2 합계 계산을 위한 변수
int count = 0;                           // 데이터 카운트 변수

unsigned long lastUploadTime = 0;        // 마지막 데이터 업로드 시간
const unsigned long uploadInterval = 60000; // 데이터 업로드 간격 (밀리초 단위)

// 시간 및 날짜 관련 변수
String formattedDate;                    // NTP로부터 받은 날짜 및 시간 문자열
String dayStamp;                         // 날짜만 추출한 문자열
String timeStamp;                        // 시간만 추출한 문자열
String path = "/test";                 // Firebase 경로 변수 지정

void setup() {
  Serial.begin(115200);                  // 시리얼 통신 초기화
  connectToWiFi();                       // Wi-Fi 연결 함수 호출
  config.host = firebaseHost;            // Firebase 설정에 데이터베이스 URL 설정
  config.signer.tokens.legacy_token = firebaseAuth; // Firebase 설정에 인증 토큰 설정
  connectToFirebase();                   // Firebase 연결 함수 호출
  timeClient.begin();                    // NTP 클라이언트 시작
  timeClient.setTimeOffset(32400);       // 시간대 오프셋 한국으로 설정 (UTC+9)
  connectToMAX30105();                   // MAX30105 센서 초기화
}

void loop() {
  sendTimeToFirebase();                  // 시간 데이터를 Firebase에 전송
  long irValue = particleSensor.getIR(); // MAX30105 센서에서 IR 값 읽기
  long redValue = particleSensor.getRed(); // MAX30105 센서에서 적색 값 읽기

  // 심박수 감지 및 계산
  if (checkForBeat(irValue)) {
    long delta = millis() - lastBeat;    // 이전 심박과 현재 심박 사이의 시간 차이 계산
    lastBeat = millis();                 // 현재 시간 갱신
    beatsPerMinute = 60 / (delta / 1000.0); // BPM 계산

    // BPM 값이 유효한 경우 저장
    if (beatsPerMinute < 255 && beatsPerMinute > 20) {
      rates[rateSpot++] = (byte)beatsPerMinute; // 배열에 BPM 저장
      rateSpot %= RATE_SIZE;                    // 배열 인덱스 순환
      beatAvg = 0;                              // 평균 BPM 초기화

      for (byte i = 0; i < RATE_SIZE; i++)      // 평균 BPM 계산
        beatAvg += rates[i];
      beatAvg /= RATE_SIZE;
    }
  }

  // 산소포화도 계산
  spo2 = calculateSpO2(irValue, redValue);
  if (irValue < 50000) {                       // IR 값이 일정 수준 이하이면 인식 불가 출력
    Serial.println("인식 불가");
  } 
  else {
    // 측정된 BPM과 산소포화도 출력
    Serial.print("BPM=");
    Serial.print(beatsPerMinute);
    Serial.print(", Avg BPM=");
    Serial.print(beatAvg);
    Serial.print(", SpO2=");
    Serial.println(spo2);
  }

  count++;                                     // 데이터 카운트 증가

  // 데이터 업로드 간격 확인 후 Firebase에 데이터 전송
  if (millis() - lastUploadTime >= uploadInterval) {  
    int averageBPM = bpmSum / count;           // 평균 BPM 계산
    int averageSPo2 = spo2Sum / count;         // 평균 SpO2 계산

    // 유효한 평균값이 있는 경우 Firebase에 전송
    if (averageBPM >= 0 && averageSPo2 >= 0) {
      minuteJson.add("BPM", averageBPM);     // JSON 객체에 평균 BPM 추가
      minuteJson.add("spO2", averageSPo2);   // JSON 객체에 평균 SpO2 추가

      // Firebase에 JSON 데이터 전송 시도
      if (Firebase.setJSON(firebaseData, path, json)) {
        Serial.println("데이터 전송 성공");
      } 
      else {
        // 전송 실패 시 오류 메시지 출력
        Serial.println("데이터 전송 실패");
        Serial.println(firebaseData.errorReason());
      }
    }

    // 데이터 초기화 및 업로드 시간 갱신
    bpmSum = 0;
    spo2Sum = 0;
    count = 0;
    lastUploadTime = millis();
  }
  delay(100);                                // 짧은 지연 추가
}

// Wi-Fi 연결 함수
void connectToWiFi() {
  WiFi.begin(ssid, password);                // Wi-Fi 연결 시작
  while (WiFi.status() != WL_CONNECTED) {    // Wi-Fi가 연결될 때까지 대기
    delay(1000);                             // 1초 지연
    Serial.println("Connecting to WiFi..."); // 연결 중 메시지 출력
  }
  Serial.println("~~~~~Connected to WiFi~~~~~"); // 연결 성공 메시지 출력
}

// Firebase 연결 함수
void connectToFirebase() {
  Firebase.begin(&config, &auth);            // Firebase 초기화
  Firebase.reconnectWiFi(true);              // Wi-Fi가 끊겼을 때 자동 재연결 설정
  firebaseData.setResponseSize(1024);        // Firebase 응답 데이터 크기 설정 (최대 1024바이트)
  Serial.println("~~~~~Connected to Firebase~~~~~"); // Firebase 연결 성공 메시지 출력
}

// MAX30105 센서 초기화 함수
void connectToMAX30105() {
  if (!particleSensor.begin(Wire, I2C_SPEED_FAST)) { // 센서 초기화 실패 시 메시지 출력
    Serial.println("센서를 찾을 수 없습니다.");
    while (1);                                   // 무한 대기 (실패 시)
  }
  particleSensor.setup();                        // 센서 설정
  particleSensor.setPulseAmplitudeRed(0x3F);     // 적색 LED의 세기 설정
  particleSensor.setPulseAmplitudeGreen(0);      // 녹색 LED 끄기
}

void sendTimeToFirebase(){
  while (!timeClient.update()) {         
    timeClient.forceUpdate();            // 강제로 시간 업데이트를 시도하여 최신 시간 정보를 가져옴
    delay(60000);                        // 60초마다 시간 업데이트 시도 (네트워크 지연 대비)
  }

  // 업데이트된 시간 문자열 가져오기
  formattedDate = timeClient.getFormattedDate(); 
  Serial.println(formattedDate);         // 전체 날짜 및 시간 문자열을 시리얼 모니터에 출력

  // 날짜와 시간 추출
  int splitT = formattedDate.indexOf("T"); // 'T' 문자의 위치를 찾아 날짜와 시간을 분리
  dayStamp = formattedDate.substring(0, splitT); // 'T' 이전 부분은 날짜로 추출
  timeStamp = formattedDate.substring(splitT + 1, formattedDate.length() - 1); // 'T' 이후 부분은 시간으로 추출

  // 연도, 월, 일, 시간, 분으로 분리
  String year = dayStamp.substring(0, 4);      // 연도를 추출
  String month = dayStamp.substring(5, 7);     // 월을 추출
  String day = dayStamp.substring(8, 10);      // 일을 추출
  String hour = timeStamp.substring(0, 2);     // 시간을 추출
  String minute = timeStamp.substring(3, 5);   // 분을 추출

  // Firebase에 연도, 월, 일 전송
  minuteJson.set(minute);                     // 분 데이터를 minuteJson 객체에 설정
  hourJson.set(hour, minuteJson);             // 시간 데이터를 hourJson 객체에 설정
  dayJson.set(day, hourJson);                 // 일 데이터를 dayJson 객체에 설정
  monthJson.set(month, dayJson);              // 월 데이터를 monthJson 객체에 설정
  json.set(year, monthJson);                  // 연도를 JSON 객체에 설정

  // Firebase에 JSON 데이터 전송
  if (Firebase.pushJSON(firebaseData, path, json)) {
    Serial.println("Date sent to Firebase successfully!"); // 전송 성공 메시지를 시리얼 모니터에 출력
  } else {
    // 전송 실패 시 에러 메시지를 시리얼 모니터에 출력
    Serial.print("Error sending date to Firebase: ");
    Serial.println(firebaseData.errorReason());
  }
}

// 심박수 감지 함수(정확도 수정)
bool checkForBeat(long sample) {
  static long prevSample = 0;                 // 이전 샘플 값을 저장하는 변수
  static long threshold = 1000;               // 심박 감지를 위한 임계값 설정
  static bool pulseDetected = false;          // 심박이 감지되었는지 여부를 나타내는 플래그
  long delta = sample - prevSample;           // 현재 샘플과 이전 샘플 간의 차이 계산

  if (!pulseDetected && delta > threshold) {  // 이전에 심박이 감지되지 않았고, 변화량이 임계값을 초과할 경우
    pulseDetected = true;                     // 심박 감지 플래그 설정
    prevSample = sample;                      // 현재 샘플을 이전 샘플로 업데이트
    return true;                              // 심박 감지됨을 반환
  }
  else if (pulseDetected && delta < 0) {      // 심박이 감지된 후 변화량이 감소할 경우
    pulseDetected = false;                    // 심박 감지 플래그 해제
  }

  prevSample = sample;                        // 현재 샘플을 이전 샘플로 업데이트
  return false;                               // 심박 감지되지 않음을 반환
}

// 산소포화도 계산 함수
int calculateSpO2(long irValue, long redValue) {
  int ratio = (int)redValue / (int)irValue;   // 적색 값과 IR 값의 비율을 계산
  int spo2Value = 104 - 17 * ratio;           // 산소포화도 계산 공식 적용

  if (spo2Value < 0) spo2Value = 0;           // 산소포화도 값이 0 미만이면 0으로 설정
  else if (spo2Value > 100) spo2Value = 100;  // 산소포화도 값이 100을 초과하면 100으로 설정

  return spo2Value;                           // 계산된 산소포화도 값을 반환
}