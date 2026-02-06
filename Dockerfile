# 1. 자바 17 환경 준비
FROM amazoncorretto:17

# 2. 작업 폴더 생성
WORKDIR /app

# 3. 시간대 설정 (KST) - 로그 시간이 한국 시간으로 찍히게 함
ENV TZ=Asia/Seoul

# 4. Python 3.8+ 설치 (Yahoo Finance 데이터 수집용)
# Amazon Linux 2: amazon-linux-extras로 Python 3.8 설치
# Amazon Linux 2023: dnf로 Python 3.9+ 설치
RUN if command -v amazon-linux-extras &> /dev/null; then \
        amazon-linux-extras install python3.8 -y && \
        yum install -y gcc python38-devel && \
        yum clean all && \
        alternatives --install /usr/bin/python3 python3 /usr/bin/python3.8 1; \
    elif command -v dnf &> /dev/null; then \
        dnf install -y python3.11 python3.11-pip gcc python3.11-devel && \
        dnf clean all && \
        alternatives --install /usr/bin/python3 python3 /usr/bin/python3.11 1; \
    else \
        yum install -y python3 python3-pip gcc python3-devel && \
        yum clean all; \
    fi && \
    python3 --version

# 5. pip 업그레이드 및 Python 패키지 설치
# urllib3<2.0: OpenSSL 1.0.2k 호환성 유지 (urllib3 v2.0+는 OpenSSL 1.1.1+ 필요)
# Python 3.8+에서는 대부분의 패키지 최신 버전 사용 가능
RUN python3 -m pip install --upgrade pip setuptools wheel && \
    python3 -m pip install --no-cache-dir \
        'urllib3<2.0' \
        yfinance \
        mysql-connector-python \
        python-dotenv \
        pandas \
        numpy

# 6. Python 스크립트 복사
COPY scripts/ /app/scripts/

# 7. 빌드된 Jar 파일 복사 (이름을 app.jar로 통일)
COPY app.jar app.jar

# 8. 서버 실행 (메모리 제한 옵션 추가)
# -Xmx384m: 램을 최대 384MB까지만 쓰라고 제한 (서버 다운 방지)
ENTRYPOINT ["java", "-Xmx384m", "-jar", "app.jar"]