# Sleepkeep — Implementation Plan

Companion to [DESIGN.md](DESIGN.md). There are eight steps, and each one ends in a build that
installs and runs on both phones.

## How each step runs

1. I explain what the step builds and list the exact files.
2. I write the code. You review the diff, and I paste any file on request.
3. Build and test:
   ```
   gradlew.bat assembleDebug testDebugUnitTest
   ```
   Instrumented tests run where the step has them.
4. Install on the Samsung (Android 13) and the Xiaomi (Android 16), then check the step's
   "done when" items.
5. Fix anything that fails before moving on. Commit with your OK.

Package paths below are under `app/src/main/java/<package>/`, written as `…/`.

---

## Step 1 — Project setup

**Goal:** an empty Compose app that builds, installs and launches.

**Files**

- Build config:
  - `settings.gradle.kts`, `build.gradle.kts`, `gradle.properties`
  - `gradle/libs.versions.toml`
  - `gradle/wrapper/*`, `gradlew`, `gradlew.bat`
  - `.gitignore`
- App module:
  - `app/build.gradle.kts`, `app/proguard-rules.pro`
  - `app/src/main/AndroidManifest.xml`: `allowBackup=false`, no permissions yet
  - `app/src/main/res/values/strings.xml`, `colors.xml`, `themes.xml`, `values-night/themes.xml`
  - `res/xml/data_extraction_rules.xml`
  - launcher icon (`mipmap-anydpi/`, `drawable/ic_launcher_foreground.xml`)
- Code: `…/MainActivity.kt`, `…/ui/theme/Color.kt`, `Theme.kt` (default Material typography, so
  no `Type.kt`)

**Work**

- Pin Gradle 9.6.0, AGP 9.4.0 and Kotlin 2.4.21, a set already cached and known to build on this
  PC. KSP and Room are added and checked at the start of step 2, where they're first used.
- Use AGP 9's built-in Kotlin: no separate Kotlin Android plugin.
- `git init` with the GitHub repo as `origin`.

**Status:** done 2026-10-28. Builds, installs and launches on both phones; not yet committed.

**Done when**

- The debug build installs and shows a placeholder screen on both phones.
- The merged manifest has no `INTERNET` permission.

---

## Step 2 — Sessions: database, Start/End, dashboard basics

The brief's steps 3, 4 and 5 are merged here. The active session has to live in Room from the
start, so an in-memory version would be thrown away.

**Goal:** start and end a session that survives the app being killed, and see last night on the
dashboard.

**Files**

- `…/SleepApp.kt`, `…/AppContainer.kt`
- `…/data/SleepSession.kt`, `SoundEvent.kt`, `MonitoringSegment.kt`. All three tables are
  created now, so schema v1 doesn't need a migration later.
- `…/data/SleepDao.kt`, `SleepDatabase.kt`, `SleepRepository.kt`, `SessionRules.kt`
- `…/ui/AppNavHost.kt`
- `…/ui/dashboard/DashboardScreen.kt`, `DashboardViewModel.kt`, `CheckInSheet.kt`
- `…/ui/common/TimeFormat.kt`
- Modified: `MainActivity.kt`, `AndroidManifest.xml` (Application class), `strings.xml`
- Tests: `app/src/test/…/SessionRulesTest.kt`, `app/src/androidTest/…/SleepRepositoryTest.kt`
- `app/schemas/` (exported schema)

**Done when**

- After Start sleep, both a force-stop (`adb shell am force-stop`) and a reboot leave the session
  still active on reopen.
- A second session can't be started.
- End sleep shows the check-in sheet. A session longer than 14 h triggers the "forgot to end?"
  prompt.
- Dashboard shows last night's time in bed, sleep estimate, bedtime and wake-up time.
- Unit and instrumented tests pass.

**Status:** done 2026-10-28 on both phones: 10 unit tests pass, the 5 instrumented tests pass on
each phone, and start → force-stop → reopen → end → check-in works. Not yet checked: a reboot
during a session, and the long-session dialog on a real device (only its time logic is
unit-tested). Changes from the list above:

- `AppNavHost.kt` moves to step 3, where the second screen arrives.
- The card is labelled "Last sleep", not "Last night", since the latest session may be older
  than last night.

