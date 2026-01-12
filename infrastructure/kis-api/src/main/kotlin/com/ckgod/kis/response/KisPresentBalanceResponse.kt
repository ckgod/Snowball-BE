package com.ckgod.kis.response

import com.ckgod.domain.model.HoldingStock
import com.ckgod.domain.model.PresentAccountStatus
import com.ckgod.domain.utils.roundTo2Decimal
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlin.text.ifEmpty

@Serializable
data class KisPresentBalanceResponse(
    @SerialName("rt_cd") val resultCode: String,
    @SerialName("msg_cd") val messageCode: String,
    @SerialName("msg1") val message: String,
    @SerialName("output1") val balanceDetails: List<BalanceDetail> = emptyList(),
    @SerialName("output2") val currencyDetails: List<CurrencyDetail> = emptyList(),
    @SerialName("output3") val balanceSummary: BalanceSummary
) {
    fun toDomain(): PresentAccountStatus {
        val balanceDetail = this.balanceDetails.map { item ->
            HoldingStock(
                ticker = item.productNumber,
                name = item.productName,
                quantity = item.balanceQuantity.ifEmpty { "0" },
                avgPrice = item.averageUnitPrice.ifEmpty { "0.0" },
                investedAmount = item.foreignPurchaseAmount.ifEmpty { "0.0" },
                currentPrice = item.overseasCurrentPrice.ifEmpty { "0.0" },
                profitRate = item.evaluationProfitLossRate.ifEmpty { "0.0" }
            )
        }
        val usdAccount = currencyDetails.firstOrNull()

        val totalBuyingValueUsd = balanceSummary.purchaseAmountTotal.toDouble()
        val totalEvaluateValueUsd = balanceSummary.evaluationAmountTotal.toDouble()
        val totalUsdCash = usdAccount?.foreignDepositAmount?.toDouble() ?: 0.0
        val totalAvailableUsdCash = usdAccount?.foreignWithdrawableAmount?.toDouble() ?: 0.0

        return PresentAccountStatus(
            totalAssetValueUsd = (totalAvailableUsdCash + totalEvaluateValueUsd).roundTo2Decimal(), // foreignDepositAmount + evlu_amt_smtl
            totalBuyingValueUsd = totalBuyingValueUsd.roundTo2Decimal(), // pchs_amt_smtl
            totalEvalValueUsd = totalEvaluateValueUsd.roundTo2Decimal(),  // evlu_amt_smtl
            totalProfitUsd = (totalEvaluateValueUsd - totalBuyingValueUsd).roundTo2Decimal(), // evlu_amt_smtl - pchs_amt_smtl
            totalProfitRate = balanceSummary.evaluationEarningsRate.toDouble().roundTo2Decimal(), // evaluationEarningsRate
            totalCashUsd = totalUsdCash,  // foreignDepositAmount
            orderableCashUsd = totalAvailableUsdCash.roundTo2Decimal(), // foreignWithdrawableAmount
            lockedCashUsd = usdAccount?.foreignBuyMarginAmount?.toDouble()?.roundTo2Decimal() ?: 0.0, // foreignBuyMarginAmount
            holdings = balanceDetail
        )
    }
}

@Serializable
data class BalanceDetail(
    @SerialName("pdno") val productNumber: String,
    @SerialName("prdt_name") val productName: String,
    @SerialName("cblc_qty13") val balanceQuantity: String,
    @SerialName("thdt_buy_ccld_qty1") val todayBuyExecutedQuantity: String,
    @SerialName("thdt_sll_ccld_qty1") val todaySellExecutedQuantity: String,
    @SerialName("ccld_qty_smtl1") val executedQuantityTotal: String,
    @SerialName("ord_psbl_qty1") val orderableQuantity: String,
    @SerialName("frcr_pchs_amt") val foreignPurchaseAmount: String,
    @SerialName("frcr_evlu_amt2") val foreignEvaluationAmount: String,
    @SerialName("evlu_pfls_amt2") val evaluationProfitLossAmount: String,
    @SerialName("evlu_pfls_rt1") val evaluationProfitLossRate: String,
    @SerialName("bass_exrt") val baseExchangeRate: String,
    @SerialName("buy_crcy_cd") val buyCurrencyCode: String,
    @SerialName("ovrs_now_pric1") val overseasCurrentPrice: String,
    @SerialName("avg_unpr3") val averageUnitPrice: String,
    @SerialName("tr_mket_name") val tradingMarketName: String,
    @SerialName("natn_kor_name") val nationKoreanName: String,
    @SerialName("pchs_rmnd_wcrc_amt") val purchaseRemainderWonAmount: String,
    @SerialName("thdt_buy_ccld_frcr_amt") val todayBuyExecutedForeignAmount: String,
    @SerialName("thdt_sll_ccld_frcr_amt") val todaySellExecutedForeignAmount: String,
    @SerialName("unit_amt") val unitAmount: String,
    @SerialName("std_pdno") val standardProductNumber: String,
    @SerialName("prdt_type_cd") val productTypeCode: String,
    @SerialName("scts_dvsn_name") val securitiesDivisionName: String,
    @SerialName("loan_rmnd") val loanRemainder: String,
    @SerialName("loan_dt") val loanDate: String,
    @SerialName("loan_expd_dt") val loanExpirationDate: String,
    @SerialName("ovrs_excg_cd") val overseasExchangeCode: String,
    @SerialName("item_lnkg_excg_cd") val itemLinkageExchangeCode: String
)

