---
name: coupon-technical-writing
description: Use when writing backend blog posts or technical articles grounded in this coupon system, especially question-driven prompts like 왜 outbox 패턴을 사용하는가?, Lua 스크립트는 왜 쓰는가?, 왜 Kafka 로 intake 와 execution 을 분리했는가?, 블로그 글 써줘, or 이 구조를 설명하는 기술 글 써줘. For actual article drafting, automatically fan out to planner -> drafter -> reviewer with a bounded revision loop.
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
   - author voice: [references/author-style-profile.md](./references/author-style-profile.md)
   - visuals: [references/visual-playbook.md](./references/visual-playbook.md)
   - output shape: [references/article-template.md](./references/article-template.md)
   - review rules: [references/review-checklist.md](./references/review-checklist.md)
4. If the user asks for an actual article or draft, automatically fan out in this order:
   - `technical-writing-planner`
   - `technical-writing-drafter`
   - `technical-writing-reviewer`
5. If reviewer finds material issues, run one bounded refinement loop:
   - send the reviewer delta back to `technical-writing-drafter`
   - rerun `technical-writing-reviewer` for confirmation
   - cap this loop at one extra redraft unless the user explicitly asks for more iteration
6. Use planner output as the contract for drafter and reviewer.
7. Read only the nearest supporting runtime doc or code slice needed to defend the main claim.

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
- For full drafts, always include `개요`, `초안`, `이미지 제안`, and `근거 팩`.
- `초안` is the publishable MDX by default.
- publishable MDX drafts should include frontmatter by default:
  - `title`
  - `description`
  - `date`
  - `tags`
- When the article benefits from a representative cover image, place it immediately after frontmatter and before the first section.
- If no approved image URL or asset exists yet, keep the cover image in `이미지 제안` instead of inventing one.
- Never expose a raw `Local References` section unless the user explicitly asks for an internal memo format.
- Keep repo-local grounding in code blocks, `코드 컨텍스트` blocks, short code excerpts, and inline flow explanations.
- Keep `근거 팩` focused on `External References` and optional `Writing Benchmarks`, not repo file listings.
- Translate repo-local grounding into one of these publishable forms:
  - short code excerpts
  - `코드 컨텍스트` blocks
  - inline file or flow explanations
  - section-level captions or notes tied to the current code path
- Add `Writing Benchmarks` only when a benchmark article materially influenced the structure.
- Default to omitting `Writing Benchmarks`.
- Never include a benchmark link merely because it was loaded or mentioned during planning.
- If a benchmark article's topic is unrelated to the current article, omit it unless the structural borrowing is explicit and visible in the draft.
- When `Writing Benchmarks` is included, every item must state the specific structural reason it appears, such as `목차 선제시`, `비교표 구성`, or `최종 결정 섹션`.
- If you cannot explain the benchmark's concrete influence in one short phrase, remove it from the final output and keep it out of the `근거 팩`.
- Blend the user's own writing pattern lightly:
  - warm first-person framing
  - practical pain-point focus
  - one or two short analogies when they improve readability
  - no forced humor or excessive metaphors
- Prefer direct-experience first-person phrasing such as `이번 프로젝트에서 제가...`, `구현하면서 제가...`, `제가 중요하게 봤던 포인트는...`.
- Avoid meta audience-framing phrases like `백엔드 개발자 관점에서`, `독자 관점에서` unless the contrast itself is the point.
- For MDX posts on velog-style platforms, inline HTML is allowed when it materially improves publishability:
  - hero image block such as `<img ... width="100%" />`
  - anchored citations such as `<a href="#ref-e1">[E1]</a>`
  - matching reference ids in the final `References` section
- Default to polished Korean `합니다체` for article prose.
- Avoid `한다체` narration unless it is a quote, label, code-adjacent explanation, or compact note.
- Prefer titles that make the tech topic and scope obvious:
  - concrete tech name + `사용기` or `과정`
  - short hook + comma + topic + `에 대해`
  - a focused question when the article is question-driven
- Do not optimize for brevity alone.
- Keep the core message clear, but expand sections that need detail:
  - tricky runtime boundaries
  - tradeoff comparisons
  - failure handling and retries
  - why other options were rejected
  - code or architecture behavior that readers can easily misunderstand
- Short paragraphs are good, but the article can be long when the topic genuinely needs more context.
- When text alone is weak, insert an `이미지 제안:` block with the section, image type, why it helps, and a caption draft.
- In Mermaid diagrams, default to reader-friendly Korean semantic labels.
- Do not use raw command names such as `EXPIRE occupied-count`, `SADD usersKey userId`, or `SISMEMBER ...` as node labels unless the exact command itself is the teaching point.
- Keep low-level command names in code blocks, tables, or surrounding explanation, and let the diagram communicate business meaning first.
- When the topic is Redis Lua in this repository, explain `reserve`, `release`, and `rebuild` as concrete functions:
  - when each function is called
  - what state each function changes
  - why the three functions should be understood as one state-management set
- For Redis Lua posts shaped like the current publish pattern, prefer this section flow unless the topic strongly needs another order:
  - `서론`
  - `왜 이 글을 쓰게 되었는가`
  - `Lua 스크립트는 무엇인가`
  - `구조 고정`
  - `reserve / release / rebuild`
  - `왜 Redis 기본 명령 조합만으로는 부족했는가`
  - `WATCH/MULTI`, 파이프라이닝, 락 비교
  - `현재 프로젝트에서는 Lua를 어떻게 사용했는가`
  - `실패 처리와 상태 복구`
  - `헷갈리기 쉬운 지점`
  - `최종 결정`
  - `결론`
- Write in Korean unless the user explicitly asks for another language.

## Output Guidance

- For topic ideation:
  include `제목 후보 / 독자 / 한 줄 주장 / 비교할 대안 / 근거 문서 / 외부 reference pack`
- For a planner result:
  include `제목 후보 / 독자 / 한 줄 주장 / 비교할 대안 / 개요 / 섹션별 근거 / 이미지 제안 / 근거 팩 초안 / publish 메모 / frontmatter 메모 / citation 방식`
- For a full draft:
  return `개요 / 초안 / 이미지 제안 / 근거 팩`

## References

- Topic and question bank: [references/competency-map.md](./references/competency-map.md)
- Source policy: [references/source-policy.md](./references/source-policy.md)
- Decision reference map: [references/decision-reference-map.md](./references/decision-reference-map.md)
- Writing benchmarks: [references/blog-pattern-benchmarks.md](./references/blog-pattern-benchmarks.md)
- Author style profile: [references/author-style-profile.md](./references/author-style-profile.md)
- Visual playbook: [references/visual-playbook.md](./references/visual-playbook.md)
- Writing structures and section templates: [references/article-template.md](./references/article-template.md)
- Reviewer checklist: [references/review-checklist.md](./references/review-checklist.md)
