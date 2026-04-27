---
name: blog-image-qa-loop
description: Use when reviewing generated infographic images for Korean technical blog posts. Checks for broken Hangul, awkward spacing, typo-like copy, overlong text blocks, mixed-language clutter, clipped labels, and mismatches between source metrics and the rendered image. Produces a concise regenerate-or-approve decision plus corrected labels and revised prompt guidance.
---

# Blog Image QA Loop

Use this skill after images are generated and before they are delivered to the user.

## Goal

Catch visual or copy problems that are common in generated Korean infographics, then turn them into a short regenerate plan.

## Review Order

### 1. Text fidelity

Check whether rendered text matches the intended facts:

- titles
- scenario names
- metric values
- success or failure labels
- summary strip meaning

### 2. Korean quality

Check for:

- broken or unnatural Hangul
- wrong spacing
- typo-like wording
- awkward mixed Korean and English
- sentence-like text that should have been compressed

### 3. Layout safety

Check for:

- text clipping
- cramped boxes
- too many words in one block
- tiny footnote-like copy
- speech bubbles with full sentences
- chips whose text visually spills or feels too tight
- labels that technically fit but do not preserve enough left-right padding
- mixed Korean-English labels whose longest line uses too much of the chip width
- rows or columns that are not aligned to a stable grid
- arrows that look like `>-` instead of `->`
- arrowheads that are open chevrons instead of filled directional heads
- arrows drawn on top of chips instead of between chips
- gutters between chips that are too narrow for the arrow to read cleanly
- decorative dots or icons that collide with labels
- decorative shapes that do not add meaning and only fill space
- hand-drawn substitute icons where a clearer standard brand icon would be easier to recognize
- a chip or summary strip that intrudes into a neighboring section
- summary strips or helper chips that visually merge with another section because the safe zone is too small

### 4. Publication quality

Check whether the image still reads well when embedded in a blog at normal width.

If a reader cannot understand the image by scanning:

- title
- major labels
- one short bottom summary

then the image needs regeneration.

## Regeneration Rules

When issues exist:

- reduce text before changing style
- replace sentences with labels
- replace explanation blocks with one short summary strip
- keep numbers exact
- keep conclusions short and front-loaded

## Decision Format

Return results in this shape:

- `판정`: 승인 or 재생성
- `문제`: flat bullet list
- `교정 문구`: old -> new
- `재생성 지시`: prompt constraints for the next run

## Hard Stops

Always request regeneration if any of these happen:

- important metric value is wrong
- Korean spacing is visibly wrong
- a text block reads like a broken sentence
- conclusion copy is too long for the box
- labels are clipped or obviously crowded
- icons feel decorative rather than semantic
- chip text has insufficient padding
- mixed-language labels are not split early enough to preserve padding
- arrows are visually ambiguous
- arrows do not have enough gutter or feel attached to chip borders
- section boundaries are visually violated
