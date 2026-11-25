# PULSE Smart Gym — Project Agent Rules

## Mandatory GitHub Sync & Commit Workflow

Repository: `https://github.com/AyaSheta13/PULSE-SmartGym`

After every completed implementation task (Design System migration, Navigation 3, Room database, authentication, application screens, workout logic, history/analytics, BLE integration, bug fixes, refactoring, or documentation updates), follow this exact workflow:

**Implement → Build → Test → Review Diff → Commit → Push → Verify Remote**

### Commit Rules
- Create one focused commit per completed task or coherent change.
- Use clear, descriptive conventional commit messages, such as:
  - `chore(p0): integrate existing design system module`
  - `feat(designsystem): migrate PULSE color and typography tokens`
  - `feat(navigation): implement Navigation 3 app shell`
  - `feat(workouts): implement workout planning with Room`
  - `feat(session): implement persistent workout session state`
  - `feat(device): add simulated BLE connection states`
  - `fix(history): correct partial session statistics`
- Include all relevant source and documentation changes.
- Never commit broken or incomplete work as completed.
- Never include credentials, API keys, local machine files, or build artifacts.
- Do not combine unrelated changes in one commit.
- Never use `git push --force`.
- Never delete unrelated existing work or silently discard uncommitted changes.
- Automatically commit and push **only** the work completed for the currently authorized task; do not start another implementation phase without authorization.

### Failure Handling
- If build or test verification fails, report the failure and do **not** push as a completed task.
- If GitHub authentication or network access fails, keep changes locally and clearly report that the push did not succeed.

### Mandatory Final Report
After every completed task, report:
1. Task or phase name
2. Summary of changes
3. Files created or modified
4. Build and test results
5. Commit message and Commit SHA
6. Branch name and Push status
7. GitHub commit URL
8. Any remaining issues

Only report **PUSHED SUCCESSFULLY** after verifying that the commit exists on the remote repository.
