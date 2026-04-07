# Kafka Broker Topology Evolution

## 목적

이 문서는 현재 쿠폰 시스템의 Kafka 구성이 어디까지 구현되어 있고, 이후 운영형 브로커 토폴로지로 어떻게 확장할지 정리한 계획서다.

핵심 전제는 다음과 같다.

- 현재 시스템의 source of truth는 여전히 MySQL이다.
- Kafka는 request 실행 버스이며, DB request 상태를 대체하지 않는다.
- 새로운 Kafka 확장은 `ZooKeeper 회귀`가 아니라 `KRaft 기반 확장`을 기본값으로 본다.

## 현재 상태

현재 로컬 구성은 [`docker-compose.yml`](/Users/yunbeom/ybcha/coupon-system-design-kt/docker/docker-compose.yml) 기준 단일 브로커다.

| 항목 | 현재 값 | 의미 |
| --- | --- | --- |
| broker count | 1 | 장애 허용 없음 |
| controller mode | KRaft | ZooKeeper 없음 |
| replication factor | 1 | leader 손실 시 복제본 승격 불가 |
| topic partitions | 3 | request consumer concurrency와 동일 |
| transport security | PLAINTEXT | 로컬 개발 전용 |
| topic provisioning | Spring `NewTopic` | 앱 시작 시 자동 생성 |

## 결론: 확장되어도 ZooKeeper가 필수는 아니다

새로 확장하는 Kafka라면 ZooKeeper를 도입해야 하는 것은 아니다.

오히려 기본 판단은 다음이 맞다.

- 새 운영 토폴로지: KRaft 우선
- 기존 운영 표준이 ZooKeeper 기반인 경우만 예외적으로 유지 또는 마이그레이션 고려

이 저장소의 다음 목표는 `쿠폰 시스템 정합성 + 운영성`이지, 과거 메타데이터 아키텍처를 재현하는 것이 아니다. 따라서 운영형 브로커 설계도 KRaft 기준으로 정리한다.

## 왜 KRaft를 기본값으로 보는가

- 최신 Kafka 운영 방향이 ZooKeeper 외부 의존성 제거에 맞춰져 있다.
- broker/controller 구성이 한 기술 스택 안에서 정리돼 토폴로지가 단순하다.
- 새로운 클러스터를 설계할 때 ZooKeeper 운영 지식과 장애 포인트를 추가로 끌고 오지 않아도 된다.
- 현재 쿠폰 시스템은 Kafka를 event sourcing 저장소가 아니라 command bus로 사용하므로, 운영 단순성이 우선이다.

## 언제 ZooKeeper를 다시 고려할 수 있는가

아래 경우가 아니라면 새로 ZooKeeper를 도입하지 않는다.

- 회사 공용 Kafka 플랫폼이 아직 ZooKeeper 기반이고, 애플리케이션이 그 공용 플랫폼에 반드시 올라가야 할 때
- 플랫폼 팀이 KRaft 운영 표준을 아직 승인하지 않았을 때
- 기존 multi-tenant Kafka 운영 절차, 보안, 관측, 백업 체계가 ZooKeeper 기반에만 맞춰져 있을 때

즉, 이건 기술적으로 더 좋아서가 아니라 `조직 표준 적합성` 때문에 선택되는 예외다.

## 운영형 목표 토폴로지

초기 운영형 목표는 아래를 기본값으로 둔다.

| 항목 | 목표 값 | 이유 |
| --- | --- | --- |
| broker count | 3 | 한 노드 장애 허용 |
| controller quorum | 3 | KRaft 메타데이터 가용성 확보 |
| replication factor | 3 | topic leader 장애 복구 가능 |
| min.insync.replicas | 2 | `acks=all` 의미 보장 |
| rack awareness | 가능 시 적용 | AZ 분산 준비 |
| listener security | SASL/SCRAM 또는 mTLS | 내부 서비스 보호 |
| ACL | topic/group 단위 제한 | worker, UI, admin 권한 분리 |

## 단계별 진화 계획

### Stage 0. 현재 상태 유지

대상 환경:

- local
- 개인 개발 환경

