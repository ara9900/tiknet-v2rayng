# TikNet on v2rayNG (Xray core)

Fork of [2dust/v2rayNG](https://github.com/2dust/v2rayNG) customized for **TikNet** panel login.

- Package: `com.tik.net.xray` (so it can sit next to Flutter TikNet `com.tik.net`)
- App name: TikNet
- Login → import personal subscription via panel API
- Account screen: sync subscription, catalog servers, logout
- Connection / ping / Reality: stock **v2rayNG + Xray**

## Panel

Same as Flutter TikNet:

1. `https://ara9900.github.io/app-config/config.json`
2. `https://panel.tikn.ir/static/config.json`
3. Fallback `https://panel.tikn.ir`

## Build

GitHub Actions workflow: **Build APK** (`.github/workflows/build.yml`).

Required secrets (same names as upstream v2rayNG):

- `APP_KEYSTORE_BASE64`
- `APP_KEYSTORE_PASSWORD`
- `APP_KEYSTORE_ALIAS`
- `APP_KEY_PASSWORD`

If secrets are missing, the TikNet workflow generates a one-off CI keystore so you still get an installable APK artifact.
