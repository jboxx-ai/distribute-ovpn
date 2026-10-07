# Android Client Application

## 1. Subsystem Overview
The Android client application (`:apps:android`) integrates shared KMP domain logic with Android's native `VpnService` subsystem and Jetpack Compose UI.

---

## 2. Technical Stack & Components

- **Runtime**: Android 8.0+ (API 26+)
- **Architecture**: MVVM with Kotlin Coroutines & Flow
- **VPN Core**: `android.net.VpnService` managing virtual TUN interface.
- **Key Modules**:
  - `MainActivity`: UI lifecycle, permission request handoff (`VpnService.prepare`), and live status tracking.
  - `VpnTunnelService`: Foreground service hosting tunnel lifecycle (`ACTION_CONNECT`, `ACTION_DISCONNECT`).
  - `AndroidTunnelController`: Implements `TunnelController` contract to drive `VpnService` from shared KMP logic.
  - `AndroidSecureStorage`: Hardware-backed Android Keystore adapter for persistent device API keys and signing keys.

---

## 3. Tunnel Lifecycle & Permission Flow

```text
User Tap [Connect]
       │
       ▼
VpnService.prepare(context) ──[Requires User Consent]──► System Permission Dialog
       │                                                          │
       │ (Permission Granted)                                     │ (User Approves)
       ▼                                                          ▼
Intent -> VpnTunnelService.ACTION_CONNECT ◄───────────────────────┘
       │
       ▼
Builder.establish() -> Virtual TUN Interface FileDescriptor (ParcelFileDescriptor)
       │
       ▼
Native OpenVPN Core Hook / Packet Loop
       │
       ▼
StateFlow<TunnelState> -> CONNECTED -> UI Updates
```
