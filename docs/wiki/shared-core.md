# Shared Multiplatform Domain & Cryptography Core

## 1. Multiplatform Modules
The core intelligence and security validations reside entirely within pure Kotlin Multiplatform code under `shared/*`.

---

## 2. `.ovpn` AST Parser & Policy Validator (`:shared:profile-policy`)

To prevent Remote Code Execution (RCE) and rogue routing injection, every `.ovpn` configuration is parsed and evaluated before ingestion or staging.

### 2.1 Forbidden Directives (RCE Prevention)
Configurations containing any of the following directives are rejected:
- `script-security`
- `up`, `down`, `route-up`, `route-pre-down`, `ipchange`
- `client-connect`, `client-disconnect`
- `auth-user-pass-verify`, `tls-verify`
- `plugin`

### 2.2 Mandatory Directives
Configurations MUST contain:
1. `client`
2. `dev (tun|tap)`
3. `proto (udp|tcp)`
4. `remote <host> <port>`
5. `ca` directive or inline `<ca>...</ca>` certificate block.

### 2.3 Adversarial Normalization
The parser strips comments (`#`, `;`), collapses irregular whitespace, normalizes directives to lowercase, and handles mixed casing to prevent bypass attempts.

---

## 3. Cryptographic Operations (`:shared:crypto`)

- **Ed25519 Signatures**: Uses Edwards-curve Digital Signature Algorithm over raw UTF-8 profile payload bytes.
- **SHA-256 Digest**: Computed across payload bytes for zero-alloc identity checks.
- **Key Management**: Public keys are delivered during enrollment and pinned in client secure storage.
