---
name: blog-image-korean-copy-guard
description: Use when generating Korean technical blog infographics or diagrams that contain on-image text. Enforces short headline-style Korean labels, avoids sentence endings like 합니다/이다/했다, reduces mixed-language copy, and prefers publication-safe text that is less likely to render with broken Hangul, awkward spacing, or typo-like artifacts.
---

# Blog Image Korean Copy Guard

Use this skill when the user wants Korean text embedded inside generated images for a technical article, infographic, or slide-like diagram.

## Goal

Keep on-image copy short, front-loaded, and typographically safe.

## Default Rules

- Prefer noun phrases over sentences.
- Prefer two to six words per label.
- Avoid sentence endings such as `~다`, `~입니다`, `~했다`.
- Do not place long explanations inside the image if a caption outside the image can carry the meaning.
- Use Korean first. Keep English only for unavoidable technical terms such as `API`, `DLQ`, `Redis`, `Kafka`, `requestId`, `Idempotency-Key`.
- If English is required, keep it to one short token or one short phrase per text block.
- Avoid parentheses unless they are required for a technical term.
- Avoid slashes when a clearer separator exists.
- Prefer `성공`, `실패`, `전송 오류`, `상태 조회`, `사전 준비`, `운영 판단` 같은 짧은 두괄식 표현을 사용한다.

## Layout Rules

- Chip text must fit with visible left and right padding.
- Keep at least `16px` left and right inner padding for short labels.
- Keep at least `20px` left and right inner padding for mixed Korean-English labels.
- Treat only `70%~75%` of a chip width as usable text width; leave the rest as padding.
- If a label is long, widen the chip before shrinking the font.
- Do not let text touch chip borders.
- Prefer two-line labels over a single overlong line.
- For long technical tokens such as `Idempotency-Key`, split into multiple lines before allowing overflow.
- If a mixed-language token still feels tight after two lines, widen the chip again before lowering font size.
- Keep each chip vertically centered.
- Align chips to a clear grid; avoid uneven gaps that look accidental.
- Reserve explicit gutter space between chips when arrows sit between them.
- Arrows must live in the gutter, not on top of chips.
- Keep at least `40px` gutter between adjacent chips in wide desktop diagrams.
- For dense multi-step flows, prefer `48px~64px` gutter before shrinking card width.
- Decorative dots, icons, or badges must never overlap text.
- Do not use filler geometry with no semantic meaning.
- If an icon does not clearly communicate the concept, remove it instead of adding decoration.
- Prefer one simple semantic SVG icon over multiple abstract circles or bars.
- When a well-known brand or product icon exists, prefer the official or community-standard SVG mark over a hand-drawn substitute.
- Use external brand icons only when they improve recognition more than they add visual noise.
- If a chip contains both iconography and text, separate them by row or by left-right columns with padding.
- Secondary chips or summary strips must not intrude into neighboring sections.
- Each major section should have a visible safe zone around it.
- Arrows should read visually as `->`, not as a chevron-only `>-`.
- Prefer a straight line with a filled triangular arrowhead marker at the end.
- Do not use open chevron-style arrowheads.
- Keep arrow lines visually centered in the gutter with at least `10px` space from both neighboring chip borders.
- If the flow is curved, keep the final arrowhead explicit and directional.
- Decorative dots should keep at least `16px` distance from any text block.
- Section headers, cards, and summary strips should each keep their own vertical lane; do not stack them close enough to feel merged.

## Copy Compression

Rewrite long source text into one of these shapes before putting it into the image:

- title: `핵심 주제`
- label: `대상 + 동작`
- metric: `지표 + 값`
- conclusion strip: `핵심 해석 한 줄`

Examples:

- `이번 측정에서 문제는 DLQ보다 공개 진입 경로의 응답 안정성에 가까웠다`
  -> `문제는 DLQ보다 응답 경로`
- `전송 오류는 DLQ보다, 같은 요청을 식별하고 상태를 다시 설명하는 계약이 더 중요했다`
  -> `핵심은 재처리보다 상태 재확인`
- `응답을 놓쳐도 requestId로 다시 확인 가능`
  -> `requestId로 상태 재확인`

## Korean Safety Checklist

Before approving a prompt or regenerated image, check that:

- every Korean phrase still sounds natural when shortened
- spacing is standard Korean spacing
- there are no unnatural mixed forms like `확인가능`, `재실행됨`, `반환가능`
- long explanation text has been moved out of the image where possible
- the image can still be understood by scanning titles, labels, and one bottom summary strip
- no label is wider than its chip without enough padding
- no mixed-language label visually approaches the left or right border
- no chip row looks visually misaligned
- arrows read as `->` rather than `>-`
- decorative marks do not overlap labels
- summary chips stay within their own section boundaries

## Prompting Pattern

When drafting prompts for image generation:

- explicitly request `short Korean labels`
- explicitly request `headline-style text, not full sentences`
- explicitly request `minimal on-image copy`
- include the exact short labels you want rendered
- tell the model to move nuance into composition, color, grouping, and arrows rather than extra text
- explicitly request `chips wide enough for text with padding`
- explicitly request `straight arrows with clear arrowheads`
- explicitly request `dedicated gutter space for arrows`
- explicitly request `no overlapping icons, dots, or summary chips`

## Output Shape

When asked to review or rewrite image text, return:

1. `문제 텍스트`
2. `교정 텍스트`
3. `프롬프트 반영 규칙`
