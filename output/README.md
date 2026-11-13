# StudIN — test builds

| File | Size | Use it for |
|---|---|---|
| [`StudIN-v2.2.0-arm64.apk`](StudIN-v2.2.0-arm64.apk) | 28.8 MB | **Recommended.** Almost every Android phone made since ~2017 (64-bit ARM). |
| [`StudIN-v2.2.0-universal.apk`](StudIN-v2.2.0-universal.apk) | 70.7 MB | Any device: older 32-bit phones and Android Studio x86_64 emulators. |

* Version **2.2.0** (versionCode 4) · package `com.studin.app` · Android 8.0+ (minSdk 26), targets Android 16 (SDK 36)
* Release build (R8-minified), signed with a **debug/test key** — not the Play Store key.
* Checksums: [`SHA256SUMS.txt`](SHA256SUMS.txt)

## What's new in 2.2.0
* **Controlled AI Tutor** — the app now routes "who are you", greetings, revision plans, quizzes,
  stress and off-topic messages itself; only real study questions reach Gemma, with strict rules,
  calmer sampling, a ~120-word cap and an output guard against loops and invented follow-ups.
* **No more raw markdown** — `**bold**`, `code` and headings render properly; copy gives clean text.
* **Faster & lighter** — no double model loads, model prewarms when you open AI Tutor and is freed
  when idle; smoother streaming.

## What's new in 2.1.0
* **AI Tutor is one page** — the Content check and Model tabs are gone.
* **Download AI Tutor** — one tap + confirm downloads Google's Gemma 3 1B (555 MB) from Hugging Face in
  the background, verifies it (pinned revision + SHA-256) and switches the tutor to it; then it runs offline.
* The app now uses the internet **only** for that optional download. Nothing you study is uploaded.

## Install on a phone
1. Open this folder on GitHub from your phone, tap the APK, then tap **Download** (the "View raw" link).
2. Open the downloaded file. If asked, allow **Install unknown apps** for your browser or Files app.
3. Tap **Install** and open **StudIN**.

Or from a computer with USB debugging enabled:
```bash
adb install -r StudIN-v2.2.0-arm64.apk
```

> If an older StudIN test build signed with a different key is installed, Android refuses the update
> ("App not installed" / signature conflict). Uninstall the old StudIN first, then install this one.

## What to test
1. **Onboarding** — swipe the 5 illustrated intro slides, pick an exam, set a goal, finish setup.
2. **Home** — countdown, *Continue studying*, today's plan; try light/dark in Settings → Appearance.
3. **Focus** — start a 15-min session, pause/resume, check the notification's Pause/Stop buttons.
4. **Blocking (optional)** — Settings → Permission health: allow *Usage access* (and *Focus Guard*
   for Smart mode), set an app to **Block** in Focus → Rules, start a session and open that app.
   The break screen should appear with *Back to studying*, an earned break and an emergency exit.
5. **Study** — review flashcards (tap to flip, grade), take a quiz, import a CSV, generate cards
   from notes or a photo (Study → AI cards).
6. **AI Tutor** — try the four quick actions, then tap **Download AI Tutor** → confirm. Gemma 3 1B
   (555 MB) downloads in the background with a notification, is verified, and the tutor switches to it.
   Wi-Fi recommended.
7. **Insights** — switch 7D / 30D / 12W after a few sessions.

Nothing you study is uploaded. The only network use is the optional **Download AI Tutor** (Gemma 3 1B, 555 MB).

## Build it yourself
```bash
./gradlew :app:assembleRelease                              # universal → app/build/outputs/apk/release/
./gradlew :app:assembleRelease -Pstudin.abis=arm64-v8a      # phone-only, smaller
```
