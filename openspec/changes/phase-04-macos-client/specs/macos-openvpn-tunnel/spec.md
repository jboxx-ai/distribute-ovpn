# macOS OpenVPN Tunnel Specification

## Purpose

Defines observable tunnel behavior through Apple's NetworkExtension framework.

## ADDED Requirements

### Requirement: System-managed tunnel configuration
The application SHALL use the supported macOS VPN extension mechanism to establish the OpenVPN packet tunnel.

#### Scenario: Connect

- **WHEN** the user connects with an active verified profile and required system authorization
- **THEN** the system SHALL establish the packet tunnel and the app SHALL report connected state


#### Scenario: Extension failure

- **WHEN** the packet tunnel extension fails to start
- **THEN** the app SHALL preserve profile state and report a tunnel error distinct from a sync error


### Requirement: Disconnect
The user SHALL be able to stop the active macOS VPN tunnel.

#### Scenario: Explicit disconnect

- **WHEN** the user selects Disconnect
- **THEN** the packet tunnel SHALL stop and the app SHALL report disconnected state
