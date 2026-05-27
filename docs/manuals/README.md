# 매뉴얼 (본인 작성 4 개)

본인 (김태경, ENG 역할) 이 직접 작성한 매뉴얼 4 개입니다. 한컴오피스 HWP 로 작성 → 본인이 in-place PII 마스킹 → 한컴 COM 자동 PDF 변환.

| 파일 | 페이지 | 영역 |
|---|---|---|
| **`김태경_로드셀센서_연결과설정.pdf`** | 11 | ★ **로드셀 (HX711) 메인 산출물** — ESP32 D32 PRO + HX711 24-bit ADC + 회로 연결 + 아두이노 IDE 설정 + 전체 코드 설명 |
| `김태경_심박수_통계화면.pdf` | 25 | 심박수 통계 화면 UI/백엔드 (`activity_heart_rate_statistics.java`) |
| `김태경_움직임_통계화면.pdf` | 25 | 움직임 통계 화면 UI/백엔드 (`activity_movement_statistics.java`) |
| `김태경_수면_통계화면.pdf` | 28 | 수면 통계 화면 UI/백엔드 (`activity_sleep_time_statistics.java`) |

## ★ 로드셀 매뉴얼 특징

- HX711 24-bit ADC 의 의미를 직접 설명: *"로드셀에서 나오는 신호는 매우 약하므로, HX711을 통해 신호를 증폭한 후 ESP32에서 측정할 수 있도록 합니다"*
- ESP32 D32 PRO 보드 선정 이유 명시: *"GPIO 핀을 통해 다양한 센서 및 장치와 연결"*, *"Wi-Fi와 Bluetooth를 지원"*
- Arduino IDE 라이브러리 설치 + Firebase 연결 설정 단계별 안내
- 전체 코드 + 코드 설명 포함

## 학기 컨텍스트

- 과목: 설계 및 프로젝트 기본 II (2024-2 학기)
- 학과: 건양대학교 의료IT공학과
- 팀: 2조 케어링 (4명)

## 다른 팀원의 매뉴얼

본인 외 ◯◯◯ (QA, 5개) + ◯◯◯ (CM, 2개) 의 매뉴얼은 본 레포에 포함되지 않습니다 (`_archive/local-only/originals/manuals_other_team_hwp/` 에 사용자 PC 만 보관).

## ★ 자동 변환 결과

| HWP → PDF 변환 | 한컴 COM (Win32 자동화) |
|---|---|
| 도구 | `HWPFrame.HwpObject` (한컴오피스 COM) |
| 시그니처 | `Open(path, "HWP", "")` + `SaveAs(path, "PDF", "")` |
| 보안 모듈 | `RegisterModule("FilePathCheckDLL", "AutomationModule")` |
| 결과 | 본인 매뉴얼 4 + 최종보고서 1 = **5/5 자동 변환 성공** |
