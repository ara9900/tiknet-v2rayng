# TikNet on v2rayNG (Xray core)

Fork of [2dust/v2rayNG](https://github.com/2dust/v2rayNG) for **TikNet**.

- **Package:** `com.tik.net` (same as Flutter TikNet — panel in-app update installs over previous)
- **Signing:** same production keystore as Flutter TikNet (`ANDROID_KEYSTORE_*` secrets)
- **versionCode:** must stay higher than last Flutter build (currently `40332` / `4.3.32`)
- **Quality telemetry:** after real-ping / auto-test, app POSTs `link_quality` to the panel (ASN×config matrix at `/admin/app-telemetry`).

## Dual product (same APK)

Panel `/api/customer/me` returns `product_kind`:

| `product_kind` | After login |
|---|---|
| `vpn` (default) | Full subscription configs (VLESS/… ) |
| `game_dns` | Only WireGuard game/DNS allowlist; catalog hidden; softer UI copy |

Cafe Bazaar / Myket can list the same `com.tik.net` as game DNS; VPN customers still update from the store and unlock full configs by logging in with a VPN plan account.

## Panel update

Upload the ARM64 APK to panel static releases and set app-update `versionCode` **> installed** (e.g. `40300`).
