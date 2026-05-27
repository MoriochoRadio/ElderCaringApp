# Arduino 펌웨어 (ESP32 + 4 센서)

> ESP32 D32 PRO 보드 4 대로 4 종류의 센서를 분산 측정 → Firebase Realtime DB 로 전송.
> 본인 메인 영역: **로드셀 (HX711) — 화장실 모니터링**.

## 활성 .ino (현재 사용)

| 파일 | 센서 | 담당 |
|---|---|---|
| **`loadcell.v.2.0.0.ino`** | **HX711 + SEN-10245 하프브리지 로드셀 × 2** | ★ **본인 (메인)** |
| `Loadcell.v.1.2.0.ino` | (이전 버전, 단일 로드셀) | 본인 |
| `Loadcell.v.1.1.0.ino` | (가장 초기 버전) | 본인 |
| `Max.v.2.0.0.ino` | MAX30102 심박 + SpO2 | ◯◯◯ + ◯◯◯ (공동) |
| `Mpu.v.2.0.0.ino` | MPU-6050 자이로 + 가속도 | ◯◯◯ |
| `Pir_v_2_0_0.ino` | HC-SR501 PIR 인체감지 | ◯◯◯ |

## ★ `로드셀_임시_작동법.txt` — 본인 톤 자산

회로 학습 메모. 하프브리지 로드셀 2 개를 하나의 HX711 으로 연결하는 임시 방법 발명 기록.

> *"두 로드셀에 동일한 하중을 가하면 신호가 상쇄되어 감지가 되지 않기 때문입니다"*

이 한 줄이 학기 중간에 회로 이해도가 어떻게 자랐는지를 박제합니다.

## ESP32 ↔ HX711 핀맵

| HX711 | ESP32 D32 PRO |
|---|---|
| DT (DOUT) | GPIO 18 |
| SCK | GPIO 19 |
| VCC | 3.3V |
| GND | GND |

## ★ 임계 무게 (Threshold) 진화

| 버전 | `weightThreshold` | 비고 |
|---|---|---|
| v1.1.0 | **70.0** | 단일 로드셀 |
| v1.2.0 | 70.0 | (변경 없음) |
| **v2.0.0** | **200.0** | 두 로드셀 병렬 연결 → 신호 ~3배 |

`scale.set_scale()` 호출은 한 번도 없습니다. 정량 calibration 없이 임계 매직넘버만 조정하는 학부생 정직 패턴.

## `minSitDuration = 15000ms` (15초)

화장실 사용으로 인정하는 최소 앉기 시간. 잠시 무게가 실린 것 (옷이 떨어진 경우 등) 은 무시.

## 의존 라이브러리

```cpp
#include <HX711.h>           // by Bogdan Necula
#include <FirebaseESP32.h>   // by Mobizt
#include <WiFi.h>            // ESP32 표준
#include <time.h>            // NTP 동기화 (KST UTC+9)
```

심박 / MPU / PIR / NTPClient 는 각 센서 영역의 README 참조.

## WiFi + Firebase 설정 (실제 동작 시)

본 레포의 .ino 들은 WiFi SSID / PWD / Firebase 토큰이 마스킹되어 있습니다 (`<YOUR_WIFI_SSID>` 등). 실제 사용 시:

```cpp
#define WIFI_SSID "your_wifi_name"
#define WIFI_PASSWORD "your_wifi_password"
#define FIREBASE_HOST "your-project-default-rtdb.firebaseio.com"
#define FIREBASE_AUTH "your_firebase_legacy_token"
```

원본 시크릿은 GitHub push 전 마스킹되었고, Firebase 콘솔에서 무효화 완료되었습니다.

## `_archive/` — 학기 중 진화 흔적 보존

26 개 이전 버전 .ino 가 들어있습니다. 본인 톤 자산:

- `roadcell0.1.0.ino` — *"로드셀" → "roadcell"* 영구 타이포
- `Loadcell.V.1.1.0.ino` — 대문자 V (v 와 V 혼용)
- `Max.v.1.0.0/Max.v.0.1.0.ino` — 폴더명 v1.0.0 인데 파일명 v0.1.0 (폴더-파일 명명 불일치)
- `Mpu.v.1.1.0/pedometer_V_1_0_0.ino` — Mpu 폴더 안에 pedometer 파일 (★ 명명 사고)
- `contact.V.0.1.0.ino` ~ `contact.v.0.6.0.ino` — 6 단계 진화 (◯◯◯ 영역으로 추정)
- `Max.v.1.0.0.ino` line 163 — `Firebase.updateNode(firebaseData, patc:\Users\<PC_konyang_lab>\Downloads\Max.v.0.1.0\Max.v.0.1.0.inoh, ...)` ← **컴파일 불가능한 paste 사고가 박제됨**

## 참고

- 시계열 zip 진화는 `../../docs/_archive/iterations/README.md` 참조.
- NTPClient 라이브러리는 Arduino IDE 의 라이브러리 매니저로 설치 (본 레포에 동봉되지 않음).
