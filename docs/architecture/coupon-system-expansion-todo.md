# Coupon System Expansion TODO

## 현재 기준

- 발급 경로의 공식 계약은 `Redis reserve + direct Kafka publish + worker persist` 이다.
- 관측성은 `기본 Micrometer + 구조화 로그 + Grafana/Loki 조회` 기준으로 유지한다.
- Loki collector 기본값은 Promtail이 아니라 Alloy다.

## 우선 TODO

- provisioned `Coupon Issuance Runtime` 대시보드와 실제 로그 필드 계약이 계속 일치하는지 유지한다.
- 운영 환경의 Loki label 기준(`app`, `env`, `traceId`)을 문서화한다.
- load test 결과를 발급 로그와 함께 보는 운영 체크리스트를 보강한다.

## 다음 단계 TODO

- 로그 기반 조회만으로 부족해질 때에만 business counter/timer를 Prometheus에 추가한다.
- stage/prod 토픽 생성은 플랫폼 관리 방식으로 분리하고, 애플리케이션은 local/dev 기본값만 가진다.
- hot coupon 편중 상황에서 worker concurrency와 partition 조정 기준을 실측값으로 정리한다.

## 보류 TODO

- durable acceptance를 요구할 때 `request table + relay/outbox + CDC`를 별도 트랙으로 검토한다.
- Redis Cluster sharding, autoscaling, broker multi-node HA는 실제 운영 요구가 생길 때 검토한다.
