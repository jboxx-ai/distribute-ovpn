# Console Authentication UI Specification

## Purpose

Defines the operator multi-factor authentication UI, session lifecycle, and fleet device trust management views in the Kumo UI Console Registry.

## ADDED Requirements

### Requirement: Multi-Factor Operator Login Screen
The Console Registry web application SHALL display a structured login screen built with Kumo UI tokens requiring Username, Password, and 6-digit TOTP code.

#### Scenario: Operator login and session storage
- **WHEN** an operator enters valid credentials and TOTP code
- **THEN** the application SHALL store the authenticated session and redirect to the overview dashboard

### Requirement: Fleet Trust & Revocation UI
The Device Fleet view SHALL display user ownership, active device token status, last check-in timestamp, and a one-click revocation action.

#### Scenario: Admin revoking an active device
- **GIVEN** an active device in the fleet list
- **WHEN** an administrator clicks "Revoke Device" and confirms
- **THEN** the UI SHALL trigger `POST /api/v1/devices/{id}/revoke`, update the status badge to `REVOKED`, and show a success notification
