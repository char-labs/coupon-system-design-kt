---
name: coupon-java25-gradle-validation
description: Use when validating or triaging build, test, lint, or JDK issues in this coupon system. Covers Java 25, Gradle module selection, ktlintFormat before validation, and the smallest useful proof command for each module. Trigger on prompts about 빌드, 테스트, ktlint, lint, compile, validation, or Java 25.
metadata:
  short-description: Validate coupon-system changes with Java 25 and Gradle
---

# Coupon Java 25 Gradle Validation

Use this skill when you need the smallest validation command that proves a change is safe.

## Workflow

1. Read `./AGENTS.md`.
2. Confirm Java 25.
3. Run the narrowest `ktlintFormat` task that matches the changed modules.
4. Choose the smallest validation command that exercises the changed behavior.
5. Escalate to the broader API build only when module-local checks are insufficient.

## Commands

- `JAVA_HOME=$(/usr/libexec/java_home -v 25) ./gradlew :coupon:coupon-domain:test`
- `JAVA_HOME=$(/usr/libexec/java_home -v 25) ./gradlew :storage:db-core:compileKotlin`
- `JAVA_HOME=$(/usr/libexec/java_home -v 25) ./gradlew :storage:redis:compileKotlin`
- `JAVA_HOME=$(/usr/libexec/java_home -v 25) ./gradlew :coupon:coupon-api:compileKotlin`
- `JAVA_HOME=$(/usr/libexec/java_home -v 25) ./gradlew :coupon:coupon-api:build --no-daemon`

## Resources

- [validation.md](./references/validation.md)
