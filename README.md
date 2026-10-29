# Sleepkeep

A simple, private sleep log for Android.

- Tap **Start sleep** at bedtime and **End sleep** in the morning.
- Optionally let the app listen overnight for loud sounds.
- Review past nights.
- See how your last week compares with general adult sleep guidance.

Everything stays on the phone: no account, no internet access, no ads, no analytics, no cloud
backup.

> **Status:** all features are built and tested on one phone (Samsung, Android 13). The real
> overnight tests and Play Store publishing are still to do. See [Progress](#progress).

## Features

- **Sleep sessions:**
  - Start/End buttons, saved immediately, so an active session survives the app being closed or
    killed.
  - Only one active session at a time.
  - Optional morning check-in (time to fall asleep, time awake) for a rough sleep estimate.
    Without it, the app shows time in bed and says so.
- **History:**
  - Every session, newest first, with naps tagged.
  - Each night has a page where you can edit its times (with checks for overlaps and impossible
    times), edit the check-in, or delete it.
- **Sound monitoring** (optional):
  - A foreground service listens during the session with the screen off.
  - Loud sounds are measured against the room's own background noise.
  - Only the time, length and rough loudness are kept. Audio is analysed on the phone and never
    saved.
  - Interruptions are recorded and shown, e.g. "Not monitored 03:12–04:00: the app was stopped".
  - The app never claims a sound was snoring.
- **Sleep guidance:**
  - Average sleep over the last 7 days against the general adult guideline of at least 7 hours
    (CDC, NHLBI).
  - Separately, how regular your bedtime and wake-up time were.
  - General guidance, not a medical assessment. No sleep stages or diagnoses.
- **Settings & privacy:** what's stored and where, permission status, help if the phone's battery
  saver stops monitoring, and **Delete all data**.

## Tech

- Kotlin and Jetpack Compose (Material 3); Room; Navigation Compose.
- No dependency-injection framework, so the setup stays beginner-friendly.
- minSdk 26 (Android 8.0), targetSdk and compileSdk 36.
- AGP 9.4.0 with built-in Kotlin 2.4.21, Gradle 9.6.0, JDK 17.

```
gradlew.bat assembleDebug          # debug app
gradlew.bat testDebugUnitTest      # JVM unit tests
gradlew.bat connectedDebugAndroidTest   # database tests on a connected phone
gradlew.bat assembleProfile        # release code (R8) signed with the debug key, for phone testing
gradlew.bat bundleRelease          # Play bundle; needs keystore.properties (see docs/RELEASE.md)
```

## Docs

- [docs/DESIGN.md](docs/DESIGN.md): screens, data model, monitoring and guidance rules.
- [docs/PLAN.md](docs/PLAN.md): the 8 build steps and the status of each.
- [docs/TEST_LOG.md](docs/TEST_LOG.md): overnight test procedure and results.
- [docs/RELEASE.md](docs/RELEASE.md): release checklist and Play Console answers.
- [docs/PRIVACY_POLICY.md](docs/PRIVACY_POLICY.md): privacy policy, a draft with placeholders.

## Progress

_Updated 2026-10-28._

| Step | Status |
|---|---|
| 1. Project setup | ✅ Done, on both phones |
| 2. Sessions, database, dashboard | ✅ Done, on both phones |
| 3. History, night detail, edit, delete | ✅ Done on the Samsung · Xiaomi not checked |
| 4. Microphone service, permissions, gaps | ✅ Done on the Samsung · Xiaomi not checked |
| 5. Loud sound detection | ✅ Done on the Samsung · Xiaomi not checked |
| 6. Sleep guidance | ✅ Done on the Samsung · Xiaomi not checked |
| 7. Settings, privacy, delete all | ✅ Code done · ⏳ overnight tests to do |
| 8. Release preparation | ✅ Prepared · ⏳ signing key and Play Console to do |

**Tests:** 47 pass: 40 JVM unit tests and 7 instrumented database tests.

**Still to do before release:**

- At least two real overnight runs per phone, unplugged ([docs/TEST_LOG.md](docs/TEST_LOG.md)).
  Adjust the loud-sound thresholds, or add a wake lock, if the results call for it.
- Check steps 3–7 on the Xiaomi (Android 16).
- Not yet tried: a phone call during monitoring, a phone restart mid-session, and the "Did you
  forget to end it?" prompt on a real session over 14 hours.
- Create the upload key and `keystore.properties`, fill in the privacy policy placeholders, set
  the version to 1.0.0, build the signed bundle, and complete the Play Console forms
  ([docs/RELEASE.md](docs/RELEASE.md)).
