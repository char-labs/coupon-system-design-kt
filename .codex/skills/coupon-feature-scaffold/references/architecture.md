# Architecture Map

## Module Roles

- `coupon:coupon-api`: HTTP controllers, request and response DTOs, web config, security config, filters, Swagger, application bootstrap
- `coupon:coupon-domain`: business services, domain models, repository interfaces, commands, criteria, transaction, lock, and cache abstractions
- `coupon:coupon-enum`: enums shared across modules
- `storage:db-core`: JPA entities, Spring Data repositories, domain repository implementations
- `storage:redis`: Redis adapters and Redis configuration
- `support:logging`: logback, tracing bridge, and logging configuration
- `support:monitoring`: actuator and Prometheus configuration

## Typical Vertical Slice

1. Controller
2. API request and response DTOs
3. Domain service
4. Domain model and detail model
5. Domain repository interface
6. Command and criteria types
7. Storage entity
8. JPA repository
9. Core repository adapter

For stateful flows also review:

- ownership and permission checks
- inventory or quantity mutation in the same transaction
- lock requirement for high-contention updates
- cache invalidation or metric updates if the read path depends on them

## Path Convention

- `coupon-api` uses physical directories rooted at `com.coupon/...`
- package declarations still use `com.coupon...`
- preserve that path quirk unless the whole module is being normalized separately
