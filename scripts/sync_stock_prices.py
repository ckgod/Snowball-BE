#!/usr/bin/env python3
"""
Yahoo Finance 레버리지 ETF 가격 데이터 동기화 스크립트

사용법:
    python sync_stock_prices.py full    # 전체 히스토리컬 데이터 로드 (초기 실행)
    python sync_stock_prices.py daily   # 최근 5일 데이터 업데이트 (일일 스케줄)

환경변수:
    DB_HOST: MariaDB 호스트 (기본값: localhost)
    DB_PORT: MariaDB 포트 (기본값: 3306)
    DB_USER: MariaDB 사용자 (기본값: root)
    DB_PASSWORD: MariaDB 비밀번호 (필수)
    DB_NAME: 데이터베이스 이름 (기본값: snowball)
"""

import yfinance as yf
import mysql.connector
from datetime import datetime, timedelta
import os
import sys
from pathlib import Path
from typing import Optional
from dotenv import load_dotenv

# 프로젝트 루트의 .env 파일 로드
# scripts/ 디렉토리에서 실행되므로 상위 디렉토리의 .env 파일을 찾음
env_path = Path(__file__).parent.parent / '.env'
load_dotenv(dotenv_path=env_path)

# 환경변수에서 DB 설정 읽기
DB_HOST = os.getenv("DB_HOST", "localhost")
DB_PORT = int(os.getenv("DB_PORT", "3306"))
DB_USER = os.getenv("DB_USER", "root")
DB_PASSWORD = os.getenv("DB_PASSWORD", "")
DB_NAME = os.getenv("DB_NAME", "snowball")

# 추적할 레버리지 ETF 종목 리스트
# 필요에 따라 추가/삭제 가능
TICKERS = [
    "TQQQ",  # ProShares UltraPro QQQ (3x Nasdaq-100)
    "SOXL",  # Direxion Daily Semiconductor Bull 3X
]


def get_db_connection():
    """MariaDB 연결 생성"""
    try:
        conn = mysql.connector.connect(
            host=DB_HOST,
            port=DB_PORT,
            user=DB_USER,
            password=DB_PASSWORD,
            database=DB_NAME
        )
        return conn
    except mysql.connector.Error as err:
        print(f"❌ Database connection error: {err}")
        sys.exit(1)


def fetch_historical_data(ticker: str, start_date: str, end_date: Optional[str] = None):
    """
    Yahoo Finance에서 히스토리컬 데이터 가져오기

    Args:
        ticker: 종목 심볼 (예: TQQQ)
        start_date: 시작일 (YYYY-MM-DD)
        end_date: 종료일 (YYYY-MM-DD), None이면 현재까지

    Returns:
        pandas DataFrame with OHLCV data
    """
    try:
        print(f"📊 Fetching data for {ticker} from {start_date}...")
        stock = yf.Ticker(ticker)

        # auto_adjust=False를 사용하여 Adj Close를 별도로 받음
        hist = stock.history(start=start_date, end=end_date, auto_adjust=False)

        if hist.empty:
            print(f"⚠️  No data available for {ticker}")
            return None

        print(f"✅ Fetched {len(hist)} records for {ticker}")
        return hist

    except Exception as e:
        print(f"❌ Error fetching data for {ticker}: {str(e)}")
        return None


def insert_or_update_price(cursor, ticker: str, date, row):
    """
    DB에 가격 정보 삽입 또는 업데이트 (UPSERT)

    Args:
        cursor: MySQL cursor
        ticker: 종목 심볼
        date: 날짜 (pandas Timestamp)
        row: pandas Series with OHLCV data
    """
    sql = """
    INSERT INTO stock_price_history
    (ticker, date, open, high, low, close, adj_close, volume, created_at, updated_at)
    VALUES (%s, %s, %s, %s, %s, %s, %s, %s, %s, %s)
    ON DUPLICATE KEY UPDATE
        open = VALUES(open),
        high = VALUES(high),
        low = VALUES(low),
        close = VALUES(close),
        adj_close = VALUES(adj_close),
        volume = VALUES(volume),
        updated_at = VALUES(updated_at)
    """

    now = datetime.now()
    values = (
        ticker,
        date.date(),
        float(row['Open']),
        float(row['High']),
        float(row['Low']),
        float(row['Close']),
        float(row['Adj Close']),
        int(row['Volume']),
        now,
        now
    )

    cursor.execute(sql, values)


def sync_ticker(ticker: str, days_back: Optional[int] = None):
    """
    특정 종목의 가격 정보 동기화

    Args:
        ticker: 종목 심볼
        days_back: 최근 N일만 업데이트 (None이면 전체 히스토리)
    """
    conn = get_db_connection()
    cursor = conn.cursor()

    try:
        if days_back:
            # 최근 N일만 업데이트
            start_date = (datetime.now() - timedelta(days=days_back)).strftime("%Y-%m-%d")
            print(f"🔄 Updating {ticker} for the last {days_back} days...")
        else:
            # 전체 히스토리컬 데이터 (상장일부터)
            # 레버리지 ETF는 대부분 2008년 이후 상장
            start_date = "2008-01-01"
            print(f"🚀 Loading full historical data for {ticker}...")

        hist = fetch_historical_data(ticker, start_date)

        if hist is None or hist.empty:
            print(f"⚠️  No data to sync for {ticker}")
            return

        count = 0
        for date, row in hist.iterrows():
            insert_or_update_price(cursor, ticker, date, row)
            count += 1

        conn.commit()
        print(f"✅ {ticker}: {count} records synced successfully")

    except Exception as e:
        conn.rollback()
        print(f"❌ {ticker}: Sync failed - {str(e)}")

    finally:
        cursor.close()
        conn.close()


def main():
    """메인 함수"""
    if len(sys.argv) < 2:
        print("Usage: python sync_stock_prices.py [full|daily]")
        print("")
        print("  full  : Load full historical data from inception (one-time setup)")
        print("  daily : Update recent 5 days (for daily scheduled sync)")
        sys.exit(1)

    mode = sys.argv[1].lower()

    print("=" * 60)
    print("📈 Stock Price Sync - Yahoo Finance to MariaDB")
    print("=" * 60)
    print(f"Mode: {mode.upper()}")
    print(f"Tickers: {', '.join(TICKERS)}")
    print(f"Database: {DB_HOST}:{DB_PORT}/{DB_NAME}")
    print("=" * 60)
    print("")

    if mode == "full":
        # 전체 히스토리컬 데이터 로드 (초기 실행)
        print("🚀 Starting FULL historical data sync...")
        print("⚠️  This may take a few minutes for all tickers")
        print("")

        for ticker in TICKERS:
            sync_ticker(ticker, days_back=None)
            print("")  # 종목 간 구분선

    elif mode == "daily":
        # 최근 5일만 업데이트 (일일 스케줄)
        print("📅 Starting DAILY update (last 5 days)...")
        print("")

        for ticker in TICKERS:
            sync_ticker(ticker, days_back=5)

    else:
        print(f"❌ Invalid mode: {mode}")
        print("Usage: python sync_stock_prices.py [full|daily]")
        sys.exit(1)

    print("")
    print("=" * 60)
    print("✨ Sync completed!")
    print("=" * 60)


if __name__ == "__main__":
    main()
