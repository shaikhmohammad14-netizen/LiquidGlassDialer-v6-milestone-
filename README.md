# Liquid Glass Dialer — iOS-inspired Hybrid Android Edition

A hybrid Android dialer project: a local HTML/CSS/JavaScript Liquid Glass interface hosted in Android WebView, with Java integration for Android Telecom, contacts and call history.

## Build on GitHub Actions

1. Upload the **contents** of this folder to the root of a GitHub repository (do not upload this folder as a nested ZIP).
2. Ensure `.github/workflows/android.yml` is included.
3. Open **Actions → Android Build → Run workflow**.
4. Download the `LiquidGlassDialer-debug-apk` artifact from a successful run.

The workflow installs Gradle 8.9 directly. A Gradle wrapper script/JAR is not included in this source package.

## Features implemented in this revision

- WebView-hosted Liquid Glass interface with an iOS-inspired translucent visual treatment.
- Native `TelecomManager.placeCall()` call initiation, with the system dialer as a fallback.
- Android dialer role request and call-screening role request.
- Native Contacts Provider query for contacts (up to 500 entries).
- Native Call Log query for recent calls (up to 100 entries).
- Basic native in-call controls: answer, reject, end, mute, speaker and hold/resume.
- On-device blocked/allowed/spam number lists and a call-screening service.

## Important limitations and device testing

This is a development build, not a certified or fully validated replacement for the Vivo Phone app. Install and test on the Vivo V70 Elite. Android/OriginOS may restrict call-log access, full-screen incoming-call UI, background execution, or role availability. Grant permissions and set the app as the default phone app before evaluating call handling.

The HTML incoming-call preview is a design demo, not a real incoming call. The call-assistant transcript, AI, translation, recording, voicemail and noise-removal controls are placeholders and are **not implemented**. Call recording is intentionally not enabled. Dual-SIM selection is not yet exposed as a user-selectable feature; Android may use the system's preferred SIM/account. Emergency calls should always be made using the device's native emergency dialer.

## Minimum requirements

- Android 8.0 (API 26) or later.
- Android 10+ recommended for role-based default dialer and call-screening flows.
- Compile SDK 35, target SDK 35, Java 17, Android Gradle Plugin 8.7.3.

## Privacy

Contacts and call history are queried locally from Android providers. This project does not upload contacts, call logs or call audio to a server. Review the source and Android permission prompts before granting access.

## Version 6.0 — Milestone 1 update

This source update adds a Telecom-driven native call screen that refreshes when call state changes, displays a connected-call timer, and exposes answer/reject/end actions from the call notification. Mute state and speaker routing are synchronized with the active `InCallService`; hold/resume uses the active Telecom `Call`.

The app must be selected as the default phone app for Android to bind its `InCallService`. Call-screening role and permissions are separate. Full-screen incoming-call presentation depends on Android notification/full-screen-intent policy and the device manufacturer; the ongoing call notification remains the fallback. Test on a real device with a second phone. Emergency calling remains the responsibility of the system dialer.

This milestone is source-updated but has **not been compiled or device-tested in this environment**. Use the GitHub Actions workflow to build the APK, then install and test before treating call handling as verified. Android/OEM restrictions may affect behavior.
