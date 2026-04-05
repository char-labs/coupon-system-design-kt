# Coupon Codex Guide

## Architecture

- `coupon:coupon-api`: controllers, request and response DTOs, security, async config, filters, application wiring
- `coupon:coupon-domain`: business services, domain models, repository interfaces, command and criteria types, transaction, cache, and lock abstractions
- `coupon:coupon-enum`: enums shared across modules
- `storage:db-core`: JPA entities, Spring Data repositories, and domain repository implementations
- `storage:redis`: Redis-backed cache, token, and lock adapters
- `support:logging`: logback, Sentry, tracing bridge, and log configuration
- `support:monitoring`: actuator and Prometheus exposure

## Auto-Loading

- Run `codex -C .` from the repository root so repo-local `.codex/skills` and `.codex/agents` are available.
- Skills can auto-trigger when the prompt explicitly names them, such as `$coupon-code-review`, or when the prompt closely matches the skill `name` and `description`.
- Agents do not auto-run. Files under `.codex/agents` only make the agents available for explicit `Spawn ...` requests.
- A skill may recommend using an agent, but do not assume the skill will spawn it automatically.

## Automatic Behavior Rules

- Skill-friendly prompts:
  - `코드 리뷰해줘`
  - `병목 있는지 봐줘`
  - `슬로우 쿼리 위험 체크해줘`
  - `commit-push 해줘`
  - `커밋 메시지랑 검증 커맨드 잡아줘`
  - `클린 아키텍처 기준으로 어디에 둬야 해?`
- Agent prompts must be explicit:
  - `Spawn feature_mapper to map the affected modules, files, lock/cache/tx touchpoints, and smallest validation scope for this coupon feature. Wait for it and return one implementation plan in Korean.`
  - `Spawn performance_reviewer to review this branch for N+1, slow query risk, lock contention, cache hot keys, thread-pool saturation, retry storms, and event backpressure. Wait for it and summarize only concrete findings in Korean.`
- `/agent` only shows currently active threads. It does not list every available agent in the repo.

## Validation Defaults

- Use Java 25 for Gradle commands.
- On macOS prefer:
  `JAVA_HOME=$(/usr/libexec/java_home -v 25)`
- Format before validation when Kotlin sources changed.
- Smallest proof commands:
  - `JAVA_HOME=$(/usr/libexec/java_home -v 25) ./gradlew :coupon:coupon-domain:test`
  - `JAVA_HOME=$(/usr/libexec/java_home -v 25) ./gradlew :storage:db-core:compileKotlin`
  - `JAVA_HOME=$(/usr/libexec/java_home -v 25) ./gradlew :storage:redis:compileKotlin`
  - `JAVA_HOME=$(/usr/libexec/java_home -v 25) ./gradlew :coupon:coupon-api:compileKotlin`
- Broader CI-parity check:
  - `JAVA_HOME=$(/usr/libexec/java_home -v 25) ./gradlew :coupon:coupon-api:build --no-daemon`
- Format commands:
  - `./gradlew :coupon:coupon-domain:ktlintFormat`
  - `./gradlew :storage:db-core:ktlintFormat`
  - `./gradlew :storage:redis:ktlintFormat`
  - `./gradlew :coupon:coupon-api:ktlintFormat`
  - `./gradlew ktlintFormat` for broad or cross-module churn

## Commit Workflow

- Always inspect `git status` first and split unrelated work into separate commit groups.
- Run the narrowest relevant `ktlintFormat` task before validation.
- Prefer English commit messages using `[Topic] Issue summary`.
- Recommended topic labels:
  - `[Feature]`
  - `[Fix]`
  - `[Refactor]`
  - `[Test]`
  - `[Docs]`
  - `[Chore]`
- When using `coupon-commit-push`, wait for the explicit approval token `확인` before mutating git state.

## Repo-Local Skills

- `coupon-feature-scaffold`
  - Add or extend a vertical feature slice without drifting from the existing API, domain, and storage boundaries.
