# Dependency And Architecture Adoption Rubric

Use this flow when the question is "should we add this library, SDK, transport, or architecture pattern?"

## 1. Local Facts First

Inspect repo-local sources before making any claim:

- `settings.gradle.kts`
- nearest `build.gradle.kts`
- `gradle.properties`
- the nearest existing feature slice and adapter
- architecture docs under `docs/architecture/`

Confirm whether the repository already has:

- an equivalent dependency
- an existing adapter or integration style
- a nearby feature slice that should own the behavior

## 2. Official External Facts Second

Only when current vendor behavior matters, verify with first-party sources:

- official docs
- official SDK docs
- release notes
- artifact pages

Avoid blogs, forum posts, and secondary summaries unless the official source is unavailable. If the official source is unavailable, say the evidence is incomplete.

## 3. Repo-Fit Questions

Evaluate the option against the current repository:

- Does it fit the module boundary, or does it leak transport or vendor details into orchestration?
- Does it reduce code and operational cost compared with extending what already exists?
- Does it stay legible for future agents and reviewers?
- Is it compatible with the current Kotlin, Spring Boot, Java, and runtime setup?
- What is the smallest validation command that would prove the choice safely?

Default bias:

- prefer existing dependencies and existing adapter patterns
- prefer boring, explicit infrastructure over opaque magic
- keep SDK wiring in infra or config, not domain orchestration

## 4. Output Shape

Answer in this order:

1. `로컬 사실`
2. `외부 사실`
3. `권장안`
4. `미도입 이유`
5. `최소 검증`

If the repo does not need a new dependency, say so directly and explain what should be reused instead.
