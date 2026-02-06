# 1. Python 3.10 이미지에서 Python 환경 복사
FROM python:3.10-slim as python-base

# 2. 자바 17 환경 준비
FROM amazoncorretto:17

# 3. Python 3.10 복사 (공식 이미지에서 검증된 빌드)
COPY --from=python-base /usr/local /usr/local

# 4. 작업 폴더 생성
WORKDIR /app

# 5. 시간대 설정 (KST) - 로그 시간이 한국 시간으로 찍히게 함
ENV TZ=Asia/Seoul

# 6. Python 버전 확인 (공식 Python 3.10 이미지 사용)
RUN python3 --version && pip3 --version

# 7. pip 업그레이드 및 Python 패키지 설치
# 로컬 Python 3.10 환경에서 테스트 완료된 구성
RUN pip3 install --upgrade pip setuptools wheel && \
    pip3 install --no-cache-dir \
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