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

## P4 Quality
- [x] Unit tests for QR parse and `/me` tolerant parsing
