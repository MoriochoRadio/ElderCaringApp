# 자료실 (docs/)

학기 산출물의 마스킹된 사본 + 시계열 진화 자산을 보관합니다.

## 직접 자료

| 파일 | 설명 | 작성자 | 마스킹 상태 |
|---|---|---|---|
| `presentation_final_redacted.pptx` | 16주차 최종 발표 (51 슬라이드, 시연 영상 1) | 팀 공동 (PPT 제작: ◯◯◯ + ◯◯◯) | core_properties + 본인 외 한글이름 + 학번 + 전화번호 마스킹 완료 |
| `final_report_redacted.pdf` | 최종보고서 (252 페이지) | 팀 공동 | 본인이 한컴에서 in-place 마스킹 후 한컴 COM 으로 PDF 변환. 잔존 PII 26 건은 §`_step2b-report.md` 참조 |
| `esp32_setup_guide_redacted.docx` | ESP32 보드 환경 설정 가이드 (17 단락) | ◯◯◯ | author/last_modified_by 초기화 완료 |
| `2024-2-Monitoring.json` | Firebase Realtime DB schema export | 본인 (DB 구조 학습용) | 더미 데이터만 (홍길동 / hgd12345) — ★ **학부 데모 단계 password 평문 저장** 그대로 박제 |

## `manuals/`

본인 작성 매뉴얼 4 개 (한컴 HWP → 한컴 COM PDF 변환).

상세는 `manuals/README.md` 참조.

## `_archive/`

- `iterations/` — 학기 중 caring0.9.0 ~ caring08 까지 **27 개 시계열 zip**. 빌드 캐시 제거 + 시크릿 마스킹 + 재압축 (1.2 GB → 5.7 MB, 99.5% 절감)
- `local-only/` — `.gitignore` 대상. 사용자 PC 에만 보관:
  - `originals/` — 마스킹 전 원본
  - `백업용zip.zip` (1.5 GB)
  - `_step_reports/` — Phase C 정리 워크플로 보고서

## PII 마스킹 정책

본 폴더의 자료들은 다음 정책으로 마스킹되었습니다 (CLAUDE.md §7-6 + Director 결정 5-4/5-5):

- **본인 외 한글 이름** (4명) → `◯◯◯`
- **본인 외 학번** → `◯◯◯◯◯◯◯◯`
- **본인 학번** → `XXXXXXXX` (Q-A=A1, §7-6)
- **본인 이름 (김태경)** → 유지 (Q-B=B1)
- **전화번호** → `◯◯◯-◯◯◯◯-◯◯◯◯`
- **지도교수 이름** → `◯◯◯`
- **PC 계정명** → `<PC_A>` (본인 외) / `tkkim` 유지 (본인)
- **WiFi 자격증명, Firebase 토큰** → `<YOUR_WIFI_SSID>`, `<FIREBASE_AUTH_SECRET>` 등 (Phase 6 일괄 치환)

## ★ 잔존 PII 안내 — 최종보고서 PDF

`final_report_redacted.pdf` 안에 본인이 한컴에서 in-place 마스킹 시 놓친 부분 26 건이 남아있을 수 있습니다 (page 22-98 의 회의록/역할 분담 표). 본인이 *"처리가 안 되어 보여도 상관없다"* 의사를 명시했으므로 push 진행. 추후 PDF 편집기로 추가 마스킹 가능.

상세 위치는 `_archive/local-only/_step_reports/_step2b_phase3_pdf_pii_results.txt` 참조.
