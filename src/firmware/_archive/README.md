# Firmware `_archive/` — 진화 흔적 보존 폴더

본 폴더는 학기 중 작성된 **26 개 이전 버전 .ino** 를 그대로 보존합니다. 정리하지 않고 학습 흔적 그대로 두는 이유는, **학부생의 정직한 시행착오와 명명 변천사**가 README의 "본인 톤" 섹션의 결정적 증거이기 때문입니다.

## 보존된 학습 흔적

### 1. 명명 일관성 미숙 — `roadcell` ↔ `Loadcell` ↔ `loadcell`

- `roadcell0.1.0.ino` — 영구 타이포 (학기 초기, *"로드셀"* 을 *"roadcell"* 로 적음)
- `Loadcell.V.1.1.0.ino` — 대문자 V (다른 모든 파일은 소문자 v)
- `Loadcell0.2.0.ino`, `Loadcell1.0.0.ino` — dot-version 누락

### 2. 폴더-파일 이름 불일치 — 명명 사고들

- `Loadcell.v.1.0.0/Loadcell.v.1.0.0.ino` — 정상
- `Max.v.0.1.0/Max.v.0.1.0.ino` — 정상
- **`Max.v.1.0.0/Max.v.0.1.0.ino`** — 폴더는 v1.0.0 인데 파일은 v0.1.0 (사고 #1)
- **`Mpu.v.1.1.0/pedometer_V_1_0_0.ino`** — Mpu 폴더 안에 pedometer 파일 (사고 #2)
- **`Pir.v.1.1.0/Pir.v.1.0.0.ino`** — Pir.v.1.1.0 폴더 안에 v.1.0.0 파일 (사고 #3)

### 3. paste 사고 (◯◯◯ 영역)

`Max.v.1.0.0.ino` line 163 (마스킹 후 보존):
```
Firebase.updateNode(firebaseData, patc:\Users\<PC_konyang_lab>\Downloads\Max.v.0.1.0\Max.v.0.1.0.inoh, liveJson)
```

PC 경로가 코드 안에 잘못 붙여넣어진 후 그대로 commit. 컴파일 불가능한 상태로 박제. (PC 계정명만 마스킹됨, 본질은 보존)

### 4. `contact.*` 시리즈 6 단계

`contact.V.0.1.0.ino` → `v.0.2.0` → `v.0.2.1` → `v.0.3.0` → `v.0.5.0` → `v.0.6.0` (★ 0.4.0 누락)

→ 0.5.0 으로 점프하는 명명 사고 + 0.6.0 까지 6 단계 진화 흔적.

### 5. `pedometer.V.0.1.0.ino` + `pedometer.V.0.1.1.ino` + `pedometer_V_1_0_0.ino`

- `pedometer.V.0.1.0.ino` — dot 구분자
- `pedometer.V.0.1.1.ino` — 같은 패턴
- `pedometer_V_1_0_0.ino` — **underscore 로 바뀜** (★ 명명 컨벤션 학기 중간에 바뀐 흔적)

### 6. `heartrateV.0.1.0.ino`

`heartrate` + `V.0.1.0` 사이 공백/언더스코어 없음. 다른 모든 파일은 `_v.X.Y.Z` 패턴.

## 시크릿 마스킹

26 .ino 모두 WiFi SSID/PWD, Firebase 토큰이 마스킹된 사본입니다 (`<YOUR_WIFI_SSID>` 등). 원본 시크릿은 `../../../docs/_archive/local-only/originals/` 에 보존됨.

## 시계열 zip 진화

이보다 더 자세한 진화 (Android Java + 펌웨어 통합) 는 `../../../docs/_archive/iterations/README.md` 참조.