---

## Step 3 — History, night detail, edit, delete

**Goal:** browse past nights, fix times and remove sessions.

**Files**

- `…/ui/history/HistoryScreen.kt`, `HistoryViewModel.kt`
- `…/ui/night/NightDetailScreen.kt`, `NightDetailViewModel.kt`, `EditTimesDialog.kt`
- Modified: `AppNavHost.kt`, `SleepRepository.kt`, `SleepDao.kt`, `strings.xml`
- Tests: more cases in `SessionRulesTest.kt`

**Done when**

- History lists sessions newest first, with nap tags.
- Editing into an overlap, a future time, or end-before-start is rejected with a clear message.
- Edit check-in updates the sleep estimate.
- Deleting asks first and removes the session.

**Status:** done on the Samsung 2026-10-28: 14 unit tests and 6 instrumented tests pass (the new
ones cover nap tagging and the delete cascade). History, night detail, editing times (refused when
too short, accepted at exactly 24 h), editing the check-in and deleting all work. The Xiaomi check
is still to do: it refused the USB install. Changes from the list above:

- Edited times are rounded down to the whole minute, since the pickers only show minutes. This
  fixes 14:22 → 14:22 the next day being refused as a few seconds over 24 h.
- The active session shows in history but isn't tappable. Editing or deleting it comes with
  step 4, which has to stop monitoring first.
- A small `SleepStats.kt` with night dates and nap tagging arrives now; step 6 extends it.
- The back arrow is a drawable, to avoid adding the Material icons library.

---

## Step 4 — Microphone service, permissions, coverage

The riskiest part, so it is split into two steps. This step makes the service run reliably and
report gaps honestly, without any detection yet.

**Goal:** monitoring runs in a foreground service, and every way it can fail is visible.

**Files**

- `…/monitor/SoundMonitorService.kt`: `AudioRecord` loop, level computation, heartbeat, silence
  detection
- `…/monitor/MonitorNotification.kt`, `MonitorStatus.kt`
- `…/ui/common/MicPermission.kt`: explanation dialog, request, denied and permanently-denied
  states
- Modified:
  - `AndroidManifest.xml`: permissions and the `<service>` entry
  - `AppContainer.kt`, `SleepRepository.kt`, `SleepDao.kt` (segments)
  - `DashboardScreen.kt` and `DashboardViewModel.kt` (switch, status, Resume)
  - `strings.xml`

**Done when** (on both phones)

- The permission flow works: the explanation shows first; denying still starts the session;
  denying twice gives a Settings button.
- The notification shows while monitoring, and its Stop action works.
- Swiping the app away keeps monitoring running.
- `adb shell am kill` or force-stop, followed by reopening, shows "stopped around HH:MM" with
  Resume.
- A phone call and the mic privacy toggle each produce a labelled gap.

**Status:** done on the Samsung 2026-10-28; the Xiaomi was disconnected. Checked on the Samsung:

- The explanation shows before Android's prompt.
- Denying the microphone still starts the session, with "the microphone isn't allowed" and an
  Open settings button.
- After allowing it, Resume starts monitoring as a foreground service, recording 16 kHz from the
  VOICE_RECOGNITION source.
- Recording continued after 75 s in the background.
- A force-stop followed by reopening shows "stopped around 15:14" (the last heartbeat) and Resume
  works.
- The mic privacy toggle shows "silent since 15:15", and monitoring picks up again by itself when
  the microphone returns.
- Both the in-app and the notification Stop buttons work, and so does End sleep.
- The notification is hidden when notifications are denied, and the app says so.
- The switch remembers the last session's choice.

Not checked yet:

- The Xiaomi.
- A phone call.
- A screen-off, unplugged night (USB charging keeps Android out of Doze). The first real night
  answers whether a wake lock is needed.

Changes from the list above:

- `MicPermission.kt` holds the permission helpers and the explanation dialog. The monitoring
  views are in `MonitoringViews.kt`, and `AudioInput.kt` opens the microphone.
- "Another app has the mic" and "mic turned off" share one label. Both arrive as exact digital
  silence, and telling them apart needs an API 29+ callback that adds little.
