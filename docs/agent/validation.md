# Agent Validation Map

Prefer plain `./gradlew ...` commands. The root build already targets Java 25, so a local environment with a Java 25 Gradle launcher does not need `JAVA_HOME=...` on every command.

## Java Check

1. Run `./gradlew -version` when the environment is unclear.
2. If the Gradle launcher JVM is Java 25, use plain `./gradlew`.
3. If the launcher JVM is not Java 25, rerun the same command with `JAVA_HOME=$(/usr/libexec/java_home -v 25)`.

## Format First

- `./gradlew :coupon:coupon-api:ktlintFormat`
- `./gradlew :coupon:coupon-domain:ktlintFormat`
- `./gradlew :coupon:coupon-worker:ktlintFormat`
- `./gradlew :storage:db-core:ktlintFormat`
- `./gradlew :storage:redis:ktlintFormat`
- `./gradlew :support:logging:ktlintFormat`
- `./gradlew :support:monitoring:ktlintFormat`
- `./gradlew ktlintFormat`

## Smallest Proof Commands

- `coupon-domain` logic or repository contract change:
  `./gradlew :coupon:coupon-domain:test`
- `coupon-api` controller, DTO, filter, config, or wiring change:
  `./gradlew :coupon:coupon-api:compileKotlin`
- `coupon-worker` Kafka, outbox, or scheduling change:
  `./gradlew :coupon:coupon-worker:compileKotlin`
- `coupon-worker` runtime behavior with stronger proof:
  `./gradlew :coupon:coupon-worker:test --no-daemon`
- `storage:db-core` JPA adapter change:
  `./gradlew :storage:db-core:compileKotlin`
- `storage:redis` adapter, lock, or Lua script change:
  `./gradlew :storage:redis:compileKotlin`
- `support:logging` or `support:monitoring` module change:
  `./gradlew :support:logging:compileKotlin`
  `./gradlew :support:monitoring:compileKotlin`
- Cross-module or higher-confidence proof:
  `./gradlew :coupon:coupon-api:build --no-daemon`

## Selection Rule

Choose the smallest command that exercises the changed behavior. Only escalate to the broader API build when module-local proof is not enough.
