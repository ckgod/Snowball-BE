package com.ckgod.domain.stock

data class Stock(
    /**
     * 단축 코드
     */
    val shortCode: String,

    /**
     * 표준 코드
     */
    val standardCode: String,

    /**
     * 한글 종목명
     */
    val name: String,

    /**
     * 증권 그룹 구분 코드
     *
     * ST:주권, MF:증권투자회사, RT:부동산투자회사
     *
     * SC:선박투자회사, IF:사회간접자본투융자회사, DR:주식예탁증서
     *
     * EW:ELW, EF:ETF, SW:신주인수권증권
     *
     * SR:신주인수권증서, BC:수익증권, FE:해외ETF, FS:외국주권
     */
    val groupCode: String,

    /**
     * 시가총액 규모 구분 코드 유가
     * (0: 제외, 1: 대, 2: 중, 3: 소)
     */
    val marketCapScale: String,

    /**
     * 지수업종대분류
     */
    val sectorLarge: String,

    /**
     * 기준가
     */
    val basePrice: Long?,

    /**
     * 거래정지 여부
     */
    val isTradingHalted: Boolean,

    /**
     * 관리종목 여부
     */
    val isManaged: Boolean,

    /**
     * 시장경고 코드
     * (00: 정상, 01: 주의, 02: 투자경고, 03: 투자위험)
     */
    val marketWarning: String,
)
