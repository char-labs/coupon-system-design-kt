---
name: coupon-observability-playbook
description: Use when adding or reviewing logs, metrics, traces, Prometheus exposure, Grafana or Loki readiness, or optional Datadog integration for the coupon system. Trigger on prompts about 로그, 메트릭, tracing, 프로메테우스, Grafana, Loki, Datadog, or 관측성.
metadata:
  short-description: Add or review logs, metrics, and traces for coupon flows
---

# Coupon Observability Playbook

Use this skill when a change needs to be measurable and debuggable in production.

## Workflow

1. Read `./AGENTS.md`.
2. Read [metric-catalog.md](./references/metric-catalog.md).
3. Identify the business flow and failure modes.
4. If alerting or vendor integration is involved, follow [docs/agent/adoption-rubric.md](../../../docs/agent/adoption-rubric.md) before choosing a new SDK or transport style.
5. Add or review:
   - structured logs
   - trace or span tags
   - Prometheus counters, timers, and gauges
   - async backlog or executor pressure signals
6. If Grafana, Loki, Datadog, Slack, or similar setup is missing, capture that as TODO rather than assuming dashboards or SDKs already exist.

## Rules

- Prefer business metrics for coupon issue, use, cancel, lock failure, retry, and cache hit or miss patterns.
- Do not over-instrument every internal method. Focus on externally visible flow boundaries and shared bottlenecks.
- Distinguish current Prometheus support from future dashboarding or alerting work.
- For pure vendor or SDK adoption questions, prefer `$coupon-tech-adoption-review`.
