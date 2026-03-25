---
name: coupon-cache-topology
description: Use when designing or reviewing caching for the coupon system. Covers Redis cache usage, future local-plus-remote cache layering, TTL ownership, invalidation, hot-key risk, cache stampede prevention, and serialization cost. Trigger on prompts about 캐시, 로컬 캐시, 리모트 캐시, TTL, hot key, or stampede.
metadata:
  short-description: Review coupon-system cache design and hot-key risks
---

# Coupon Cache Topology

Use this skill when cache correctness or cache efficiency matters.

## Workflow

1. Read `./AGENTS.md`.
2. Read [cache-checklist.md](./references/cache-checklist.md).
3. Decide whether the read path needs caching at all.
4. If caching is justified, decide:
   - key owner
   - TTL owner
   - invalidation trigger
   - hot-key risk
   - serialization size
5. Treat local in-memory cache as future work unless explicitly implemented.

## Rules

- Keep cache key and TTL ownership in service or finder logic, not in entities.
- Do not cache mutable business state without a clear invalidation path.
- Call out stampede risk for popular coupon metadata or inventory reads.
- Prefer explicit cache miss behavior over hidden fallback magic.
