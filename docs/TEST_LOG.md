# Sleepkeep — Overnight Test Log

Step 7 needs at least **two real nights per phone** before tuning the loud-sound thresholds and
deciding whether a wake lock is needed. Fill in one row per night.

## How to run a test night

1. Note the battery percentage, then unplug the phone. Unplugged matters: charging keeps Android
   out of Doze, which is the main thing being tested.
2. In Sleepkeep, turn on **Monitor sounds overnight** and tap **Start sleep**. Allow the
   microphone if asked.
3. Put the phone where you'd normally keep it, screen off. Don't open the app during the night.
4. In the morning, tap **End sleep**, answer or skip the check-in, then tap **View night**.
5. Note the battery percentage, and copy what the **Sounds** card says: "Monitored X of Y", any
   "Not monitored" lines, and the number of loud sounds.
6. Note anything you know was noisy (a fan, traffic, a partner, a pet, an alarm) and roughly when.

## Results

| # | Date | Phone | Bed → up | Battery start → end | Monitored | Gaps (times + reason) | Loud sounds | Known noises | Notes |
|---|------|-------|----------|---------------------|-----------|-----------------------|-------------|--------------|-------|
| 1 |      | Samsung SM-A037F (Android 13) | | | | | | | |
| 2 |      | Samsung SM-A037F (Android 13) | | | | | | | |
| 3 |      | Xiaomi 23117RA68G (Android 16) | | | | | | | |
| 4 |      | Xiaomi 23117RA68G (Android 16) | | | | | | | |

## What the results decide

- **Gaps saying "the app was stopped"** with the phone unplugged: the phone's battery saver closed
  the app. Try again with battery set to Unrestricted (Settings & privacy explains how). If gaps
  remain, the release notes and store listing must say monitoring can be interrupted on that phone.
- **"Monitored" much shorter than time in bed, with no gap lines**: monitoring was running but not
  hearing. That would point to the CPU sleeping, and the fix is a partial wake lock.
- **Loud sounds**: many events in a quiet room means the thresholds are too sensitive. Missing
  events you know happened means they're too strict. The thresholds are in
  `app/src/main/java/com/anupshrestha/sleepkeep/monitor/LoudnessDetector.kt`.
- **Battery**: a drop above roughly 15% for a night is worth looking into.
- **"Repeating sound" label**: stays out of v1 unless these nights show the detector separates
  steady repeating sounds reliably. The default is no.
