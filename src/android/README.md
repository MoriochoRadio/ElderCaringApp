# Android 앱 (Caring) — 빌드 가이드

> Java + Firebase Realtime DB 기반 노인 케어 모니터링 앱.
> minSdk 24 / targetSdk 34, Gradle 8.7, Java 1.8 호환.

## 디렉토리 구조

```
src/android/
├── build.gradle, settings.gradle, gradle.properties  (Gradle 8.7)
├── gradlew, gradlew.bat, gradle/wrapper/
└── app/
    ├── build.gradle
    ├── google-services.json.example   ← Firebase 키 템플릿 (실제 키는 마스킹됨)
    ├── proguard-rules.pro
    └── src/main/
        ├── AndroidManifest.xml
        ├── java/com/example/caring01/    ← 패키지명 그대로 (★ 학부생 패턴 보존)
        │   ├── MainActivity.java          ← LAUNCHER
        │   ├── MainActivity2.java
        │   ├── LoginActivity.java
        │   ├── JoinActivity.java          ★ FernFlower 디컴파일 복원본 (.class → .java)
        │   ├── UserActivity.java
        │   ├── StatisticsActivity.java         ← 전체 통계 화면 (◯◯◯ 담당)
        │   ├── ToiletUsage.java                ← 화장실 데이터 모델 (◯◯◯ 담당)
        │   ├── ToiletUsageAdapter.java
        │   ├── activity_toilet_statistics.java ← 화장실 통계 화면 (◯◯◯ 담당)
        │   ├── activity_heart_rate_statistics.java    ★ 본인 (675 줄)
        │   ├── activity_movement_statistics.java      ★ 본인 (678 줄)
        │   └── activity_sleep_time_statistics.java    ★ 본인 (748 줄)
        └── res/  (drawable, layout, mipmap, values)
```

## 빌드 명령

```bash
# Windows
gradlew.bat assembleDebug

# macOS / Linux
./gradlew assembleDebug
```

빌드 산출물: `app/build/outputs/apk/debug/app-debug.apk`

## Firebase 설정 (실제 빌드 시 필요)

본 레포의 `google-services.json` 은 API key 가 마스킹된 템플릿입니다. 실제 빌드/실행하려면:

1. [Firebase 콘솔](https://console.firebase.google.com/) 에서 새 Android 프로젝트 생성
2. 패키지명: `com.example.caring01` 등록
3. `google-services.json` 다운로드 → `app/` 폴더에 저장
4. Firebase Realtime Database 활성화

## 주요 의존성 (app/build.gradle)

- `com.google.firebase:firebase-bom:33.5.1`
- `com.google.firebase:firebase-database:21.0.0`
- `com.google.firebase:firebase-auth:23.1.0`
- `com.jjoe64:graphview:4.2.2` ★ 본인 통계 화면 4 개에서 그래프 렌더링
- `androidx.appcompat:appcompat:1.7.0`
- `com.google.android.material:material:1.12.0`

## 본인 담당 영역

| 파일 | 줄 수 | 한국어 주석 줄 |
|---|---|---|
| `activity_heart_rate_statistics.java` | 675 | 116 |
| `activity_movement_statistics.java` | 678 | 113 |
| `activity_sleep_time_statistics.java` | 748 | 142 |
| (합계) | **2,101 줄 / 371 한국어 주석 줄** | |

본인 외 팀원 4명 (◯◯◯) 의 담당 영역: 로그인/회원가입/메인/화장실 통계/전체 통계 화면.

## 참고

- 본 Android 프로젝트는 `caring02/` 라는 5중 중첩 zip-in-zip 폴더에서 평탄화된 것입니다. 시계열 진화는 `../../docs/_archive/iterations/` 참조.
- `JoinActivity.java` 는 학기 어느 시점에 원본 `.java` 가 손실되어 `.class` 에서 FernFlower 디컴파일로 복원된 흔적이 그대로 박제되어 있습니다. 파일 첫 줄의 *"Source code recreated from a .class file by IntelliJ IDEA"* 주석이 그 증거입니다.
