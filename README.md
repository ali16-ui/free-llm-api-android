# FreeLLMAPI for Android (Android 13+)

WebView app that opens your FreeLLMAPI dashboard. minSdk 33, targetSdk 34, no third-party libraries,
debug-signed so it installs without "package invalid"/parse errors (enable "Install unknown apps").

FreeLLMAPI itself is a Node + SQLite server, so it still has to run somewhere: Termux on the same phone
(see docs/en/install/02-android-termux.md, URL http://127.0.0.1:5173), a PC/Docker on your LAN, or a hosted server.
On first launch enter that URL; long-press Back later to change it.

## Build
- Android Studio: File > Open this folder > Build > Build APK(s)  → app/build/outputs/apk/debug/app-debug.apk
- CLI (JDK 17 + Android SDK 34): `gradle assembleDebug`
- No local setup: push this folder to a GitHub repo → Actions → "Build APK" → download artifact.
