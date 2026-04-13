# Scroll Block 🛑

**Scroll Block** is an Android application designed to detect and interrupt "mindless scrolling" habits. It monitors your scrolling patterns in the background and triggers a high-urgency overlay when it detects you've been scrolling for too long, helping you regain focus and intentionality.

## 🚀 Features

- **Background Detection**: Uses an Android `AccessibilityService` to monitor scrolling across all apps (Instagram, LinkedIn, Firefox, etc.) without requiring the app to be in the foreground.
- **Smart Detection Logic**: 
    - **5-Minute Sliding Window**: Tracks scroll activity over a rolling 5-minute period to catch long-term "trance" states.
    - **Uniform Point System**: Every distinct scroll action counts as 1 point, ensuring consistent detection across different apps.
    - **Anti-Fidget Debounce**: Implements a 300ms debounce to ignore accidental finger holds, tremors, or slow drags.
    - **Idle Reset**: Automatically clears your scroll history if you take a meaningful break (15+ seconds).
- **High-Urgency Overlay**:
    - Interrupts your scroll with a full-screen red warning modal.
    - **Mandatory 3s Countdown**: Forces a 3-second pause before the "Got it" button becomes active.
    - **Playful/Interrogative Tone**: Asks "Hey, are you paying attention?" to nudge you out of autopilot.
- **Directional Filtering**: Intelligently ignores upward scrolls (returning to top) and horizontal swipes (switching tabs/stories) to reduce false positives.
- **Material 3 UI**: Clean, modern dashboard to manage permissions and toggle the detector state.
- **Persistence**: The service is designed to survive task removal (swiping the app away from recents).

## 🛠 How It Works

The app uses several specific metrics and constraints to differentiate between "reading" and "mindless scrolling":
- **Window**: 300,000ms (5 Minutes)
- **Threshold**: 120 points (Approx. 24 scrolls per minute)
- **Debounce**: 300ms (Only 1 point allowed per 300ms period)
- **Idle Reset**: 15,000ms (15 seconds of inactivity resets the counter)

## 🛡 Permissions Required

- **Accessibility Service**: Required to detect scroll events across the system.
- **System Alert Window (Display over other apps)**: Required to show the interruption overlay.

## 📱 Installation & Setup

1. Open the app and grant the **Display over other apps** permission.
2. Enable the **Scroll Block Accessibility Service** in your device's Accessibility settings.
3. Toggle the detector to **Enabled** on the main dashboard.
4. Go about your day! If you start doom-scrolling, we'll let you know.

## 🛠 Technical Details

- **Language**: Kotlin
- **UI Framework**: Jetpack Compose (Material 3)
- **Service**: `AccessibilityService`
- **Storage**: `SharedPreferences` for persistent state management.

---
*Built to help you stay present.*
