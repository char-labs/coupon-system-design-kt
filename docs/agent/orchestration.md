# Specialist Orchestration

Use specialist fan-out by default for non-trivial coupon-system tasks. The main agent stays responsible for the final synthesis and for continuing implementation after the specialist answers return.

## When To Fan Out

- Scope is unclear
- More than one module or concern is affected
- The task changes runtime behavior, concurrency, data ownership, observability, or external integrations
- The task asks for review, architecture direction, optimization, or investigation

Do not fan out trivial single-file edits or purely mechanical churn.

## Start Point

- Start with `feature_mapper` when scope or validation surface is unclear.
- Skip `feature_mapper` only when the task is already tightly scoped and the affected slice is obvious.

## Specialist Matrix

- `tech_adoption_advisor`
  - dependency, SDK, integration, transport, or architecture adoption choice
- `consistency_guard`
  - mutable flow, retry safety, lock boundary, idempotency, fallback, async consistency
- `storage_reliability_specialist`
  - `db-core`, `redis`, query shape, cache ownership, lock semantics, invalidation
- `observability_guard`
  - logs, metrics, traces, alerts, async backlog visibility, dashboard readiness
- `performance_reviewer`
  - N+1, slow query risk, hot key, lock contention, backlog, saturation, retry storm
- `architecture-auditor`
  - module placement, dependency direction, boundary drift
- `security-auditor`
  - auth, validation, secret exposure, abuse paths, unsafe defaults
- `code_reviewer`
  - pre-merge correctness, regression, missing tests
- `ci_triager`
  - failing Gradle, compile, test, lint, or JDK issues

## Default Bundles

- Feature implementation:
  `feature_mapper` -> add `tech_adoption_advisor`, `consistency_guard`, `storage_reliability_specialist`, `observability_guard` as needed
- Bug fix:
  `feature_mapper` when scope is unclear -> add the specialist closest to the failing layer -> finish with `code_reviewer` when regression risk is high
- Review request:
  `code_reviewer` primary -> add `performance_reviewer`, `architecture-auditor`, `security-auditor`, `storage_reliability_specialist` only when the diff warrants it
- Dependency or architecture question:
  `tech_adoption_advisor` primary -> add `architecture-auditor` if module boundary impact matters
- Runtime or performance work:
  `performance_reviewer` + `observability_guard` + `storage_reliability_specialist`; add `loadtest-engineer` only when implementation or runbook work is requested
- Validation failure:
  `ci_triager` primary -> add `feature_mapper` only if the failing surface is still unclear

## Synthesis Rule

- Wait for the specialists that unblock the next decision.
- Prefer parallel read-only specialists over serial fan-out when they answer different questions.
- Resolve conflicts by prioritizing current code and architecture docs first, then specialist findings, then external references.
- Return one combined answer or one implementation path, not disconnected specialist dumps.
