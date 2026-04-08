---
name: coupon-clean-architecture-guard
description: Use when deciding where new code belongs or when reviewing clean architecture drift in this coupon system. Covers module boundaries, DTO placement, service orchestration, repository ownership, cross-cutting support modules, and when shared abstractions are justified.
metadata:
  short-description: Guard module boundaries and clean architecture decisions
---

# Coupon Clean Architecture Guard

Use this skill when the task is "where should this live?" or "does this design still match the architecture?"

## Workflow

1. Read `./AGENTS.md`.
2. Read [module-boundaries.md](./references/module-boundaries.md).
3. When third-party integration, alerting, or a new client or adapter is involved, inspect the nearest module `build.gradle.kts`, any repo version catalog files, and `settings.gradle.kts` before inventing a new abstraction.
4. If repo-local files do not settle the decision and current vendor APIs matter, verify with official external references before recommending an adoption direction.
5. Identify the smallest layer that owns the behavior.
6. Prefer the nearest existing feature slice over repo-wide refactors.
7. Call out boundary drift before suggesting new abstractions.

## Rules

- `coupon-api` owns HTTP, security, filters, and request or response mapping.
- `coupon-domain` owns orchestration, invariants, repository interfaces, commands, criteria, and shared transaction, cache, or lock abstractions.
- `storage:db-core` and `storage:redis` own implementation details of those domain interfaces.
- `support:*` is for cross-cutting observability and infrastructure wiring, not feature business logic.
- Add a new shared abstraction only when at least two feature slices genuinely need it.
- Do not claim a library is absent until repo-local dependency declarations are checked.
- When vendor SDK use is warranted, prefer a domain-facing port and an infra adapter over leaking transport details into orchestration code.