원칙:

- single broker KRaft
- PLAINTEXT
- auto topic creation allowed
- Kafka UI 포함

### Stage 1. 운영 사전 검증 환경

대상 환경:

- dev shared
- staging

추가할 것:

- broker 3대
- controller quorum 3대
- replication factor 3
- topic creation을 앱 자동 생성 대신 admin provisioning으로 이관 검토
- broker 로그, JMX, consumer lag 수집

아직 하지 않을 것:

- multi-region
- cross-cluster replication

### Stage 2. 운영 기본형

대상 환경:

- production

필수 조건:

- topic IaC 또는 platform request 기반 생성
- ACL 적용
- DLQ 운영 절차 문서화
- broker/node 장애 훈련
- consumer lag alert
- oldest request age alert

이 단계부터는 애플리케이션이 topic을 생성하지 않아도 되는 구조가 더 낫다.

### Stage 3. 확장형

도입 조건:

- `coupon.issue.requested.v1` backlog가 지속적으로 커짐
- worker scale-out이 partition 수에 막힘
- lifecycle fan-out consumer가 분리됨

확장 방향:

- partition 수 재조정
- relay / consumer runtime 분리
- 필요 시 lifecycle topic 별도 분리
- 필요 시 cluster separation
  - command bus cluster
  - analytics / replay cluster

## 애플리케이션 측 준비 항목

브로커 토폴로지를 확장하기 전에 애플리케이션에서 먼저 보강해야 하는 것들이다.

### 1. Topic provisioning 분리

현재는 [`CouponIssueRequestKafkaConfig.kt`](/Users/yunbeom/ybcha/coupon-system-design-kt/coupon/coupon-worker/src/main/kotlin/com.coupon/config/CouponIssueRequestKafkaConfig.kt) 의 `NewTopic` bean이 topic 생성을 담당한다.

운영으로 가면 아래처럼 나누는 편이 낫다.

- local/dev: `NewTopic` 허용
- stage/prod: topic auto creation 비활성화, 운영 provisioning 사용

### 2. Bootstrap server profile 분리

현재는 worker가 `spring.kafka.bootstrap-servers` 하나만 본다.

운영형으로 가면 최소 아래를 분리해야 한다.

- local broker endpoint
- shared dev/staging cluster endpoint
- production cluster endpoint

### 3. Broker 상태를 acceptance 정책에 반영

현재 request acceptance는 DB/outbox 기준으로 안전하지만, broker 장애 시 relay backlog가 쌓인다.

정책을 미리 정해야 한다.

- broker down이어도 request는 계속 `PENDING`으로 수락할 것인가
- 일정 backlog 이상이면 intake를 제한할 것인가

이 결정은 성능보다 운영 안정성 기준으로 정한다.

## 운영 체크리스트

운영형으로 전환할 때 최소 확인 항목은 아래다.

- broker 3대 이상인지
- controller quorum 3대인지
- topic replication factor 3인지
- `min.insync.replicas=2` 이상인지
- producer `acks=all`, idempotence enabled인지
- consumer lag 대시보드가 있는지
- DLQ 증가 알림이 있는지
- topic 생성 절차가 문서화되어 있는지
- replay 운영자가 누구인지 정해져 있는지

## 현재 프로젝트의 권장 결론

현재 쿠폰 시스템은 앞으로 확장되더라도 기본값을 이렇게 둔다.

- 로컬: single broker KRaft 유지
- 운영 전 단계: 3 broker KRaft
- 운영: 3 broker 이상 KRaft + ACL + observability + provisioning 분리
- ZooKeeper: 기존 조직 표준에 묶일 때만 예외적으로 고려

## 참고 링크

- [Apache Kafka: Differences Between KRaft mode and ZooKeeper mode](https://kafka.apache.org/40/documentation/zk2kraft.html)
- [Apache Kafka: ZooKeeper to KRaft migration and deprecation context](https://kafka.apache.org/37/documentation/#kraft_zk_migration)
- [Confluent: KRaft overview](https://docs.confluent.io/platform/current/kafka-metadata/kraft.html)
