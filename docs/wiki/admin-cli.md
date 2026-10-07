# Operator CLI: vpnctl & Audit Subsystem

## 1. Overview
The `:backend:admin-cli` (`vpnctl`) provides operators and CI/CD pipelines with an automated interface for profile administration, enrollment token generation, artifact staging, and device access revocation.

---

## 2. Command Reference

### 2.1 Profile Operations
- `vpnctl profile create <id> <name> [description]`  
  Creates a new logical VPN profile.
- `vpnctl profile upload <profile-id> <path-to-ovpn-file>`  
  Parses, verifies policy compliance, computes SHA-256 digest, Ed25519 signs, and registers a new immutable profile version.
- `vpnctl profile promote <profile-id> <version> <STABLE|BETA|CANARY>`  
  Promotes the specified version to target release channel.

### 2.2 Device & Access Management
- `vpnctl device create-token <device-name> <platform> <profile-id> <channel> [expires-mins]`  
  Generates a one-time enrollment token.
- `vpnctl device revoke <device-id>`  
  Revokes device access with zero downtime.

---

## 3. Secret Redaction & Audit Trail Architecture

All administrative actions trigger synchronous writes to the `audit_logs` table via `AuditService`.

### Sanitization Patterns
The sanitization filter strictly redacts the following sensitive formats before persistence:
1. `password=([^&\s]+)` $\rightarrow$ `password=[REDACTED_SECRET]`
2. `bearer\s+([a-zA-Z0-9_-]+)` $\rightarrow$ `bearer [REDACTED_SECRET]`
3. `apikey=([^&\s]+)` $\rightarrow$ `apikey=[REDACTED_SECRET]`
4. `-----BEGIN[ A-Z0-9_-]+PRIVATE KEY-----[\s\S]+?-----END[ A-Z0-9_-]+PRIVATE KEY-----` $\rightarrow$ `[REDACTED_SECRET]`
