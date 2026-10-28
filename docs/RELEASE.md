# Sleepkeep — Release Checklist

Requirements checked on 2026-10-28 against Google's current help pages; re-check anything marked
_verify_ in Play Console when you get there.

## 1. Before the release build

- [ ] Overnight tests done and logged in [TEST_LOG.md](TEST_LOG.md): at least 2 nights per phone.
- [ ] Loud-sound thresholds adjusted if those nights call for it.
- [ ] Xiaomi checks for steps 3–7 done.
- [ ] `versionCode` / `versionName` in `app/build.gradle.kts` set for release (suggested: `1` /
      `1.0.0`). Raise `versionCode` by one for every upload after that.
- [ ] Optional: update the libraries lint flags as outdated. Each update needs a full retest, so
      skip it if everything works.
- [x] Target API 36: required for new apps since 2026-08-31, and already set.
- [x] No `INTERNET` permission in the release manifest. The permissions are `RECORD_AUDIO`,
      `FOREGROUND_SERVICE`, `FOREGROUND_SERVICE_MICROPHONE`, `POST_NOTIFICATIONS`, and AndroidX's
      internal broadcast permission.
- [x] R8 shrinking on. The `profile` build (release code signed with the debug key) passed the
      main flows on the Samsung with no crashes.

## 2. Upload key (you do this; it involves passwords)

1. In Android Studio: **Build → Generate Signed App Bundle or APK → Android App Bundle → Create
   new…**
2. Save the keystore **outside** the project folder, e.g. `C:\Users\USER\keys\sleepkeep-upload.jks`.
   Use alias `upload`.
3. Back the keystore file and its passwords up somewhere safe. With Play App Signing a lost upload
   key can be reset, but only through Google support, which is slow.
4. Create `keystore.properties` in the project root. It's git-ignored and must never be committed.

   ```properties
   storeFile=../keys/sleepkeep-upload.jks
   keyAlias=upload
   # Either put the passwords here, or leave them out and set the environment variables
   # SLEEPKEEP_STORE_PASSWORD and SLEEPKEEP_KEY_PASSWORD before building.
   storePassword=...
   keyPassword=...
   ```

   `storeFile` is relative to the project root.

## 3. Build the bundle

```
gradlew.bat bundleRelease
```

The output is `app/build/outputs/bundle/release/app-release.aab`. Without `keystore.properties`
it's unsigned and Play will refuse it.

For a phone test of release code before uploading, use `gradlew.bat assembleProfile` and install
`app/build/outputs/apk/profile/app-profile.apk`.

## 4. Play Console

### Account and testing

- [ ] _Verify:_ new personal developer accounts must run a **closed test with at least 12 testers
      for 14 days in a row** before they can publish to production. Plan for that time.
- [ ] Use **Play App Signing** (the default for new apps): upload with your upload key and Google
      holds the app signing key.

### Store listing (drafts)

- **App name:** `Sleepkeep: Sleep Tracker` (24 of 30 characters)
- **Short description** (max 80): `Log your sleep and notice loud night sounds. Private, offline, no account.`
- **Full description:**

  > Sleepkeep is a simple, private sleep log.
  >
  > • Tap Start sleep when you go to bed and End sleep when you get up.
  > • Optionally answer two quick morning questions for a rough sleep estimate.
  > • Optionally let it listen overnight for loud sounds. Audio is analysed on your phone and never saved; only the time and loudness of each sound is kept.
  > • See your history, fix times you got wrong, and delete any night.
  > • Compare your last week with the general adult guideline of at least 7 hours (CDC and NHLBI), and see how regular your bedtime and wake-up time were.
  >
  > Private by design: no account, no internet access, no ads, no analytics, no cloud backup. Everything stays on your phone, and you can delete it all at any time.
  >
  > Sleepkeep is not a medical device. It doesn't measure sleep stages, detect snoring reliably, or diagnose sleep disorders. Loud sounds can come from anything: snoring, talking, traffic or pets.

- **Category:** Health & Fitness
- **Screenshots:** dashboard (idle and sleeping), night page with Sounds, sleep guidance,
  Settings & privacy. Taking them from the phone is fine.

### App content forms

| Form | Answer |
|---|---|
| **Privacy policy** | Fill in the placeholders in [PRIVACY_POLICY.md](PRIVACY_POLICY.md) (date, developer name, contact email), push, then use `https://github.com/AnupShrestha1/Sleepkeep-SleepTracker/blob/main/docs/PRIVACY_POLICY.md`. The repo is public. If you make it private, host the policy somewhere public first. |
| **Data safety** | "Does your app collect or share any of the required user data types?" → **No**. Google's rule: data "only processed locally on the user's device and not sent off device does not need to be disclosed". The form and the privacy policy link are still required. |
| **Health apps declaration** | Required for every app. Select **Health and fitness → Sleep Management**. Not a medical device. No health research. |
| **Foreground service permissions** | Type **microphone**. Use case: closest preset to background audio capture, or "Other". Description and user impact below. Needs a **video link** (an unlisted YouTube video is fine). |
| **App access** | All functionality is available without login or special access. |
| **Ads** | No ads. |
| **Content rating** | Questionnaire: no violence, sexuality, gambling or user-generated content. Expect the lowest rating. |
| **Target audience** | 18 and over. This matches the adult guidance and keeps the app out of the Families policy. |
| **Government / financial / news** | No. |

**Foreground service: description**

> During a sleep session the user starts, and only if they turned on "Monitor sounds overnight", the app listens through the microphone while the screen is off to note when loud sounds happen. Audio is analysed on the device in real time and never stored or sent. The user can stop monitoring at any time from the app or the notification.

**Foreground service: user impact if deferred or interrupted**

> Part of the night would not be monitored. The app detects this (it saves a heartbeat every minute) and shows the gap on that night's page ("Not monitored 03:12–04:00: the app was stopped"), so results are never silently incomplete. Monitoring can't restart itself in the background on Android 14+, so the user resumes it from the app.

**Video (about 30–60 s):**

1. Open the app.
2. Turn on **Monitor sounds overnight**.
3. Tap **Start sleep**, the explanation, then **Allow**.
4. Show "Listening for loud sounds" and the notification.
5. Turn the screen off and back on.
6. Make a sound, tap **End sleep**, then show the night's **Sounds** section.

## 5. After upload

- [ ] Install from the closed-testing track on both phones and repeat a short session with
      monitoring.
- [ ] Keep `app/schemas/` committed. Any future database change needs a version bump and a
      migration, or users lose their data on update.
