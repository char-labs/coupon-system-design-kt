---
name: coupon-technical-writing
description: Use when writing backend blog posts or technical articles grounded in this coupon system, especially question-driven prompts like 왜 outbox 패턴을 사용하는가?, Lua 스크립트는 왜 쓰는가?, 왜 Kafka 로 intake 와 execution 을 분리했는가?, 블로그 글 써줘, or 이 구조를 설명하는 기술 글 써줘. For actual article drafting, automatically fan out to planner -> drafter -> reviewer.
metadata:
  short-description: Auto-orchestrate reference-backed backend blog writing for the coupon system
---

# Coupon Technical Writing

Use this skill when the user wants backend blog topics, outlines, full drafts, or portfolio-ready
technical explanations based on this repository.

## Workflow

1. Read `./AGENTS.md` and [docs/agent/technical-writing-workflow.md](../../../docs/agent/technical-writing-workflow.md).
2. Read the current architecture contract first:
   - [docs/architecture/current-architecture-overview.md](../../../docs/architecture/current-architecture-overview.md)
   - [docs/architecture/coupon-issuance-runtime.md](../../../docs/architecture/coupon-issuance-runtime.md)
3. Load the matching reference files:
   - topic bank: [references/competency-map.md](./references/competency-map.md)
   - source rules: [references/source-policy.md](./references/source-policy.md)
   - topic-by-topic external map: [references/decision-reference-map.md](./references/decision-reference-map.md)
   - writing patterns: [references/blog-pattern-benchmarks.md](./references/blog-pattern-benchmarks.md)
   - visuals: [references/visual-playbook.md](./references/visual-playbook.md)
   - output shape: [references/article-template.md](./references/article-template.md)
   - review rules: [references/review-checklist.md](./references/review-checklist.md)
4. If the user asks for an actual article or draft, automatically fan out in this order:
   - `technical-writing-planner`
   - `technical-writing-drafter`
   - `technical-writing-reviewer`
5. Use planner output as the contract for drafter and reviewer.
6. Read only the nearest supporting runtime doc or code slice needed to defend the main claim.

## Rules

- Keep `current implementation facts` and `future TODO or evolution` separate.
- Do not describe `request table`, `relay`, `CDC`, or durable acceptance as currently implemented.
- `POST /coupon-issues` `SUCCESS` means `Redis reserve + Kafka broker ack`, not final DB persistence.
- Outbox currently handles lifecycle projection durability, not intake durability.
- Mention the worker component scan exception when it materially affects the story.
- Prefer repo-local facts and code anchors over generic backend theory.
- Collect references in this priority order:
  - local docs and code
  - official docs and vendor docs
  - pattern authorities
  - benchmark blogs for writing structure
- Use benchmark blogs for structure only, not for factual proof.
- For full drafts, always include `개요`, `초안`, `이미지 제안`, and `References`.
- `References` must list at least `Local References` and `External References`.
- Add `Writing Benchmarks` only when a benchmark article materially influenced the structure.
- When text alone is weak, insert an `이미지 제안:` block with the section, image type, why it helps, and a caption draft.
- Write in Korean unless the user explicitly asks for another language.

## Output Guidance

- For topic ideation:
  include `제목 후보 / 독자 / 한 줄 주장 / 비교할 대안 / 근거 문서 / 외부 reference pack`
- For a planner result:
  include `제목 후보 / 독자 / 한 줄 주장 / 비교할 대안 / 개요 / 섹션별 근거 / 이미지 제안 / references 초안`
- For a full draft:
  return `개요 / 초안 / 이미지 제안 / References`

## References

- Topic and question bank: [references/competency-map.md](./references/competency-map.md)
- Source policy: [references/source-policy.md](./references/source-policy.md)
- Decision reference map: [references/decision-reference-map.md](./references/decision-reference-map.md)
- Writing benchmarks: [references/blog-pattern-benchmarks.md](./references/blog-pattern-benchmarks.md)
- Visual playbook: [references/visual-playbook.md](./references/visual-playbook.md)
- Writing structures and section templates: [references/article-template.md](./references/article-template.md)
- Reviewer checklist: [references/review-checklist.md](./references/review-checklist.md)
