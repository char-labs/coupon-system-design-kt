# Kafka 상세 설정 참조

## 토픽 설계

| 토픽 | 파티션 | 복제본 | 용도 |
|------|--------|--------|------|
| `coupon.issue` | 10 | 1 (로컬) | 쿠폰 발급 요청 |
| `coupon.issue.dlq` | 1 | 1 | 실패 메시지 |
| `coupon.issue.result` | 10 | 1 | 발급 결과 알림 |

## Producer 설정 (처리량 최적화)

```yaml
spring:
  kafka:
    producer:
      batch-size: 16384        # 16KB 배치
      linger-ms: 5             # 5ms 대기 후 배치 전송
      buffer-memory: 33554432  # 32MB 버퍼
      compression-type: snappy
      acks: all                # 모든 ISR 확인 (내구성)
```

## Consumer 설정

```yaml
spring:
  kafka:
    consumer:
      max-poll-records: 500    # 한 번에 최대 500건
      fetch-min-size: 1
      fetch-max-wait: 500
      enable-auto-commit: false # 수동 커밋으로 정확한 at-least-once
```

## KRaft 모드 (Zookeeper 없음)

Spring Boot 3.x + Kafka 3.3+ 조합에서 KRaft 모드 사용 가능. `bitnami/kafka:3.7` 이미지가 KRaft를 기본 지원한다.

## 로컬 테스트

```bash
# 토픽 생성 확인
docker exec kafka kafka-topics.sh --bootstrap-server localhost:9092 --list

# 메시지 확인
docker exec kafka kafka-console-consumer.sh \
  --bootstrap-server localhost:9092 \
  --topic coupon.issue \
  --from-beginning
```
