# AURORA TV

<p align="center">
  <img src="docs/images/aurora-home.svg" alt="AURORA TV home screen concept" width="100%">
</p>

<p align="center">
  <strong>A cinematic, remote-first home launcher for Android TV and Google TV.</strong><br>
  A clean home for your installed apps, designed around the television screen and the D-pad.
</p>

<p align="center">
  <img src="https://img.shields.io/badge/platform-Android%20TV%20%7C%20Google%20TV-243b5a" alt="Platform">
  <img src="https://img.shields.io/badge/minimum%20Android-5.0%20(API%2021)-3b536f" alt="Minimum Android">
  <img src="https://img.shields.io/badge/language-Java-orange" alt="Java">
  <img src="https://img.shields.io/badge/build-GitHub%20Actions-success" alt="GitHub Actions">
</p>

## The experience

AURORA TV is an independent Android launcher project focused on a polished, cinematic television interface. It aims to make the TV home screen feel calm, visual and easy to navigate from the sofa.

### Home screen concept
<img src="docs/images/aurora-home.svg" alt="AURORA TV home screen concept" width="100%">

### App library concept
<img src="docs/images/aurora-library.svg" alt="AURORA TV app library concept" width="100%">

### Control Center concept
<img src="docs/images/aurora-control-center.svg" alt="AURORA TV control center concept" width="100%">

> These images are design concepts for the intended experience, not screenshots captured from a running device. The current app implementation is earlier and more limited; see the status below.

## Implemented

- Native Android application registered as a HOME / LEANBACK launcher.
- Discovers installed Leanback and standard launchable apps, including Android 11+ package-visibility declarations.
- Remote-friendly focus navigation, app launching and animated focus feedback.
- App Library with live search and a four-column TV grid, reachable from the Search navigation item and the remote Search key.
- Dedicated Favorites screen with long-press management and INFO-key app details.
- Per-profile Aurora, Midnight and Ocean appearance themes.
- Per-profile favourites, profile creation, switching and deletion.
- Per-profile Continue row that restores the last launched app when it is still installed.
- Quick Control Center shortcuts for Network, Bluetooth, Display, Sound, Profiles, Search, Appearance and refresh.
- Idle clock screensaver with manual preview and remote/click dismissal.
- GitHub Actions debug APK build.

## Remaining work

- TV-device testing for D-pad focus order, Home-key behavior, default launcher selection and manufacturer-specific settings.
- Visual refinement, real device screenshots and accessibility checks.
- More complete profile-level personalization and additional tvOS-inspired interaction details.

## Build

Open the repository in Android Studio with JDK 17, sync Gradle, then run the `app` configuration or build the `:app:assembleDebug` task.

The GitHub Actions workflow builds a debug APK on pushes to `main`. Open the repository's **Actions → Android build → Artifacts** to retrieve the generated debug artifact.

## Compatibility

The project declares Android 5.0 (API 21) as its minimum. This is a build target, not a guarantee that every TV model supports every launcher feature. Default Home selection, app discovery and device settings vary by manufacturer and Android version; real-device testing is still required.

## Project principles

- Television-first layout and remote control navigation.
- Lightweight native Android implementation.
- Local-first operation; no account or cloud service is required by the current baseline.
- Installed applications keep their own names and icons.

## Disclaimer

AURORA TV is an independent community project and is not affiliated with, endorsed by, or sponsored by Apple. Any visual inspiration is limited to general interface principles; Apple branding and proprietary assets are not included.

## License

No open-source license has been added yet. All rights remain with the repository owner unless a license is added.
