---
name: coupon-tech-adoption-review
description: Use when evaluating whether to add a dependency, SDK, integration style, or architecture pattern to this coupon system. Trigger on prompts like 이 라이브러리 도입할까?, 이 의존성/SDK 지금 추가하는 게 맞아?, 이 구조가 맞아?, or 이 통합 방식이 적절해?
metadata:
  short-description: Review dependency and architecture adoption choices for the coupon system
---

# Coupon Tech Adoption Review

Use this skill when the main question is whether a new library, SDK, vendor integration, or architecture direction is the right fit.

## Workflow

1. Read `./AGENTS.md` and [docs/agent/adoption-rubric.md](../../../docs/agent/adoption-rubric.md).
2. Inspect `settings.gradle.kts`, `gradle.properties`, the nearest `build.gradle.kts`, and the nearest existing slice or adapter.
3. Confirm whether the repository already has an equivalent dependency or integration style.
4. If current vendor behavior matters, verify it with official external references only.
5. Evaluate repo fit: boundary ownership, operational cost, agent legibility, Kotlin or Spring compatibility, and smallest validation scope.
6. Answer in this order: `로컬 사실 / 외부 사실 / 권장안 / 미도입 이유 / 최소 검증`.

## Rules

- Do not claim a library is absent until local build files are checked.
- Prefer extending an existing dependency or adapter before adding a new one.
- Keep SDK wiring in infra-facing adapters or config, not domain orchestration.
- Use first-party documentation, SDK docs, release notes, or artifact pages as external sources.
- If the official source is missing or ambiguous, say the evidence is incomplete instead of guessing.
