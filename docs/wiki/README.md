# OpenSpec KMP VPN Platform — Feature Architecture & System Wiki

Welcome to the comprehensive technical documentation and worldwide engineering reference for the **OpenSpec KMP VPN Platform**.

This platform provides an enterprise-grade, self-hosted control plane for OpenVPN configuration lifecycle management, cryptographic payload signing, device trust orchestration, and transactional zero-downtime client synchronization across mobile and desktop platforms.

---

## 📚 Documentation Index

### 1. [System Architecture & Monorepo Topology](system-architecture.md)
Comprehensive topology, module dependency matrix, boundary constraints, and threat model foundations.

### 2. [Control Plane: Registry Server & Storage Subsystem](registry-server.md)
Detailed specification of the Ktor REST API, PostgreSQL schema, transaction models, Ed25519 profile signing, and health probe integrations.

### 3. [Operator CLI: `vpnctl` & Audit Subsystem](admin-cli.md)
Administrative command-line workflows, zero-trust token generation, device revocation, and secret-redacted tamper-evident audit logging.

### 4. [Shared Multiplatform Domain & Cryptography Core](shared-core.md)
Deep dive into shared KMP logic: `ApiEnvelope<T>`, multiplatform `.ovpn` AST parser, adversarial policy engine, and cryptographic verification suites.

### 5. [Transactional Synchronization & Safe Activation Engine](sync-and-activation.md)
The 7-state Activation Journal state machine, Last-Known-Good (LKG) pointer recovery, automatic health probes, and canary/staged rollouts.

### 6. [Android Client Application](android-app.md)
Native Android application architecture, Jetpack Compose UI, `VpnService` tunnel lifecycle, and background profile synchronizers.

### 7. [macOS Client Application](macos-app.md)
SwiftUI architecture, `VpnCore.framework` Kotlin/Native umbrella integration, `NetworkExtension` / `NEPacketTunnelProvider`, and Keychain security.

### 8. [Production Deployment & Docker Infrastructure](docker-deployment.md)
Multi-stage build pipelines, container security, PostgreSQL high-availability configuration, and reverse-proxy TLS termination.

---

## 🏛️ Platform Core Tenets

```text
┌──────────────────────────────────────────────────────────────────────────┐
│                           CORE SECURITY LAWS                             │
├──────────────────────────────────────────────────────────────────────────┤
│ 1. Zero Trust Artifacts: Never trust downloaded bytes via HTTPS alone.   │
│    Every payload requires Ed25519 signature verification.                │
│                                                                          │
│ 2. Isolated Device Identity: Enforce per-device cryptographic keys.      │
│    No shared master API keys across physical clients.                    │
│                                                                          │
│ 3. Atomic Safe Activation: Never replace Last-Known-Good (LKG) state     │
│    until the candidate configuration passes live health probing.         │
│                                                                          │
│ 4. Control Plane Decoupling: Registry downtime MUST NOT disrupt          │
│    already established tunnels or local offline activations.             │
│                                                                          │
│ 5. Strict Secret Redaction: Private keys, passwords, and bearer tokens   │
│    must be redacted before reaching logs, telemetry, or audit sinks.     │
└──────────────────────────────────────────────────────────────────────────┘
```
