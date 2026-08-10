# TikNet on v2rayNG (Xray core)

Fork of [2dust/v2rayNG](https://github.com/2dust/v2rayNG) for **TikNet**.

- **Package:** `com.tik.net` (same as Flutter TikNet — panel in-app update installs over previous)
- **Signing:** same production keystore as Flutter TikNet (`ANDROID_KEYSTORE_*` secrets)
- **versionCode:** must stay higher than last Flutter build (currently `40300` / `4.3.0`)

## Panel update

Upload the ARM64 APK to panel static releases and set app-update `versionCode` **> installed** (e.g. `40300`).
