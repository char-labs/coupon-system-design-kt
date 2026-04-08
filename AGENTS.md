# Coupon Codex Guide

## Architecture

- `coupon:coupon-api`: controllers, DTOs, security, async config, filters, wiring
- `coupon:coupon-domain`: services, domain models, repository interfaces, command and criteria, tx/cache/lock abstractions
- `coupon:coupon-enum`: shared enums
- `storage:db-core`: JPA entities, Spring Data repositories, domain repository implementations
- `storage:redis`: Redis cache, token, and lock adapters
- `support:logging`: logback, Sentry, tracing bridge
- `support:monitoring`: actuator and Prometheus exposure

## Auto-Loading

- Run `codex -C .` from the repo root so `.codex/skills` and `.codex/agents` are available.
- Auto-trigger entrypoints are the coupon-native skills only.
- Claude-aligned roles are kept as agents, not skills. Use them with explicit `Spawn ...`.
- Agents do not auto-run. A skill may recommend an agent, but it does not spawn it for you.

## Automatic Behavior Rules

- Skill-friendly prompts:
  - `코드 리뷰해줘`
  - `종합 리뷰해줘`
  - `아키텍처 리뷰해줘`
  - `보안 점검해줘`
  - `코드 스타일 리뷰해줘`
  - `병목 있는지 봐줘`
  - `성능 분석해줘`
  - `슬로우 쿼리 위험 체크해줘`
  - `commit-push 해줘`
  - `커밋 메시지랑 검증 커맨드 잡아줘`
  - `클린 아키텍처 기준으로 어디에 둬야 해?`
- Agent prompts must be explicit:
  - `Spawn feature_mapper to map the affected modules, files, lock/cache/tx touchpoints, and smallest validation scope for this coupon feature. Wait for it and return one implementation plan in Korean.`
  - `Spawn code_reviewer to review this branch for bugs, regressions, missing tests, and architecture drift. Wait for it and summarize only concrete findings in Korean.`
  - `Spawn performance_reviewer to review this branch for N+1, slow query risk, lock contention, cache hot keys, thread-pool saturation, retry storms, and event backpressure. Wait for it and summarize only concrete findings in Korean.`
  - `Spawn architecture-auditor to review this branch for dependency drift and layer boundary violations. Wait for it and summarize only concrete findings in Korean.`
  - `Spawn security-auditor to review this branch for auth, validation, secret exposure, and unsafe default risks. Wait for it and summarize only concrete findings in Korean.`
  - `Spawn style-auditor to review this branch for Kotlin idioms, naming drift, duplication, test quality, and error-handling inconsistency. Wait for it and summarize only concrete findings in Korean.`
  - `Spawn system-architect to design a 10k RPS evolution plan for this coupon system. Wait for it and return one plan in Korean.`
- `/agent` only shows active threads, not every available agent.

## Validation Defaults

- Use Java 25 for Gradle commands.
- On macOS prefer `JAVA_HOME=$(/usr/libexec/java_home -v 25)`.
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
  - `./gradlew ktlintFormat`

## Commit Workflow

- Inspect `git status` first and split unrelated work into separate commit groups.
- Run the narrowest relevant `ktlintFormat` task before validation.
- Prefer English commit messages with `[Topic] Issue summary`.
- Recommended topic labels: `[Feature]`, `[Fix]`, `[Refactor]`, `[Test]`, `[Docs]`, `[Chore]`
- When using `coupon-commit-push`, wait for the explicit approval token `확인` before mutating git state.

## Repo-Local Skills

- `coupon-code-review`
  - Main auto-trigger review entrypoint for correctness, architecture, security, style, and bottleneck checks.
- `coupon-feature-scaffold`
  - Add or extend a vertical feature slice without drifting from existing boundaries.
- `coupon-clean-architecture-guard`
  - Decide where new code belongs and review clean architecture drift.
- `coupon-reliability-patterns`
  - Review lock, retry, fallback, consistency, and event boundary decisions.
- `coupon-cache-topology`
  - Review Redis-centric cache design, TTL ownership, and hot-key risk.
- `coupon-observability-playbook`
  - Add or review logs, traces, Prometheus metrics, and dashboard readiness.
- `coupon-java25-gradle-validation`
  - Choose the smallest Java 25 validation command for changed modules.
- `coupon-subagent-usage`
  - Copy-paste Spawn prompts for repo-local agents.
- `coupon-commit-push`
  - Plan safe commit groups, formatting, and validation before commit or push.

## Custom Agents

- Claude-aligned Spawn-only agents:
  - `architecture-auditor`
  - `security-auditor`
  - `style-auditor`
  - `system-architect`
  - `mq-integrator`
  - `perf-optimizer`
  - `loadtest-engineer`
  - `observability-engineer`
- Coupon-native agents:
  - `feature_mapper`
  - `api_reviewer`
  - `storage_reliability_specialist`
  - `consistency_guard`
  - `observability_guard`
  - `performance_reviewer`
  - `ci_triager`
  - `commit_push_guard`
  - `code_reviewer`

## Quick Commands

- Open an interactive Codex session:
  `codex -C .`
- Feature map:
  `codex exec -C . "Spawn feature_mapper to map the affected modules, files, lock/cache/tx touchpoints, and smallest validation scope for this coupon feature. Wait for it and return one implementation plan in Korean."`
- General review:
  `codex exec -C . "Spawn code_reviewer to review this branch for bugs, regressions, missing tests, and architecture drift. Wait for it and summarize only concrete findings in Korean."`
- Bottleneck review:
  `codex exec -C . "Spawn performance_reviewer to review this branch for N+1, slow query risk, lock contention, cache hot keys, thread-pool saturation, retry storms, and event backpressure. Wait for it and summarize only concrete findings in Korean."`
- Architecture lens:
  `codex exec -C . "Spawn architecture-auditor to review this branch for dependency drift and layer boundary violations. Wait for it and summarize only concrete findings in Korean."`
- Security lens:
  `codex exec -C . "Spawn security-auditor to review this branch for auth, validation, secret exposure, and unsafe default risks. Wait for it and summarize only concrete findings in Korean."`
- High-scale planning:
  `codex exec -C . "Spawn system-architect to design a 10k RPS evolution plan for this coupon system. Wait for it and return one plan in Korean."`
- Commit planning:
  `codex exec -C . "Spawn commit_push_guard to inspect current changes, propose safe commit groups, list ktlintFormat steps, and choose Java 25 validation commands before push. Do not mutate git. Respond in Korean."`

## Usage Rules

- Do not use subagents for trivial single-file edits.
- Claude-aligned roles are explicit Spawn entrypoints only.
- Prefer the nearest existing feature slice before inventing a new abstraction.
- Before concluding that a third-party dependency is absent or proposing a custom adapter, inspect the nearest `build.gradle.kts`, any repo version catalog files, and `settings.gradle.kts` first.
- If repo-local files do not settle the decision and the current vendor API or SDK behavior matters, verify with official external references such as official docs, SDK docs, release notes, or artifact pages.
- Clearly separate locally verified facts from externally sourced guidance when explaining adoption direction.
- Distinguish static review signals from measured production evidence.
- Do not claim Kafka, CDC, DB replication, Redis Cluster sharding, or autoscaling behavior as already implemented.
- Read [docs/architecture/coupon-system-expansion-todo.md](docs/architecture/coupon-system-expansion-todo.md) for the current gap list and expansion TODO.
