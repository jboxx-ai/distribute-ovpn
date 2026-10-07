# Implementation Order Within Each Phase

Use the same operating sequence for every OpenSpec phase:

1. Validate `proposal.md`.
2. Validate every capability `spec.md`.
3. Review `design.md` against specs; do not put new product behavior only in design.
4. Ensure `design.md` has no unresolved blocking questions.
5. Execute `tasks.md` from top to bottom.
6. Mark each completed task `[x]`.
7. Run `openspec validate`.
8. Run phase-specific automated tests.
9. Run the phase acceptance scenarios.
10. Sync accepted behavior into long-lived specs.
11. Archive the completed OpenSpec change.

Do not combine two implementation phases into one giant apply cycle. The point of the phase split is that each merged phase leaves the monorepo buildable and behaviorally coherent.
