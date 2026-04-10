# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

---

## Project Overview

**Scroll Block** is an Android application built with Kotlin and Jetpack Compose. The project uses Gradle as its build system with the following key versions:

- Android Gradle Plugin: 9.1.0
- Kotlin: 2.2.10
- Compose BOM: 2026.02.01
- Target SDK: 36, Compile SDK: 36, Min SDK: 24

---

## Architecture

The project follows a standard Android architecture with a single `MainActivity` that uses Jetpack Compose for the UI. Key structure:

```
scroll_block/
├── app/
│   └── src/main/java/com/example/scrollblock/
│       └── MainActivity.kt  # Activity with Compose content
│   └── src/main/java/com/example/scrollblock/ui/theme/
│       ├── Theme.kt         # ScrollBlockTheme wrapper
│       ├── Color.kt         # Purple/Pink color scheme
│       └── Type.kt          # Material typography
├── app/src/main/res/        # Layouts, drawables, themes.xml
└── gradle/libs.versions.toml # Dependency versions
```

**UI Pattern**: `enableEdgeToEdge()` → `setContent(ScrollBlockTheme → Scaffold → Greeting)`

---

## Build & Development Commands

```bash
# Install dependencies
./gradlew build --refresh-dependencies

# Build app bundle
./gradlew assembleDebug

# Run unit tests (run locally, not via hooks)
./gradlew test

# Run instrumented tests (run locally, not via hooks)
./gradlew connectedAndroidTest
```

**Single test execution**:
```bash
# Specific test file
./gradlew test --tests ExampleUnitTest
```

> **Note**: Tests are run manually. Git hooks have been disabled for now.

---

## Testing Setup

**Unit tests** (JUnit 4) in `app/src/test/java/...`
**Instrumented tests** (AndroidJUnit4 + Espresso) in `app/src/androidTest/java/...`

Test libraries managed via `gradle/libs.versions.toml`:
- `junit:4.13.2`
- `androidx.test.ext:junit:1.3.0`
- `androidx.espresso-core:3.7.0`
- Compose UI Test libraries

---

## IDE Configuration

- Requires Android Studio / IntelliJ with Gradle plugin
- JVM heap size set to 2GB (`-Xmx2048m`) in `gradle.properties`
- Compose plugin must be enabled (`buildFeatures.compose = true`)

---

## Project-Specific Files

| File | Purpose |
|------|---------|
| `app/build.gradle.kts` | App module config with Compose features |
| `app/proguard-rules.pro` | ProGuard/R8 optimization rules |
| `gradle/libs.versions.toml` | All dependency versions (use for consistency) |
| `gradlew` | Gradle wrapper script |

---

## Key Components

- **ScrollBlockTheme** (`Theme.kt`): Material3 theme with purple/pink dynamic colors
- **Scaffold** + **Greeting** composable pattern in `MainActivity.kt`
- Edge-to-edge display mode enabled
