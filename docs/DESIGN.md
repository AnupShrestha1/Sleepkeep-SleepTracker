# Sleepkeep — App Design

Status: draft for approval · 2026-10-28

Play Store title: **Sleepkeep: Sleep Tracker** (24 of the 30 characters Play allows)

A small native Android app for logging sleep with two buttons, optionally listening for loud
sounds overnight, keeping a history, and comparing the last week against public adult sleep
guidance. Everything stays on the phone.

The original brief is the source of scope. This document records how it will be built and the
decisions taken during review.

---

## 1. Decisions taken during review

| Topic | Decision |
|---|---|
| Sleep duration | The app measures **time in bed** (Start → End). **Sleep estimate** = time in bed − the user's own morning answers (time to fall asleep, time awake). If the check-in is skipped, the night counts as time in bed and the app says so. |
| Schedule consistency | Shown as a description ("bedtime varied by about 40 min"), with no pass/fail grade. CDC and NHLBI give no numeric threshold, so the app doesn't make one up. |
| Sound monitoring | v1 records **loud sound events** relative to the room's own background level. It never uses the word "snoring". A "repeating sound" label is only added after it is tested on real nights. |
| Interruptions | Monitoring writes a heartbeat once a minute. Gaps are shown truthfully ("not monitored 03:12–04:00"). Monitoring cannot restart itself, because Android 14+ only lets the app start the microphone service while it is on screen. |
| What counts as a night | A session belongs to the local date it **ended** on. The longest session ending that day is the night. Shorter ones are naps: shown in history, left out of weekly figures. |
| Enough data | Weekly figures need at least 3 nights in the last 7 days, and always say how many nights they use. Missing nights are not counted as zero. |
| Age | No age input. Every CDC adult group (18–60, 61–64, 65+) starts at 7 hours, so "at least 7 hours for adults" holds. The app states that it is meant for adults. |
| Time | Timestamps are stored as UTC epoch millis plus the session's time-zone ID. Durations come from timestamps, so daylight-saving nights are correct. |
| Backup | Android cloud backup and device-to-device transfer are **disabled**, so data never leaves the phone. |
| Process | Each step is explained with its file list; you review the diff, and I paste code on request. Git, with one commit per finished step. |

---

## 2. Screens and flows

Five screens, one activity, Material 3. Light or dark follows the system. No animations
beyond Material defaults.

### 2.1 Dashboard

Idle:

```
┌──────────────────────────────────┐
│ Sleepkeep                   [⚙]  │
├──────────────────────────────────┤
│ LAST NIGHT · Tue 27 Oct          │
│ 7 h 40 m in bed                  │
│ About 7 h 10 m asleep (your est.)│
│ Bed 23:05 · Up 06:45             │
│ 3 loud sounds · 1 monitoring gap │
│                    View night ›  │
├──────────────────────────────────┤
│ LAST 7 DAYS · 5 nights           │
│ Average sleep 6 h 50 m           │
│ Below the 7-hour adult guideline │
│ Bedtime varied by about 40 min   │
│ Wake time varied by about 15 min │
│ General guidance, not a medical  │
│ assessment.          Details ›   │
├──────────────────────────────────┤
│ Monitor sounds overnight   [ ● ] │
│ ┌──────────────────────────────┐ │
│ │         START SLEEP          │ │
│ └──────────────────────────────┘ │
│           Sleep history ›        │
└──────────────────────────────────┘
```

Sleeping (active session):

```
│ SLEEPING since 23:05 · 2 h 10 m  │
│ Sound monitoring: on             │
│   — or — off (microphone denied) [Settings]
│   — or — stopped around 03:12    [Resume]
│ ┌──────────────────────────────┐ │
│ │          END SLEEP           │ │
│ └──────────────────────────────┘ │
```

- The elapsed time updates once a minute.
- The "Monitor sounds" switch defaults to whatever the previous session used, so no settings
  storage is needed.
- With fewer than 3 nights, the weekly card reads "Track at least 3 nights to see your weekly
  summary (1 so far)".

### 2.2 Start Sleep flow

1. Tap **Start sleep**.
2. If monitoring is on and the microphone isn't granted:
   - First show the app's own explanation: "Used only to notice loud sounds while you sleep.
     Audio is analysed on this phone and never saved or sent anywhere."
   - Then show the system prompt.
   - On Android 13+, notification permission is requested in the same step. It is optional.
