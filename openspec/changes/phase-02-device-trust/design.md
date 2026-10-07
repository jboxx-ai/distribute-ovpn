# Design — Phase 02

## Trust model

Three separate trust mechanisms are required:

1. TLS authenticates the registry endpoint and protects transport.
2. Per-device authentication authorizes access.
3. Artifact signatures authenticate profile content even if storage/CDN/transport is compromised.

## Device identity

V1 recommended model:
- device generates an asymmetric keypair in Android Keystore or macOS Keychain/Secure Enclave where available
- enrollment sends public key plus platform metadata
- server returns device ID and authorization material bound to that device

The concrete request-signing mechanism may use signed challenges or short-lived server tokens derived from device authentication. The public behavior remains revocable per-device access.

## Enrollment token

Enrollment tokens:
- random high entropy
- single use
- short expiry
- scoped to optional profile/channel
- stored server-side only as a secure digest

## Artifact signing

Use Ed25519 unless deployment requirements force another algorithm.

Signed payload contains:
- manifest schema version
- profile ID
- profile version
- SHA-256 digest
- byte size
- security-policy version
- signing key ID

Private signing material is never stored in the source repository.

## Key rotation

Design the manifest for multiple trusted public key IDs. Clients can receive a future trust bundle only through an already trusted signed update or an application release.

## Revocation

Server revocation immediately blocks new profile retrieval. Previously downloaded profile content remains on the device; revocation is not claimed to erase local state.

## Open Questions

None blocking implementation.
