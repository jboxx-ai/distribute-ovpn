# Android Client Status Specification

## Purpose

Defines client status reporting without exposing VPN secrets.

## ADDED Requirements

### Requirement: Status heartbeat
The client SHALL report device version, installed profile version, sync outcome, and tunnel state without sending private profile credentials.

#### Scenario: Connected heartbeat

- **WHEN** a heartbeat occurs while connected
- **THEN** the registry SHALL receive connected state and installed profile version but not private key material