- `coupon-clean-architecture-guard`
  - Decide where new code belongs and review clean architecture drift.
- `coupon-reliability-patterns`
  - Review lock, retry, fallback, consistency, and event boundary decisions without pretending missing infra already exists.
- `coupon-cache-topology`
  - Review Redis-centric cache design, future L1 cache layering, TTL ownership, and hot-key risks.
- `coupon-observability-playbook`
  - Add or review logs, traces, Prometheus metrics, and future dashboarding guidance.
- `coupon-java25-gradle-validation`
  - Choose the smallest Java 25 validation command for the changed modules.
- `coupon-subagent-usage`
  - Route work to the right repo-local agent with copy-paste prompt patterns.
- `coupon-commit-push`
  - Plan safe commit groups, enforce `ktlintFormat`, and choose validation commands before commit or push.
- `coupon-code-review`
  - Review for bugs, regressions, missing tests, N+1, slow query risk, lock contention, cache issues, and operational bottlenecks.

## Custom Agents

- `feature_mapper`
  - Map the affected modules, files, and validation scope before implementation.
- `api_reviewer`
  - Review controller, DTO, auth, response contract, and docs drift.
- `storage_reliability_specialist`
  - Review `db-core` and `redis` changes for repository, transaction, cache, and lock consistency.
- `consistency_guard`
  - Review idempotency, retry safety, fallback, event boundaries, and message-loss risks.
- `observability_guard`
  - Review logging fields, metrics, tracing, and dashboard readiness.
- `performance_reviewer`
  - Review static bottleneck risks such as N+1, slow query patterns, lock contention, hot keys, and backpressure.
- `ci_triager`
  - Triage Java 25, Gradle, ktlint, compile, and test failures.
- `commit_push_guard`
  - Plan commit groups, `ktlintFormat`, validation commands, and push readiness without mutating git.
- `code_reviewer`
  - Review the branch for correctness, regressions, tests, and architecture drift.

## Quick Commands

- Open an interactive Codex session:
  `codex -C .`
- Ask for a feature map:
  `codex exec -C . "Spawn feature_mapper to map the affected modules, files, lock/cache/tx touchpoints, and smallest validation scope for this coupon feature. Wait for it and return one implementation plan in Korean."`
- Ask for a general review:
  `codex exec -C . "Spawn code_reviewer to review this branch for bugs, regressions, missing tests, and architecture drift. Wait for it and summarize only concrete findings in Korean."`
- Ask for a bottleneck review:
  `codex exec -C . "Spawn performance_reviewer to review this branch for N+1, slow query risk, lock contention, cache hot keys, thread-pool saturation, retry storms, and event backpressure. Wait for it and summarize only concrete findings in Korean."`
- Ask for storage consistency review:
  `codex exec -C . "Spawn storage_reliability_specialist to review whether db-core and redis changes match repository, transaction, cache, and lock patterns. Wait for it and summarize the real risks in Korean."`
- Ask for commit and push planning:
  `codex exec -C . "Spawn commit_push_guard to inspect current changes, propose safe commit groups, list ktlintFormat steps, and choose Java 25 validation commands before push. Do not mutate git. Respond in Korean."`
- Run a parallel review:
  `codex exec -C . "Review this branch. Spawn code_reviewer, performance_reviewer, and storage_reliability_specialist in parallel. Have code_reviewer focus on regressions, performance_reviewer focus on bottlenecks, and storage_reliability_specialist focus on db-core/redis consistency. Wait for all of them and summarize only concrete findings in Korean."`

## Usage Rules

- Do not use subagents for trivial single-file edits.
- Prefer the nearest existing feature slice before inventing a new abstraction.
- Distinguish static review signals from measured production evidence.
- Do not claim Kafka, CDC, DB replication, Redis Cluster sharding, or autoscaling behavior as already implemented. Treat them as future work until code and infrastructure exist.
- Read [docs/architecture/coupon-system-expansion-todo.md](docs/architecture/coupon-system-expansion-todo.md) for the current gap list and expansion TODO.
