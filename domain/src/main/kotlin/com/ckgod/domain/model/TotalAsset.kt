package com.ckgod.domain.model


data class TotalAsset(
    val assets: List<AssetItem>
)

data class AssetItem(
    val type: AssetType,
    val purchaseAmount: Double,              // 매입금액
    val evaluationAmount: Double,            // 평가금액
    val evaluationProfitLoss: Double,        // 평가손익금액
    val creditLoanAmount: Double,            // 신용대출금액
    val realNetAssetAmount: Double,          // 실제순자산금액
    val wholeWeightRate: Double              // 전체비중율
)

enum class AssetType(val label: String) {
    STOCKS("주식"),
    FUND_MMW("펀드/MMW"),
    IMA("IMA"),
    BONDS("채권"),
    ELS_DLS("ELS/DLS"),
    WRAP("WRAP"),
    TRUST("신탁"),
    RP_PROMISSORY_NOTE("RP/발행어음"),
    OVERSEAS_STOCKS("해외주식"),
    OVERSEAS_BONDS("해외채권"),
    GOLD_SPOT("금현물"),
    CD_CP("CD/CP"),
    ELECTRONIC_SHORT_TERM_BOND("전자단기사채"),
    OTHER_COMPANY_PRODUCT("타사상품"),
    FOREIGN_ELECTRONIC_SHORT_TERM_BOND("외화전자단기사채"),
    FOREIGN_ELS_DLS("외화 ELS/DLS"),
    FOREIGN_CURRENCY("외화"),
    DEPOSIT("예수금"),
    SUBSCRIPTION_DEPOSIT("청약자예수금"),
    TOTAL("합계")
}
