---
name: coupon-commit-push
description: Use when committing or pushing coupon-system changes. Plans safe commit groups, enforces `ktlintFormat` before validation, chooses Java 25 validation commands, requires explicit confirmation, and helps with English `[Topic] Issue summary` commit messages. Trigger on prompts like 커밋해줘, 푸시 준비해줘, commit-push, or 커밋 메시지랑 검증 커맨드 잡아줘.
metadata:
  short-description: Safe coupon-system commit and push workflow
---

# Coupon Commit Push

Use this skill when the user asks to commit, push, or do a combined commit-push flow.

## Workflow

1. Read `./AGENTS.md`.
2. Inspect `git status` and split unrelated changes into the smallest safe commit groups.
3. Run the narrowest matching `ktlintFormat` task before validation.
4. For each group, choose the smallest Java 25 validation command that proves the change.
5. Propose the ordered commit and push plan first using English `[Topic] Issue summary` commit messages.
6. Wait for the explicit approval token `확인` before mutating git state.
7. After approval, stage one group at a time, commit, restage if formatting changed files, then push once at the end.

## Notes

- Prefer topic labels such as `[Feature]`, `[Fix]`, `[Refactor]`, `[Test]`, `[Docs]`, and `[Chore]`.
- Always write coupon-system commit messages in English.
- Do not revert unrelated user changes.
- Read [commit-push.md](./references/commit-push.md) for validation mapping, formatting rules, and prompt templates.
