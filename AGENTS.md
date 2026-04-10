# Coupon Agent Map

## Snapshot

- Core runtime: `Redis reserve -> Kafka publish -> worker consume -> distributed lock -> DB persist`
- `POST /coupon-issues` `SUCCESS` means `Redis reserve + Kafka broker ack`, not final DB persistence
- outbox worker handles `COUPON_ISSUED / COUPON_USED / COUPON_CANCELED` projection durability, not intake durability
- Redis ownership is split: Lua script for issue-state reserve or release, Redisson for distributed lock and processing limit
- Main modules: `coupon-api`, `coupon-domain`, `coupon-worker`, `storage:db-core`, `storage:redis`, `support:logging`, `support:monitoring`
- Current structural exception: `coupon-worker` still uses explicit component scan and excludes intake or restaurant packages

## Read First

1. [docs/agent/index.md](docs/agent/index.md)
2. [docs/architecture/current-architecture-overview.md](docs/architecture/current-architecture-overview.md)
3. [docs/architecture/coupon-issuance-runtime.md](docs/architecture/coupon-issuance-runtime.md)
4. [coupon/coupon-worker/docs/coupon-kafka-runtime-guide.md](coupon/coupon-worker/docs/coupon-kafka-runtime-guide.md)
5. [coupon/coupon-worker/docs/phase-2-outbox-worker-runtime.md](coupon/coupon-worker/docs/phase-2-outbox-worker-runtime.md)
6. [docs/agent/orchestration.md](docs/agent/orchestration.md)
7. [docs/architecture/coupon-system-expansion-todo.md](docs/architecture/coupon-system-expansion-todo.md)
8. [docker/README.md](docker/README.md)

## Validation

- Prefer plain `./gradlew ...` commands
- When the environment is unclear, check `./gradlew -version` first
- If the Gradle launcher JVM is not Java 25, rerun the same command with `JAVA_HOME=$(/usr/libexec/java_home -v 25)`
- Format Kotlin changes before validation
- Use [docs/agent/validation.md](docs/agent/validation.md) for the module-to-command matrix

## Adoption Research

- For dependency, SDK, integration, or architecture adoption questions, read [docs/agent/adoption-rubric.md](docs/agent/adoption-rubric.md)
- Start with local facts: `settings.gradle.kts`, nearest `build.gradle.kts`, `gradle.properties`, current feature slice, and architecture docs
- Use official external references only when vendor behavior or current API support matters
- Separate `로컬 사실 / 외부 사실 / 권장안 / 미도입 이유`
- Prefer repo-fit and clear ownership over novelty; keep SDK wiring in infra-facing adapters

## Entrypoints

- Skills
  - `coupon-specialist-orchestration`: default router for non-trivial tasks; fan out to specialists before implementation
  - `coupon-java25-gradle-validation`: smallest safe format and validation command
  - `coupon-tech-adoption-review`: dependency or SDK adoption review
  - `coupon-technical-writing`: question-driven backend blog writing with automatic `planner -> drafter -> reviewer` fan-out, references, and image suggestions
  - `coupon-clean-architecture-guard`: layer and module placement
  - `coupon-code-review`: correctness, bottleneck, and regression review
  - `coupon-subagent-usage`: copy-paste Spawn prompts
- Default fan-out
  - If scope is unclear or work spans multiple concerns, start with `feature_mapper`
  - Add only the specialists that materially change the answer: `tech_adoption_advisor`, `consistency_guard`, `storage_reliability_specialist`, `observability_guard`, `performance_reviewer`, `architecture-auditor`, `security-auditor`, `code_reviewer`, `ci_triager`
  - Synthesize specialist outputs and continue implementation; do not fan out trivial single-file edits
  - For backend blog drafting requests, `coupon-technical-writing` should fan out to `technical-writing-planner -> technical-writing-drafter -> technical-writing-reviewer` and return `개요 / 초안 / 이미지 제안 / References`
- Agents
  - `Spawn feature_mapper to map the affected modules, files, lock/cache/tx touchpoints, and smallest validation scope for this coupon feature. Wait for it and return one implementation plan in Korean.`
  - `Spawn tech_adoption_advisor to evaluate this dependency or architecture choice using repo-local facts first and official sources second. Return a recommendation in Korean.`
  - `Spawn technical-writing-planner to restate the blog topic in repo-accurate terms, gather local and external references, choose benchmark writing patterns, and return 제목 후보 / 독자 / 한 줄 주장 / 대안 / 개요 / 이미지 제안 / references 초안 in Korean.`
  - `Spawn technical-writing-drafter to write the article from the approved outline and reference pack. Always return 개요 / 초안 / 이미지 제안 / References in Korean.`
  - `Spawn technical-writing-reviewer to verify factual correctness, reference quality, image suggestions, and writing flow for the article draft. Return concrete corrections in Korean.`
  - `Spawn code_reviewer to review this branch for bugs, regressions, missing tests, and architecture drift. Wait for it and summarize only concrete findings in Korean.`
  - `Spawn architecture-auditor to review this branch for dependency drift and layer boundary violations. Wait for it and summarize only concrete findings in Korean.`
