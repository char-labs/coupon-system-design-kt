---
name: coupon-code-review
description: Use when reviewing a coupon-system branch, diff, or PR. This is the main auto-trigger review entrypoint for correctness, architecture, security, style, and bottleneck checks. Focus on bugs, regressions, missing tests, architecture drift, auth and validation risk, N+1, slow query risk, repeated lookups, lock contention, cache hot keys, retry storms, thread-pool saturation, event backpressure, and other operational bottlenecks before style comments. Trigger on prompts like 코드 리뷰해줘, 종합 리뷰해줘, 아키텍처 리뷰해줘, 보안 점검해줘, 코드 스타일 리뷰해줘, 병목 있는지 봐줘, 성능 분석해줘, 슬로우 쿼리 위험 체크해줘, or 운영 영향 큰 부분 리뷰해줘.
metadata:
  short-description: Review coupon-system changes for real correctness and performance risks
---

# Coupon Code Review

Use this skill when the user asks for any review, regression scan, bottleneck review, or pre-merge check.

## Workflow

1. Read `./AGENTS.md`.
2. Map changed modules first.
3. If the diff claims a dependency is absent or introduces a custom client or adapter, verify repo-local dependency declarations first and check official external references when current vendor behavior matters.
4. Review correctness, contract, auth, and validation risks before style comments.
5. Review performance and operational risks next.
6. Output findings first, ordered by severity, with file references and impact.
7. Distinguish measured bottlenecks from static risk signals. If runtime proof is missing, say so and recommend instrumentation or a test.

## Focus Areas

- API contract, validation, auth, response wrapping, and docs drift
- transaction boundaries, null handling, and branching logic
- repository contracts, query patterns, cache invalidation, and lock behavior
- custom external adapter choices that ignore an existing SDK or repo-local dependency declaration
- N+1, repeated single-row lookups, slow page or count queries, and needless hydration
- thread-pool saturation, long queues, retry storms, event backlog, and missing observability

## Notes

- When architecture dominates, recommend `architecture-auditor`.
- When security dominates, recommend `security-auditor`.
- When style or test quality dominates, recommend `style-auditor`.
- When performance dominates, recommend `performance_reviewer`.
- When `db-core` or `redis` dominates, recommend `storage_reliability_specialist`.
- Read [review-checklist.md](./references/review-checklist.md) for the detailed checklist and output shape.
