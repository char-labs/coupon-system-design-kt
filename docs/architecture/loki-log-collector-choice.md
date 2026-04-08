# Loki Log Collector Choice

## 목적

이 문서는 현재 저장소에서 Loki 로그 수집기로 무엇을 기본 선택하는지와, `Promtail`을 왜 기본 경로로 채택하지 않는지 설명한다.

## Promtail이란

`Promtail`은 Loki로 로그를 보내는 수집 에이전트다.
일반적으로 아래 역할을 맡는다.

- 파일, Docker/CRI 로그, journal, syslog, Kafka 등에서 로그를 읽는다.
- pipeline stage로 json, regex, docker, cri 파싱을 수행한다.
- label 추가, drop/filter, output 변환을 수행한다.
- 최종 로그를 Loki로 push 한다.

즉, Loki 자체가 로그를 능동적으로 수집하지 못하므로, Promtail 같은 collector가 중간에서 로그를 읽어 전달하는 구조다.

## Promtail의 장점

- YAML 기반이라 설정 진입 장벽이 낮다.
- 다양한 입력 소스를 바로 지원한다.
- pipeline stage가 풍부해 로그 파싱과 라벨링을 collector 단계에서 끝내기 쉽다.
- `metrics` stage를 사용하면 로그 기반 메트릭을 Promtail `/metrics`로 노출할 수 있다.

## 왜 기본 채택하지 않는가

현재 저장소의 기본 선택은 `Grafana Alloy`다.
Promtail을 기본 collector로 채택하지 않는 이유는 아래와 같다.

- Grafana 공식 문서 기준으로 Promtail은 deprecated 상태다.
- Promtail은 LTS가 `2026-02-28`까지이고, EOL은 `2026-03-02`다.
- 현재 Grafana의 향후 확장 방향은 Alloy 쪽에 더 가깝다.
- 이 저장소가 필요한 기능은 `Docker 로그 수집 + service label 부여 + Loki 전달` 정도라서 Alloy 최소 구성으로 충분하다.
- Promtail을 추가로 붙이면 local stack에서 collector가 중복되어 운영 복잡도와 중복 수집 위험이 커진다.

## 현재 선택

로컬 기본 스택은 아래다.

- application: JSON stdout 로그 출력
- collector: Alloy
- storage/query: Loki
- UI: Grafana
- compose: `docker-compose.observability.yml`

즉, 현재 기본 경로는 `docker logs -> Alloy -> Loki -> Grafana`다.

## Promtail을 다시 검토할 조건

아래 조건이 생기기 전까지는 기본 stack에 Promtail을 넣지 않는다.

- Alloy가 현재 필요한 source 또는 processing을 지원하지 못하는 경우
- 운영 환경이 이미 Promtail 표준을 강하게 전제하고 있는 경우
- 단기 실험 또는 비교 목적의 local-only profile이 별도로 필요한 경우

그 외에는 현재처럼 Alloy 단일 collector를 유지한다.
