# Tasks

## 1. Threat model

- [ ] 1.1 Create formal threat model and map mitigations to requirements/tests.
- [ ] 1.2 Add adversarial `.ovpn` fixture corpus including directive obfuscation attempts.
- [ ] 1.3 Verify diagnostics and telemetry redaction under failure paths.

## 2. Compatibility

- [ ] 2.1 Create client/server compatibility matrix and automated contract tests.
- [ ] 2.2 Implement safe upgrade-required and unsupported-schema responses.
- [ ] 2.3 Add downgrade protection and authorized rollback semantics.

## 3. Release integrity

- [ ] 3.1 Add SBOM/dependency inventory to all production builds.
- [ ] 3.2 Configure production Android signing pipeline.
- [ ] 3.3 Configure macOS signing, NetworkExtension signing, and notarization pipeline.

## 4. Resilience

- [ ] 4.1 Test client operation during registry outage.
- [ ] 4.2 Test signing-key rotation with existing enrolled devices.
- [ ] 4.3 Test revoked device, expired credential, and compromised artifact scenarios.

## 5. Production gate

- [ ] 5.1 Run backup and signing-key recovery drills.
- [ ] 5.2 Run bad-profile staged rollout and rollback drill on Android and macOS.
- [ ] 5.3 Complete production readiness checklist and document accepted residual risks.
