# Single Host Production Guide (2Core 8GB)

## 목적

이 문서는 `coupon-api.yogieat.com` 단일 공개 도메인을 가진 `2Core 8GB` 한 대 배포를 기준으로,
운영형 Docker Compose 구성과 현재 저장소 기준 가용 범위를 정리한다.

전제는 아래와 같다.

- 배포 대상: 가비아 클라우드 단일 VM
- 공개 엔드포인트: `coupon-api.yogieat.com`
- 배포 방식: `docker compose`
- 현재 런타임 계약: `Redis reserve -> Kafka publish -> worker consume -> distributed lock -> DB persist`
- `POST /coupon-issues` 의 `SUCCESS` 의미: 최종 DB 저장이 아니라 `Redis reserve + Kafka broker ack`

세부 계약은 [coupon-issuance-runtime.md](/Users/yunbeom/ybcha/coupon-system-design-kt/docs/architecture/coupon-issuance-runtime.md),
현재 모듈 구조는 [current-architecture-overview.md](/Users/yunbeom/ybcha/coupon-system-design-kt/docs/architecture/current-architecture-overview.md)를 기준으로 본다.

## 권장 토폴로지

단일 호스트에서는 아래 구성으로 시작한다.

- `coupon-app`
  - 공개 API, 관리자 API, 인증, intake
- `coupon-worker`
  - Kafka consume, retry/DLQ, outbox poller
- `mysql`
  - source of truth
- `redis`
  - reserve state, distributed lock, processing limit
- `kafka`
  - accepted issue command transport
- `kafka-ui`
  - 토픽, lag, consumer 상태 확인
- `grafana`
  - 운영 대시보드
- `loki`
  - 구조화 로그 저장소
- `alloy`
  - Docker stdout collector
- `influxdb`
  - k6 대시보드 datasource

운영 기본 원칙은 단순하다.

- `coupon-app`, `coupon-worker`, `kafka-ui`, `grafana`, `loki`, `alloy`, `influxdb`는 호스트 포트로 publish 하고, 필요하면 앞단 Nginx가 이를 reverse proxy 한다.
- `MySQL`, `Redis`, `Kafka`는 private network 안에 둔다.
- `prod` compose는 운영 편의를 위해 `kafka-ui + grafana + loki + alloy + influxdb`까지 함께 포함한다.

## 2Core 8GB 권장 배분

| 서비스 | CPU | 메모리 limit | 메모리 reservation | 핵심 설정 |
| --- | --- | --- | --- | --- |
| `coupon-app` | `1.00` | `1536MB` | `768MB` | `-Xmx768m`, DB pool `12`, Tomcat `80/10` |
| `coupon-worker` | `0.50` | `896MB` | `384MB` | `-Xmx384m`, DB pool `8`, concurrency `1` |
| `mysql` | `0.50` | `2048MB` | `1024MB` | `max-connections=50`, buffer pool `768M` |
| `kafka` | `0.40` | `1536MB` | `768MB` | heap `768M` |
| `redis` | `0.10` | `192MB` | `64MB` | `noeviction`, AOF on |
| `kafka-ui` | `0.10` | `256MB` | `64MB` | 상태 확인용 UI |
| `grafana` | `0.15` | `384MB` | `128MB` | 운영 대시보드 |
| `loki` | `0.15` | `384MB` | `128MB` | 로그 저장소 |
| `alloy` | `0.10` | `128MB` | `64MB` | Docker 로그 collector |
| `influxdb` | `0.15` | `256MB` | `128MB` | k6 datasource |

전체 메모리 limit 합은 약 `7.6GB`다. 즉 `2Core 8GB`에서 전부 함께 올리면 디버깅 편의성은 높지만 headroom은 꽤 줄어든다.
운영 안정성을 더 우선하면 `kafka-ui/grafana/loki/alloy/influxdb`를 별도 호스트로 분리하는 편이 낫다.

## 현재 저장소 기준 운영 주의점

### 1. 기본 worker JVM은 2Core 8GB 단일 호스트에 그대로 두면 과하다

