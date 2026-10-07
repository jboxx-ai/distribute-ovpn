# Device Assignment Specification

## Purpose

Defines which logical profile and release channel a device may synchronize.

## ADDED Requirements

### Requirement: Explicit assignment
A device SHALL receive only profiles and channels explicitly assigned to it or its authorized group.

#### Scenario: Assigned profile

- **WHEN** an enrolled device requests its current profile
- **THEN** the service SHALL resolve the device's assigned profile and channel


#### Scenario: Unauthorized profile

- **WHEN** a device requests a profile outside its assignment
- **THEN** the service SHALL deny the request
