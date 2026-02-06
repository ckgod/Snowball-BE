# 1. 자바 17 환경 준비
FROM amazoncorretto:17

# 2. 작업 폴더 생성
WORKDIR /app

# 3. 시간대 설정 (KST) - 로그 시간이 한국 시간으로 찍히게 함
ENV TZ=Asia/Seoul

# 4. Python 3.7 설치 (시스템 기본 Python)
RUN yum install -y python3 python3-pip && \
    yum clean all && \
    python3 --version

# 5. pip 업그레이드 및 Python 패키지 설치
# typing-extensions: Python 3.7에서 TypedDict 지원 (multitasking 의존성)
# urllib3<2.0: OpenSSL 1.0.2k 호환성
RUN python3 -m pip install --upgrade pip setuptools wheel && \
    python3 -m pip install --no-cache-dir \
        typing-extensions \
        'urllib3<2.0' \
        yfinance \
        mysql-connector-python \
        python-dotenv \
        pandas \
        numpy

# 8. Python 스크립트 복사
COPY scripts/ /app/scripts/

# 9. 빌드된 Jar 파일 복사 (이름을 app.jar로 통일)
COPY app.jar app.jar

# 10. 서버 실행 (메모리 제한 옵션 추가)
# -Xmx384m: 램을 최대 384MB까지만 쓰라고 제한 (서버 다운 방지)
ENTRYPOINT ["java", "-Xmx384m", "-jar", "app.jar"]