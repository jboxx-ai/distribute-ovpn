# Phase 03 — Android Managed OpenVPN Client MVP

## Why

The first end-to-end client must prove enrollment, profile synchronization, secure staging, Android VPN permission handling, and OpenVPN tunnel execution on a real device.

## What Changes

- Create the Android application and Compose UI.
- Implement device enrollment and secure local identity storage.
- Implement manual Sync Profile using shared KMP synchronization logic.
- Integrate Android `VpnService` and native OpenVPN engine.
- Persist active and staged profile versions.
- Report synchronization and tunnel state to the registry.

## Capabilities

### New Capabilities

- `android-enrollment`
- `android-profile-sync`
- `android-openvpn-tunnel`
- `android-client-status`

## Impact

- Produces the first usable end-to-end MVP.
- Automatic rollout and rollback policy are deferred to Phase 05.
