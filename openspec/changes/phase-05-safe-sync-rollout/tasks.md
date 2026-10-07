# Tasks

## 1. Transactional client state

- [x] 1.1 Implement persisted activation journal and last-known-good pointer.
- [x] 1.2 Implement crash recovery for every activation transition.
- [x] 1.3 Add deterministic state-machine tests covering interruption at each transition.

## 2. Health and rollback

- [x] 2.1 Define pluggable tunnel health checks.
- [x] 2.2 Implement automatic rollback after candidate activation failure.
- [x] 2.3 Report activation and rollback outcomes to registry.

## 3. Channels and rollouts

- [x] 3.1 Add stable/beta channel publication state.
- [x] 3.2 Add rollout persistence, deterministic cohort eligibility, pause, cancel, and complete actions.
- [x] 3.3 Make manifest resolution rollout-aware.

## 4. Background sync

- [x] 4.1 Implement Android background check scheduling.
- [x] 4.2 Implement macOS best-effort background check scheduling.
- [x] 4.3 Honor MANUAL, NOTIFY, and AUTO_APPLY policies.

## 5. Cross-platform acceptance

- [x] 5.1 Run controlled bad-profile rollout and prove both platforms recover.
- [x] 5.2 Verify server pause prevents additional devices from receiving the candidate.
- [x] 5.3 Verify clients distinguish downloaded, staged, activated, and verified states.
