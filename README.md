# Tally

A score tracker for Wear OS. I built this because I am notoriously bad at keeping score when playing squash -- I lose count around 7-4 and then we spend 30 seconds reconstructing the rally. Tally fixes that.

Works on any Wear OS 3+ watch (Galaxy Watch, Pixel Watch, etc.). No account, no internet, no phone required during play.

## Sports

Squash, badminton, tennis, table tennis, pickleball, racquetball, padel.

## Modes

- **2-player** -- Split-screen tap scoring. Top half scores for one player, bottom half for the other. Handles serving rotation and game tracking automatically.
- **3-player cut-throat** -- Server vs receiver with a waiting player rotating in. Option for self-score only (tap anywhere to increment your own count).
- **Quick start** -- Pick a sport, tap start, go. Name fields and advanced settings are available but not required.

## Features

- To 11 or indefinite play
- Optional voice announcements using on-device TTS
- Undo last point
- Ambient mode support -- stays visible when the screen dims
- Match history stored locally on the watch
- Companion phone app with a home screen widget (scores synced via Wear Data Layer)

## Tech

- Kotlin, Jetpack Compose for Wear OS
- Minimum SDK 30 (Wear OS 3)
- Scoring logic is immutable -- each point produces a new `Match` state
- StateFlow-driven UI

## Project structure

```
app/src/main/java/com/squashscore/
  model/          Player, GameState, Sport, Match
  viewmodel/      MatchViewModel (state + actions + TTS)
  tts/            TtsManager (Android TTS wrapper)
  data/           MatchRepository (JSON persistence), WearDataSync
  ui/             Screens: Setup, Score, ThreePlayer, Summary, History
phone/            Companion app with widget
```

## Build

```bash
./gradlew assembleDebug     # Build debug APK
./gradlew installDebug      # Install to connected watch via ADB
```

For ADB over Wi-Fi on the watch:

```
Settings > Developer Options > ADB Debugging > Debug over Wi-Fi
adb connect <watch-ip>:5555
```

## License

MIT
