# Module Boundaries

- Put DTO-to-domain mapping at the API edge unless the mapping is shared by multiple inbound adapters.
- Keep repository interfaces in `coupon-domain`, even when only one storage implementation exists today.
- Keep transaction, cache, and lock coordination close to service or finder logic rather than inside entities.
- Keep entity mutation small and persistence-focused. Cross-aggregate orchestration belongs in services.
- Add to `coupon-enum` only when the enum is shared across modules. Feature-local state can stay in the feature package.
