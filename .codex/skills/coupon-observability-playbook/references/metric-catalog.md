# Metric Catalog

## High-Signal Business Metrics

- coupon issue attempts, success, duplicate rejection, sold-out rejection
- coupon use success and invalid state rejection
- coupon cancel success and restore-quantity count
- lock acquisition success, timeout, and duration
- cache hit, miss, and write count on hot read paths

## Operational Signals

- executor queue depth and rejection count
- SQL latency and slow query count once instrumentation exists
- Redis command latency once instrumentation exists
- retry count and fallback activation count once such policies exist

## Current Limits

- Prometheus exposure exists.
- Grafana, Loki, and Datadog dashboards are not wired in this repo yet.
