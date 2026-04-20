# Docker Compose Layout

Docker Compose는 역할 기준으로 아래처럼 분리합니다.

- `docker-compose.infrastructure.yml`
  - MySQL, Redis, Kafka, Kafka UI
- `docker-compose.runtime.yml`
  - `coupon-app`, `coupon-worker`
- `docker-compose.observability.yml`
  - Grafana, Loki, Alloy
- `docker-compose.load-test.yml`
  - InfluxDB
- `docker-compose.prod.yml`
  - `2Core 8GB` 단일 호스트 기준 `runtime + infrastructure + ops UI` 운영 예시

운영 가이드는 아래 문서를 봅니다.

- [production-single-host-2c8g.md](/Users/yunbeom/ybcha/coupon-system-design-kt/docker/production-single-host-2c8g.md)
- [.env.example](/Users/yunbeom/ybcha/coupon-system-design-kt/.env.example)

호환용 진입점도 유지합니다.

- `docker-compose.yml`
  - `infrastructure + runtime`
- `docker-compose.k6-observability.yml`
  - `observability + load-test`

로컬 compose는 기본적으로 `~/.env`를 `env_file`로 읽습니다.

- 예시 파일: [home.env.example](/Users/yunbeom/ybcha/coupon-system-design-kt/docker/home.env.example)
- 필수 키:
  - `MYSQL_DATABASE`
  - `MYSQL_ROOT_PASSWORD`
  - `MYSQL_USER`
  - `MYSQL_PASSWORD`
  - `DATASOURCE_DB_CORE_USERNAME`
  - `DATASOURCE_DB_CORE_PASSWORD`
- 선택 키:
  - `ADMIN_PASSWORD`
  - `JWT_SECRET_KEY`

기본 로컬 런타임:

```bash
docker compose \
  -f docker/docker-compose.infrastructure.yml \
  -f docker/docker-compose.runtime.yml \
  up --build
```

부하 테스트까지 포함한 전체 스택:

```bash
docker compose \
  -f docker/docker-compose.infrastructure.yml \
  -f docker/docker-compose.runtime.yml \
  -f docker/docker-compose.observability.yml \
  -f docker/docker-compose.load-test.yml \
  up --build
```

단일 호스트 운영 예시:

```bash
docker compose \
  -f docker/docker-compose.prod.yml \
  up -d --build
```
