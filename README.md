# Snowball

**무한매수법 자동 거래 시스템**

레버리지 ETF(TQQQ, SOXL, FNGU)에 대한 무한매수법 전략을 자동으로 실행하는 Kotlin 기반 백엔드 시스템입니다.

## 주요 기능

### 자동화
- **자동 정산**: 매일 오전 7시 계좌 잔고 동기화 및 전략 상태 업데이트
- **자동 주문**: 매일 오후 6시 매수/매도 주문 자동 생성 및 전송
- **수동 실행**: 명령줄로 정산/주문 작업을 즉시 실행 가능

### REST API
- **종목 상태 조회**: 투자 전략 상태, T값, 별%, 수익률 등 실시간 조회
- **계좌 정보**: 보유 종목, 평가 손익, 현재가 등
- **거래 히스토리**: 과거 매매 내역 조회
- **병렬 처리**: 여러 종목 동시 조회로 빠른 응답 속도

### 투자 전략
- **무한매수법**: 분할 매수/익절 전략 자동 실행
- **T값 기반 관리**: 현재 투자 단계(전반전/후반전/쿼터모드) 자동 판단
- **가격 충돌 방지**: 매수가가 매도가보다 낮게 자동 조정
- **종목별 설정**: 종목마다 다른 분할 수, 목표 수익률 설정 가능

## 기술 스택

### Backend
- **Language**: Kotlin 2.1.0
- **Framework**: Ktor 3.0.2 (Server + Client)
- **Database**: MariaDB (Exposed ORM v1)
- **Scheduler**: Quartz
- **Serialization**: kotlinx.serialization

### Infrastructure
- **External API**: 한국투자증권(KIS) Open API
- **Container**: Docker
- **CI/CD**: GitHub Actions
- **Deployment**: AWS EC2

## 아키텍처

멀티모듈 구조:

```
snowball/
├── application/          # 서버 진입점, 설정, 스케줄러
├── domain/              # 비즈니스 로직, UseCase, 엔티티
├── presentation/        # REST API 라우팅, 응답 모델
└── infrastructure/
    ├── database/       # DB 구현 (Exposed + MariaDB)
    └── kis-api/        # 한투 API 클라이언트
```

## 데이터베이스

### 테이블 구조
- **investment_status**: 종목별 투자 전략 상태 (T값, 별%, 평단가 등)
- **trade_history**: 거래 내역
- **auth_tokens**: API 인증 토큰

### 자동 마이그레이션
Exposed ORM의 자동 마이그레이션으로 스키마 변경 자동 적용

## CI/CD

GitHub Actions를 통한 자동 배포:
1. master 브랜치 push 시 트리거
2. shadowJar 빌드
3. Docker 이미지 생성
4. EC2 인스턴스로 파일 전송
5. Docker 컨테이너 재시작
