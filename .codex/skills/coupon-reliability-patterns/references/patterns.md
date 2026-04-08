# Reliability Patterns

## Available Today

- `@Transactional` on service boundaries, with a dedicated `REQUIRES_NEW` transaction runner only for explicit propagation control
- selective `@WithDistributedLock` on public entrypoints whose lock key can be derived directly from method arguments
- keep the low-level `Lock` component behind lock infrastructure or aspects rather than injecting it into business services
- Redis-backed cache through injected `Cache`
- local async event handlers through `@TransactionalEventListener`

## Review Checklist

- Does the flow need a lock to prevent duplicate issue or conflicting status changes?
- Is the operation idempotent if a retry occurs?
- Is fallback safe, or would it hide a broken business invariant?
- Is there a follow-up event or async task that needs stronger delivery guarantees later?
- Is the flow observable enough to debug retries, timeouts, and lock failures?

## Future TODO

- durable messaging backbone
- outbox plus CDC pipeline
- standardized retry and circuit-breaking policy
- failover-aware persistence and cache topology
