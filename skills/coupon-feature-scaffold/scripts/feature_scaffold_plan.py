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
    p = names.package
    c = names.pascal
    return [
        f"coupon/coupon-api/src/main/kotlin/com.coupon/controller/{p}/{c}Controller.kt",
        f"coupon/coupon-api/src/main/kotlin/com.coupon/controller/{p}/request/{c}Request.kt",
        f"coupon/coupon-api/src/main/kotlin/com.coupon/controller/{p}/response/{c}Response.kt",
        f"coupon/coupon-api/src/main/kotlin/com.coupon/controller/{p}/response/{c}PageResponse.kt",
        f"coupon/coupon-domain/src/main/kotlin/com/coupon/{p}/{c}.kt",
        f"coupon/coupon-domain/src/main/kotlin/com/coupon/{p}/{c}Detail.kt",
        f"coupon/coupon-domain/src/main/kotlin/com/coupon/{p}/{c}Service.kt",
        f"coupon/coupon-domain/src/main/kotlin/com/coupon/{p}/{c}Repository.kt",
        f"coupon/coupon-domain/src/main/kotlin/com/coupon/{p}/command/{c}Command.kt",
        f"coupon/coupon-domain/src/main/kotlin/com/coupon/{p}/criteria/{c}Criteria.kt",
        f"storage/db-core/src/main/kotlin/com/coupon/storage/rdb/{p}/{c}Entity.kt",
        f"storage/db-core/src/main/kotlin/com/coupon/storage/rdb/{p}/{c}JpaRepository.kt",
        f"storage/db-core/src/main/kotlin/com/coupon/storage/rdb/{p}/{c}CoreRepository.kt",
    ]


def stateful_additions(names: FeatureNames) -> list[str]:
    p = names.package
    c = names.pascal
    return [
        f"coupon/coupon-domain/src/main/kotlin/com/coupon/{p}/{c}Status.kt or coupon/coupon-enum/src/main/kotlin/com/coupon/enums/{c}Status.kt",
        f"coupon/coupon-domain/src/main/kotlin/com/coupon/{p}/{c}Issue.kt",
        f"coupon/coupon-domain/src/main/kotlin/com/coupon/{p}/{c}IssueDetail.kt",
        f"coupon/coupon-domain/src/main/kotlin/com/coupon/{p}/{c}IssueService.kt",
        f"coupon/coupon-domain/src/main/kotlin/com/coupon/{p}/{c}IssueRepository.kt",
        f"coupon/coupon-domain/src/main/kotlin/com/coupon/{p}/command/{c}IssueCommand.kt",
        f"coupon/coupon-domain/src/main/kotlin/com/coupon/{p}/criteria/{c}IssueCriteria.kt",
        f"coupon/coupon-api/src/main/kotlin/com.coupon/controller/{p}/{c}IssueController.kt",
        f"coupon/coupon-api/src/main/kotlin/com.coupon/controller/{p}/request/{c}IssueRequest.kt",
        f"coupon/coupon-api/src/main/kotlin/com.coupon/controller/{p}/response/{c}IssueResponse.kt",
        f"coupon/coupon-api/src/main/kotlin/com.coupon/controller/{p}/response/{c}IssuePageResponse.kt",
        f"storage/db-core/src/main/kotlin/com/coupon/storage/rdb/{p}/{c}IssueEntity.kt",
        f"storage/db-core/src/main/kotlin/com/coupon/storage/rdb/{p}/{c}IssueJpaRepository.kt",
        f"storage/db-core/src/main/kotlin/com/coupon/storage/rdb/{p}/{c}IssueCoreRepository.kt",
    ]


def auth_adjacent_paths(names: FeatureNames) -> list[str]:
    p = names.package
    c = names.pascal
    return [
        f"coupon/coupon-api/src/main/kotlin/com.coupon/controller/{p}/{c}Controller.kt or existing auth-related controller",
        f"coupon/coupon-api/src/main/kotlin/com.coupon/controller/{p}/request/{c}Request.kt",
        f"coupon/coupon-api/src/main/kotlin/com.coupon/controller/{p}/response/{c}Response.kt",
        f"coupon/coupon-domain/src/main/kotlin/com/coupon/{p}/{c}Service.kt or Facade.kt",
        f"coupon/coupon-domain/src/main/kotlin/com/coupon/{p}/command/{c}Command.kt",
        "coupon/coupon-api/src/main/kotlin/com.coupon/config/SecurityConfig.kt if access rules change",
        "coupon/coupon-api/src/main/kotlin/com.coupon/filter/JwtAuthenticationFilter.kt if authentication extraction changes",
    ]


def main() -> None:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("feature_name", help="Feature name, e.g. reward or point-balance")
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
    print("- Wrap mutable service flows with `Tx.writeable {}`.")
    print("- Reuse nearby features before introducing new abstractions.")


if __name__ == "__main__":
    main()
