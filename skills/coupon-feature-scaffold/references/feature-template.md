# Feature Checklist

## Usage

Run the scaffold planner first:

```bash
python3 scripts/feature_scaffold_plan.py reward --kind crud
```

Then create only the files the feature actually needs.

## CRUD Feature

### API

- `coupon/coupon-api/src/main/kotlin/com.coupon/controller/<feature>/<Feature>Controller.kt`
- `coupon/coupon-api/src/main/kotlin/com.coupon/controller/<feature>/request/<Feature>Request.kt`
- `coupon/coupon-api/src/main/kotlin/com.coupon/controller/<feature>/response/<Feature>Response.kt`
- `coupon/coupon-api/src/main/kotlin/com.coupon/controller/<feature>/response/<Feature>PageResponse.kt`

### Domain

- `coupon/coupon-domain/src/main/kotlin/com/coupon/<feature>/<Feature>.kt`
- `coupon/coupon-domain/src/main/kotlin/com/coupon/<feature>/<Feature>Detail.kt`
- `coupon/coupon-domain/src/main/kotlin/com/coupon/<feature>/<Feature>Service.kt`
- `coupon/coupon-domain/src/main/kotlin/com/coupon/<feature>/<Feature>Repository.kt`
- `coupon/coupon-domain/src/main/kotlin/com/coupon/<feature>/command/<Feature>Command.kt`
- `coupon/coupon-domain/src/main/kotlin/com/coupon/<feature>/criteria/<Feature>Criteria.kt`

### Storage

- `storage/db-core/src/main/kotlin/com/coupon/storage/rdb/<feature>/<Feature>Entity.kt`
- `storage/db-core/src/main/kotlin/com/coupon/storage/rdb/<feature>/<Feature>JpaRepository.kt`
- `storage/db-core/src/main/kotlin/com/coupon/storage/rdb/<feature>/<Feature>CoreRepository.kt`

## Stateful Feature

Start with the CRUD checklist, then add:

- `coupon/coupon-domain/src/main/kotlin/com/coupon/<feature>/<Feature>Status` if no shared enum exists yet
- Child flow models such as `<Feature>Issue.kt` and `<Feature>IssueDetail.kt`
- Child flow service and repository
- Ownership-aware controller endpoints
- Related aggregate quantity adjustments inside the same service transaction

## Auth-Adjacent Change

Start from the smallest necessary slice:

- Controller endpoint in `coupon-api`
- Request/response DTOs
- Domain service or facade updates
- Token/history repository changes if the flow mutates auth state
- `SecurityConfig` or filter changes only when the endpoint accessibility rules change

## Review Checklist

- Do class names match neighboring features?
- Does every mutable service method use the correct `Tx` wrapper?
- Are repository interfaces defined in domain and implementations in storage?
- Are request and response mappings kept at the API edge?
- Did you reuse `ErrorType` and existing enums where possible?
- Did you avoid adding new abstractions that only one feature uses?
