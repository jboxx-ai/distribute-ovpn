# End-to-End Acceptance Matrix

## Core workflow

| Capability | Android | macOS | Server |
|---|---:|---:|---:|
| enroll device | ✓ | ✓ | ✓ |
| device-scoped authentication | ✓ | ✓ | ✓ |
| fetch assigned manifest | ✓ | ✓ | ✓ |
| verify digest/signature | ✓ | ✓ | signs |
| parse/policy-check `.ovpn` | ✓ | ✓ | ✓ |
| stage candidate | ✓ | ✓ | tracks |
| connect OpenVPN | ✓ | ✓ | n/a |
| report tunnel state | ✓ | ✓ | ✓ |
| rollback failed candidate | ✓ | ✓ | tracks |
| stable/beta channels | consumes | consumes | ✓ |
| staged rollout | consumes | consumes | ✓ |
| revoke device | enforced | enforced | ✓ |

## End-to-end release gate

A release candidate passes only when all of these are demonstrated:

1. Fresh Android device enrolls and connects.
2. Fresh Mac enrolls and connects.
3. Registry publishes a new valid version.
4. Both devices sync the candidate.
5. Both activate and report verified health.
6. Registry publishes an intentionally invalid candidate to beta.
7. Beta clients reject or roll back without losing the last-known-good version.
8. Rollout is paused and stable clients remain untouched.
9. A device is revoked and can no longer fetch protected profile data.
10. Registry is stopped; both clients can still use their already verified active profile.
11. Backup is restored into a clean deployment and manifest/device assignment state matches the backup point.
