---
name: coupon-feature-scaffold
description: Scaffold new Kotlin Spring features for this coupon-system multi-module architecture. Use when Codex needs to add or extend a business feature that must follow the existing `coupon-api -> coupon-domain -> storage` layering, including controllers, request and response DTOs, command and criteria objects, services, repository interfaces, storage implementations, and related lock, cache, or transaction touchpoints.
metadata:
  short-description: Add a feature slice that matches the current architecture
---

# Coupon Feature Scaffold

Use this skill to add a new feature without drifting from the project's vertical slice pattern.

## Workflow

1. Read `./AGENTS.md` and [architecture.md](./references/architecture.md).
2. Classify the feature as `crud`, `stateful`, or `auth-adjacent`.
3. Mirror the nearest existing feature:
   - `Coupon` for aggregate CRUD
   - `CouponIssue` for state transitions, ownership checks, and quantity changes
   - `Auth` or `User` for authentication and principal-driven flows
4. Generate a file checklist before editing:
   `python3 .codex/skills/coupon-feature-scaffold/scripts/feature_scaffold_plan.py <feature-name> --kind <crud|stateful|auth-adjacent>`
5. Implement the smallest consistent slice through API, domain, and storage.
6. If the flow has concurrency or cache sensitivity, explicitly decide whether it needs `Tx`, `Lock`, `Cache`, or monitoring updates.

## Rules

- Keep controllers and DTOs in `coupon:coupon-api`.
- Keep orchestration, models, repository interfaces, commands, criteria, and shared abstractions in `coupon:coupon-domain`.
- Keep JPA and Redis adapters thin inside `storage:db-core` and `storage:redis`.
- Use `Tx.writeable {}` for mutable service flows and `Tx.readable {}` only when a read path needs explicit transactional scope.
- Reuse existing enums and errors before creating new ones.
- Do not introduce message queue or CDC dependencies as if they already exist. Record those as TODO when relevant.

## Resources

- [architecture.md](./references/architecture.md)
- [feature-template.md](./references/feature-template.md)
- [coupon-system-expansion-todo.md](../../../docs/architecture/coupon-system-expansion-todo.md)
