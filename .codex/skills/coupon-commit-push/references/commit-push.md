# Commit Push Reference

## Format Before Validation

- `./gradlew :coupon:coupon-api:ktlintFormat`
- `./gradlew :coupon:coupon-domain:ktlintFormat`
- `./gradlew :storage:db-core:ktlintFormat`
- `./gradlew :storage:redis:ktlintFormat`
- `./gradlew ktlintFormat` when churn spans many modules

## Validation Mapping

- domain logic or repository contract changes:
  `./gradlew :coupon:coupon-domain:test`
- `db-core` adapter changes:
  `./gradlew :storage:db-core:compileKotlin`
- `redis` adapter changes:
  `./gradlew :storage:redis:compileKotlin`
- controller, DTO, filter, or config changes:
  `./gradlew :coupon:coupon-api:compileKotlin`
- worker runtime changes:
  `./gradlew :coupon:coupon-worker:compileKotlin`
- cross-module or higher confidence:
  `./gradlew :coupon:coupon-api:build --no-daemon`

If `./gradlew -version` shows a non-Java-25 launcher, rerun the same command with `JAVA_HOME=$(/usr/libexec/java_home -v 25)`.

## Commit Grouping Hints

- Keep one independently reviewable vertical feature slice in one commit.
- Split formatting churn away from behavior changes when it obscures review.
- Split infra, docs, and tests away from product behavior when they can stand alone.
- Use English commit messages in the `[Topic] Issue summary` format.
- Prefer `Feature`, `Fix`, `Refactor`, `Test`, `Docs`, and `Chore` as topic labels.

## Prompt Templates

- Skill-based planning:
  `codex exec -C . 'Use $coupon-commit-push to propose a safe commit and push sequence for the current branch. Do not mutate git yet.'`
- Agent-based planning:
  `codex exec -C . "Spawn commit_push_guard to inspect current changes, propose safe commit groups, list ktlintFormat steps, and choose Java 25 validation commands before push. Do not mutate git. Respond in Korean."`
