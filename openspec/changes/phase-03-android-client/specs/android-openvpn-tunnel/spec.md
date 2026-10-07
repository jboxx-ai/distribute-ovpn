# Android OpenVPN Tunnel Specification

## Purpose

Defines user-visible VPN connection behavior on Android.

## ADDED Requirements

### Requirement: VPN permission
The client SHALL request Android VPN user consent before establishing its first system VPN tunnel.

#### Scenario: Permission denied

- **WHEN** the user denies VPN permission
- **THEN** the client SHALL not start the tunnel and SHALL remain usable for profile management


### Requirement: Connect active profile
The client SHALL connect using only a verified active profile.

#### Scenario: Successful connection

- **WHEN** the user selects Connect with a valid active profile and VPN permission
- **THEN** the client SHALL establish the VPN and expose a connected state


#### Scenario: No active profile

- **WHEN** the user selects Connect without a verified active profile
- **THEN** the client SHALL refuse to start the tunnel


### Requirement: Disconnect
The user SHALL be able to stop an active VPN tunnel explicitly.

#### Scenario: User disconnect

- **WHEN** the user selects Disconnect
- **THEN** the client SHALL stop the tunnel and report a disconnected state
