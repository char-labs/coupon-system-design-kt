# Agent Docs Index

Use this directory as the agent-facing table of contents. The root `AGENTS.md` stays short and points here; architecture docs remain the source of truth for current behavior.

## Read Order

1. [../architecture/current-architecture-overview.md](../architecture/current-architecture-overview.md)
2. [../architecture/coupon-issuance-runtime.md](../architecture/coupon-issuance-runtime.md)
3. [../../coupon/coupon-worker/docs/coupon-kafka-runtime-guide.md](../../coupon/coupon-worker/docs/coupon-kafka-runtime-guide.md)
4. [orchestration.md](./orchestration.md)
5. [validation.md](./validation.md)
6. [adoption-rubric.md](./adoption-rubric.md)
7. [technical-writing-workflow.md](./technical-writing-workflow.md)

## Task Routing

- Need the current runtime or module map: start with the two architecture docs.
- Need the worker or Kafka runtime view: read the worker runtime guide.
- Need to decide which specialists to call: read [orchestration.md](./orchestration.md).
- Need a build, test, lint, or compile command: read [validation.md](./validation.md).
- Need to evaluate a new dependency, SDK, or architecture direction: read [adoption-rubric.md](./adoption-rubric.md).
- Need backend blog topics, article outlines, or drafts with external references and image suggestions: read [technical-writing-workflow.md](./technical-writing-workflow.md).

## Notes

- Older phase documents are useful background, not automatic proof that a design is already implemented.
- Prefer the nearest existing slice before inventing a new abstraction.
- Keep the final answer explicit about what was verified locally and what came from official external references.
