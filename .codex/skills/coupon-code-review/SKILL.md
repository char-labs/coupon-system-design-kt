---
name: coupon-code-review
description: Use when reviewing a coupon-system branch, diff, or PR. Focus on bugs, regressions, missing tests, architecture drift, N+1, slow query risk, repeated lookups, lock contention, cache hot keys, retry storms, thread-pool saturation, event backpressure, and other operational bottlenecks before style comments. Trigger on prompts like 코드 리뷰해줘, 병목 있는지 봐줘, 슬로우 쿼리 위험 체크해줘, N+1 있는지 봐줘, or 운영 영향 큰 부분 리뷰해줘.
metadata:
  short-description: Review coupon-system changes for real correctness and performance risks
---

# Coupon Code Review

Use this skill when the user asks for a review, regression scan, bottleneck review, or pre-merge check.

## Workflow

1. Read `./AGENTS.md`.
2. Map changed modules first.
3. Review correctness risks before style comments.
4. Review performance and operational risks next.
5. Output findings first, ordered by severity, with file references and impact.
6. Distinguish measured bottlenecks from static risk signals. If runtime proof is missing, say so and recommend instrumentation or a test.

## Focus Areas

- API contract, validation, auth, response wrapping, and docs drift
- transaction boundaries, null handling, and branching logic
- repository contracts, query patterns, cache invalidation, and lock behavior
- N+1, repeated single-row lookups, slow page or count queries, and needless hydration
- thread-pool saturation, long queues, retry storms, event backlog, and missing observability

## Notes

- When performance dominates, recommend `performance_reviewer`.
- When `db-core` or `redis` dominates, recommend `storage_reliability_specialist`.
- Read [review-checklist.md](./references/review-checklist.md) for the detailed checklist and output shape.
