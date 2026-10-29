# Sleepkeep — Implementation Plan

Companion to [DESIGN.md](DESIGN.md). There are eight steps, and each one ends in a build that
installs and runs on both phones.

## Overall progress (2026-10-28)

All eight steps are built and committed. Each step's **Status** below has the details.

| Step | Commit | State |
|---|---|---|
| 1 | `2404e3c` | Done, both phones |
| 2 | `c372b87` | Done, both phones |
| 3 | `7cd7225` | Done on the Samsung; Xiaomi not checked |
| 4 | `0e972ad` | Done on the Samsung; Xiaomi not checked |
| 5 | `aac2e34` | Done on the Samsung; Xiaomi not checked |
| 6 | `2254d8b` | Done on the Samsung; Xiaomi not checked |
| 7 | `570b28d` | Code done; overnight tests to do |
| 8 | `11eb38d` | Prepared; signing key and Play Console to do |

47 tests pass (40 unit, 7 instrumented).

Open items, in order:

1. Overnight runs: at least 2 nights per phone, logged in [TEST_LOG.md](TEST_LOG.md).
2. The Xiaomi checks for steps 3–7.
3. Not yet tried: a phone call during monitoring, a restart mid-session, and the
   long-session prompt on a real session.
4. From the nights: tune the thresholds and decide on a wake lock.
5. Set the version to 1.0.0.
6. Upload key, privacy policy placeholders, signed bundle and Play Console
   ([RELEASE.md](RELEASE.md)).

## Features after v1 (agreed 2026-10-29)

These were reviewed for honesty: nothing here claims to measure what a phone can't.

| # | Feature | State | Notes |
|---|---|---|---|
| 1 | **Trends** | ✅ Built | Insights has a Week / Month / 3 months switch. Long ranges show nightly dots with a 7-night rolling average, plus a bedtime→wake-up range chart and weekday vs weekend averages. The hour axis is zoomed to the data, so dots are used instead of bars. |
| 2 | **"How rested do you feel?"** | ✅ Built | Database v2 (Room auto-migration, tested against a real v1 file). A 1–5 morning self-rating, shown against duration and schedule. Replaces any computed "sleep quality" score, which would be invented. |
| 3 | **Sleep sounds** | Later | Generated white, pink and brown noise, plus a few CC0 rain or storm loops, with a fade-out timer. Monitoring ignores the time sounds are playing, and the night page marks it. |
| 4 | **Saved sound clips** | Later | Opt-in, about 10 s per loud sound, deleted after 7 days, stored only on the phone. Never labelled "snoring". Needs the privacy policy, in-app privacy text and Play Data safety updated. |
| 5 | **Plain alarm and timer** | Later | A set-time alarm and a "wake me in N h" timer; dismissing it can end the session. No "smart" light-sleep alarm. Needs the exact-alarm and full-screen-intent permissions, and careful reliability testing. |
| 6 | **Health Connect import** | Later | Reads sleep stages measured by a wearable, rather than estimating them. Was out of scope in the original brief; the user opted in on 2026-10-29. |

Rejected:

- **Sleep-cycle tracking from the phone:** the microphone can't tell sleep stages apart.
- **A computed sleep-quality score:** there's no sensor data to base it on.

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

**Status:** done on the Samsung 2026-10-28; the Xiaomi was disconnected.

- 30 unit tests pass. They include 9 detector tests and 7 coverage tests (`Coverage.kt`, a new
  file that computes monitored time and the gaps with their reasons).
- 6 instrumented tests pass.
- Windows alarm sounds played from the PC speakers after warm-up produced 9 events (1–22 s,
  moderate to very loud) on the night page, under "Monitored 3 m of 4 m".

Not checked yet:

- A whole real night: false alarms from a fan, traffic or a partner, and how many events a normal
  night produces. The thresholds get tuned from those nights in step 7.
- A night with gaps on the page; only the coverage unit tests cover them.

Changes from the list above:

- Night detail lists the first 20 events, with "Show all".
- Short events show seconds ("5 s").

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

**Status:** done on the Samsung 2026-10-28.

- 40 unit tests pass, 14 of them for `SleepStats`.
- 6 instrumented tests pass.
- On the phone: the dashboard says "Track at least 3 nights… (0 so far)" and counts up. Three
  nights back-dated with Edit times show the weekly card and the guidance screen. The CDC link
  opens Chrome, and Back returns to the app.

Changes from the list above:

- The schedule text adds one sentence: the CDC gives no number for "close enough", so the app
  describes the schedule rather than grading it.
- Minute counts use plurals ("1 minute").
- The weekly card also shows during an active session.

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

**Status:** the code half is done on the Samsung 2026-10-28.

- Settings & privacy opens from the gear on the dashboard. It shows the privacy statement,
  permission status, battery help and the app version.
- Delete all data, run during monitoring, stopped the service and left an empty dashboard and
  history.
- 40 unit tests and 7 instrumented tests pass. The new instrumented test checks delete-all.
- `docs/TEST_LOG.md` has the overnight procedure and a results table.

Still to do:

- The overnight runs: at least two per phone.
- The Xiaomi checks for steps 3–7.
- A phone call during monitoring.
- Tuning the thresholds from the nights.

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

**Status:** prepared 2026-10-28; signing and publishing wait for you.

- Checked the current Play rules:
  - Data safety: on-device-only data isn't "collected", but the form and a privacy policy are
    still required.
  - Health apps declaration: required for every app; this one goes under Sleep Management.
  - Microphone foreground service: needs a description, a user-impact note and a video.
- Release signing reads a git-ignored `keystore.properties`. Without it the bundle is unsigned.
- A `profile` build type (release code, debug key) is there for testing.
- `bundleRelease` builds a 4.1 MB `.aab`. The release manifest has no `INTERNET` permission.
- The profile build passed start/monitor/end, the night page, history, guidance, settings and
  delete-all on the Samsung with no crashes. It's left installed there for the overnight tests.
- `docs/RELEASE.md` has the checklist and every Play form answer drafted.
- `docs/PRIVACY_POLICY.md` is ready apart from its placeholders.

Still to do:

- You: create the upload key and `keystore.properties`.
- You: fill in the privacy policy placeholders.
- Set the version for release.
- After the overnight tests: the final signed build, the Xiaomi check of the release build, and
  the Play Console steps.

---

## Open decisions

| Decision | Needed by | Default if you don't choose |
|---|---|---|
| Application ID (permanent once uploaded to Play) | Decided | `com.anupshrestha.sleepkeep`, matching your other apps' `com.anupshrestha.*` IDs |
| App name | Decided | **Sleepkeep**; Play title "Sleepkeep: Sleep Tracker" |
| Where to host the privacy policy | Step 8 | — |
| Commit after every passing step without asking each time | Step 1 | I ask each time |
