# 1. 자바 17 환경 준비
FROM amazoncorretto:17

# 2. 작업 폴더 생성
WORKDIR /app

# 3. 시간대 설정 (KST) - 로그 시간이 한국 시간으로 찍히게 함
ENV TZ=Asia/Seoul

# 4. Python 3.9 소스 컴파일 설치 (디스크 용량 최적화)
# Python 3.9+ 필요 (multitasking의 type[Thread] 문법)
# OpenSSL 1.1.1 설치 후 Python 컴파일하여 SSL 모듈 포함
RUN yum install -y gcc make wget tar gzip zlib-devel libffi-devel && \
    # OpenSSL 1.1.1 컴파일 및 설치
    cd /tmp && \
    wget -q https://www.openssl.org/source/openssl-1.1.1w.tar.gz && \
    tar xzf openssl-1.1.1w.tar.gz && \
    cd openssl-1.1.1w && \
    ./config --prefix=/usr/local/openssl --openssldir=/usr/local/openssl no-tests && \
    make -j$(nproc) && make install_sw && \
    cd /tmp && rm -rf openssl-1.1.1w* && \
    # Python 3.9.18 컴파일 및 설치
    wget -q https://www.python.org/ftp/python/3.9.18/Python-3.9.18.tgz && \
    tar xzf Python-3.9.18.tgz && \
    cd Python-3.9.18 && \
    ./configure --with-openssl=/usr/local/openssl --enable-optimizations --with-ensurepip=install && \
    make -j$(nproc) altinstall && \
    cd /tmp && rm -rf Python-3.9.18* && \
    # 심볼릭 링크 생성
    ln -sf /usr/local/bin/python3.9 /usr/bin/python3 && \
    ln -sf /usr/local/bin/pip3.9 /usr/bin/pip3 && \
    # 빌드 도구 제거 (용량 절약)
    yum remove -y gcc make wget zlib-devel libffi-devel && \
    yum clean all && \
    rm -rf /var/cache/yum && \
    python3 --version

# 5. pip 업그레이드 및 Python 패키지 설치
# urllib3<2.0: OpenSSL 1.0.2k 호환성
RUN python3 -m pip install --upgrade pip setuptools wheel && \
    python3 -m pip install --no-cache-dir \
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