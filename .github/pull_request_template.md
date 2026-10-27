## What and why

<!-- What this PR changes and why. Link the spec: docs/specs/<feature>/spec.md -->

**Requirements / tasks:** <!-- e.g. M0-4, ACC-1 · tasks.md T3–T5 -->

## How it was tested

<!-- Tests added or changed, and anything checked by hand. -->

## Checklist

- [ ] Traces to a spec and task in `docs/specs/<feature>/tasks.md`
- [ ] Logic, endpoints and bug fixes were test-first: failing tests committed before the implementation, and no committed test changed to pass (TEST-2, TEST-11)
- [ ] Test names reference requirement IDs (TEST-1), and every test asserts behavior (no coverage-only tests)
- [ ] Coverage thresholds and exclusions unchanged, or the change was approved (TEST-9)
- [ ] `./mvnw verify` and `npm run lint`, `npm run typecheck`, `npm run test:coverage` pass locally (coverage limits and axe checks included)
- [ ] `/review-standards` run in Claude Code, with no unresolved findings (GIT-3)
- [ ] Deliberate rule exceptions have a comment citing the rule ID
- [ ] No new dependency, or the plan says why (GEN-9)
- [ ] Docs updated where needed (`requirements.md` status, ADRs, `technology-guide.md`)
