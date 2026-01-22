package com.ckgod.kis.response

import com.ckgod.domain.model.AssetItem
import com.ckgod.domain.model.AssetType
import com.ckgod.domain.model.TotalAsset
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class KisTotalAssetResponse(
    @SerialName("rt_cd") val returnCode: String,
    @SerialName("msg_cd") val messageCode: String,
    @SerialName("msg1") val message: String,
    @SerialName("output1") val output1: List<Output1>,
    @SerialName("output2") val output2: Output2
) {
    @Serializable
    data class Output1(
        @SerialName("pchs_amt") val purchaseAmount: String,          // 매입금액
        @SerialName("evlu_amt") val evaluationAmount: String,        // 평가금액
        @SerialName("evlu_pfls_amt") val evaluationProfitLossAmount: String,  // 평가손익금액
        @SerialName("crdt_lnd_amt") val creditLoanAmount: String,    // 신용대출금액
        @SerialName("real_nass_amt") val realNetAssetAmount: String, // 실제순자산금액
        @SerialName("whol_weit_rt") val wholeWeightRate: String      // 전체비중율
    )

    @Serializable
    data class Output2(
        @SerialName("pchs_amt_smtl") val purchaseAmountTotal: String,              // 매입금액합계
        @SerialName("nass_tot_amt") val netAssetTotalAmount: String,               // 순자산총금액
        @SerialName("loan_amt_smtl") val loanAmountTotal: String,                  // 대출금액합계
        @SerialName("evlu_pfls_amt_smtl") val evaluationProfitLossAmountTotal: String,  // 평가손익금액합계
        @SerialName("evlu_amt_smtl") val evaluationAmountTotal: String,            // 평가금액합계
        @SerialName("tot_asst_amt") val totalAssetAmount: String,                  // 총자산금액
        @SerialName("tot_lnda_tot_ulst_lnda") val totalLoanTotalUnlistedLoan: String,  // 총대출금액총융자대출금액
        @SerialName("cma_auto_loan_amt") val cmaAutoLoanAmount: String,            // CMA자동대출금액
        @SerialName("tot_mgln_amt") val totalMarginLoanAmount: String,             // 총담보대출금액
        @SerialName("stln_evlu_amt") val stockLoanEvaluationAmount: String,        // 대주평가금액
        @SerialName("crdt_fncg_amt") val creditFinancingAmount: String,            // 신용융자금액
        @SerialName("ocl_apl_loan_amt") val oclApplicationLoanAmount: String,      // OCL_APL대출금액
        @SerialName("pldg_stup_amt") val pledgeSetupAmount: String,                // 질권설정금액
        @SerialName("frcr_evlu_tota") val foreignCurrencyEvaluationTotal: String,  // 외화평가총액
        @SerialName("tot_dncl_amt") val totalDepositAmount: String,                // 총예수금액
        @SerialName("cma_evlu_amt") val cmaEvaluationAmount: String,               // CMA평가금액
        @SerialName("dncl_amt") val depositAmount: String,                         // 예수금액
        @SerialName("tot_sbst_amt") val totalSubstituteAmount: String,             // 총대용금액
        @SerialName("thdt_rcvb_amt") val todayReceivableAmount: String,            // 당일미수금액
        @SerialName("ovrs_stck_evlu_amt1") val overseasStockEvaluationAmount: String,  // 해외주식평가금액1
        @SerialName("ovrs_bond_evlu_amt") val overseasBondEvaluationAmount: String,    // 해외채권평가금액
        @SerialName("mmf_cma_mgge_loan_amt") val mmfCmaMortgageLoanAmount: String,     // MMFCMA담보대출금액
        @SerialName("sbsc_dncl_amt") val subscriptionDepositAmount: String,        // 청약예수금액
        @SerialName("pbst_sbsc_fnds_loan_use_amt") val publicSubscriptionFundsLoanUseAmount: String,  // 공모주청약자금대출사용금액
        @SerialName("etpr_crdt_grnt_loan_amt") val enterpriseCreditGrantLoanAmount: String  // 기업신용공여대출금액
    )

    fun toDomain(): TotalAsset {
        val assetTypes = AssetType.entries.toTypedArray()

        val assets = output1.mapIndexed { index, item ->
            AssetItem(
                type = assetTypes[index],
                purchaseAmount = item.purchaseAmount.toDoubleOrNull() ?: 0.0,
                evaluationAmount = item.evaluationAmount.toDoubleOrNull() ?: 0.0,
                evaluationProfitLoss = item.evaluationProfitLossAmount.toDoubleOrNull() ?: 0.0,
                creditLoanAmount = item.creditLoanAmount.toDoubleOrNull() ?: 0.0,
                realNetAssetAmount = item.realNetAssetAmount.toDoubleOrNull() ?: 0.0,
                wholeWeightRate = item.wholeWeightRate.toDoubleOrNull() ?: 0.0
            )
        }

        return TotalAsset(
            assets = assets
        )
    }
}