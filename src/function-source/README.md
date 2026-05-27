# Firebase Cloud Functions

> Node 18 / firebase-functions v2.
> 4 센서의 LiveData → StatsData 자동 집계 트리거.

## 핵심 함수

| Trigger | 파일 | 동작 |
|---|---|---|
| `/USER/LiveData/heartRate` 쓰기 시 | `src/heartRateFunctions.js` | 심박수 통계 (`/USER/StatsData/heartRate/YYYY/MM/weekN/`) 자동 계산 |
| `/USER/LiveData/steps` 쓰기 시 | `src/stepsFunctions.js` | 걸음수 (움직임) 통계 자동 계산 |

→ 화장실 / 수면 통계는 .ino 펌웨어 (`loadcell.v.2.0.0.ino`, `Pir.v.2.0.0.ino`) 가 직접 Firebase 에 쓰기 (Cloud Functions 미사용).

## 빌드 / 배포

```bash
cd functions
npm install
firebase deploy --only functions
```

## ★ `database.rules.json` — 학습 흔적 박제

```json
{ "rules": { ".read": false, ".write": false } }
```

학부 데모 단계 (학기 중) 에는 `read: true, write: true` 였습니다 (누구나 DB 전체 r/w 가능). GitHub push 전 `false/false` 로 차단했지만, **이 사실 자체를 README 의 "본인 톤" 섹션에 박제** — 학부생이 Firebase Security Rules 의 무게를 처음 깨달은 시점.

## 두 Firebase 프로젝트

본 코드는 두 Firebase 프로젝트를 가리킵니다:

- **`monitoring-a6b20`** — Android 앱 + Arduino 펌웨어 (LiveData 쓰기)
- **`wearable-e1302`** — Cloud Functions 가 LiveData 를 읽어 StatsData 계산

학기 중간에 프로젝트가 둘로 분리된 이유는 명확하지 않음 — 본인 톤 자산의 일부.

## 의존성 (functions/package.json)

```json
{
  "engines": { "node": "18" },
  "dependencies": {
    "firebase-admin": "^12.6.0",
    "firebase-functions": "^6.0.1"
  }
}
```

## 외부 자격증명 (실제 배포 시)

- `serviceAccountKey.json` 은 본 레포에 포함되지 않습니다. 로컬 에뮬레이션 시 `index.js` 가 참조하므로 Firebase 콘솔에서 다운로드 필요.
- 배포 환경에서는 `admin.credential.applicationDefault()` 사용 → IAM 권한 기반.
