# Snowball

**레버리지 ETF 자동매매 시스템** — Kotlin 기반 백엔드

![Architecture](./architecture.svg)

---

## 아키텍처

```
snowball/
├── presentation/          # REST 라우팅, Response Mapper, Rate Limiting
├── application/           # Ktor 서버 진입점, Koin DI, Quartz 스케줄러
├── domain/                # Use Cases, Domain Models, Repository 인터페이스
├── infrastructure/
│   ├── database/          # Exposed ORM, MariaDB Repository 구현체
│   └── kis-api/           # KIS Open API HTTP 클라이언트, Repository 구현체
├── snowball-models/       # KMP 공유 모델
└── scripts/               # Python yfinance 주가 데이터 수집 스크립트
```

---

## 기술 스택

| 구분 | 기술 |
|------|------|
| Language | Kotlin 2.1.0 (KMP) |
| Framework | Ktor 3.0.2 (Server + HTTP Client) |
| DI | Koin |
| ORM | Exposed v1 (Kotlin SQL DSL) |
| Database | MariaDB 10.11 |
| Scheduler | Quartz (Cron 기반) |
| Serialization | kotlinx.serialization |
| Runtime | Netty · JVM 17 |
| Data Source | Yahoo Finance (Python yfinance) |
| Broker API | 한국투자증권 KIS Open API (Real / Mock) |
| Container | Docker Compose |
| CI/CD | GitHub Actions → Oracle Cloud (Ubuntu VM) |
| Reverse Proxy | Caddy 2 (HTTPS, Let's Encrypt 자동 갱신) |

---

## 배포

```
GitHub Actions
    └─▶ Docker build
            └─▶ Oracle Cloud VM
                    ├─ caddy (Port 80/443 → app:8080, https://ckg-snowball.duckdns.org)
                    ├─ app   (내부 8080 만 노출, Java 17 JRE + Python 3, -Xmx384m)
                    └─ db    (MariaDB 10.11, 127.0.0.1:3306)
```

- 외부 진입은 Caddy 하나뿐이다. app 은 호스트 포트를 열지 않는다.
- 인증서는 `caddy_data` 볼륨에 보관되어 `docker compose down` 으로 지워지지 않는다.
- 오라클 보안 목록에서 80(인증서 발급)·443 인그레스가 열려 있어야 한다.
