# Device Enrollment Specification

## Purpose

Defines secure first-time registration of a client device.

## ADDED Requirements

### Requirement: One-time enrollment authorization
A device SHALL enroll only with an unexpired, unused enrollment authorization issued by an operator.

#### Scenario: Successful enrollment

- **GIVEN** a valid unused enrollment authorization

- **WHEN** a device submits its generated public identity and metadata
- **THEN** the server SHALL register the device and invalidate the authorization


#### Scenario: Reused token

- **WHEN** a consumed enrollment authorization is submitted again
- **THEN** the server SHALL reject enrollment


### Requirement: Device-generated identity
Each device SHALL generate its own private credential locally and MUST NOT upload the private component to the registry.

#### Scenario: Enrollment request contents

- **WHEN** a device enrolls
- **THEN** the request SHALL contain only public device identity material and non-secret metadata
