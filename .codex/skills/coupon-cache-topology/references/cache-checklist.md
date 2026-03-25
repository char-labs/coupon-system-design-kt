# Cache Checklist

- Remote cache exists today through Redis.
- Local cache is future work. Do not describe it as implemented.
- Review key naming, TTL ownership, invalidation, and whether cache misses fan out to a hot database path.
- Check serialization cost for large detail responses.
- Check whether failure mode should be fail-open, fail-closed, or no-cache.
- Review lock or single-flight need when a hot key is recomputed under load.
