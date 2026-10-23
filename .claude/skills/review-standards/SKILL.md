---
name: review-standards
description: Review the current branch's changes against CrewUp's coding standards (docs/coding-standards.md) before opening a pull request. Reports findings with rule IDs; does not change code unless asked.
---

# Review against CrewUp coding standards

Review the changes on the current branch against `docs/coding-standards.md` and report every violation. This is the review step required by the pull request template (GIT-3).

## Steps

1. **Read the standards.** Read `docs/coding-standards.md` in full. Rules are grouped by prefix (GEN, JAVA, MOD, API, DB, TS, TEST, AI, GIT).

2. **Find the changes.** Compare the branch with `main`:
   - `git diff main...HEAD --stat` and `git diff main...HEAD` for committed changes
   - `git status` and `git diff` for uncommitted changes (include them, and say so in the report)
   - `git log main..HEAD --format='%h %s'` for commit messages (GIT-1, GIT-2)

   Read each changed file in full where context matters, not just the diff hunks.

3. **Run the automated checks** that exist in the project, and include their failures in the report. Skip any that aren't set up yet and say which were skipped.
   - Backend: `./mvnw -q spotless:check`
   - Frontend (from the frontend directory): `npm run lint`, `npm run typecheck`, `npm run test:coverage` (coverage limits and axe checks)
   - Semgrep project rules, through Docker from the repo root (no local install needed, so don't skip it): `docker run --rm -v "$PWD:/src" -w /src semgrep/semgrep:1.182.0 semgrep scan --metrics=off --config .semgrep --error`. Semgrep scans only files git tracks, so untracked files (not yet `git add`ed) are left out; say so if there are any.

4. **Review against the rules marked *Review*.** Go through each changed file and apply every rule group relevant to it (for example, JAVA, MOD, API and TEST for a new controller and its tests). Also check:
   - every new behavior has a test, and test names reference requirement IDs (TEST-1, TEST-2)
   - **test-first order (TEST-2, TEST-11):** for each test-first task, a `test(...)` commit with the failing tests comes before its `feat(...)` / `fix(...)` commit. Check that the implementation commit didn't change those tests: `git diff <test-commit> <impl-commit> -- <test files>` must be empty, unless an approved `test(...): correct …` commit sits in between
   - new page-level components have an axe component test (TEST-10)
   - **coverage (TEST-9):** any change to coverage thresholds or exclusions (`pom.xml` JaCoCo config, `vitest.config.ts`) was approved by the user and gives a reason; every new test asserts behavior tied to an acceptance criterion (no assertion-free tests, no tests that only call code to cover it); no speculative code that nothing uses yet (GEN-10)
   - the change traces to a task in the relevant `docs/specs/<feature>/tasks.md`
   - deliberate rule exceptions have a comment citing the rule ID; those are not findings

5. **Verify each finding** before reporting it: re-read the code and confirm the rule really applies. Leave out anything you can't point to in the code.

## Report

Group findings by severity:

- **Must fix:** security rules (GEN-6, GEN-7, JAVA-13, API-2, TS-10, AI-2), module boundaries (MOD-*), applied migrations edited (DB-2), missing tests or test-first order broken (TEST-2), committed tests changed to pass (TEST-11), lowered coverage thresholds or new exclusions without approval (TEST-9), failing automated checks.
- **Should fix:** other rule violations.
- **Consider:** suggestions that aren't rule violations. Keep these few.

For each finding give: the rule ID, a clickable `file:line` reference, one sentence on what's wrong, and the suggested fix.

End with a one-line verdict: **Ready for PR**, or **Not ready** with the count of must-fix findings.

Don't change any code. If the user asks for fixes afterward, fix one finding at a time and rerun the relevant checks.

If a rule seems wrong or missing, say so in a separate "Standards feedback" note. Don't add rules on your own.
