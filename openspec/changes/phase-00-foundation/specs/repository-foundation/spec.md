# Repository Foundation Specification

## Purpose

Defines observable repository and build behavior required for all product components.

## ADDED Requirements

### Requirement: Single monorepo build graph
The repository SHALL build backend, shared KMP modules, Android application code, and Apple framework code from one version-controlled root.

#### Scenario: Clean checkout build

- **GIVEN** a supported build environment and clean repository checkout

- **WHEN** the documented root build command is executed
- **THEN** all enabled modules compile without requiring untracked local files


#### Scenario: Module boundary enforcement

- **WHEN** a shared module attempts to depend on an application module
- **THEN** the build SHALL fail or architecture verification SHALL report the violation


### Requirement: Cross-platform shared compilation
Shared KMP source sets SHALL compile for every target required by the current phase.

#### Scenario: Shared contracts compile

- **WHEN** CI compiles shared contracts for JVM, Android, and macOS targets
- **THEN** the same public model definitions SHALL compile successfully for all targets


### Requirement: Reproducible local bootstrap
A developer SHALL be able to bootstrap the repository using documented commands without manually creating generated source files.

#### Scenario: Bootstrap from clean machine

- **WHEN** a developer follows the bootstrap documentation on a supported host
- **THEN** all required generated files SHALL be created by build tooling or checked into source control
