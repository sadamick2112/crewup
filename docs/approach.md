# CrewUp: Spec-Driven Approach — SpecKit vs. Native Claude Code Files

**Status:** Recommendation, draft v0.1 · October 6, 2026

## Recommendation

**Start with native Claude Code files, organized the way spec-driven development (SDD) organizes work.** Revisit SpecKit as a deliberate experiment on one Phase 2 feature.

In practice, that means:

- Product-level specs in `docs/` (requirements, architecture, roadmap). These already exist as this set of files.
- One folder per feature in `docs/specs/<feature>/` with `spec.md`, `plan.md`, and `tasks.md`. This is the same spec → plan → tasks discipline SpecKit enforces, without the tooling.
- A short `CLAUDE.md` that acts as your "constitution": principles, stack, commands, and conventions.
- Your own skills and subagents in `.claude/`, added once you've repeated a step by hand two or three times.

## Why not SpecKit first

| Factor | SpecKit | Native Claude Code files |
|---|---|---|
| Learning Claude Code itself | Wraps Claude's primitives behind its own commands | You learn CLAUDE.md, skills, subagents, hooks, and plan mode directly |
| Scope of a "spec" | Feature-scoped; you'd still need product-level docs like these | Product docs and feature specs live side by side |
| Overhead | Generates many artifacts (constitution, spec, plan, tasks, contracts) that you must review | As light or heavy as you choose |
| Guardrails | Enforced workflow with clarify and analyze steps | Only as disciplined as you are, unless you build gates (skills, hooks) |
| Tool portability | Works across 30+ agents (Copilot, Gemini, Codex, and others) | Markdown specs are portable; skills are Claude-specific |
| Resume story | "Used SpecKit" | "Designed my own spec-driven workflow with Claude Code skills, subagents, and hooks" |

**The deciding factor for you:** employers care that you can direct AI agents through specs, review their output, and keep quality high. Which tool you used matters much less. Building the workflow yourself demonstrates that more convincingly, and it teaches the Claude primitives you said you want to learn. As one comparison puts it, tooling choice matters far less than spec quality.

**The main risk** of the native approach is drift back into unstructured prompting. The mitigation is the feature folder convention below, plus a `/spec-feature` skill that enforces it.

## How each feature flows

1. **Spec:** `docs/specs/<feature>/spec.md` holds user stories, acceptance criteria (pulled from the requirements IDs), and what's out of scope. Ask Claude to interview you and flag gaps before you approve it.
2. **Plan:** In Claude Code **plan mode**, Claude writes `plan.md` covering affected modules, API changes, data migrations, and the test approach. You review and edit it, because this is where your principal-engineer judgment matters most.
3. **Tasks:** `tasks.md` is a checklist of small, independently testable steps.
4. **Implement:** Claude works one task at a time, running tests and committing after each.
5. **Review:** A fresh-context review (later, a `code-reviewer` subagent) checks the diff against the spec's acceptance criteria.
6. **Close:** Update the requirement status and note any decisions in `docs/adr/`.

## What to add to `.claude/`, and when

Add each item only after you've done its job by hand a few times. That's how you learn what belongs in it.

| When | Add | Purpose |
|---|---|---|
| Milestone 1 | `CLAUDE.md` | Constitution: principles, stack, commands, conventions |
| After 2–3 features | Skill `spec-feature` | Scaffolds `docs/specs/<feature>/` from requirement IDs and interviews you |
| After 2–3 features | Skill `new-activity-type` | Encodes the Activity Type plug-in pattern for both backend and frontend |
| Once the code base has patterns | Subagent `code-reviewer` | Fresh-context review against the spec and conventions; read-only tools |
| Once tests exist | Subagent `test-writer` | Writes missing tests from acceptance criteria |
| Once CI exists | Hooks | Run formatter and linter after edits; block commits when tests fail |
| Phase 2 | Experiment: SpecKit | Run one feature (for example, Movies) through SpecKit and write a short comparison for your README or blog |

## When multiple agents make sense

Not in the MVP's first milestones. Add the reviewer subagent first, because a separate context reviewing the work catches what the author context misses. Run parallel agents (for example, separate git worktrees building the frontend and backend of one feature) only after the API contract for that feature is fixed. Otherwise they'll diverge.

## Your Pro plan in practice

- Claude Code usage shares one pool with Claude chat, with a 5-hour rolling window and a weekly cap. Keep sessions focused on one task, and start a fresh session per task to keep context small.
- **CrewUp's own AI features call the Claude API, and those calls are billed separately from your Pro subscription.** You'll need an API key on Anthropic's Console with a spending limit set. See the AI section in `architecture.md`.

## Sources

- [github/spec-kit](https://github.com/github/spec-kit)
- [GitHub Spec Kit vs Kiro vs Claude Code SDD Workflows](https://glukhov.org/ai-devtools/ai-coding-assistants/spec-kit-vs-kiro-vs-claude-code/)
- [Visual Studio Magazine: GitHub open sources kit for spec-driven AI development](https://visualstudiomagazine.com/articles/2025/09/03/github-open-sources-kit-for-spec-driven-ai-development.aspx)
