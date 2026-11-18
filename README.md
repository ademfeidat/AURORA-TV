# AURORA TV

A standalone Android TV / Google TV home launcher focused on a cinematic, remote-first experience.

## Current implementation
- Native Android launcher entry point (HOME / LEANBACK_LAUNCHER)
- Android 5.0+ baseline (minSdk 21)
- Discovers installed TV and standard launchable apps
- D-pad navigation and OK/Enter launch
- Horizontal app rail with focus animation
- App library dialog
- Local profile label, theme selection and screensaver timeout
- No cloud services or account requirement

## Build
Open this project in Android Studio (JDK 17), allow Gradle sync, then build the `:app` debug APK.

## Compatibility notes
The project declares API 21 as its minimum. Actual launcher replacement and settings behavior can vary by TV manufacturer and Android version. Test on the target device before relying on it as the default Home app.

## Roadmap
1. Complete tvOS-inspired Home experience and navigation.
2. Full app library and search.
3. Functional Control Center via supported Android settings intents.
4. Wallpaper and screensaver customization.
5. Device compatibility, accessibility and release build validation.

AURORA TV is an independent project and is not affiliated with Apple. Apple trademarks and logos are not included in this repository.
