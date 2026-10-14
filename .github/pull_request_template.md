## What and why

<!-- What this PR changes and why. Link the spec: docs/specs/<feature>/spec.md -->

**Requirements / tasks:** <!-- e.g. M0-4, ACC-1 · tasks.md T3–T5 -->

## How it was tested

<!-- Tests added or changed, and anything checked by hand. -->

## Checklist

- [ ] Traces to a spec and task in `docs/specs/<feature>/tasks.md`
- [ ] Tests added or updated in the same change, named with requirement IDs (TEST-1, TEST-2)
- [ ] `./mvnw verify` and `npm run lint`, `npm run typecheck`, `npm test` pass locally
- [ ] `/review-standards` run in Claude Code, with no unresolved findings (GIT-3)
- [ ] Deliberate rule exceptions have a comment citing the rule ID
- [ ] No new dependency, or the plan says why (GEN-9)
- [ ] Docs updated where needed (`requirements.md` status, ADRs, `technology-guide.md`)
