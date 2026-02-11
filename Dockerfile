# 1. Debian 기반 Java 17 환경 (Python 3.11 설치 가능)
FROM eclipse-temurin:17-jre

# 2. 작업 폴더 생성
WORKDIR /app

# 3. 시간대 설정 (KST) - 로그 시간이 한국 시간으로 찍히게 함
ENV TZ=Asia/Seoul

# 4. Python 3 설치 (Yahoo Finance 데이터 수집용)
RUN apt-get update && \
    apt-get install -y --no-install-recommends python3 python3-pip && \
    apt-get clean && \
    rm -rf /var/lib/apt/lists/* && \
    python3 --version

# 5. Python 패키지 설치
RUN pip3 install --no-cache-dir --break-system-packages \
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
