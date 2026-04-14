# Scroll Block 🛑

**Scroll Block** is an Android application designed to detect and interrupt "mindless scrolling" habits. It monitors your scrolling patterns in the background and triggers a high-urgency overlay when it detects you've been scrolling for too long, helping you regain focus and intentionality.

## 🚀 Features

- **Background Detection**: Uses an Android `AccessibilityService` to monitor scrolling across all apps (Instagram, LinkedIn, Firefox, etc.) without requiring the app to be in the foreground.
- **Advanced Customization**: 
    - **Dynamic Detection Window**: Adjust how far back to monitor activity (1–30 min).
    - **Scroll Threshold**: Fine-tune the number of scrolls allowed before the block triggers.
    - **Inactivity Reset**: Set how long you need to stop scrolling before your progress is cleared.
    - **Scroll Sensitivity**: Calibrate the minimum gap between movements to count as a unique scroll.
    - **Block Duration**: Customize the mandatory wait time on the warning overlay.
- **Smart Detection Logic**: 
    - **Sliding Window**: Tracks scroll activity over a rolling period to catch "trance" states.
    - **Anti-Fidget Debounce**: Ignores accidental finger holds, tremors, or slow drags.
    - **Idle Reset**: Automatically clears your scroll history if you take a meaningful break.
- **High-Urgency Overlay**:
    - Interrupts your scroll with a full-screen warning modal.
    - **Mandatory Countdown**: Forces a pause before the "Got it" button becomes active.
- **Directional Filtering**: Intelligently ignores upward scrolls (returning to top) and horizontal swipes to reduce false positives.
- **Modern Material 3 UI**: 
    - Clean dashboard with an expandable **Advanced Options** section.
    - Smooth animations and scrollable interface for all screen sizes.
    - Quick "Reset to Defaults" functionality.

## 🛠 Default Metrics

The app comes pre-configured with these recommended settings:
- **Window**: 5 Minutes
- **Threshold**: 80 scrolls
- **Inactivity Reset**: 15 seconds
- **Scroll Sensitivity**: 300ms
- **Block Duration**: 3 seconds

## 🛡 Permissions Required

- **Accessibility Service**: Required to detect scroll events across the system.
- **System Alert Window (Display over other apps)**: Required to show the interruption overlay.

## 📱 Installation & Setup

1. Open the app and grant the **Display over other apps** permission.
2. Enable the **Scroll Block Accessibility Service** in device settings.
3. Toggle the detector to **Enabled** on the main dashboard.
4. (Optional) Expand **Advanced Options** to customize the detection sensitivity to your liking.

## 🛠 Technical Details

- **Language**: Kotlin
- **UI Framework**: Jetpack Compose (Material 3)
- **Service**: `AccessibilityService`
- **Iconography**: Material Icons Extended
- **Storage**: `SharedPreferences` with real-time service synchronization.

---
*Built to help you stay present.*