- A gap from a killed app ends at its last heartbeat, so it can be up to a minute early.

---

## Step 5 — Loud sound detection

**Goal:** loud sound events are recorded and shown per night.

**Files**

- `…/monitor/LoudnessDetector.kt`
- `app/src/test/…/LoudnessDetectorTest.kt`
- Modified: `SoundMonitorService.kt` (feed the detector, save events), `SleepRepository.kt`,
  `SleepDao.kt`, `NightDetailScreen.kt`, `NightDetailViewModel.kt`, `strings.xml`

**Done when**

- Detector unit tests pass for a clap, a constant fan, a slow rise and a near-silent room.
- On a phone, a few claps produce events, and a running fan produces none after warm-up.
- Night detail shows coverage, gaps with reasons, and the event list with its "can't tell sounds
  apart" note.

---

## Step 6 — Sleep guidance

**Goal:** the weekly duration and schedule summaries, with sources and disclaimer.

**Files**

- `…/guidance/SleepGuidelines.kt`, `SleepStats.kt`
- `app/src/test/…/SleepStatsTest.kt`
- `…/ui/guidance/GuidanceScreen.kt`, `GuidanceViewModel.kt`
- Modified: `DashboardScreen.kt`, `DashboardViewModel.kt` (weekly card), `AppNavHost.kt`,
  `strings.xml`

**Done when**

- Unit tests cover:
  - the midnight wrap: 23:30 and 00:30 are 60 min apart
  - a DST night in a test zone
  - naps excluded
  - fewer than 3 nights gives "not enough data"
  - the time-in-bed caveat count
- The dashboard card and guidance screen match the wording in DESIGN §7.3.
- The source links open in the browser.

---

## Step 7 — Settings, privacy, delete all; full test pass

**Goal:** finish the privacy features, then test the whole app on both phones over real nights.

**Files**

- `…/ui/settings/SettingsScreen.kt`, `SettingsViewModel.kt`
- Modified: `SleepRepository.kt` (`deleteAll`), `AppNavHost.kt`, `strings.xml`
- `docs/TEST_LOG.md`: results per phone and per night

**Done when**

- Delete all data stops monitoring and leaves an empty app.
- The permission status and battery-settings links work.
- The full manual checklist (DESIGN §10) passes on both phones.
- There are at least two overnight runs per phone, with battery use, gaps and event counts
  logged.
- Detector thresholds are tuned from those nights. A decision is recorded on whether any
  "repeating sound" label is justified (the default is no).

---

## Step 8 — Release preparation

**Goal:** a signed Android App Bundle and everything Play Console asks for.

**Files**

- Modified: `app/build.gradle.kts` (release signing read from a git-ignored `keystore.properties`,
  R8 and resource shrinking)
- `docs/RELEASE.md`: checklist and answers for the Play forms
- `docs/PRIVACY_POLICY.md`: the text for you to host (Play needs a public URL)

**Work**

- Re-check Play's target API requirement (API 36 as of 2026-08-31).
- Fill in the Play forms:
  - **Data safety**: likely "no data collected", since everything is processed on the device;
    verify against Google's current definitions.
  - **Health apps declaration**.
  - **Foreground service declaration** for the microphone: a description plus a short video of
    the feature.
  - Content rating, and target audience 18+, which matches the adult guidance.
- **You** create the upload key in Android Studio (Build → Generate Signed App Bundle), since I
  don't handle keystore passwords. I wire the config to read it.
- Build `bundleRelease`, install the release build on both phones and smoke-test it.

**Done when**

- A signed `.aab` builds.
- The release build works on both phones with R8 on.
- Every Play form has a drafted answer in `RELEASE.md`.

---

## Open decisions

| Decision | Needed by | Default if you don't choose |
|---|---|---|
| Application ID (permanent once uploaded to Play) | Decided | `com.anupshrestha.sleepkeep`, matching your other apps' `com.anupshrestha.*` IDs |
| App name | Decided | **Sleepkeep**; Play title "Sleepkeep: Sleep Tracker" |
| Where to host the privacy policy | Step 8 | — |
| Commit after every passing step without asking each time | Step 1 | I ask each time |
