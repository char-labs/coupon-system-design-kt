# Coupon System Expansion TODO

## Current Baseline

- MySQL single primary persistence through `storage:db-core`
- Redis single-node cache and lock adapters through `storage:redis`
- Transaction wrapper through `Tx`
- Prometheus exposure through actuator and Micrometer
- Logging, tracing bridge, and Sentry integration through `support:logging`
- Async event listeners for local asynchronous follow-up work

## Not Implemented Yet

- Local in-memory L1 cache layered with Redis
- Kafka or another durable message backbone
- Outbox pattern and CDC pipeline
- DB replication, partitioning, and automated failover
- Redis Cluster sharding and hot-slot mitigation
- Structured retry and fallback policies for external dependencies
- TPS-focused load testing and saturation baselines
- Grafana, Loki, and optional Datadog dashboards and alert policies

## Recommended Next Steps

1. Add SQL, Redis, and executor latency instrumentation so bottlenecks can be measured instead of inferred.
2. Introduce a repeatable load-test scenario for coupon issue, use, and cancel flows.
3. Add a local cache layer only after cache hit, invalidation, and staleness rules are explicit.
4. Add durable event delivery through Kafka plus outbox before claiming cross-service consistency guarantees.
5. Revisit retry, fallback, and circuit-breaking once real external clients or queues exist.
