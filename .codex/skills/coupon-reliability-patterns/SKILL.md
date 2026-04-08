---
name: coupon-reliability-patterns
description: Use when designing or reviewing coupon-system reliability patterns for consistency, lock usage, retry safety, fallback behavior, event boundaries, message-loss prevention, distributed transaction tradeoffs, or future Kafka, outbox, and CDC adoption. Trigger on prompts about 정합성, retry, fallback, 메시지 유실, CDC, 분산 트랜잭션, or 안정성 설계.
metadata:
  short-description: Review reliability and consistency patterns for the coupon system
---

# Coupon Reliability Patterns

Use this skill when the main question is correctness under concurrency or failure.

## Workflow

1. Read `./AGENTS.md`.
2. Read [patterns.md](./references/patterns.md).
3. Separate what the repo supports now from future design goals.
4. If retry, alerting, delivery, or adapter design depends on a third-party SDK or vendor behavior, inspect repo-local dependency declarations first and verify official external references when needed.
5. Review each mutable flow for:
   - transaction boundary
   - lock requirement
   - idempotency requirement
   - retry safety
   - fallback behavior
   - event emission point
6. If durable messaging or CDC is required but absent, record it as TODO instead of pretending it exists.

## Current Baseline

- single database transaction via `Tx`
- Redis-backed distributed lock via `Lock`
- Redis-backed remote cache via `Cache`
- local async follow-up work via `@Async` + `@TransactionalEventListener`

## Rules

- Only retry operations that are safe or idempotent.
- Prefer explicit failure over silent fallback when business invariants would be broken.
- Use lock review for duplicate issue, oversell, or racing status transition paths.
- Treat Kafka, outbox, CDC, DB replication, and Redis Cluster as future infrastructure until code and infra exist.
- Do not conclude that a custom transport is required until local dependencies and official vendor references are checked.
