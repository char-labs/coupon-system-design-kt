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
  - `coupon-technical-writing`: question-driven backend blog writing with automatic `planner -> drafter -> reviewer` fan-out, publishable MDX-first drafting, frontmatter and hero-image aware publication harness, external reference packs, code-context blocks and short code excerpts instead of raw `Local References`, inline external citation anchors when useful, concrete headline patterns, polished `합니다체`, benchmark-noise filtering, reader-friendly Korean Mermaid labeling, direct-experience first-person tone, and bounded drafter-reviewer refinement loops
  - `coupon-clean-architecture-guard`: layer and module placement
  - `coupon-code-review`: correctness, bottleneck, and regression review
  - `coupon-subagent-usage`: copy-paste Spawn prompts
- Default fan-out
  - If scope is unclear or work spans multiple concerns, start with `feature_mapper`
  - Add only the specialists that materially change the answer: `tech_adoption_advisor`, `consistency_guard`, `storage_reliability_specialist`, `observability_guard`, `performance_reviewer`, `architecture-auditor`, `security-auditor`, `code_reviewer`, `ci_triager`
  - Synthesize specialist outputs and continue implementation; do not fan out trivial single-file edits
  - For backend blog drafting requests, `coupon-technical-writing` should fan out to `technical-writing-planner -> technical-writing-drafter -> technical-writing-reviewer`, then run `drafter -> reviewer` once more when the reviewer reports material issues. Return `개요 / 초안 / 이미지 제안 / 근거 팩`.
- Agents
  - `Spawn feature_mapper to map the affected modules, files, lock/cache/tx touchpoints, and smallest validation scope for this coupon feature. Wait for it and return one implementation plan in Korean.`
  - `Spawn tech_adoption_advisor to evaluate this dependency or architecture choice using repo-local facts first and official sources second. Return a recommendation in Korean.`
  - `Spawn technical-writing-planner to restate the blog topic in repo-accurate terms, gather local and external references, choose benchmark writing patterns, and return 제목 후보 / 독자 / 한 줄 주장 / 대안 / 개요 / 이미지 제안 / 근거 팩 초안 / publish 메모 / frontmatter 메모 / citation 방식 in Korean. Prefer concrete tech headlines and polished 합니다체 guidance. Omit benchmark references by default; include them only when a specific benchmark materially changed the outline, and state the structural reason per benchmark. Do not require a `Local References` section in outputs. Plan where repo facts should surface as code-context blocks, short code excerpts, inline flow explanations, captions, or inline citation anchors. For Mermaid-oriented image plans, prefer reader-friendly Korean semantic labels over raw command names. Prefer direct-experience first-person framing over meta phrases like "백엔드 개발자 관점에서". For Redis Lua topics in this repo, explicitly plan how reserve/release/rebuild each function will be explained. When the target is a publishable MDX post, also plan frontmatter, top hero image placement, and the default section order.`
  - `Spawn technical-writing-drafter to write the article from the approved outline and evidence pack. Always return 개요 / 초안 / 이미지 제안 / 근거 팩 in Korean. Keep article prose in 합니다체 and reflect the author's recent headline and section patterns. Never expose a raw "Local References" section unless the user explicitly asks for a working memo format. Translate repo-local facts into code-context blocks, short code excerpts, inline flow explanations, and section-level grounding instead. Keep `근거 팩` focused on external references and optional writing benchmarks. Do not carry unrelated or weakly related benchmark links into the final output. In Mermaid diagrams, use reader-friendly Korean labels by default; keep raw Redis/Kafka/API command names in code blocks or nearby explanations instead of node labels unless the exact command itself is the teaching point. Prefer "이번 프로젝트에서 제가..." 같은 직접 경험 서술을 쓰고, "백엔드 개발자 관점에서" 같은 메타 독자 프레이밍은 피한다. For Redis Lua topics, explain reserve/release/rebuild as concrete functions with role and call timing, not just names. For publishable MDX, include frontmatter by default, place a representative hero image right after frontmatter when available, and allow `<a href="#ref-e1">[E1]</a>` style inline citations with matching ids in `References` when the platform supports it. When reviewer feedback arrives, revise against the delta instead of redrafting blindly.`
  - `Spawn technical-writing-reviewer to verify factual correctness, evidence quality, image suggestions, title quality, and writing flow for the article draft. Return concrete corrections in Korean. Explicitly flag unrelated benchmark references or benchmark sections with no visible structural impact as noise and recommend removal. Also flag Mermaid nodes or edges that expose raw command names where Korean semantic labels would better serve readers. Flag meta audience framing such as "백엔드 개발자 관점에서" when direct-experience first-person prose would read more naturally. For Redis Lua topics, flag drafts that mention reserve/release/rebuild only by name without explaining each function's role. Also flag drafts that still expose raw Local References headings instead of absorbing repo-local proof into code-context blocks or inline explanations. Check frontmatter completeness, hero image relevance, and inline citation anchor correctness when those harness features are used. If material issues remain, return a compact revision brief for the drafter and require one more confirmation pass after revision; cap the loop at two review rounds.`
  - `Spawn code_reviewer to review this branch for bugs, regressions, missing tests, and architecture drift. Wait for it and summarize only concrete findings in Korean.`
  - `Spawn architecture-auditor to review this branch for dependency drift and layer boundary violations. Wait for it and summarize only concrete findings in Korean.`
