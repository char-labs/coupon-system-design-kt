---
name: coupon-subagent-usage
description: Use when you want to invoke coupon-system repo-local agents from the repository root. Gives ready-to-run Spawn prompt templates for feature mapping, code review, performance review, storage consistency review, observability review, CI triage, and commit-push planning, with Korean output by default.
metadata:
  short-description: Route coupon-system work to the right repo-local agent
---

# Coupon Subagent Usage

Use this skill when you want copy-paste prompts for repo-local agents.

## Workflow

1. Run from the repository root with `codex -C .` or `codex exec -C .`.
2. Remember that agents are not auto-run. You must explicitly request `Spawn ...`.
3. Pick the smallest agent that matches the task.
4. Fan out only when the work is meaningfully parallelizable.

## Commands

- Feature mapping:
  `Spawn feature_mapper to map the affected modules, files, lock/cache/tx touchpoints, and smallest validation scope for this coupon feature. Wait for it and return one implementation plan in Korean.`
- General code review:
  `Spawn code_reviewer to review this branch for bugs, regressions, missing tests, and architecture drift. Wait for it and summarize only concrete findings in Korean.`
- Performance or bottleneck review:
  `Spawn performance_reviewer to review this branch for N+1, slow query risk, lock contention, cache hot keys, thread-pool saturation, retry storms, and event backpressure. Wait for it and summarize only concrete findings in Korean.`
- Storage consistency review:
  `Spawn storage_reliability_specialist to review whether db-core and redis changes match repository, transaction, cache, and lock patterns. Wait for it and summarize the real risks in Korean.`
- Observability review:
  `Spawn observability_guard to review logs, metrics, tracing, and dashboard readiness for this change. Wait for it and summarize the concrete gaps in Korean.`
- CI triage:
  `Spawn ci_triager to explain why the selected Java 25 Gradle validation command is failing. Return the root cause, the smallest proof command, and the next fix to try in Korean.`
- Commit and push planning:
  `Spawn commit_push_guard to inspect current changes, propose safe commit groups, list ktlintFormat steps, and choose Java 25 validation commands before push. Do not mutate git. Respond in Korean.`

## Notes

- Agents are available because they exist under `.codex/agents`, but they still need explicit `Spawn`.
- If a review spans correctness, performance, and storage risk, run multiple review agents in parallel.
- Read [commands.md](./references/commands.md) for task-to-agent mapping and benefits.
