# ElderCaringApp — Health Monitoring System for Seniors Living Alone

🇰🇷 [한국어](README.md) · 🇬🇧 English

> An undergraduate team project that measures the daily life of a senior living alone (sitting, heart rate, movement, toilet use) by **combining four sensors at once — a load cell (HX711), a MAX30102 heart-rate sensor, an MPU-6050 gyroscope, and an HC-SR501 PIR** — so that a guardian can check the senior's condition in real time, *as if they were nearby even while far away*. This was the **redesign phase** of the 2024 Fall course *"Design and Project Fundamentals II"* in the Department of Medical IT Engineering — a 4-person team project carried out right after [SchoolbusRFID](https://github.com/MoriochoRadio/SchoolbusRFID) (the reverse-engineering phase) in the same course, same semester.

![Status](https://img.shields.io/badge/status-preserved-blue) ![Year](https://img.shields.io/badge/year-2024--2-blue) ![Platform](https://img.shields.io/badge/platform-Android%20%2B%20ESP32-success) ![Stack](https://img.shields.io/badge/Android%20Native-Java-orange) ![Backend](https://img.shields.io/badge/Firebase-Realtime%20DB%20%2B%20Functions-yellow) ![Hardware](https://img.shields.io/badge/sensors-HX711%20%2B%20MAX30102%20%2B%20MPU--6050%20%2B%20PIR-red)

![Project Intro](assets/screenshots/project_purpose.png)

> Project introduction from the presentation slides — **Caring** = care + the present-progressive *-ing*. The turtle icon is a symbol of longevity.

---

## ⚠ About the Build Status

This repository **preserves the code exactly as it was at the December 2024 demo**. Unlike its sister project [`MedQueue`](https://github.com/MoriochoRadio/MedQueue) (no longer buildable due to the end of Xamarin support), ElderCaringApp was written on the same up-to-date 2024 stack as its sibling project [`SchoolbusRFID`](https://github.com/MoriochoRadio/SchoolbusRFID) (Gradle, AGP 8.6+, Android Java compileSdk 34, Firebase 21.x, ESP32 Arduino) and was buildable at the time of writing.

However, future buildability may change for the following reasons:
- Firebase dependencies recommend migration to 22+
- API changes in the ESP32 board package / HX711 library
- Android SDK deprecation policies

All secrets in the code (2 Firebase Auth Secrets, 4 pairs of WiFi credentials, a Google Maps API key, and two `google-services.json` files) have been replaced with masked dummies, and the credentials of the two Firebase projects (`monitoring-a6b20`, `wearable-e1302`) have been revoked and invalidated from the console. To build it yourself, you need to fill in `google-services.json.example` in [`src/android/`](src/android/) and the WiFi/Firebase placeholders in [`src/firmware/*.ino`](src/firmware/) with values for your own environment.

---

## 📚 Project Context

| Item | Details |
|---|---|
| **Period** | 2024 Fall semester (development period: 2024.08.26 ~ 2024.12.13, about 4 months) |
| **Affiliation** | Dept. of Medical IT Engineering, Konyang University |
| **Course** | Design and Project Fundamentals II (★ **redesign** phase — following the [SchoolbusRFID reverse engineering](https://github.com/MoriochoRadio/SchoolbusRFID) in the same semester) |
| **Advisors** | Professors ◯◯◯ · ◯◯◯ |
| **Team** | Team 2, Caring — 4 members (PM ◯◯◯ · QA ◯◯◯ · CM ◯◯◯ · **ENG Kim TaeKyoung (me)**) |
| **My (KimTaeKyoung) role** | **ENG — load cell (HX711) hardware lead + UI+backend of 4 statistics screens in the Android app (heart rate / sleep / movement / toilet) + software code management + midterm and final presentations** |
| **My HW lead area** | Load cell (SEN-10245 + HX711 24-bit ADC) circuit design + firmware (`src/firmware/Loadcell.v.*.ino`) + enclosure |
| **My SW lead area** | `activity_heart_rate_statistics.java` (675 lines), `activity_movement_statistics.java` (678 lines), `activity_sleep_time_statistics.java` (748 lines), `UserActivity.java`, UI/XML layouts for the 4 screens |
| **Shared work** | Main screen, login/sign-up, toilet/overall statistics, Cloud Functions, Firebase integration |

---

## ❓ Problem Definition

![Problem - Aging](assets/screenshots/problem_aging.png)

> *"Single-person senior households are increasing rapidly, and the health care of seniors living alone is vulnerable because guardians are not nearby."*

In South Korea, single-person senior households are increasing rapidly, and because guardians live far away, it is hard to know *"how my parent is doing right now"* in real time. By the time a sudden heart-rate abnormality, a lack of deep sleep, or prolonged periods without movement are discovered, it is often already too late.

This project attempted to solve that problem with a system that *"measures four facets of daily life (heart rate, sleep, movement, toilet use) simultaneously with four sensors and shares them with the guardian in real time."*

---

## 💡 Solution

![Solution Overview](assets/screenshots/solution_caring_overview.png)

> **"Guardian monitoring — detect warning signs and manage health. Analyzing the daily life patterns of seniors living alone: heart rate · sleep time · movement · urination activity"**

### Conclusion reached through market research

| | TouchCare | Galaxy Watch / Apple Watch | **Caring (this project)** |
|---|---|---|---|
| **Strengths** | Senior-friendly UI | Accurate biometric measurement | Combination of both strengths |
| **Weaknesses** | Limited measurement accuracy | Not senior-friendly (hard to wear and operate) | — |

![Caring Strategy](assets/screenshots/solution_caring.png)

→ *"Build a tailored monitoring system that combines the strengths of TouchCare with the strengths of smartwatches"* — that was the core strategy of this project. The four sensors were distributed so that measurement happens naturally in daily life, without the *burden of the senior having to wear or operate anything extra*.

---

## 🛠️ Tech Stack

![Team and Dev Environment](assets/architecture/team_and_dev_env.png)

| Area | Technology |
|---|---|
| **Mobile** | Android Native (Java) · Gradle · AGP 8.6+ · compileSdk 34 · minSdk 24 |
| **Firmware** | Arduino C++ · ESP32 D32 PRO (4 boards — one per sensor) |
| **Sensors** | HX711 24-bit ADC + SEN-10245 load cell (★ my lead area) · MAX30102 heart rate · MPU-6050 gyroscope · HC-SR501 PIR |
| **Backend** | Firebase Realtime DB (×2 projects — `monitoring-a6b20`, `wearable-e1302`) · Firebase Auth · Firebase Cloud Functions (Node 18) |
| **Visualization** | GraphView 4.2.2 (BarChart / LineGraph) |
| **Dev environment** | Android Studio · Arduino IDE · IntelliJ IDEA |

**Key dependencies** (`src/android/app/build.gradle`):
- `firebase-database`, `firebase-auth`, `firebase-functions`
- `com.jjoe64:graphview:4.2.2` (BarChart / LineGraph for the 4 statistics screens)
- Firmware: `HX711` (bogde), `MAX30105` (SparkFun), `MPU6050` (Electronic Cats), `FirebaseESP32` (Mobizt), `ArduinoJson`

---

## 🤔 Why We Built It This Way — Technology Choice Q&A

**Q. Why this combination of four sensors?**
It maps sensors 1:1 to the four facets of a senior's daily life pattern defined in the presentation — heart rate, sleep, movement, and urination: MAX30102 (heart rate), MPU-6050 (tossing and turning in bed → sleep depth), HC-SR501 PIR (movement in the living space), and the HX711 load cell (time spent sitting on the toilet/chair). The conclusion of our market research (TouchCare vs. smartwatches) was to distribute the sensors across the bed, toilet, and living space so that measurement happens naturally in daily life, without the *burden of wearing or operating* something like a smartwatch.

**Q. Why four ESP32 boards, one per sensor?**
The four sensors had to be installed in different places (toilet/chair, finger, bed, living room), so physical distribution was necessary. The choice mirrors, at the hardware level, the parallel multi-stream architecture where each node independently uploads directly to Firebase `/LiveData` over WiFi.

**Q. Why Firebase (Realtime DB + Cloud Functions)?**
Without a separate backend server, the ESP32s write directly via the `FirebaseESP32` library, and the app reflects changes in real time just by subscribing with listeners. Aggregation is computed automatically by Cloud Functions from `/LiveData` → `/StatsData`, so the statistics screens only read `/StatsData` instead of recomputing raw signals every time. It is also an extension of the Firebase real-time pattern validated in [SchoolbusRFID](https://github.com/MoriochoRadio/SchoolbusRFID) in the same semester.

**Q. Why Android native (Java)?**
The guardian app targeted Android only, and we wrote it on top of the team's Java learning foundation ([study-java](https://github.com/MoriochoRadio/study-java)) using the standard stack as of 2024 (AGP 8.6+, compileSdk 34). This contrasts with [MedQueue](https://github.com/MoriochoRadio/MedQueue) from the previous semester, which became unbuildable when official support for its cross-platform framework (Xamarin.Forms) ended.

**Q. Why thresholds like 15 seconds and 60-100 bpm?**
These are medical criteria transferred into code thresholds — a minimum sitting time of 15 seconds to filter out false positives such as clothing briefly resting on the sensor, a normal heart rate of 60–100 bpm, a recommended 7–9 hours of sleep, and so on (based on the 9 medical papers in the presentation's references). That said, the load cell has an undergraduate-level limitation: thresholds were set by tuning raw ADC values without quantitative calibration (`set_scale()`), and this too is recorded honestly below.

---

## 🏗️ System Design

The distinguishing point of this project is the **multi-stream full stack of hardware (4 sensors) + mobile (5 statistics screens) + cloud (Firebase + Functions)**. It extends SchoolbusRFID's *"single RFID signal → Android 3-role"* pattern into **"4-sensor multi-stream → Android 5 statistics screens + Cloud Functions aggregation."**

```
┌─────────────────────────────────────────────────────────────────────┐
│  4 ESP32 nodes (parallel, distributed across the senior's home)      │
├──────────────┬──────────────┬──────────────┬─────────────────────────┤
│ HX711 + load │ MAX30102     │ MPU-6050     │ HC-SR501 PIR             │
│ cell ★ mine  │ heart rate   │ gyroscope    │                          │
│ toilet/chair │ fingertip    │ tossing in   │ movement detection in    │
│ weight       │ heart rate   │ bed          │ the living space         │
└──────┬───────┴──────┬───────┴──────┬───────┴──────┬──────────────────┘
       │ WiFi         │ WiFi         │ WiFi         │ WiFi
       ▼              ▼              ▼              ▼
┌─────────────────────────────────────────────────────────────────────┐
│  Firebase Realtime DB                                                │
│  ├── /LiveData/    (real-time raw signals — every sensor node        │
│  │                  writes directly)                                 │
│  └── /StatsData/   (aggregated data — computed automatically by      │
│                     Cloud Functions)                                 │
└──────┬──────────────────────────────────────────────────────────────┘
       │ subscribe (Android Java)
       ▼
┌─────────────────────────────────────────────────────────────────────┐
│  Android app (guardian screens — 5 statistics screens)               │
├─────────────────────────┬─────────────────────────────────────────────┤
│  ★ Heart rate stats (me)│ activity_heart_rate_statistics.java (675 ln)  │
│  ★ Sleep time stats (me)│ activity_sleep_time_statistics.java (748 ln)  │
│  ★ Movement stats (me)  │ activity_movement_statistics.java (678 ln)    │
│  Toilet stats (shared)  │ activity_toilet_statistics.java               │
│  Overall stats (shared) │ StatisticsActivity.java                       │
└─────────────────────────┴─────────────────────────────────────────────┘
```

### System Structure (system connection diagram)

![System Structure](assets/architecture/system_structure.png)

### System Architecture (5-layer)

![System Architecture](assets/architecture/system_architecture.png)

---

## ✨ Key Features

### Distributed measurement with 4 sensors

| Sensor | Owner | Measurement target | PNG |
|---|---|---|---|
| **HX711 + SEN-10245 load cell** | ★ **Kim TaeKyoung (me)** | Toilet/chair usage time (15-second minimum sitting threshold) | ![Loadcell](assets/screenshots/sensor_loadcell_hx711.png) |
| MAX30102 | ◯◯◯ + ◯◯◯ (shared) | Heart rate (60-100 bpm normal range) | ![MAX30102](assets/screenshots/sensor_max30102.png) |
| MPU-6050 | ◯◯◯ | Gyroscope (tossing in bed → sleep depth) | ![MPU6050](assets/screenshots/sensor_mpu6050.png) |
| HC-SR501 PIR | ◯◯◯ | Movement detection in the living space | ![PIR](assets/screenshots/sensor_pir.png) |

### 4 statistics screens — my lead area

Each screen consists of a **time-series graph in 4 units (day/week/month/year)** plus max/average/min figures.

| | |
|---|---|
| **Heart rate statistics (me)** | **Sleep time statistics (me)** |
| ![Heart Rate](assets/screenshots/ui_heart_rate_stats.png) | ![Sleep Time](assets/screenshots/ui_sleep_time_stats.png) |
| **Movement statistics (me)** | **Login / Main (shared)** |
| ![Movement](assets/screenshots/ui_movement_stats.png) | ![Main + Login](assets/screenshots/ui_main_screen.png) |

### Demo video

<video src="assets/videos/demo_video.mp4" poster="assets/videos/demo_video_poster.png" controls width="800"></video>

> If the video above does not display, play [`assets/videos/demo_video.mp4`](assets/videos/demo_video.mp4) directly (24 MB, 0:53, 1280×720).

---

## 👤 My Contribution (KimTaeKyoung · ENG)

Since this was a team project, I clearly separate the parts I owned and wrote myself from the parts we worked on together.

### 1) HW lead — load cell (HX711) circuit and firmware

My core area in this project was **the circuit design and firmware for the load cell (HX711 ADC + SEN-10245)**. From slide 24 of the presentation, which I wrote:

> *"Sensor configuration: load cell (SEN-10245), load cell amplifier (HX711). Sensor operating principle: the load cell converts pressure applied to an object into an electrical signal to measure weight. The signal is amplified to provide an accurate weight value. Device flow: user sits down → measurement starts when a large weight change is detected → user state and time are recorded → when the weight drops again, the usage time is computed → uploaded to Firebase LiveData."*

The firmware evolved through 3 main versions — `src/firmware/Loadcell.v.1.1.0.ino` → `Loadcell.v.1.2.0.ino` → `loadcell.v.2.0.0.ino` — plus 4 early starts in `_archive/` (`roadcell0.1.0.ino`, `Loadcell.V.1.1.0.ino` (uppercase V), `Loadcell0.2.0.ino`, `Loadcell1.0.0.ino`). The trail from the first spike at the start of the semester to the stable version just before the demo is preserved as is.

### 2) SW lead — 4 Android statistics screens (my area)

I was the main owner of the **UI + backend logic (Firebase subscriptions / GraphView visualization / time-series aggregation)** for the following 4 `.java` files:

```
src/android/app/src/main/java/com/example/caring01/
├── activity_heart_rate_statistics.java     ★ 675 lines (116 Korean comments)
├── activity_movement_statistics.java       ★ 678 lines (113 Korean comments)
├── activity_sleep_time_statistics.java     ★ 748 lines (142 Korean comments)
└── UserActivity.java                       (29 lines)
```

**Shared work** (done together with the team): `MainActivity`, `LoginActivity`, `JoinActivity`, `StatisticsActivity` (overall statistics), `activity_toilet_statistics.java`, `ToiletUsage`/`ToiletUsageAdapter`, Cloud Functions, initial Firebase integration setup.

### 3) ★ "The statistics screens I kept revising" — data as proof

The [`docs/_archive/iterations/`](docs/_archive/iterations/) folder in this repo preserves **28 stages of code snapshots** from the semester. For the 3 screens I owned, **I rewrote each screen 7 to 11 times over the course of the semester**:

| My file | Total snapshots | Unique MD5 variants | Change count | Pattern |
|---|---|---|---|---|
| `activity_heart_rate_statistics.java` | 27 | **10** | **10 times** | Continuously changed all semester |
| `activity_movement_statistics.java` | 24 | **10** | **11 times** | ★ **Most actively revised** |
| `activity_sleep_time_statistics.java` | 27 | 6 | 7 times | Stable through 0.9.7~0.9.9.7, then 4 concentrated changes at the end |

Compared with files owned mainly by other teammates:

| Files not mine | Pattern |
|---|---|
| `MainActivity.java` | **Stable across 18 consecutive zips** (28e25cd1 / 4280 B unchanged) — barely touched after initial writing |
| `LoginActivity.java` | Stable across 21 consecutive zips (6067406d / 3955 B) |
| `StatisticsActivity.java` | Early stub at 0.9.0 (345 B) → completed as e1e6eaae (20383 B) at 0.9.5, then stable for 14 zips |

> *"The 3 statistics screens were the files touched most often throughout the semester. heart_rate changed hashes 10 times and movement 11 times — meaning I rewrote each screen about 10 times on average. From tiny pixel adjustments in the UI to redesigning Firebase paths, my own area was the one I revised relentlessly all semester."*

→ This is the **exact opposite of my tone** in the sister project [SchoolbusRFID](https://github.com/MoriochoRadio/SchoolbusRFID), where *"MainActivity.java was bit-identical across all 5 snapshots — it never wavered all semester."* Within the same semester, my way of working showed up in two modes: *"unwavering stability"* and *"relentless polishing."*

> For the full time-series evolution matrix of 12 files × 28 snapshots, see [`docs/_archive/iterations/README.md`](docs/_archive/iterations/README.md).

### 4) 371 lines of Korean domain comments

The 3 statistics screens I owned contain a total of **371 lines** of Korean comments — traces of me spelling out the domain meaning in my own words (translated to English below):

```java
// activity_heart_rate_statistics.java — Firebase real-time sync + 24-hour average
mDatabase.child("StatsData").child(uid).child("heart_rate").child(dateStr)
    .addValueEventListener(new ValueEventListener() {
        @Override
        public void onDataChange(@NonNull DataSnapshot snapshot) {
            // Collect heart-rate data over the 24 hours of the day
            for (DataSnapshot hourSnap : snapshot.getChildren()) {
                Integer bpm = hourSnap.child("avg").getValue(Integer.class);
                if (bpm != null) {
                    // Branch colors based on the normal range of 60~100 BPM
                    int barColor = (bpm < 60 || bpm > 100) ? Color.RED : Color.BLUE;
                    series.appendData(new DataPoint(hour, bpm), false, 24);
                }
            }
            // Add X-axis data (set so it does not go all the way to the top)
            graph.getViewport().setMaxY(140);
        }
    });
```

```java
// activity_movement_statistics.java — visualization debugging traces
// Adjust the viewport so the average line does not climb to the very top of the graph
// Set setMaxY with a +20% margin above the data's maximum value
// (if this value is set wrong, the tops of the BarChart bars get clipped)
```

```java
// activity_sleep_time_statistics.java — domain thresholds
// If sleep time is under 5 hours, use the warning color (red)
// 7 hours or more gets the good color (blue) — applying the recommended 7-9 hours for adults
if (sleepHours < 5.0) {
    bar.setSpacing(barColorWarning);
} else if (sleepHours >= 7.0) {
    bar.setSpacing(barColorGood);
}
```

---

## 📅 Project Schedule

![Gantt Chart](assets/architecture/gantt_chart.png)

Semester Gantt chart — 2024.08.26 ~ 2024.12.13, about 4 months. Within a single semester we completed the multi-stream system of **hardware (4 ESP32 + 4 sensors) + mobile (5 statistics screens) + cloud (Firebase RDB + Cloud Functions)**.

---

## 🎯 Expected Effects

![Expected Effects](assets/screenshots/outcome_expected_effects.png)

> *"Early detection and prevention of health warning signs. Long-term health trend analysis. Emotional reassurance — guardians can check on their parent's condition at any time through monitoring, and health tracking provides emotional stability to both the guardian and the senior living alone."*

The limitations of this project as an undergraduate demo, and the areas for improvement, were also stated honestly in the presentation:

| Improvement item (slide 41) | Details |
|---|---|
| Senior-friendly UI | Larger text / color contrast / simpler navigation needed |
| Push notifications | Immediate guardian alerts on warning signs not implemented |
| Professional-institution linkage | Direct linkage to medical institutions / emergency services not implemented |
| Sensor accuracy | Direct comparison of raw ADC values across 4 sensors — no quantitative calibration applied |

---

## ✍️ Code That Keeps My Own Voice

The code in this repo is not a *polished final product* — it is the raw trail left by *four undergraduate sophomores touching a 4-sensor IoT system + Firebase + Cloud Functions + 4 statistics screens for the first time, all at once*. Things deliberately left unpolished:

### 1) The load-cell workaround solved with a single circuit diagram

```
<A temporary workaround using two half-bridge load cells and one HX711 amplifier>

1. Load cell workaround
When connecting two half-bridge load cells to form a complete bridge,
a signal is produced only when the load is applied to one of the load cells.
If the same load is applied to both load cells, the signals cancel out
and nothing is detected.

2. Parts and connections
Parallel connection of load cells 1 and 2
Connect the white wire of load cell 1 and the white wire of load cell 2 to the E+ pin of the HX711
Connect the black wire of load cell 1 and the black wire of load cell 2 to the E- pin of the HX711
Connect the red wire of load cell 1 to the A+ pin of the HX711
Connect the red wire of load cell 2 to the A- pin of the HX711
→ Applying weight to load cell 1 produces a positive signal; applying weight to load cell 2 produces a negative signal

3. Connecting the ESP32 and HX711
HX711 DT pin → ESP32 GPIO 18
HX711 SCK pin → ESP32 GPIO 19
```
*— full text of `src/firmware/로드셀_임시_작동법.txt` (translated from Korean)*

This is the trace of me inventing, with a hand-drawn circuit diagram early in the semester, a *"workaround that combines two half-bridge load cells into a single HX711."* *"If the same load is applied to both load cells, the signals cancel out and nothing is detected"* — that one line preserves the thought process of an undergraduate learning the domain hands-on.

### 2) The `weightThreshold` 70 → 200 magic-number jump

```cpp
// src/firmware/Loadcell.v.1.1.0.ino:26   (early)
const float weightThreshold = 70.0;

// src/firmware/Loadcell.v.1.2.0.ino:26   (middle)
const float weightThreshold = 70.0;

// src/firmware/loadcell.v.2.0.0.ino:34   (final)
const float weightThreshold = 200.0;   // ← jump from 70 → 200!
```

When the *parallel two-load-cell connection* from 1) above was introduced, the signal strength roughly tripled, so the threshold was jumped from 70 → 200. An honest trace of undergraduate trial and error, **solved by magic-number tuning without quantitative calibration (`set_scale()`)**.

### 3) `scale.set_scale()` never called — comparing raw ADC values directly

```cpp
// src/firmware/loadcell.v.2.0.0.ino — set_scale() not called
scale.begin(LOADCELL_DOUT_PIN, LOADCELL_SCK_PIN);
scale.tare();   // only zero-point calibration
// scale.set_scale(N.NN);   ← no quantitative calibration call

// raw HX711 ADC value compared directly against a magic number (200)
float currentWeight = scale.get_units(10);
if (currentWeight > weightThreshold) { ... }
```

Only zeroing (`tare`) was done; quantitative calibration (`set_scale`) was never called even once. The *"if it works, it's OK"* undergraduate pattern of comparing raw HX711 ADC values directly against magic numbers (70, 200) is consistent across all 7 `.ino` files.

### 4) The 15-second `minSitDuration` — an elder-care domain threshold

```cpp
// src/firmware/loadcell.v.2.0.0.ino:35
const unsigned long minSitDuration = 15000; // must sit for at least 15 seconds (milliseconds)
...
if (sittingDuration >= minSitDuration) {
    int sdSeconds = sittingDuration / 1000;
    ...
    Serial.println("사용자가 일어났습니다.");
}
```

*"Cases like clothing briefly resting on the sensor do not count as toilet use"* — a trace of thinking through the elder-care domain directly. The number **15 seconds** carries the embedded domain meaning: *"this long means the senior was actually sitting on the toilet."* (The `Serial.println` string means "The user has stood up.")

### 5) The permanent `roadcell` typo + uppercase `V` naming

The naming matrix in `src/firmware/_archive/`:

```
roadcell0.1.0.ino           ← permanent "loadcell" → "roadcell" typo
Loadcell.V.1.1.0.ino        ← uppercase V (other files use lowercase v)
Loadcell0.2.0.ino           ← no dots
Loadcell1.0.0.ino           ← no dots
```

A trace of misspelling one letter of a filename in the first spike early in the semester (`loadcell` ↔ `roadcell`). Later files corrected it, but these two files were preserved as evidence of learning authenticity. The naming-consistency immaturity of mixing *uppercase V* and *lowercase v* in the same folder is preserved along with it.

### 6) FernFlower decompilation-recovery traces in `JoinActivity.java`

```java
//
// Source code recreated from a .class file by IntelliJ IDEA
// (powered by FernFlower decompiler)
//

package com.example.caring01;
```
*— `src/android/app/src/main/java/com/example/caring01/JoinActivity.java` lines 1-5*

At some point in the semester the original `.java` for `JoinActivity.java` was lost and recovered from the `.class` bytecode with IntelliJ's FernFlower decompiler — and that trace is preserved as is. All 24 zips since `iterations/caring0.9.0` are frozen in the same decompiled state, and the time-series archive preserves *not just the result but the trail of thinking*.

### 7) The honest security naivety of the undergraduate demo stage

**(a) Database rules — unrestricted r/w kept all semester**:
```json
// The state actually used during the semester (the semester version of src/function-source/database.rules.json)
{ "rules": { ".read": true, ".write": true } }
```
→ Locked down to `false / false` during cleanup, but this is the trace of a phase in which anyone could read and write all data in both Firebase projects during the undergraduate demo stage.

**(b) Plaintext password storage in `2024-2-Monitoring.json`**:
```json
"users": {
  "-OBZGJACYz__3vv9wojM": {
    "id": "hgd12345",
    "password": "hgd12345",      ← Firebase Auth not used; stored in plaintext
    "phone": "01012345678"
  }
}
```
→ A preserved trace of undergraduate security naivety: instead of adopting a dedicated authentication service like Firebase Authentication, passwords were stored in plaintext directly under the `users` node of the Firebase RDB. In the subsequent capstone ([seed-project](https://github.com/MoriochoRadio/seed-project)) we moved to Firebase Auth.

### 8) The default package name `com.example.caring01` left as is

```gradle
// src/android/app/build.gradle
namespace 'com.example.caring01'
applicationId "com.example.caring01"
```

An honest undergraduate trace of leaving Android Studio's default template package name `com.example.*` untouched right up to the demo. In actual production it should have been changed to something like `kr.ac.konyang.eldercare`, but an undergraduate demo had no bandwidth for details like this.

---

## 📂 Archive

| Material | Path | Notes |
|---|---|---|
| 🎤 Final presentation (51 slides) | [`docs/presentation_final_redacted.pptx`](docs/presentation_final_redacted.pptx) | Pushed via Git LFS (134 MB) |
| 📑 Final report (252 pages) | [`docs/final_report_redacted.pdf`](docs/final_report_redacted.pdf) | Automatic HWP→PDF conversion via Hancom |
| 📖 Manuals I wrote (4) | [`docs/manuals/`](docs/manuals/) | Load cell wiring/setup + manuals for my 3 statistics screens |
| 📖 ESP32 board environment setup guide | [`docs/esp32_setup_guide_redacted.docx`](docs/esp32_setup_guide_redacted.docx) | Written by the team |
| 📊 Firebase RDB schema export | [`docs/2024-2-Monitoring.json`](docs/2024-2-Monitoring.json) | Dummy data only, for schema study |
| 🎬 Demo video | [`assets/videos/demo_video.mp4`](assets/videos/demo_video.mp4) | 0:53 / 1280×720 |
| 📦 Time-series code snapshots (28 stages) | [`docs/_archive/iterations/`](docs/_archive/iterations/) | caring 0.9.0 ~ 0.9.9.9 (18 main) + caring01 ~ 08 (9 parallel branches) |
| 🔌 Firmware guide | [`src/firmware/README.md`](src/firmware/README.md) | HX711 pinmap + libraries |
| ⚡ Cloud Functions | [`src/function-source/`](src/function-source/) | Automatic LiveData → StatsData aggregation |

> 📌 The names of the 3 teammates and 2 professors other than me have been masked as `◯◯◯` in the body and metadata of the presentation materials. The 7 manuals by teammates other than me and the 1.5 GB consolidated backup zip are kept in `docs/_archive/local-only/` and are not pushed to GitHub.

---

## 📚 References

Categories of materials cited by this project (20 in total — see slides 42-46 of the presentation):

- **Statistics on single-person senior households and solitary deaths** (4 items) — Ministry of Health and Welfare · Statistics Korea
- **Articles and papers on medical accessibility for seniors living alone** (7 items) — medical press / academic journals
- **Evidence on the relationship between heart rate, sleep, movement, urination, and health** (9 items) — medical papers (Lancet, JAMA, The Korean Gerontological Society, etc.)

The detailed bibliography is compiled on slides 42-46 of the presentation.

---

## ✍️ Retrospective

We tried to address the social problem of the rise in single-person senior households with a system that *"measures one senior's daily life (sitting, heart rate, movement, toilet use) with four sensors bundled together, checked by the guardian in real time."* Within that, I owned the **load cell (HX711) hardware** and the **backend + UI of 3 statistics screens (heart rate / sleep / movement)** in the Android app.

It started early in the semester with a hand-drawn circuit diagram and a *workaround combining two half-bridge load cells into a single HX711*. From changing the circuit and magic-number tuning `weightThreshold 70 → 200`, I set domain thresholds like *"must sit at least 15 seconds to count as toilet use"* directly from raw ADC values, without quantitative calibration (`set_scale()`). *"If the same load is applied to both load cells, the signals cancel out and nothing is detected"* — a line like that, jotted in my notes at some point in the semester, survives verbatim in [`src/firmware/로드셀_임시_작동법.txt`](src/firmware/로드셀_임시_작동법.txt).

The Firebase real-time sync + Android 3-role pattern learned in the *reverse-engineering phase* of [`SchoolbusRFID`](https://github.com/MoriochoRadio/SchoolbusRFID) in the same semester expanded here into a *4-stream Firebase + 4 statistics-screen multi-subscription*. In exact contrast to SchoolbusRFID's *"MainActivity bit-identical all semester (`68ce8336` / 1670 B)"*, my main area in this project — the **3 statistics screens rewritten 7 to 11 times each throughout the semester** — is preserved as a trail of relentless revision across the 28 snapshots in [`docs/_archive/iterations/`](docs/_archive/iterations/). *"It never wavered"* versus *"I revised it relentlessly"* — that the same student can show both ways of working within the same semester is, I think, the evolutionary through-line of my whole portfolio.

This project was also the starting point where I learned the medical domain hands-on. Domain thresholds like *"60-100 BPM normal heart-rate range"*, *"7-9 hours of recommended sleep"*, and *"at least 15 seconds of sitting to count as toilet use"* are embedded as magic numbers in the code, and 371 lines of Korean comments preserve the thought process. This experience became the foundation of team collaboration + medical domain work in the [`seed-project`](https://github.com/MoriochoRadio/seed-project) capstone, and expanded into the medical + AI domain of [`AI_Lung_1`](https://github.com/MoriochoRadio/AI_Lung_1) / [`AILungandLiver`](https://github.com/MoriochoRadio/AILungandLiver) / [`sperm-ai`](https://github.com/MoriochoRadio/sperm-ai).

The security naivety of the undergraduate demo stage — the unrestricted `read/write: true` in `database.rules.json`, the plaintext `password` under the `users` node — is also honestly preserved. It is the trace of an undergraduate touching Firebase for the first time without knowing that *"Firebase Authentication needs to be adopted separately"*, but I believe there is value in keeping, as a record, the learning arc itself: knowing better and moving on in the next project.

The trace of the care domain being stretched to both ends within a single course in a single semester — from *a child's single RFID signal (reverse engineering)* to *a senior's 4-sensor composite signal (redesign)* — lives on in the cross-links between these two repos (SchoolbusRFID + ElderCaringApp).

---

## 🗂️ Related Repositories

### The foundational learning that made this project possible
- [`study-java`](https://github.com/MoriochoRadio/study-java) — Android Java OOP / event handlers / anonymous classes
- [`study-windows-programming`](https://github.com/MoriochoRadio/study-windows-programming) — first exposure to Firebase Realtime DB (`022_FireBase` chapter)

### Sibling project in the same course, same semester (★)
- [`SchoolbusRFID`](https://github.com/MoriochoRadio/SchoolbusRFID) — *school bus safety for children* (24-2 Design and Project Fundamentals II · reverse-engineering phase). The sister project where my tone was *"a MainActivity that never wavered all semester."*

### Other projects in the same healthcare/care domain
- [`MedQueue`](https://github.com/MoriochoRadio/MedQueue) — *patient queue monitoring* (Xamarin.Forms · 2024-1)

### The medical + AI projects that followed
- [`AI_Lung_1`](https://github.com/MoriochoRadio/AI_Lung_1) — lung medical imaging AI
- [`AILungandLiver`](https://github.com/MoriochoRadio/AILungandLiver) — lung and liver medical imaging AI
- [`sperm-ai`](https://github.com/MoriochoRadio/sperm-ai) — AI sperm motility analysis
- [`seed-project`](https://github.com/MoriochoRadio/seed-project) — Team SEED capstone (in progress)

---

## License

See [`LICENSE.md`](LICENSE.md). This repository publishes the learning outcomes of an undergraduate team project for portfolio purposes; commercial use requires team agreement.

---

*Author: [MoriochoRadio](https://github.com/MoriochoRadio) (KimTaeKyoung) · Dept. of Medical IT Engineering, Konyang University*