3. The session is saved immediately, with no end time.
4. If the microphone was granted, the foreground service starts.
5. If the microphone was denied, the session still starts and the dashboard says monitoring is
   off. A permanent denial shows a button to the app's system settings.

### 2.3 End Sleep flow

1. Tap **End sleep**. The end time is saved and monitoring stops.
2. If the session is longer than 14 h, ask: "This session is 19 h long. Did you forget to end
   it?" The choices are **Set wake-up time** and **Keep**.
3. A morning check-in sheet opens. Both questions are optional, and there is a Skip button.
   - Time to fall asleep: `Under 5 min · 15 · 30 · 45 · 60+ min · Don't know`
   - Time awake during the night: `None · 15 · 30 · 60 · 90+ min · Don't know`
4. Night detail opens.

### 2.4 Sleep history

- A list with the newest first.
- Each row shows the date, `23:05 → 06:45`, the time in bed, and a "Nap" tag where it applies.
- An active session is pinned at the top as "In progress".
- Tapping a row opens night detail.

### 2.5 Night detail

- Bedtime, wake-up time, time in bed, sleep estimate, and the check-in answers.
- **Sound** section (only if monitoring was on):
  - Coverage: "Monitored 7 h 10 m of 7 h 40 m". Each gap shows its times and reason (app
    stopped, mic in use by another app, mic turned off).
  - Loud sounds: the count, then a list of events. Each event shows its time, its duration and
    a word for its loudness (moderate / loud / very loud).
  - A note: "Loud sounds can come from snoring, talking, traffic, pets or anything else. This
    app can't tell them apart."
- Actions: **Edit times**, **Edit check-in**, **Delete** (with a confirmation).

### 2.6 Sleep guidance (from "Details ›")

- **Duration**: the 7-day average, how many nights reached 7 h, and the guideline it is compared
  against.
- **Schedule**: usual bedtime and wake time, with their average variation.
- **About these figures**: the sources, with links and the date each page was checked, plus the
  disclaimer (§6.4).

### 2.7 Settings & privacy

- What is stored and where: on this phone only, with no audio saved and no internet.
- Microphone and notification permission status, with buttons to the system settings.
- "If monitoring stops during the night", with a link to the app's battery settings.
- **Delete all data**, with a confirmation. It stops monitoring first, then clears every table.
- App version.

---

## 3. Architecture

A single `app` module and three layers. Dependencies are wired by hand in an `AppContainer`
held by the `Application`, with no DI framework.

```
 UI (Compose screens + one ViewModel per screen, StateFlow)
   │
   ▼
 SleepRepository ◄──── SoundMonitorService (foreground service)
   │                         │
   ▼                         ▼
 Room (SleepDao)        LoudnessDetector (pure Kotlin)

 Pure logic, unit-tested: SessionRules · SleepStats · LoudnessDetector
```

```
app/src/main/java/<package>/
  SleepApp.kt              Application: builds AppContainer
  AppContainer.kt          database, repository, Clock, MonitorStatus
  MainActivity.kt          edge-to-edge, theme, AppNavHost
  ui/
    AppNavHost.kt          routes: dashboard, history, night/{id}, guidance, settings
    theme/                 Color.kt, Theme.kt, Type.kt (from the template)
    dashboard/             DashboardScreen, DashboardViewModel, CheckInSheet
    history/               HistoryScreen, HistoryViewModel
    night/                 NightDetailScreen, NightDetailViewModel, EditTimesDialog
    guidance/              GuidanceScreen, GuidanceViewModel
    settings/              SettingsScreen, SettingsViewModel
    common/                MicPermission.kt, TimeFormat.kt
  data/
    SleepSession.kt, SoundEvent.kt, MonitoringSegment.kt   (entities)
    SleepDao.kt, SleepDatabase.kt, SleepRepository.kt
    SessionRules.kt        validation: overlap, ordering, limits
  monitor/
    SoundMonitorService.kt, MonitorNotification.kt, MonitorStatus.kt
    LoudnessDetector.kt
  guidance/
    SleepGuidelines.kt     constants, source names, URLs and dates
    SleepStats.kt          night attribution, averages, consistency
```

