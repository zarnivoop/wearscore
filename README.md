# Tally — Multi-Sport Score Tracker for Wear OS

Tap to score. No fumbling. Works on Samsung Galaxy Watch, Pixel Watch, and all Wear OS 3+ devices. Track scores for squash, badminton, tennis, table tennis, pickleball, racquetball, padel, and more.

## Features

- **7 sports** — Squash, Badminton, Tennis, Table Tennis, Pickleball, Racquetball, Padel
- **2-player mode** — split-screen tap scoring, PAR rules, To 11 or indefinite
- **3-player cut-throat** — server/receiver/waiting rotation, self-score option
- **Quick start** — pick your sport, tap Start, go
- **Big tap targets** — top half = server, bottom half = receiver
- **Voice announcements** — optional TTS (can toggle during play)
- **Undo** — fix mistakes instantly
- **Ambient mode** — stays visible when screen dims
- **Match history** — stored locally on the watch (JSON)
- **Phone widget** — companion app with home screen widget (Wear Data Layer)
- **Completely standalone** — no account, no internet, no phone needed during play

## Open in Android Studio

```bash
cd wearscore
# Open in Android Studio (Meerkat or later)
studio .
```

Target: Wear OS 3+ (API 30). Minimum SDK 30.

## Project Structure

```
wearscore/
├── app/src/main/
│   ├── AndroidManifest.xml
│   └── java/com/squashscore/
│       ├── model/
│       │   ├── Player.kt           # Player data class
│       │   ├── GameState.kt        # Match lifecycle enum
│       │   ├── Sport.kt            # Supported sports enum
│       │   └── Match.kt            # Core scoring logic (immutable)
│       ├── viewmodel/
│       │   └── MatchViewModel.kt   # StateFlow + scoring actions + TTS
│       ├── tts/
│       │   └── TtsManager.kt       # Built-in Android TTS
│       ├── data/
│       │   ├── MatchRepository.kt  # JSON persistence (SharedPrefs)
│       │   └── WearDataSync.kt    # Phone companion sync
│       └── ui/
│           ├── MainActivity.kt     # Entry point + screen routing
│           ├── SetupScreen.kt      # Sport picker, settings, quick start
│           ├── ScoreScreen.kt      # 2-player scoring (split screen)
│           ├── ThreePlayerScreen.kt # 3-player cut-throat + self-score
│           ├── MatchSummaryScreen.kt # Post-match stats
│           └── HistoryScreen.kt    # Past matches list
├── build.gradle.kts                # Root
├── app/build.gradle.kts            # Wear OS Compose deps
├── settings.gradle.kts
└── gradle.properties
```

## Building

```bash
./gradlew assembleDebug    # Build debug APK
./gradlew installDebug     # Install to connected watch via ADB
```

For ADB over Wi-Fi on watch:
```
Settings → Developer Options → ADB Debugging → Debug over Wi-Fi
adb connect <watch-ip>:5555
```
