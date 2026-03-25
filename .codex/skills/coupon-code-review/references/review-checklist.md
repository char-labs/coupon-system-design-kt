# Review Checklist

## Output Shape

- findings first
- severity before summary
- affected file path and impact in each finding
- separate static bottleneck suspicion from measured evidence

## Correctness Checklist

- `coupon-api`
  - request validation, auth scope, response wrapper behavior, Swagger metadata
- `coupon-domain`
  - service orchestration, `Tx` usage, invariants, branching, ownership checks
- `storage:db-core` and `storage:redis`
  - repository contract alignment, query predicates, cache invalidation, lock semantics
- tests
  - matching tests for public API, state transition, or persistence behavior changes

## Performance Checklist

- N+1 or repeated single-row lookup patterns
- expensive page or count queries
- needless large object hydration or payload serialization
- long transaction scope
- lock contention or missing lock on high-contention mutation paths
- Redis hot keys, stampede risk, or missing TTL ownership
- thread-pool saturation and oversized async queue risk
- retry storm or event backlog risk
- insufficient logs, metrics, or tracing to prove or disprove the suspected bottleneck
