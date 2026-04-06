---
name: metrics-observability
description: "Spring Boot Actuator/Prometheus 메트릭을 Grafana에 통합하고, 병목 탐지 대시보드를 구성하는 스킬. Prometheus 스크래핑 설정, Grafana 대시보드 프로비저닝, MQ/JVM/DB/Redis 메트릭 패널 구성을 수행한다. '메트릭 대시보드', 'Prometheus 설정', 'Grafana 통합', '앱 메트릭 모니터링', 'MQ 메트릭', 'JVM 모니터링' 등의 요청 시 반드시 이 스킬을 사용할 것."
---

# Metrics Observability

Spring Boot 앱 메트릭을 Grafana에 통합하고 병목 탐지 대시보드를 구성한다.

## 구성 절차

### Step 1: 현재 상태 파악

다음을 확인한다:
- `docker/docker-compose.yml` — Prometheus 서비스 존재 여부
- `docker/grafana/` — 기존 Grafana 프로비저닝 구조
- `coupon/coupon-api/src/main/resources/application.yml` — actuator 설정

### Step 2: Prometheus 설정

`docker/prometheus.yml`을 생성한다:

```yaml
global:
  scrape_interval: 10s
  evaluation_interval: 10s

scrape_configs:
  - job_name: 'spring-boot'
    metrics_path: '/actuator/prometheus'
    static_configs:
      - targets: ['app:8080']
    scrape_interval: 5s

  # MQ 메트릭 (Kafka Exporter - 도입 시)
  - job_name: 'kafka'
    static_configs:
      - targets: ['kafka-exporter:9308']
```

### Step 3: docker-compose에 Prometheus 추가

기존 `docker-compose.yml` 또는 별도 overlay 파일에 추가:

```yaml
services:
  prometheus:
    image: prom/prometheus:v2.51.0
    volumes:
      - ./prometheus.yml:/etc/prometheus/prometheus.yml
    ports:
      - "9090:9090"
    command:
      - '--config.file=/etc/prometheus/prometheus.yml'
      - '--storage.tsdb.retention.time=7d'
```

### Step 4: Grafana Prometheus 데이터소스

`docker/grafana/provisioning/datasources/prometheus.yml`:

```yaml
apiVersion: 1
datasources:
  - name: Prometheus
    type: prometheus
    url: http://prometheus:9090
    isDefault: false
    access: proxy
```

### Step 5: 병목 탐지 대시보드 패널 구성

`docker/grafana/provisioning/dashboards/app-metrics.json`에 포함할 핵심 패널:

**DB 계층:**
- HikariCP active connections: `hikaricp_connections_active{pool="HikariPool-1"}`
- HikariCP pending threads: `hikaricp_connections_pending{pool="HikariPool-1"}`
- HikariCP acquisition time p99: `hikaricp_connections_acquire_seconds`

**JVM 계층:**
- Heap usage: `jvm_memory_used_bytes{area="heap"}`
- GC pause time: `jvm_gc_pause_seconds_sum`
- Live threads: `jvm_threads_live_threads`

**Tomcat 계층:**
- Active threads: `tomcat_threads_busy_threads`
- Max threads: `tomcat_threads_config_max_threads`
- Thread 사용률: `tomcat_threads_busy_threads / tomcat_threads_config_max_threads`

**Redis 계층:**
- 명령 응답시간: `spring_data_redis_command_active`
- 연결 수: Redis actuator 메트릭

**MQ 계층 (Kafka 도입 시):**
- Consumer lag: `kafka_consumer_group_lag`
- 메시지 처리량: `kafka_consumer_records_consumed_rate`

### Step 6: Application.yml actuator 설정 확인

메트릭이 노출되어야 한다:

```yaml
management:
  endpoints:
    web:
      exposure:
        include: health,info,prometheus,metrics
  metrics:
    export:
      prometheus:
        enabled: true
  endpoint:
    health:
      show-details: always
```

### Step 7: 대시보드 변수 설정

Grafana 대시보드에 아래 변수를 추가하면 환경별로 필터링 가능:
- `$instance`: Spring Boot 인스턴스
- `$pool`: HikariCP 풀 이름

## 출력

- `docker/prometheus.yml`
- `docker/grafana/provisioning/datasources/prometheus.yml`
- `docker/grafana/provisioning/dashboards/app-metrics.json`
- 업데이트된 `docker-compose` 파일
- `_workspace/04_observability_summary.md`
