---
name: coupon-subagent-usage
description: Use when you want short Spawn prompt templates for coupon-system repo-local agents. Covers review, high-scale planning, storage and validation checks, and commit planning with Korean output by default.
metadata:
  short-description: Route coupon-system work to the right repo-local agent
---

# Coupon Subagent Usage

Use this skill when you want short copy-paste Spawn prompts.

## Rules

1. Run from the repository root with `codex -C .` or `codex exec -C .`.
2. Agents do not auto-run. You must explicitly request `Spawn ...`.
3. Pick the smallest agent that matches the task.
4. Fan out only when the work is meaningfully parallelizable.
5. When the task involves third-party library adoption or vendor integration, include a reminder to inspect repo-local dependency declarations and official external references before concluding.

## Review

- General review:
  `Spawn code_reviewer to review this branch for bugs, regressions, missing tests, and architecture drift. If the diff introduces a custom external client or claims a dependency is absent, inspect repo-local dependency declarations and official external references before concluding. Wait for it and summarize only concrete findings in Korean.`
- Bottleneck review:
  `Spawn performance_reviewer to review this branch for N+1, slow query risk, lock contention, cache hot keys, thread-pool saturation, retry storms, and event backpressure. Wait for it and summarize only concrete findings in Korean.`
- Storage review:
  `Spawn storage_reliability_specialist to review whether db-core and redis changes match repository, transaction, cache, and lock patterns. Wait for it and summarize the real risks in Korean.`
- Architecture lens:
  `Spawn architecture-auditor to review this branch for dependency drift and layer boundary violations. If third-party integration or library adoption direction matters, inspect repo-local dependency declarations and official external references before concluding. Wait for it and summarize only concrete findings in Korean.`
- Security lens:
  `Spawn security-auditor to review this branch for auth, validation, secret exposure, and unsafe default risks. Wait for it and summarize only concrete findings in Korean.`
- Style lens:
  `Spawn style-auditor to review this branch for Kotlin idioms, naming drift, duplication, test quality, and error-handling inconsistency. Wait for it and summarize only concrete findings in Korean.`

## High-Scale

- Architecture plan:
  `Spawn system-architect to design a 10k RPS evolution plan for this coupon system. Wait for it and return one plan in Korean.`
- MQ implementation:
  `Spawn mq-integrator to implement the chosen Kafka or RabbitMQ flow for coupon issuance without breaking idempotency and failure handling. Wait for it and summarize the concrete code changes and risks in Korean.`
- Runtime tuning:
  `Spawn perf-optimizer to tune JVM, HikariCP, Tomcat, async executors, and Redis settings for this coupon-system flow. Wait for it and summarize the concrete changes and validation commands in Korean.`
- Load test:
  `Spawn loadtest-engineer to add or update the k6 scenarios and runbook for this coupon-system throughput target. Wait for it and summarize the concrete changes in Korean.`
- Observability:
  `Spawn observability-engineer to add or review Prometheus, Grafana, and actuator coverage for this coupon-system flow. Wait for it and summarize the concrete gaps or changes in Korean.`

## Storage, CI, Commit

- Feature mapping:
  `Spawn feature_mapper to map the affected modules, files, lock/cache/tx touchpoints, and smallest validation scope for this coupon feature. If third-party integration or client choice matters, inspect repo-local dependency declarations and official external references before concluding. Wait for it and return one implementation plan in Korean.`
- Observability guard:
  `Spawn observability_guard to review logs, metrics, tracing, dashboard readiness, and alerting integration for this change. If vendor SDK or alert transport choice matters, inspect repo-local dependency declarations and official external references before concluding. Wait for it and summarize the concrete gaps in Korean.`
- CI triage:
  `Spawn ci_triager to explain why the selected Java 25 Gradle validation command is failing. Return the root cause, the smallest proof command, and the next fix to try in Korean.`
- Commit planning:
  `Spawn commit_push_guard to inspect current changes, propose safe commit groups, list ktlintFormat steps, and choose Java 25 validation commands before push. Do not mutate git. Respond in Korean.`
