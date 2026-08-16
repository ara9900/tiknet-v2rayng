# TikNet improvement checklist

## P0 Stability
- [x] Subscription sync / base64 import noise and empty-import error
- [x] Persian panel error mapping in UI
- [x] Profile cache preload (cached `/me` + username fallback)

## P1 Product
- [x] In-app update from panel (optional/force, download, sha256, install)
- [x] QR login + `tiknet://` deep links (already in 4.3.12; kept)
- [x] Account status badge: expired vs no-subscription separated

## P2 UX
- [x] Brand splash with shield asset
- [x] Filter: selected apps sorted first + pinned chips bar
- [x] Notifications / FAQ / diagnostics sheets retained

## P3 Performance (S24)
- [x] Keep quiet notification / no launch ping storm / busy-only heavy animations

## P5 Completeness (4.3.14)
- [x] Referral invite/share/attach UI + API
- [x] Internet troubleshoot (existing) + panel health check
- [x] Expiry / low-traffic banner + daily local notification
- [x] Device register on login / app start

## P6 Completeness (4.3.15)
- [x] Offline account badge when using cached `/me`
- [x] One-click support with ticket clipboard
- [x] Pin favorite servers in picker
- [x] Home widget status label (connect/disconnect)
