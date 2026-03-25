# Validation Matrix

## Format First

- `./gradlew :coupon:coupon-api:ktlintFormat`
- `./gradlew :coupon:coupon-domain:ktlintFormat`
- `./gradlew :storage:db-core:ktlintFormat`
- `./gradlew :storage:redis:ktlintFormat`
- `./gradlew ktlintFormat` for broad churn

## Smallest Proof Commands

- domain logic or repository contract change:
  `JAVA_HOME=$(/usr/libexec/java_home -v 25) ./gradlew :coupon:coupon-domain:test`
- JPA adapter change:
  `JAVA_HOME=$(/usr/libexec/java_home -v 25) ./gradlew :storage:db-core:compileKotlin`
- Redis adapter change:
  `JAVA_HOME=$(/usr/libexec/java_home -v 25) ./gradlew :storage:redis:compileKotlin`
- controller, filter, config, or DTO change:
  `JAVA_HOME=$(/usr/libexec/java_home -v 25) ./gradlew :coupon:coupon-api:compileKotlin`
- cross-module or stronger proof:
  `JAVA_HOME=$(/usr/libexec/java_home -v 25) ./gradlew :coupon:coupon-api:build --no-daemon`
