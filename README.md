# SquashScore — Wear OS Squash Scorer

Minimal Wear OS app for tracking squash scores. Tap to score, undo to fix. Works on Samsung Galaxy Watch, Pixel Watch, and all Wear OS 3+ devices.

## Features

- **2-player mode** — PAR scoring, best of 3/5, hand-out rotation, rest timer
- **3-player cut-throat** — server/receiver/waiting rotation, auto-rotate
- **Big tap targets** — top half = server, bottom half = receiver (no fumbling)
- **Undo last point** — tap the undo button
- **90-second rest timer** between games with automatic advance
- **Match history** — stored locally on the watch
- **Completely standalone** — no phone needed during matches, no account, no internet

## Open in Android Studio

```bash
cd wearscore
# Open in Android Studio (Meerkat or later)
studio .
```

Or: File → Open → select the `wearscore/` directory.

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
│       │   └── Match.kt            # Core scoring logic (immutable)
│       ├── viewmodel/
│       │   └── MatchViewModel.kt   # StateFlow + scoring actions
│       ├── data/
│       │   └── MatchRepository.kt  # JSON persistence (SharedPrefs)
│       └── ui/
│           ├── MainActivity.kt     # Entry point + screen routing
│           ├── SetupScreen.kt      # Player count, names, rules
│           ├── ScoreScreen.kt      # 2-player scoring (split screen)
│           ├── ThreePlayerScreen.kt # 3-player cut-throat
│           ├── MatchSummaryScreen.kt # Post-match stats
│           └── HistoryScreen.kt    # Past matches list
├── build.gradle.kts                # Root
├── app/build.gradle.kts            # Wear OS Compose deps
├── settings.gradle.kts
└── gradle.properties
```

## Scoring Rules

### 2-Player (PAR)
- Every rally = point (no service-only scoring)
- Server wins → stays, gets point
- Receiver wins → becomes server (hand-out)
- Game to 11 (configurable: 9/11/15), win by 2
- Best of 5 (configurable: 3/5)
- 90s rest between games (auto-advance)

### 3-Player (Cut-Throat)
- Two on court, one waiting
- Server vs Receiver
- Server wins → stays on, point, same rotation
- Receiver wins → becomes server, waiting player enters as receiver
- Game to 11 or 15, most points at end wins

## Building

Requires Android Studio Meerkat (2024.3+) or later.

```bash
./gradlew assembleDebug    # Build debug APK
./gradlew installDebug     # Install to connected watch via ADB
```

For ADB over Wi-Fi on watch:
```
Settings → Developer Options → ADB Debugging → Debug over Wi-Fi
adb connect <watch-ip>:5555
```

## What's Next (v2)

- Health Connect integration (workout tracking, calories, heart rate)
- Voice score announcements ("8–6, game ball")
- Phone companion app with Live Tile
- Match analytics (rally lengths, streaks, win patterns)
- Tournament / box league mode
