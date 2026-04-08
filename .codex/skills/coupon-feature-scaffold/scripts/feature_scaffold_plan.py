#!/usr/bin/env python3
"""Print a file checklist for a new feature in this coupon-system project."""

from __future__ import annotations

import argparse
from dataclasses import dataclass


def to_pascal_case(raw: str) -> str:
    parts = [part for part in raw.replace("_", "-").split("-") if part]
    return "".join(part[:1].upper() + part[1:] for part in parts)


@dataclass(frozen=True)
class FeatureNames:
    slug: str
    package: str
    pascal: str


def build_names(raw: str) -> FeatureNames:
    slug = raw.strip().lower().replace("_", "-")
    package = slug.replace("-", "")
    pascal = to_pascal_case(slug)
    return FeatureNames(slug=slug, package=package, pascal=pascal)


def crud_paths(names: FeatureNames) -> list[str]:
    package = names.package
    pascal = names.pascal
    return [
        f"coupon/coupon-api/src/main/kotlin/com.coupon/controller/{package}/{pascal}Controller.kt",
        f"coupon/coupon-api/src/main/kotlin/com.coupon/controller/{package}/request/{pascal}Request.kt",
        f"coupon/coupon-api/src/main/kotlin/com.coupon/controller/{package}/response/{pascal}Response.kt",
        f"coupon/coupon-api/src/main/kotlin/com.coupon/controller/{package}/response/{pascal}PageResponse.kt",
        f"coupon/coupon-domain/src/main/kotlin/com/coupon/{package}/{pascal}.kt",
        f"coupon/coupon-domain/src/main/kotlin/com/coupon/{package}/{pascal}Detail.kt",
        f"coupon/coupon-domain/src/main/kotlin/com/coupon/{package}/{pascal}Service.kt",
        f"coupon/coupon-domain/src/main/kotlin/com/coupon/{package}/{pascal}Repository.kt",
        f"coupon/coupon-domain/src/main/kotlin/com/coupon/{package}/command/{pascal}Command.kt",
        f"coupon/coupon-domain/src/main/kotlin/com/coupon/{package}/criteria/{pascal}Criteria.kt",
        f"storage/db-core/src/main/kotlin/com/coupon/storage/rdb/{package}/{pascal}Entity.kt",
        f"storage/db-core/src/main/kotlin/com/coupon/storage/rdb/{package}/{pascal}JpaRepository.kt",
        f"storage/db-core/src/main/kotlin/com/coupon/storage/rdb/{package}/{pascal}CoreRepository.kt",
    ]


def stateful_additions(names: FeatureNames) -> list[str]:
    package = names.package
    pascal = names.pascal
    return [
        f"coupon/coupon-domain/src/main/kotlin/com/coupon/{package}/{pascal}Status.kt or coupon/coupon-enum/src/main/kotlin/com/coupon/enums/{pascal}Status.kt",
        f"coupon/coupon-domain/src/main/kotlin/com/coupon/{package}/{pascal}Issue.kt",
        f"coupon/coupon-domain/src/main/kotlin/com/coupon/{package}/{pascal}IssueDetail.kt",
        f"coupon/coupon-domain/src/main/kotlin/com/coupon/{package}/{pascal}IssueService.kt",
        f"coupon/coupon-domain/src/main/kotlin/com/coupon/{package}/{pascal}IssueRepository.kt",
        f"coupon/coupon-domain/src/main/kotlin/com/coupon/{package}/command/{pascal}IssueCommand.kt",
        f"coupon/coupon-domain/src/main/kotlin/com/coupon/{package}/criteria/{pascal}IssueCriteria.kt",
        f"coupon/coupon-api/src/main/kotlin/com.coupon/controller/{package}/{pascal}IssueController.kt",
        f"coupon/coupon-api/src/main/kotlin/com.coupon/controller/{package}/request/{pascal}IssueRequest.kt",
        f"coupon/coupon-api/src/main/kotlin/com.coupon/controller/{package}/response/{pascal}IssueResponse.kt",
        f"coupon/coupon-api/src/main/kotlin/com.coupon/controller/{package}/response/{pascal}IssuePageResponse.kt",
        f"storage/db-core/src/main/kotlin/com/coupon/storage/rdb/{package}/{pascal}IssueEntity.kt",
        f"storage/db-core/src/main/kotlin/com/coupon/storage/rdb/{package}/{pascal}IssueJpaRepository.kt",
        f"storage/db-core/src/main/kotlin/com/coupon/storage/rdb/{package}/{pascal}IssueCoreRepository.kt",
        "Lock and cache invalidation review point",
        "Metric and trace tag review point",
    ]


def auth_adjacent_paths(names: FeatureNames) -> list[str]:
    package = names.package
    pascal = names.pascal
    return [
        f"coupon/coupon-api/src/main/kotlin/com.coupon/controller/{package}/{pascal}Controller.kt or an existing auth-related controller",
        f"coupon/coupon-api/src/main/kotlin/com.coupon/controller/{package}/request/{pascal}Request.kt",
        f"coupon/coupon-api/src/main/kotlin/com.coupon/controller/{package}/response/{pascal}Response.kt",
        f"coupon/coupon-domain/src/main/kotlin/com/coupon/{package}/{pascal}Service.kt or Facade.kt",
        f"coupon/coupon-domain/src/main/kotlin/com/coupon/{package}/command/{pascal}Command.kt",
        "coupon/coupon-api/src/main/kotlin/com.coupon/config/SecurityConfig.kt if access rules change",
        "coupon/coupon-api/src/main/kotlin/com.coupon/filter/JwtAuthenticationFilter.kt if authentication extraction changes",
    ]


def main() -> None:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("feature_name", help="Feature name, for example reward or point-balance")
    parser.add_argument(
        "--kind",
        choices=("crud", "stateful", "auth-adjacent"),
        default="crud",
        help="Architecture pattern to mirror",
    )
    args = parser.parse_args()

    names = build_names(args.feature_name)

    print(f"# Scaffold plan for {names.pascal}")
    print()
    print("## Names")
    print(f"- feature slug: `{names.slug}`")
    print(f"- package segment: `{names.package}`")
    print(f"- class prefix: `{names.pascal}`")
    print()
    print("## Files")

    paths = crud_paths(names)
    if args.kind == "stateful":
        paths += stateful_additions(names)
    elif args.kind == "auth-adjacent":
        paths = auth_adjacent_paths(names)

    for path in paths:
        print(f"- {path}")

    print()
    print("## Reminders")
    print("- Keep API files under the physical `com.coupon/...` path convention.")
    print("- Keep business orchestration in services and storage adapters thin.")
    print("- Wrap mutable service boundaries with `@Transactional`; add a dedicated `REQUIRES_NEW` transaction runner only when propagation control is required.")
    print("- Decide explicitly whether the flow needs lock, cache invalidation, or extra metrics.")
    print("- Reuse nearby features before introducing new abstractions.")


if __name__ == "__main__":
    main()
