package com.ckgod.domain.usecase

import com.ckgod.domain.model.InvestmentStatus
import com.ckgod.domain.repository.InvestmentStatusRepository
import com.ckgod.domain.repository.StockRepository

/**
 * 주문 생성 UseCase
 *
 * 역할:
 * - ticker == null: 전체 종목 주문 생성 (Job용)
 * - ticker != null: 단일 종목 주문 생성 (API/백테스트용)
 */
class GenerateOrdersUseCase(
    private val stockRepository: StockRepository,
    private val investmentStatusRepository: InvestmentStatusRepository
) {
    /**
     * 주문 생성
     *
     * @param ticker 종목 코드 (null이면 전체 종목)
     * @return 주문 생성 결과 리스트
     */
    suspend operator fun invoke(ticker: String? = null): List<OrderResult> {
        // 대상 종목 결정
        val targets = if (ticker != null) {
            val status = investmentStatusRepository.get(ticker)
            if (status != null) listOf(status) else emptyList()
        } else {
            investmentStatusRepository.findAll()
        }

        // 각 종목 주문 생성
        return targets.mapNotNull { status ->
            try {
                generateSingle(status.ticker)
            } catch (e: Exception) {
                null // 실패한 종목은 제외
            }
        }
    }

    /**
     * 단일 종목 주문 생성
     */
    private suspend fun generateSingle(ticker: String): OrderResult? {
        // 1. 현재 상태 조회
        val currentStatus = investmentStatusRepository.get(ticker) ?: return null

        // 2. 한투 API - 현재가 조회
        val marketPrice = stockRepository.getCurrentPrice(ticker)
        val currentPrice = marketPrice.price.toDoubleOrNull() ?: 0.0

        // 3. 주문 가격 계산
        val buyPrice = currentPrice * (1.0 + currentStatus.targetRate / 100.0)
        val sellPrice = if (currentStatus.avgPrice > 0) {
            currentStatus.avgPrice * (1.0 + currentStatus.targetRate / 100.0)
        } else {
            0.0
        }

        // 4. 매수 수량 계산
        val buyQuantity = (currentStatus.oneTimeAmount / buyPrice).toInt()

        // 5. DB 업데이트 (주문 가격 저장)
        val updatedStatus = currentStatus.updateOrderPrices(currentPrice)
        investmentStatusRepository.save(updatedStatus)

        // 6. 주문 정보 반환
        return OrderResult(
            ticker = ticker,
            currentPrice = currentPrice,
            buyPrice = buyPrice,
            buyQuantity = buyQuantity,
            sellPrice = sellPrice,
            targetRate = currentStatus.targetRate,
            updatedStatus = updatedStatus
        )
    }

    /**
     * 주문 생성 결과
     */
    data class OrderResult(
        val ticker: String,
        val currentPrice: Double,
        val buyPrice: Double,
        val buyQuantity: Int,
        val sellPrice: Double,
        val targetRate: Double,
        val updatedStatus: InvestmentStatus
    )
}
