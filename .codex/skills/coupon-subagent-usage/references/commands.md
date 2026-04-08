# Agent Routing

- `coupon-specialist-orchestration`
  - use as the default entrypoint for non-trivial tasks that need specialist fan-out
  - benefit: chooses the smallest useful set of specialists, then synthesizes one implementation path

- `feature_mapper`
  - use before implementation when scope is still unclear
  - benefit: finds affected modules and smallest validation scope quickly
- `code_reviewer`
  - use before merge for correctness and regression risk
  - benefit: focuses on real behavior changes first
- `performance_reviewer`
  - use when the user asks about N+1, slow query risk, or bottlenecks
  - benefit: separates operational bottleneck review from general correctness review
- `storage_reliability_specialist`
  - use when `db-core` or `redis` changes dominate
  - benefit: catches repository, cache, lock, and transaction drift
- `observability_guard`
  - use when adding new flows or when production debuggability matters
  - benefit: catches missing metrics, logs, and trace context
- `tech_adoption_advisor`
  - use before adding a dependency, SDK, or new integration style
  - benefit: separates local repo facts from official external facts and returns a concrete recommendation
- `ci_triager`
  - use for failing Gradle, JDK, lint, compile, or test signals
  - benefit: narrows down the smallest failing proof command
- `commit_push_guard`
  - use before mutating git history
  - benefit: keeps commit grouping, formatting, and validation honest
