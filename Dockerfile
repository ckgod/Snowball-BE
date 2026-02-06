# 1. 자바 17 환경 준비
FROM amazoncorretto:17

# 2. 작업 폴더 생성
WORKDIR /app

# 3. 시간대 설정 (KST) - 로그 시간이 한국 시간으로 찍히게 함
ENV TZ=Asia/Seoul

# 4. Python3 설치 (Yahoo Finance 데이터 수집용)
# Amazon Linux 2023 대응: dnf 또는 yum 자동 선택
RUN if command -v dnf &> /dev/null; then \
        dnf install -y python3 python3-pip gcc python3-devel && dnf clean all; \
    else \
        yum install -y python3 python3-pip gcc python3-devel && yum clean all; \
    fi && \
    python3 --version

# 5. pip 업그레이드 및 Python 패키지 설치
RUN python3 -m pip install --upgrade pip setuptools wheel && \
    python3 -m pip install --no-cache-dir \
        'urllib3<2.0' \
        'requests<2.29.0' \
        yfinance==0.2.28 \
        mysql-connector-python==8.0.33 \
        python-dotenv==0.21.1 \
        pandas==1.3.5 \
        numpy==1.21.6

# 6. Python 스크립트 복사
COPY scripts/ /app/scripts/

# 7. 빌드된 Jar 파일 복사 (이름을 app.jar로 통일)
COPY app.jar app.jar

# 8. 서버 실행 (메모리 제한 옵션 추가)
# -Xmx384m: 램을 최대 384MB까지만 쓰라고 제한 (서버 다운 방지)
ENTRYPOINT ["java", "-Xmx384m", "-jar", "app.jar"]