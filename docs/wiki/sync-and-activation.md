# Transactional Synchronization & Safe Activation Engine

## 1. The 7-State Activation Journal State Machine

Client-side profile activation follows an atomic state machine to guarantee that an invalid or unreachable configuration never leaves the device disconnected without fallback:

```text
       ┌──────────────┐
       │     IDLE     │
       └──────┬───────┘
              │ Fetch new manifest
              ▼
       ┌──────────────┐
       │   STAGING    │◄── Download & verify Ed25519 signature & policy
       └──────┬───────┘
              │ Validated successfully
              ▼
       ┌──────────────┐
       │  ACTIVATING  │◄── Pass configuration to OS tunnel adapter
       └──────┬───────┘
              │ Tunnel interface established
              ▼
       ┌────────────────┐
       │ TESTING_HEALTH │◄── Run TunnelHealthProbe (DNS/TCP ping)
       └──────┬─────────┘
              │
       ┌──────┴──────────────────────────┐
       │ Success                         │ Failure / Timeout
       ▼                                 ▼
┌──────────────┐                  ┌──────────────┐
│  COMMITTED   │ (Update LKG)     │ ROLLING_BACK │ (Disconnect candidate)
└──────────────┘                  └──────┬───────┘
                                         ▼
                                  ┌──────────────┐
                                  │ ROLLED_BACK  │ (Restore LKG pointer)
                                  └──────────────┘
```

---

## 2. Last-Known-Good (LKG) Pointer Mechanics

1. Before attempting an update from version $N$ to $N+1$, the client locks version $N$ as the **Last-Known-Good (LKG)**.
2. If health verification fails during `TESTING_HEALTH`, the engine terminates candidate $N+1$ and triggers immediate reconnection using version $N$.
3. The LKG pointer is updated to $N+1$ only when transitioning to `COMMITTED`.

---

## 3. Release Channel & Staged Rollouts
- **Channels**: `STABLE`, `BETA`, `CANARY`.
- **Decoupled Operation**: Once committed, devices store active configurations in local encrypted storage. If the registry server is temporarily offline, clients connect using their cached active version.
