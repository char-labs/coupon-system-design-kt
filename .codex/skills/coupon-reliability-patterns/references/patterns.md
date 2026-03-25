# Reliability Patterns

## Available Today

- `Tx.writeable {}` and `Tx.readable {}`
- `Lock.executeWithLock(...)`
- Redis-backed cache through `Cache`
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