- `java.time` is used throughout; it is built in from API 26.
- A `Clock` is injected so the rules can be tested with fixed times.
- `MonitorStatus` is an in-memory `StateFlow` that the service updates. After process death it
  resets to "not running", which is how the app notices that monitoring was killed.

---

## 4. Data model (Room, schema v1)

**`sleep_sessions`**

| Column | Type | Notes |
|---|---|---|
| `id` | Long PK | auto |
| `startTime` | Long | UTC epoch ms |
| `endTime` | Long? | `null` = active session |
| `zoneId` | String | e.g. `Asia/Kathmandu`; used to show local times |
| `fallAsleepMinutes` | Int? | morning check-in; `null` = not answered |
| `awakeMinutes` | Int? | morning check-in; `null` = not answered |
| `monitoringRequested` | Boolean | the user turned monitoring on for this session |

**`monitoring_segments`**: the stretches of the night that were actually monitored.

| Column | Type | Notes |
|---|---|---|
| `id` | Long PK | |
| `sessionId` | Long FK | cascade delete, indexed |
| `startTime` | Long | |
| `lastHeartbeat` | Long | updated every 60 s while running |
| `endTime` | Long? | `null` while running |
| `endReason` | String? | `USER_STOPPED`, `SESSION_ENDED`, `MIC_SILENCED`, `MIC_ERROR`, `STOPPED_UNEXPECTEDLY` |

**`sound_events`**

| Column | Type | Notes |
|---|---|---|
| `id` | Long PK | |
| `sessionId` | Long FK | cascade delete, indexed |
| `startTime`, `endTime` | Long | |
| `peakAboveBackgroundDb` | Float | relative level; the UI turns it into a word |

Derived values are computed, not stored:

- time in bed = `endTime − startTime`
- sleep estimate = time in bed − `fallAsleepMinutes` − `awakeMinutes`, treating `null` as 0, with
  a flag when neither was answered

The schema JSON is exported to `app/schemas/` so that later versions can have real migrations.

---

## 5. Session rules (`SessionRules`, `SleepRepository`)

- **One active session.** `startSession()` runs in a Room transaction and refuses if a session
  with `endTime == null` exists. The UI also shows only one button, depending on state.
- **Survives process death.** The active session lives only in Room. On launch the dashboard
  reads it back.
- **Ending.** `endSession()` sets `endTime` and closes any open monitoring segment
  (`SESSION_ENDED`) in the same transaction, then stops the service.
- **Editing times** (night detail). Rules:
  - start < end, and end ≤ now.
  - The duration is between 5 min and 24 h.
  - No overlap with any other session: `start < other.end && end > other.start`.
  - For an active session only the start can be edited.
  - Sound events outside the new range are kept in the database but hidden.
- **Deleting** a session cascades to its segments and events. Deleting the active session stops
  monitoring first.
- **Night attribution** (`SleepStats`): night date = local date of `endTime` in `zoneId`. The
  longest session per night date is the main sleep; the others are naps.

---

## 6. Sound monitoring

### 6.1 Permissions and manifest

```xml
<uses-permission android:name="android.permission.RECORD_AUDIO" />
<uses-permission android:name="android.permission.FOREGROUND_SERVICE" />
<uses-permission android:name="android.permission.FOREGROUND_SERVICE_MICROPHONE" />
<uses-permission android:name="android.permission.POST_NOTIFICATIONS" />

<service android:name=".monitor.SoundMonitorService"
         android:foregroundServiceType="microphone"
         android:exported="false" />
```

- There is no `INTERNET` permission. The merged manifest is checked at each build step and
  before release.
- `REQUEST_IGNORE_BATTERY_OPTIMIZATIONS` isn't used, because Play restricts it. Settings links
  to the app's system page instead.

### 6.2 Service lifecycle

- **Starting.** The service is only started from the visible activity, with
  `startForegroundService` followed by `ServiceCompat.startForeground(..., MICROPHONE)`. A
  failure to start is caught and shown as "monitoring unavailable".
- **Restart behaviour.** `START_NOT_STICKY`. Android 14+ forbids a microphone service from
  starting in the background, so automatic restarts would only work on older phones. Both phones
  behave the same way: the user resumes from the app.
- **Notification.** A low-importance ongoing notification on the "Sleep monitoring" channel:
  "Listening for loud sounds · since 23:05". Tapping it opens the app; its one action is **Stop
  monitoring**, which stops monitoring but leaves the session running. If notification permission
  is denied (Android 13+), the service still runs and the dashboard notes that the notification is
  hidden.
