# Feature Checklist

## CRUD Feature

- `coupon/coupon-api/src/main/kotlin/com.coupon/controller/<feature>/<Feature>Controller.kt`
- `coupon/coupon-api/src/main/kotlin/com.coupon/controller/<feature>/request/<Feature>Request.kt`
- `coupon/coupon-api/src/main/kotlin/com.coupon/controller/<feature>/response/<Feature>Response.kt`
- `coupon/coupon-api/src/main/kotlin/com.coupon/controller/<feature>/response/<Feature>PageResponse.kt`
- `coupon/coupon-domain/src/main/kotlin/com/coupon/<feature>/<Feature>.kt`
- `coupon/coupon-domain/src/main/kotlin/com/coupon/<feature>/<Feature>Detail.kt`
- `coupon/coupon-domain/src/main/kotlin/com/coupon/<feature>/<Feature>Service.kt`
- `coupon/coupon-domain/src/main/kotlin/com/coupon/<feature>/<Feature>Repository.kt`
- `coupon/coupon-domain/src/main/kotlin/com/coupon/<feature>/command/<Feature>Command.kt`
- `coupon/coupon-domain/src/main/kotlin/com/coupon/<feature>/criteria/<Feature>Criteria.kt`
- `storage/db-core/src/main/kotlin/com/coupon/storage/rdb/<feature>/<Feature>Entity.kt`
- `storage/db-core/src/main/kotlin/com/coupon/storage/rdb/<feature>/<Feature>JpaRepository.kt`
- `storage/db-core/src/main/kotlin/com/coupon/storage/rdb/<feature>/<Feature>CoreRepository.kt`

## Stateful Feature Additions

- `<Feature>Issue` or equivalent child-flow model
- ownership-aware controller endpoints
- quantity or status mutation in one service transaction
- selective `@WithDistributedLock` decision for oversell or duplicate issue risk, and keep low-level lock execution outside business services when possible
- cache invalidation and business metric update points

## Review Reminders

- keep DTO mapping at the API edge
- keep orchestration in services and storage adapters thin
- wrap mutable flows with `@Transactional`, and add a dedicated `REQUIRES_NEW` transaction runner only when explicit propagation control is required
- prefer neighboring patterns over new abstractions
