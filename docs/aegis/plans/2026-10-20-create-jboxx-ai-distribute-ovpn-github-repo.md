# Create and Push `jboxx-ai/distribute-ovpn`

## Goal

Create the GitHub repository `https://github.com/jboxx-ai/distribute-ovpn`, prepare the local `distribute-ovpn` repository for public/private GitHub hosting without committing secrets, make coherent local atomic commits, update `origin` to the new repository, and push the intended branch only after authentication and verification succeed.

## Architecture and ownership

- **Repository owner:** the existing Git repository at `/opt/data/home/distribute-ovpn`.
- **Documentation owner:** the merged documentation tree under `docs/` and OpenSpec configuration under `openspec/`.
- **Distribution boundary:** GitHub repository `jboxx-ai/distribute-ovpn`; remote publication is an external side effect and must be the final action.
- **History boundary:** preserve existing commits and preserve the user's pre-existing Android work; do not reset, clean, or silently discard unrelated changes.
- **Secret boundary:** no credentials, tokens, private keys, local SDK paths, generated dependency trees, or environment files are to be committed or pushed.

## Current repository facts

- Current branch: `main`.
- Existing remote: `https://github.com/rifqifardi/vpn-kmp-platform.git`.
- The working tree is dirty and contains both rename/documentation changes and pre-existing Android implementation changes.
- `local.properties` exists locally and is already ignored after the rename preparation.
- The Gradle test gate was previously blocked because no Java runtime was available.
- The console UI build previously passed with `npm run build`; generated `dist/` output must not be regenerated or included unless explicitly required by the repository's existing release contract.

## Requirement-ready check

- **Approved outcome:** one GitHub repository under `jboxx-ai`, named `distribute-ovpn`, containing the renamed project and merged documentation.
- **Required actions:** harden `.gitignore`, inspect the complete candidate Git payload, split local changes into atomic commits, authenticate to GitHub, create/configure the repository if it does not exist, update `origin`, verify the remote, push `main`.
- **Required security evidence:** tracked-file secret scan, ignored-file checks, staged-payload review, and a final pushed-commit file review.
- **Open authorization boundary:** creating a repository and pushing are external actions. Execute only after the user approves the proposed atomic grouping and provides or completes GitHub authentication in the UI/credential manager.
- **Repository visibility:** unspecified. Ask before creation unless the GitHub account or organization policy already establishes the intended visibility. Recommended default: private until the user explicitly chooses public.

## TDD route

- **Mode:** auto.
- **Decision:** skipped.
- **Authority:** this is repository/distribution configuration and Git history work, not a runtime behavior change.
- **Test posture:** use repository integrity, secret-scan, Git payload, and available build checks instead of test-first implementation.

## Atomic commit proposal

The atomic-commit workflow requires approval before the first commit. Do not run `git add .`, `git add -A`, destructive reset/clean commands, or `git push` before that approval.

| Order | Commit | Paths | Purpose | Depends on |
|---|---|---|---|---|
| 1 | `chore(repo): ignore local development files` | `.gitignore` | Keep `local.properties` and any confirmed local-only files out of the repository. Extend only with patterns justified by the payload audit. | none |
| 2 | `refactor(repo): rename project to distribute ovpn` | `settings.gradle.kts`, `backend/registry-server/src/main/kotlin/fyi/rifqifardi/vpn/backend/Application.kt`, `backend/console-ui/src/App.tsx` and any other confirmed source references | Remove old repository/project naming from runtime and source paths while preserving behavior. | #1 |
| 3 | `docs(repo): merge distribute ovpn documentation` | `README.md`, `docs/`, `openspec/` | Keep the merged plan, wiki, prototypes, acceptance material, roadmap, and OpenSpec configuration in the main repository. | #1 |
| 4 | `feat(android): add onboarding registry flow` | The pre-existing Android implementation files only: `apps/android/build.gradle.kts`, `apps/android/src/main/AndroidManifest.xml`, `apps/android/src/main/kotlin/fyi/rifqifardi/vpn/android/MainActivity.kt`, `apps/android/src/main/kotlin/fyi/rifqifardi/vpn/android/OnboardingState.kt`, `apps/android/src/main/kotlin/fyi/rifqifardi/vpn/android/OnboardingStore.kt`, `apps/android/src/main/kotlin/fyi/rifqifardi/vpn/android/RegistryClient.kt`, `apps/android/src/test/`, `gradle/libs.versions.toml` | Preserve and commit the existing Android work as its own reviewable change, only if the user confirms it belongs in this push. If not approved, leave it uncommitted and exclude it from the push. | #1 |

Generated console `dist/` changes are excluded by default. If the source rename requires a committed generated artifact under this repository's established contract, handle it as a separate explicitly approved `build(console-ui): refresh production bundle` commit after source changes; otherwise restore/exclude it.

## Execution tasks

### 1. Reconcile the working tree

- Capture a task-start snapshot of `git status --porcelain=v1`, `git diff --stat`, `git diff`, and untracked paths.
- Confirm which changes predate this task and preserve them.
- Inspect the staged index currently present; clear only the index when beginning approved atomic grouping, not the working tree.
- Do not use destructive cleanup commands.

