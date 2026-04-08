---
name: coupon-specialist-orchestration
description: Default orchestration entrypoint for non-trivial coupon-system tasks. Use when a feature, bug fix, refactor, review, architecture question, performance issue, or investigation may span multiple concerns or modules and should benefit from specialist agents before implementation continues.
metadata:
  short-description: Orchestrate coupon-system specialist agents for non-trivial tasks
---

# Coupon Specialist Orchestration

Use this skill when the task is not a trivial single-file edit and specialist fan-out will improve the answer or the implementation plan.

## Workflow

1. Read `./AGENTS.md` and [docs/agent/orchestration.md](../../../docs/agent/orchestration.md).
2. Decide whether the task is trivial. If it is trivial, work locally without spawning specialists.
3. If scope or validation surface is unclear, start with `feature_mapper`.
4. Fan out only the specialists that answer materially different questions.
5. Prefer parallel read-only specialist calls when the questions are independent.
6. Synthesize the results into one recommendation, plan, or implementation path.
7. Continue the implementation after the specialist results are integrated.

## Specialist Routing

- Use `tech_adoption_advisor` for dependency, SDK, integration, or architecture adoption choices.
- Use `consistency_guard` for retry, lock, idempotency, fallback, or async consistency questions.
- Use `storage_reliability_specialist` for `db-core`, `redis`, query, cache, or lock ownership questions.
- Use `observability_guard` for logs, metrics, traces, alerting, or async backlog visibility.
- Use `performance_reviewer` for N+1, hot key, contention, backlog, or saturation risk.
- Use `architecture-auditor` for module placement or dependency direction drift.
- Use `security-auditor` for auth, validation, abuse, or secret exposure risk.
- Use `code_reviewer` for pre-merge correctness and regression review.
- Use `ci_triager` for failing Gradle, compile, test, lint, or JDK signals.

## Rules

- Do not spawn specialists for trivial or purely mechanical edits.
- Avoid duplicate lenses that answer the same question.
- Keep the final answer unified; do not hand the user raw, unsynthesized specialist output.
- Prefer repo-local facts and current architecture docs over assumptions.