- **Recording.** `AudioRecord` with the `VOICE_RECOGNITION` source (less automatic gain on most
  devices, falling back to `MIC`), mono 16-bit at 16 kHz with a 44.1 kHz fallback. The audio
  buffer is analysed and discarded; nothing is written to storage.
- **The read loop** runs on one background thread and does three things:
  - computes a level every 100 ms
  - feeds the level to `LoudnessDetector`
  - updates the segment's heartbeat every 60 s
- **Wake lock.** None is planned, because an active recording normally keeps the audio path
  awake. The overnight tests in step 7 check this; if a test phone sleeps the CPU, a partial wake
  lock is added.

### 6.3 Interruptions: always shown, never hidden

| What happens | How it's detected | What the user sees |
|---|---|---|
| Mic permission denied | permission check | "Sound monitoring off — microphone not allowed" + Settings button |
| App/process killed (battery saver, OOM, permission revoked, reboot) | On launch: an open segment with an old heartbeat while `MonitorStatus` is not running. The segment is closed at `lastHeartbeat` with `STOPPED_UNEXPECTEDLY`. | "Monitoring stopped around 03:12" + **Resume** |
| Another app takes the mic (a call) | `AudioRecordingCallback.isClientSilenced` (API 29+); all-zero audio on older versions | A gap labelled "mic in use by another app" |
| Mic privacy toggle off (Android 12+) | Exactly-zero samples for 10 s | A gap labelled "microphone turned off" |
| `AudioRecord` error | the read returns an error code | The segment closes with `MIC_ERROR`; the notification says monitoring stopped |

### 6.4 `LoudnessDetector` (pure Kotlin, unit-tested)

The input is a level in dBFS every 100 ms. All values below are starting points to tune in
step 7.

- **Background level**: the 20th percentile of the last 5 minutes of levels, recomputed every
  5 s. A constant fan becomes background within a few minutes.
- **Warm-up**: no events during the first 60 s.
- **Event start**: level ≥ background + 15 dB **and** ≥ −50 dBFS for 3 windows in a row
  (300 ms). The absolute floor stops tiny sounds in a near-silent room from counting.
- **Event end**: level < background + 10 dB for 20 windows in a row (2 s).
- **Output**: start, end, and peak above background. The UI's words are:
  - 15–25 dB: moderate
  - 25–35 dB: loud
  - 35 dB and up: very loud

Phone microphones aren't calibrated, so the app never shows decibel numbers.

---

## 7. Sleep guidance

### 7.1 Stored guidance (`SleepGuidelines.kt`)

The guidance is stored in code, so it works offline.