### 2. Harden ignore rules and perform the payload audit

- Review `.gitignore` for Android, Gradle, IDE, Node, environment, credential, signing, and local build output patterns.
- Add only patterns needed to prevent local secrets and machine-specific files from entering Git. At minimum retain `local.properties`; add `.env`, signing material, and common secret/config patterns only if they match this project's actual local workflow and do not hide required examples.
- Keep safe templates such as `infra/docker/.env.example` tracked.
- Inspect tracked and untracked files for private keys, certificates, tokens, passwords, signing files, environment files, SDK paths, and large generated artifacts.
- Use a staged-payload review rather than relying only on filename scans; inspect suspicious text content without printing secret values.

### 3. Apply and verify project rename/docs merge

- Confirm no old `vpn-kmp-platform`, `vpn-kmp-platform-docs`, `VPN KMP`, or related identifier remains in tracked searchable project content, excluding intentionally historical Git metadata and generated artifacts that are not published.
- Confirm the docs plan is inside the main repository and no separate docs directory is required for the GitHub payload.
- Confirm README links resolve to repository-relative files.

### 4. Create approved atomic commits

- Create the atomic-commit backup branch and snapshot references before changing the index.
- Present the grouping table and wait for explicit approval before the first commit.
- Stage named paths per approved group only; never stage the whole tree.
- Use Conventional Commit messages shown above, with no `--no-verify`.
- Keep pre-existing Android work separate from rename/docs work, or explicitly exclude it if the user chooses not to publish it yet.
- Read back each commit's SHA, message, and file list.

### 5. Prepare GitHub authentication and repository creation

- Check `gh` authentication, SSH authentication, and Git credential helper without exposing tokens.
- Prefer `gh` if installed and authenticated; otherwise use an existing SSH credential or the platform's masked GitHub authentication flow.
- Never request or accept a token, password, private key, or one-time code in chat.
- Confirm the GitHub account has permission to create repositories under `jboxx-ai`.
- Check whether `jboxx-ai/distribute-ovpn` already exists before creating it. If it exists, stop and inspect it rather than overwriting or force-pushing.
- Create the repository with the user-approved visibility and a project description; do not initialize it with a README, license, or `.gitignore` if that would create an unrelated remote root commit.

### 6. Configure and push safely

- Set `origin` to the new repository URL only after the local commits and remote target are verified.
- Verify the remote URL and authenticated repository identity.
- Push `main` with `git push --set-upstream origin main`; do not force-push.
- If the remote is non-empty or diverged, stop and request a merge/reconciliation decision.
- After pushing, inspect the remote branch and commit file list, and verify no secret or local-only file was published.

## Verification

Required checks before claiming completion:

1. `git diff --check` on the approved changes.
2. Secret/payload audit over tracked files and the exact staged commit payload; confirm no secrets, private keys, tokens, `.env` files, `local.properties`, or unintended generated dependencies are included.
3. Legacy-name search over published source/docs content.
4. Repository-specific checks:
   - `npm run build` in `backend/console-ui` if source or UI assets changed.
   - `./gradlew test` if a Java runtime is available; otherwise report the exact blocker and do not imply the Gradle suite passed.
5. For every commit: `git show --stat --oneline <sha>` and `git show --check <sha>`.
6. Before push: `git status --short --branch`, `git log --oneline`, `git remote -v`, and a final staged/pushed file audit.
7. After push: `git ls-remote origin refs/heads/main` and verify it matches local `HEAD`; inspect the GitHub repository metadata without exposing credentials.

## Risks and stop conditions

- **Remote ownership/visibility ambiguity:** stop and ask; do not guess public versus private.
- **Existing remote repository:** stop before creation or push if `jboxx-ai/distribute-ovpn` already has content or history.
- **Dirty pre-existing Android work:** do not include it without explicit confirmation of the commit group.
- **Secret finding:** remove it from the candidate payload, rotate it if it was ever committed or published, and stop before push.
- **No Java runtime:** report Gradle verification as blocked; console UI verification does not cover Android/Kotlin.
- **Authentication unavailable:** leave local commits and remote configuration unchanged; provide the exact user-side authentication step needed.
- **Generated output drift:** do not commit generated files merely because a local build changed hashes.

## Retirement and compatibility

- Retire the old local directory/project name from active source, docs, and remote configuration.
- Preserve Git history; the rename is a logical/project identity change, not a history rewrite.
- Do not delete the old GitHub repository or alter external references automatically. The old remote remains a possible compatibility boundary until the new repository is confirmed reachable.
- Do not retain duplicate docs as a second source of truth after the merged tree is verified; the main repository's `docs/` is canonical.

## Expected delivery

- Local project path: `/opt/data/home/distribute-ovpn`.
- GitHub repository: `https://github.com/jboxx-ai/distribute-ovpn`.
- `main` pushed without force and without secrets.
- Atomic commit list and SHAs.
- Verification results, including any unavailable checks.
- Any intentionally excluded dirty files and the reason they remain excluded.
