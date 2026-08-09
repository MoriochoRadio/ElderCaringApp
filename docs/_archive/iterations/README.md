# 시계열 zip Archive (27 스냅샷)

학기 중 작성된 **27 개 시계열 zip** 을 빌드 캐시 제거 + 시크릿 마스킹 + 재압축한 것입니다.

**1.2 GB → 5.7 MB (99.5% 절감)**.

## 두 브랜치

### 메인 라인 (`caring0.9.0` ~ `caring0.9.9.9`) — 18 개
| zip | 크기 | 특징 |
|---|---|---|
| caring0.9.0 ~ caring0.9.4 | (0.9.4 누락) | 초기 |
| caring0.9.5 | — | **ToiletUsage / ToiletUsageAdapter 추가** (화장실 통계 모델 등장) |
| caring0.9.7 ~ caring0.9.9 | — | 본인 통계화면 안정기 (10 zip 동일 hash 유지) |
| caring0.9.9.1 ~ caring0.9.9.9 | 9 hotfix | 학기 막판 세부 수정 |

### 병렬 브랜치 (`caring01` ~ `caring08`) — 9 개
| zip | 크기 | 특징 |
|---|---|---|
| caring01 | 18.9 MB | 가장 작은 — 초기 시도 |
| caring01_1 | 39.7 MB | 1 의 분기 사본 |
| caring02 | 39.5 MB | **`JoinActivity.java` 원본** (.class 디컴파일 아님) — 학기 어느 시점에 원본 소스가 손실되기 전의 마지막 백업 |
| caring03 ~ caring08 | 약 39-40 MB | 작업 사본 (MainActivity 가 매 zip 마다 다른 hash — 다른 팀원의 개별 sandbox 추정) |

→ caring0.9.x 본선과 caring01-08 병렬은 **다른 팀원의 사본** 일 가능성. ToiletUsage 가 caring01-08 에는 없음.

## ★ 본인 (김태경) 파일의 MD5 변천사

본인 담당 3 통계화면이 **학기 내내 가장 자주 수정**된 파일들입니다.

| 본인 파일 | unique MD5 수 | 패턴 |
|---|---|---|
| `activity_heart_rate_statistics.java` | **10 hash 변종** | 0.9.0 → 0.9.9.9 사이 6 번 진화 + caring01-08 병렬 브랜치 2 별도 hash + CURRENT |
| `activity_movement_statistics.java` | **10 hash 변종** | 0.9.0 부터 11 번 진화 (가장 활발) |
| `activity_sleep_time_statistics.java` | 6 hash 변종 | 0.9.0\~0.9.3 stable → 0.9.5 대규모 변경 → 0.9.7\~0.9.9.7 stable (10 zip 동일) → 막판 3 변종 |

→ **본인 톤 메시지**: *"3 통계 화면은 학기 내내 가장 자주 손이 닿은 파일들이다. UI 의 사소한 픽셀 조정부터 Firebase 경로 재설계까지, 본인 영역만은 끊임없이 수정했다."*

## ◯◯◯ (QA) 파일의 MD5 변천사 — 대조

| 파일 | unique MD5 | 패턴 |
|---|---|---|
| `MainActivity.java` | 8 hash | 18 zip 동안 28e25cd1 동일 → 병렬 브랜치만 변동 |
| `LoginActivity.java` | 6 hash | 21 zip 동안 6067406d 동일 |
| `StatisticsActivity.java` | 6 hash | 0.9.5 부터 e1e6eaae 14 zip 동안 안정 |

→ ◯◯◯ 의 패턴: **"초기에 만들어 놓고 후반에는 거의 안 만짐"** — 본인 (김태경) 의 *"끝까지 수정"* 과 대조됨.

## SchoolbusRFID 와의 비교

| | SchoolbusRFID | ElderCaringApp |
|---|---|---|
| 시계열 zip 수 | 4 | **27** (밀도 7배) |
| 본인 메인 파일의 MD5 변천 | **5x 비트 동일** (안 흔들렸다) | **10 hash 변종** (끊임없이 수정) |
| 본인 톤 메시지 | *"학기 내내 안 흔들렸다"* | *"학기 내내 끊임없이 수정했다"* |

같은 학기·같은 과목의 *역설계 → 재설계* 학습 진화가 정반대 본인 톤 자산으로 박제됨.

## 마스킹 정책 (전 zip 공통)

- 빌드 캐시 제거: `.gradle/`, `.idea/`, `build/`, `app/build/`, `.git/`, `local.properties`, `gradle-wrapper.jar`
- WiFi 자격증명 (4 쌍) → `<YOUR_WIFI_SSID>`, `<YOUR_WIFI_PASSWORD>`
- Firebase legacy token (2 종) → `<FIREBASE_AUTH_SECRET>`
- `google-services.json` 안 AIza key → `<YOUR_FIREBASE_API_KEY>`
- PC paste 사고 (`Max.v.1.0.0.ino` line 163) — PC 계정만 마스킹, 사고 자체는 보존

## 원본

마스킹 전 원본 27 zip 은 `../local-only/originals/zips/` 에 사용자 PC 만 보관.