@Serializable
data class CurrencyDetail(
    @SerialName("crcy_cd") val currencyCode: String,                        // 통화 코드: USD ...
    @SerialName("crcy_cd_name") val currencyCodeName: String,
    @SerialName("frcr_buy_amt_smtl") val foreignBuyAmountTotal: String,         // 외화 매수금액 합계
    @SerialName("frcr_sll_amt_smtl") val foreignSellAmountTotal: String,        // 외화 매도금액 합계
    @SerialName("frcr_dncl_amt_2") val foreignDepositAmount: String,            // 외화 예수금액
    @SerialName("frst_bltn_exrt") val firstBulletinExchangeRate: String,
    @SerialName("frcr_buy_mgn_amt") val foreignBuyMarginAmount: String,         // 외화 매수 증거금액
    @SerialName("frcr_etc_mgna") val foreignEtcMargin: String,                  // 외화 기타 증거금
    @SerialName("frcr_drwg_psbl_amt_1") val foreignWithdrawableAmount: String,  // 외화 출금 가능 금액
    @SerialName("frcr_evlu_amt2") val withdrawableWonAmount: String,            // 출금 가능 금액 - 원화
    @SerialName("acpl_cstd_crcy_yn") val localCustodyCurrencyYn: String,
    @SerialName("nxdy_frcr_drwg_psbl_amt") val nextDayForeignWithdrawableAmount: String // 익일 출금 가능 금액
)

@Serializable
data class BalanceSummary(
    @SerialName("pchs_amt_smtl") val purchaseAmountTotal: String,               // 매입 금액 합계 - $
    @SerialName("evlu_amt_smtl") val evaluationAmountTotal: String,             // 평가 금액 합계 - $
    @SerialName("evlu_pfls_amt_smtl") val evaluationProfitLossAmountTotal: String, // 평가 손익 합계 - $
    @SerialName("dncl_amt") val depositAmount: String,
    @SerialName("cma_evlu_amt") val cmaEvaluationAmount: String,
    @SerialName("tot_dncl_amt") val totalDepositAmount: String,
    @SerialName("etc_mgna") val etcMargin: String,
    @SerialName("wdrw_psbl_tot_amt") val withdrawableTotalAmount: String,
    @SerialName("frcr_evlu_tota") val foreignEvaluationTotal: String,
    @SerialName("evlu_erng_rt1") val evaluationEarningsRate: String,
    @SerialName("pchs_amt_smtl_amt") val purchaseAmountTotalAmount: String,
    @SerialName("evlu_amt_smtl_amt") val evaluationAmountTotalAmount: String,
    @SerialName("tot_evlu_pfls_amt") val totalEvaluationProfitLossAmount: String,
    @SerialName("tot_asst_amt") val totalAssetAmount: String,
    @SerialName("buy_mgn_amt") val buyMarginAmount: String,
    @SerialName("mgna_tota") val marginTotal: String,
    @SerialName("frcr_use_psbl_amt") val foreignUsableAmount: String,
    @SerialName("ustl_sll_amt_smtl") val unsettledSellAmountTotal: String,
    @SerialName("ustl_buy_amt_smtl") val unsettledBuyAmountTotal: String,
    @SerialName("tot_frcr_cblc_smtl") val totalForeignBalanceTotal: String,
    @SerialName("tot_loan_amt") val totalLoanAmount: String
)