[Dockerfile](/Users/yunbeom/ybcha/coupon-system-design-kt/docker/Dockerfile#L49) 기본값이 `-Xmx1024m -Xms512m` 이다.
실험 중 worker limit을 `768MB`로 걸면 OOMKilled가 재현됐다.

그래서 prod compose는 worker에 별도 `JAVA_OPTS`를 준다.

### 2. 로컬 load-test 프로필은 prod에 넣으면 안 된다

[LocalAdminBootstrap.kt](/Users/yunbeom/ybcha/coupon-system-design-kt/coupon/coupon-api/src/main/kotlin/com/coupon/bootstrap/LocalAdminBootstrap.kt#L20)는 `local`, `load-test`에서만 동작한다.
운영 compose는 `SPRING_PROFILES_ACTIVE=docker`만 사용한다.

### 3. 기본 runtime pool/thread 값은 단일 2Core에는 과하다

- [application.yml](/Users/yunbeom/ybcha/coupon-system-design-kt/coupon/coupon-api/src/main/resources/application.yml#L39): Tomcat `600/100`
- [db-core.yml](/Users/yunbeom/ybcha/coupon-system-design-kt/storage/db-core/src/main/resources/db-core.yml#L80): 기본 DB pool `50/10`
- [worker.yml](/Users/yunbeom/ybcha/coupon-system-design-kt/coupon/coupon-worker/src/main/resources/worker.yml#L24): worker concurrency `3`

prod compose는 이 값을 2Core 8GB에 맞게 낮춰서 시작한다.

## 2026-04-10 로컬 근사 부하 테스트 결과

아래 결과는 `2026-04-10`에 clean local Docker stack을 올린 뒤,
컨테이너 리소스를 `2Core 8GB`에 가깝게 제한해서 측정한 값이다.
실제 가비아 VM과 완전히 같지는 않으므로 운영 해석은 한 단계 보수적으로 가져간다.

### 검증 중 발견한 보정 사항

- `smoke`는 원래 `couponId` 64비트 정수 파싱 정밀도 손실 때문에 false negative가 났다.
- 현재 저장소에 반영된 k6 helper 수정으로 raw body 기준 exact id 매칭을 하도록 보정했다.
- `smoke`는 iteration이 끝나지 않아도 성공처럼 보일 수 있었는데, 완료 counter threshold를 추가해 막았다.
- `issue-overload`는 기존에는 `500` 응답을 counter에 정확히 잡지 못했는데, safe helper로 보정했다.

### 신뢰 가능한 측정값

| 시나리오 | 조건 | 결과 |
| --- | --- | --- |
| `smoke` | clean stack | 통과 |
| `issue-overload` | `60 VU / 2m` | 통과, `p95 96.36ms`, unexpected failure `0` |
| `issue-overload` | `65 VU / 2m` | 통과, `p95 94.09ms`, unexpected failure `0` |
| `issue-overload` | `70 VU / 2m` | 실패, `p95 201.14ms`, unexpected failure `18` |

### 해석

- sustained safe range는 대략 `60~65 VU` 부근으로 본다.
- `65 VU`는 clean pass였지만 운영 공개치는 여유를 두고 `50~60 VU`에서 시작하는 편이 맞다.
- `70 VU`는 이미 실패가 발생하므로 단일 `2Core 8GB` 운영 상한으로 보지 않는다.
- 관측성 스택까지 같은 호스트에 같이 올리면 실제 운영 상한은 이보다 더 보수적으로 잡는 편이 맞다.

중요한 점은 `issue-overload`가 최종 DB 반영 throughput이 아니라 intake acceptance 경로를 강하게 보는 시나리오라는 점이다.
즉, 이 수치는 “지속 요청 압력에 대한 수락 안정성”으로 해석해야지, “최종 발급 row/sec”와 동일하게 보면 안 된다.

## 도메인과 네트워크

- 공개 도메인: `coupon-api.yogieat.com`
- 추가 도메인 예시:
  - `kafka-ui.yogieat.com`
  - `grafana.yogieat.com`
  - `loki.yogieat.com`
  - `alloy.yogieat.com`
  - `influxdb.yogieat.com`
- DNS: `A` 레코드가 VM 공인 IP를 가리켜야 한다
- 방화벽: `80/tcp`, `443/tcp`만 허용
- 내부 포트:
  - `coupon-app:8080`
  - `coupon-worker:8081`
  - `mysql:3306`
  - `redis:6379`
  - `kafka:9092`
- 부가 서비스 포트:
  - `kafka-ui:8080`
  - `grafana:3000`
  - `loki:3100`
  - `alloy:12345`
  - `influxdb:8086`

## 실행 파일

운영 배포 기본 파일은 아래다.

- Compose: [docker-compose.prod.yml](/Users/yunbeom/ybcha/coupon-system-design-kt/docker/docker-compose.prod.yml)
- env 예시: [.env.example](/Users/yunbeom/ybcha/coupon-system-design-kt/.env.example)

## 배포 순서

`docker-compose.prod.yml`는 저장소 루트의 `.env`를 기준으로 읽도록 맞춰뒀다.
즉, 인스턴스에서도 저장소 루트에 `.env`만 두고 아래 명령을 실행하면 된다.

1. [.env.example](/Users/yunbeom/ybcha/coupon-system-design-kt/.env.example)를 참고해 저장소 루트에 `.env`를 만든다.
2. `APP_HOST_PORT`, `WORKER_HOST_PORT`, `KAFKA_UI_HOST_PORT`, `GRAFANA_HOST_PORT`, DB 비밀번호, JWT secret, Grafana 관리자 계정을 교체한다.
3. 호스트 Nginx가 각 호스트 포트를 원하는 도메인으로 reverse proxy 하게 설정한다.
4. 아래 명령으로 기동한다.

```bash
docker compose \
  -f docker/docker-compose.prod.yml \
  up -d --build
```

5. 기동 후 아래를 확인한다.

```bash
curl -si http://127.0.0.1:18080/actuator/health
curl -si http://127.0.0.1:18085
curl -si http://127.0.0.1:3000/api/health
docker compose -f docker/docker-compose.prod.yml ps
```

## 운영 해석 기준

- 첫 운영 기준은 `50~60 VU sustained` 수준에서 시작한다.
- p95가 눈에 띄게 튀거나, backlog가 쌓이거나, `500`이 보이면 상한을 즉시 낮춘다.
- 트래픽이 계속 커지면 가장 먼저 외부화할 후보는 `Kafka`다.
- 그 다음은 `MySQL`과 VM CPU 확장이다.

## 단일 호스트 한계

이 compose는 “소규모 운영을 한 대에서 시작하는” 용도다.
아래 중 하나가 필요해지면 단일 `2Core 8GB`를 졸업하는 편이 낫다.

- 지속적으로 `70 VU` 이상 acceptance 부하를 소화해야 함
- Kafka lag와 worker backlog가 반복적으로 누적됨
- 관리자 기능, 배치, 관측성 스택까지 같은 호스트에 함께 올려야 함
- 장애 도메인을 `MySQL + Kafka + API + worker` 한 박스로 묶는 게 부담됨
