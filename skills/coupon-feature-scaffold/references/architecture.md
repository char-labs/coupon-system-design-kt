# Architecture Map

## Module Roles

- `coupon/coupon-api`: HTTP controllers, request/response DTOs, web config, security config, filters, Swagger, application bootstrap
- `coupon/coupon-domain`: business services, domain models, repository interfaces, commands, criteria, transaction helpers, shared page/cache abstractions
- `coupon/coupon-enum`: enums shared across modules
- `storage/db-core`: JPA entities, Spring Data repositories, domain repository implementations
- `storage/redis`: Redis adapters and Redis configuration
- `support/logging`: logback and logging configuration
- `support/monitoring`: monitoring configuration

## Typical Vertical Slice

For an aggregate-style feature, mirror this chain:

1. Controller
2. API request/response DTOs
3. Domain service
4. Domain model/detail model
5. Domain repository interface
6. Command and criteria types
7. Storage entity
8. JPA repository
9. Core repository adapter

For a stateful feature, add:

- State validation in the service
- Ownership and permission checks in the service
- Quantity or related aggregate updates in the same transaction

## Naming Patterns

Feature name `Reward` usually maps to:

- `RewardController`
- `RewardService`
- `RewardRepository`
- `RewardCoreRepository`
- `RewardJpaRepository`
- `RewardEntity`
- `Reward`, `RewardDetail`
- `RewardRequest`, `RewardResponse`, `RewardPageResponse`
- `RewardCommand`, `RewardCriteria`

If the feature has issued or child records, mirror `CouponIssue` style:

- `RewardIssue`
- `RewardIssueService`
- `RewardIssueRepository`
- `RewardIssueRequest`, `RewardIssueResponse`
- `RewardIssueCommand`, `RewardIssueCriteria`

## Transaction Rules

- Use `Tx.writeable {}` for create, update, activate, deactivate, issue, use, cancel, delete
- Use `Tx.readable {}` only when a read path needs an explicit transactional scope
- Keep repository implementations thin; orchestration belongs in services

## API Conventions

- Prefer `@Tag` and `@Operation` on controllers
- Use `@ResponseStatus(HttpStatus.CREATED)` for create endpoints
- Use `@PreAuthorize("hasAuthority('ADMIN')")` for admin-only mutation endpoints
- Inject authenticated user as `user: User` where ownership matters
- Use paging with `OffsetPageRequest(page, size)`

## Path Conventions

The project has an important path quirk:

- `coupon-domain` and storage modules use standard source paths such as `com/coupon/...`
- `coupon-api` uses physical directories like `com.coupon/controller/...` while the package declaration is still `com.coupon.controller...`

Preserve the existing `coupon-api` directory layout unless the whole module is being normalized separately.
