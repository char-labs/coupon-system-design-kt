---
name: coupon-feature-scaffold
description: Scaffold new Kotlin Spring features for this coupon-system multi-module architecture. Use when Codex needs to add or extend a business feature that must follow the existing `coupon-api -> coupon-domain -> storage` layering, including controllers, request/response DTOs, command/criteria objects, services, repository interfaces, storage implementations, and related enums or errors.
---

# Coupon Feature Scaffold

## Overview

Use this skill to add a new feature without drifting from the project's established module boundaries and naming patterns.
Generate a concrete file plan first, then implement the smallest consistent vertical slice through API, domain, and storage.

## Quick Start

1. Identify the feature type:
- CRUD aggregate similar to `Coupon`
- Stateful flow similar to `CouponIssue`
- Security-sensitive endpoint similar to `Auth`

2. Read [architecture.md](./references/architecture.md) before creating files.

3. Generate a file checklist with:

```bash
python3 scripts/feature_scaffold_plan.py <feature-name> --kind <crud|stateful|auth-adjacent>
```

4. Create files in this order:
- Domain model and repository interface
- Command and criteria objects
- Service orchestration and transaction boundary
- Storage entity, JPA repository, and core repository
- API request/response DTOs and controller
- Security, enums, or error additions only if required

5. Match existing neighboring code instead of inventing a new abstraction.

## Workflow

### 1. Classify the change

- Choose `crud` when the feature mainly creates, lists, reads, updates, activates, deactivates, or deletes an aggregate.
- Choose `stateful` when the feature has ownership checks, inventory changes, or status transitions.
- Choose `auth-adjacent` when the change touches JWT, signup/signin flows, user injection, or security configuration.

### 2. Mirror a nearby feature

Use an existing feature as the source pattern:

- `Coupon` for aggregate CRUD
- `CouponIssue` for state transitions and ownership validation
- `Auth` and `User` for authentication and principal-driven endpoints

Do not mix patterns unless the new feature genuinely needs both.

### 3. Preserve module boundaries

- Keep controllers, filters, config, request/response DTOs in `coupon/coupon-api`
- Keep business models, services, repository interfaces, commands, criteria, and shared support code in `coupon/coupon-domain`
- Keep JPA entities and repository implementations in `storage/db-core`
- Keep Redis-specific adapters in `storage/redis`
- Add enums to `coupon/coupon-enum` only when the new feature introduces a new cross-module vocabulary

### 4. Preserve transaction style

- Keep transaction entry points in service methods using `Tx.writeable {}` or `Tx.readable {}`
- Avoid putting transaction orchestration in controllers or repository adapters
- Keep state validation close to the service layer

### 5. Finish the vertical slice

Before stopping, verify that the feature has:

- An inbound API path
- Domain command and/or criteria types where mutation happens
- A repository interface in domain and implementation in storage
- DTO mapping on the API edge
- Error handling consistent with existing `ErrorType` and `ErrorException` usage

## File Planning Rules

Use [feature-template.md](./references/feature-template.md) as the default checklist.

Adjust the checklist as follows:

- Omit write endpoints if the request is read-only
- Omit Redis files if no cache or token-style storage is needed
- Omit new enums when an existing enum already expresses the state
- Add `@PreAuthorize` only when the endpoint requires explicit authority checks

## Project-Specific Warnings

- The `coupon-api` Kotlin source tree uses a physical path rooted at `com.coupon/...`, while package declarations remain `com.coupon...`. Preserve the existing directory convention instead of normalizing it midstream.
- Follow existing DTO naming such as `FeatureRequest.Create`, `FeatureRequest.Update`, `FeatureResponse.Detail`, and `FeaturePageResponse` when the feature shape matches those patterns.
- When the service updates state, return the fresh detail model when the neighboring feature does so.
- Add tests when touching business invariants, status transitions, or security configuration.

## Resources

- [architecture.md](./references/architecture.md): module map, naming rules, and path conventions
- [feature-template.md](./references/feature-template.md): per-layer file checklist and implementation order
- `scripts/feature_scaffold_plan.py`: generate a feature-specific checklist before editing code
