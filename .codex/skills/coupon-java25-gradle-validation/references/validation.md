# Validation Matrix

This file mirrors the source of truth in [docs/agent/validation.md](../../../../docs/agent/validation.md).

## Java Check

- Prefer plain `./gradlew ...`
- If `./gradlew -version` shows a non-Java-25 launcher, rerun with `JAVA_HOME=$(/usr/libexec/java_home -v 25)`

## Format First

- `./gradlew :coupon:coupon-api:ktlintFormat`
- `./gradlew :coupon:coupon-domain:ktlintFormat`
- `./gradlew :coupon:coupon-worker:ktlintFormat`
- `./gradlew :storage:db-core:ktlintFormat`
- `./gradlew :storage:redis:ktlintFormat`
- `./gradlew :support:logging:ktlintFormat`
- `./gradlew :support:monitoring:ktlintFormat`
- `./gradlew ktlintFormat` for broad churn

## Smallest Proof Commands

- domain logic or repository contract change:
  `./gradlew :coupon:coupon-domain:test`
- JPA adapter change:
  `./gradlew :storage:db-core:compileKotlin`
- Redis adapter change:
  `./gradlew :storage:redis:compileKotlin`
- controller, filter, config, or DTO change:
  `./gradlew :coupon:coupon-api:compileKotlin`
- worker runtime or Kafka change:
  `./gradlew :coupon:coupon-worker:compileKotlin`
- cross-module or stronger proof:
  `./gradlew :coupon:coupon-api:build --no-daemon`