| Source | Adult recommendation | Page date (checked 2026-10-28) |
|---|---|---|
| CDC, [About Sleep](https://www.cdc.gov/sleep/about/index.html) | 18–60: 7 or more hours · 61–64: 7–9 · 65+: 7–8. Also says to go to bed and get up at the same time every day. | May 15, 2024 |
| NHLBI, [How Much Sleep Is Enough?](https://www.nhlbi.nih.gov/health/sleep/how-much-sleep) | Adults: 7–9 hours a night | March 24, 2022 |

The check uses one number: **at least 7 hours**, which every adult group above shares. Links
open in the browser, so the app itself needs no internet.

### 7.2 Calculations (`SleepStats.kt`)

- **Window**: main sleeps whose night date falls in the last 7 calendar days, today included.
- **Duration**:
  - the mean sleep estimate over those nights
  - whether that mean is ≥ 7 h
  - the count of nights ≥ 7 h
  - the count of nights that fell back to time in bed
- **Consistency**, measured separately for bedtimes and wake times:
  - The times are local clock times treated as positions on a 24-hour circle.
  - The **usual time** is their circular mean.
  - The **variation** is the mean absolute difference from it, in minutes, wrapped to ±12 h. So
    23:30 and 00:30 are 60 min apart.
- **Minimum**: 3 nights; otherwise the result is "not enough data".

### 7.3 Wording (in `strings.xml`)

- *Duration met*: "Your average sleep over the last 7 days was 7 h 20 m, based on 5 nights.
  That meets the general guideline of at least 7 hours for adults."
- *Duration not met*: "… 6 h 30 m … That's below the general guideline of at least 7 hours for
  adults. 2 of 5 nights reached 7 hours."
- *Time-in-bed caveat*: "3 of these nights use time in bed because the morning check-in was
  skipped. Time in bed is usually a little longer than actual sleep."
- *Schedule*: "You usually went to bed around 23:20. On average your bedtime was 40 minutes away
  from that. You usually woke around 06:50, varying by about 15 minutes. The CDC recommends going
  to bed and getting up at the same time every day."
- *Disclaimer*, shown on the dashboard card in short and in full on the guidance screen: "These
  are general guidelines for adults, not a medical assessment. Sleep needs vary. This app doesn't
  measure sleep stages or detect sleep disorders. If you're concerned about your sleep, talk to a
  doctor. Recommended amounts are higher for people under 18."

---

## 8. Privacy

- **No account and no backend.** No ads, no analytics, no crash-reporting SDKs.
- **No `INTERNET` permission.**
- **`android:allowBackup="false"`.** A `dataExtractionRules` file excludes everything from cloud
  backup and device transfer.
- **No audio storage.** Raw audio is never stored; only event timestamps and relative levels are
  kept.
- **Delete all data** stops the service and calls `clearAllTables()`. There are no other stores
  (no SharedPreferences or DataStore).

---

## 9. Toolchain and dependencies

**Local toolchain (found on this PC)**

| | |
|---|---|
| Android Studio | 2026.2 (build 262.x) |
| JDK | `JAVA_HOME` = JDK 17.0.20 (Studio bundles JBR 25) |
| Cached locally | Gradle 9.6.0, AGP 9.4.0, Kotlin 2.4.21 |
| SDK | platforms 35 and 36; build-tools 36.0.0; one emulator (API 37) |
| Test phones | Samsung SM-A037F, Android 13 (API 33); Xiaomi 23117RA68G, Android 16 (API 36) |

The two phones cover both sets of foreground-service rules (13 and 14+) and two manufacturers
known for aggressive battery management. API 26–32 is not on hardware; an API 26 emulator image
is an optional ~1 GB download.

**SDK levels**

- `minSdk 26`
- `compileSdk 36`
- `targetSdk 36`. Play has required API 36 for new apps and updates since 2026-08-31; this is
  re-checked at release.

**Dependencies** (versions pinned in `gradle/libs.versions.toml`; compatibility confirmed in
step 1)

| Purpose | Artifacts |
|---|---|
| UI | Compose BOM: `ui`, `material3`, `ui-tooling-preview`; `activity-compose` |
| Navigation | `navigation-compose`, with string routes (avoids the serialization plugin) |
| State | `lifecycle-viewmodel-compose`, `lifecycle-runtime-compose` |
| Storage | `room-runtime`, `room-ktx`, `room-compiler` via KSP |
| Tests | `junit`, `kotlinx-coroutines-test`, `androidx.test.ext:junit`, `room-testing` |

Plugins:

- Android application
- Compose compiler
- KSP

Kotlin setup follows AGP 9's built-in Kotlin support. Nothing else is added without asking.

---

## 10. Testing

- **JVM unit tests**:
  - `SessionRules`: overlap, ordering and limits
  - `SleepStats`: midnight wrap, DST nights, naps, under 3 nights, the time-in-bed fallback
  - `LoudnessDetector`: synthetic level sequences for a single clap, a constant fan, a slow
    rise, and a near-silent room
- **Instrumented tests** (in-memory Room): only one active session can exist; cascade deletes;
  `endSession` closes open segments.
- **Manual checklist** on both phones (step 7):
  - Permissions: grant, deny, deny twice, revoke mid-session; notifications denied.
  - Interruptions: swipe the app away, `adb shell am kill`, force-stop, reboot during a session,
    the mic privacy toggle, a phone call during monitoring.
  - Data: delete a session, delete all data.
  - At least **two real overnight runs per phone**, recording battery used, gaps and event
    counts.

I can build, run the unit and instrumented tests, and install on both phones over USB. I can't
verify a real night; the overnight runs need you.

---

## 11. Not in v1

Taken from the brief: ML, sleep stages, apnea, smart alarms, wearables, Health Connect, cloud
sync, social features, subscriptions, gamification and complex analytics.

Also deferred from review:

- the "repeating sound" label
- manually adding a missed night
- an overnight timeline chart
- optional audio clips
- a sensitivity setting
