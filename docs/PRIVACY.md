# StudIN Privacy Policy (draft)

_Last updated: 12 November 2026. Draft for review before Play submission._

**Short version: everything you study stays on your phone.** StudIN never uploads your
data and has no account, ads, trackers or analytics SDKs (ML Kit's usage-log uploader is
disabled at build time).

The app uses the internet for **one thing only**: when you tap **Download AI Tutor**, it
downloads Google's Gemma 3 1B model (555 MB) from Hugging Face. The download is authorised with
StudIN's built-in read-only Hugging Face key (or, as a fallback, your own token). The key is sent
only to `huggingface.co`; nothing about you or your studies is sent. Gemma is provided under and
subject to the [Gemma Terms of Use](https://ai.google.dev/gemma/terms).

## What StudIN stores (on your device only)
| Data | Why | Retention |
|---|---|---|
| Study profile (name, exam, exam date, daily goal) | Personalise syllabus and plans | Until you delete it |
| Syllabus topics, flashcards, review history | Spaced repetition and insights | Until you delete it |
| Focus sessions (start/end, minutes, pauses, blocked attempts) | Timer, streaks, insights | Until you delete it |
| Focus rules (app package + mode) | Focus Guard decisions | Until you delete it |
| Classification decisions (verdict, confidence, matched keywords) | Explain decisions & improve your lists | 30 days. **Full titles are never stored.** |
| Optional AI Tutor model (downloaded or imported) | Tier-2 AI tutor | Until you remove it (AI Tutor → ⋮ → Remove) |

## Permissions
| Permission | Used for | Required? |
|---|---|---|
| Notifications | Timer notification, reminders, focus alerts | Optional |
| Usage access | Detect which app is in front during a session; screen-time insights | Optional |
| Accessibility (Focus Guard) | Read the name of the foreground app and, for apps you set to **Smart**, the visible titles on screen so study videos stay allowed. Active only during focus sessions. Classified on-device; never uploaded or stored. | Optional |
| Display over other apps | Show the break screen above a blocked app | Optional |
| Battery optimisation exemption | Keep the timer alive on aggressive OEM ROMs | Optional |

StudIN never prevents you from leaving a session, uninstalling the app, opening
Settings, calling emergency services, or turning any permission off.

## Your controls
* **Export**: Settings → Export all data (JSON).
* **Delete**: Settings → Delete all data (wipes the database, preferences and AI model).
* Revoke any permission at any time from Android Settings; StudIN degrades gracefully.

## Children
StudIN is designed for students aged 13+. It collects no personal data off-device.

## Contact
support@studin.app (placeholder – replace before release).
