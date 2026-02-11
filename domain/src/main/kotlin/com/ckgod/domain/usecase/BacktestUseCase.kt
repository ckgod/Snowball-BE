package com.ckgod.domain.usecase

import com.ckgod.domain.backtest.BacktestAccountRepository
import com.ckgod.domain.backtest.BacktestInvestmentStatusRepository
import com.ckgod.domain.backtest.BacktestStockRepository
import com.ckgod.domain.backtest.BacktestTradeHistoryRepository
import com.ckgod.domain.model.*
import com.ckgod.domain.repository.StockPriceHistoryRepository
import com.ckgod.domain.utils.roundTo2Decimal
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.withContext
import org.slf4j.LoggerFactory

class BacktestUseCase(
    private val stockPriceHistoryRepository: StockPriceHistoryRepository
) {
    private val logger = LoggerFactory.getLogger(BacktestUseCase::class.java)
    private val mutex = Mutex()

    suspend fun run(request: BacktestRequest): BacktestResult {
        // 어차피 나 혼자 쓰니까 mutex로 동시 실행 제어
        if (!mutex.tryLock()) {
            throw IllegalStateException("백테스트가 이미 실행 중입니다")
        }
        try {
            return withContext(Dispatchers.Default) {
                runInternal(request)
            }
        } finally {
            mutex.unlock()
        }
    }

    private suspend fun runInternal(request: BacktestRequest): BacktestResult {
        logger.info("[Backtest] 시작 - ${request.ticker} (${request.startDate} ~ ${request.endDate})")

        // 1. 히스토리 가격 데이터 조회 (유일한 DB 호출)
        val priceData = stockPriceHistoryRepository.getPriceRange(
            request.ticker, request.startDate, request.endDate
        )
        if (priceData.isEmpty()) {
            throw IllegalArgumentException("해당 기간의 가격 데이터가 없습니다: ${request.ticker} (${request.startDate} ~ ${request.endDate})")
        }
        logger.info("[Backtest] 가격 데이터 ${priceData.size}일 조회 완료")

        // 2. 가격 데이터를 Map으로 변환 (이후 DB 호출 없음)
        val priceDataMap = priceData.associateBy { it.date }

        // 3. Backtest 전용 Repository 생성
        val backtestStockRepo = BacktestStockRepository(priceDataMap)
        val backtestAccountRepo = BacktestAccountRepository(request.initialCapital)
        val backtestStatusRepo = BacktestInvestmentStatusRepository()
        val backtestHistoryRepo = BacktestTradeHistoryRepository()

        // 4. 초기 InvestmentStatus 설정
        val initialStatus = InvestmentStatus(
            ticker = request.ticker,
            fullName = request.ticker,
            totalInvested = 0.0,
            oneTimeAmount = request.oneTimeAmount,
            initialCapital = request.initialCapital,
            division = request.division,
            avgPrice = 0.0,
            quantity = 0,
            targetRate = request.targetRate,
            realizedTotalProfit = 0.0,
            updatedAt = request.startDate.toString(),
            starMode = request.starMode
        )
        backtestStatusRepo.initialize(initialStatus)

        // 5. GenerateOrdersUseCase 생성 (backtest repos 주입)
        val generateOrdersUseCase = GenerateOrdersUseCase(
            stockRepository = backtestStockRepo,
            accountRepository = backtestAccountRepo,
            investmentStatusRepository = backtestStatusRepo,
            tradeHistoryRepository = backtestHistoryRepo
        )

        // 6. 시뮬레이션 루프
        val dailySnapshots = mutableListOf<DailySnapshot>()
        val backtestTrades = mutableListOf<BacktestTrade>()
        var totalOrders = 0
        var filledOrders = 0

        for (i in priceData.indices) {
            val today = priceData[i]

            // 현재 날짜 설정
            backtestStockRepo.currentDate = today.date

            // 전일 주문 체결 시뮬레이션 (첫 날 제외)
            val pendingOrders = backtestHistoryRepo.findPendingOrders()
            if (pendingOrders.isNotEmpty()) {
                val fillResults = simulateFills(pendingOrders, today)
                fillResults.forEach { (history, filledPrice) ->
                    val currentStatus = backtestStatusRepo.get(request.ticker)!!

                    // 매수 체결 시 현금 잔고 확인
                    if (history.orderSide == OrderSide.BUY) {
                        val cost = (history.orderQuantity * filledPrice).roundTo2Decimal()
                        if (!backtestAccountRepo.deductCash(cost)) {
                            logger.debug("[Backtest] [${today.date}] 잔고 부족으로 매수 체결 스킵: ${history.orderNo} (필요: $cost, 잔고: ${backtestAccountRepo.availableCash})")
                            backtestHistoryRepo.updateOrderStatus(
                                orderNo = history.orderNo,
                                status = OrderStatus.CANCELED,
                                filledQuantity = 0,
                                filledPrice = 0.0,
                                filledTime = java.time.LocalDateTime.now()
                            )
                            return@forEach
                        }
                    }

                    // 체결 상태 업데이트
                    backtestHistoryRepo.updateOrderStatus(
                        orderNo = history.orderNo,
                        status = OrderStatus.FILLED,
                        filledQuantity = history.orderQuantity,
                        filledPrice = filledPrice,
                        filledTime = java.time.LocalDateTime.now()
                    )

                    // 포트폴리오 업데이트
                    val holding = backtestAccountRepo.getHolding(request.ticker)
                    val currentQty = holding?.quantity?.toDoubleOrNull()?.toInt() ?: 0
                    val currentAvgPrice = holding?.avgPrice?.toDoubleOrNull() ?: 0.0
                    val currentInvested = holding?.investedAmount?.toDoubleOrNull() ?: 0.0

                    var dailyProfit = 0.0

                    if (history.orderSide == OrderSide.BUY) {
                        val newQty = currentQty + history.orderQuantity
                        val newInvested = currentInvested + (history.orderQuantity * filledPrice)
                        val newAvgPrice = if (newQty > 0) (newInvested / newQty).roundTo2Decimal() else 0.0
                        backtestAccountRepo.updateHolding(request.ticker, newQty, newAvgPrice, newInvested)
                    } else {
                        val newQty = currentQty - history.orderQuantity
                        val newInvested = if (newQty > 0) currentAvgPrice * newQty else 0.0
                        dailyProfit = (filledPrice - currentAvgPrice) * history.orderQuantity
                        backtestAccountRepo.updateHolding(
                            request.ticker, newQty, currentAvgPrice, newInvested
                        )
                        // 매도 대금 현금에 가산
                        backtestAccountRepo.addCash((history.orderQuantity * filledPrice).roundTo2Decimal())
                    }

                    // InvestmentStatus 업데이트
                    val updatedHolding = backtestAccountRepo.getHolding(request.ticker)
                    val updatedStatus = currentStatus.updateFromAccount(
                        name = request.ticker,
                        totalInvested = updatedHolding?.investedAmount?.toDoubleOrNull() ?: 0.0,
                        avgPrice = updatedHolding?.avgPrice?.toDoubleOrNull() ?: 0.0,
                        quantity = updatedHolding?.quantity?.toDoubleOrNull()?.toInt() ?: 0,
                        dailyProfit = dailyProfit
                    )
                    backtestStatusRepo.save(updatedStatus)

                    filledOrders++
                    backtestTrades.add(
                        BacktestTrade(
                            date = today.date,
                            side = history.orderSide,
                            type = history.orderType,
                            orderPrice = history.orderPrice,
                            filledPrice = filledPrice,
                            quantity = history.orderQuantity,
                            tValue = currentStatus.tValue,
                            crashRate = history.crashRate
                        )
                    )
                }

                // 미체결 주문 취소
                pendingOrders.forEach { history ->
                    val current = backtestHistoryRepo.findByOrderNo(history.orderNo)
                    if (current?.status == OrderStatus.PENDING) {
                        backtestHistoryRepo.updateOrderStatus(
                            orderNo = history.orderNo,
                            status = OrderStatus.CANCELED,
                            filledQuantity = 0,
                            filledPrice = 0.0,
                            filledTime = java.time.LocalDateTime.now()
                        )
                    }
                }
            }

            // 주문 생성 (자금 소진 전까지)
            val currentStatus = backtestStatusRepo.get(request.ticker)
            if (currentStatus != null && currentStatus.phase != TradePhase.EXHAUSTED) {
                try {
                    val results = generateOrdersUseCase(request.ticker)
                    results.forEach { result ->
                        totalOrders += result.buyOrders.size + result.sellOrders.size
                    }
                } catch (e: Exception) {
                    logger.debug("[Backtest] [${today.date}] 주문 생성 스킵: ${e.message}")
                }
            }

            // 일별 스냅샷 기록
            val snapshotStatus = backtestStatusRepo.get(request.ticker)
            val snapshotHolding = backtestAccountRepo.getHolding(request.ticker)
            val qty = snapshotHolding?.quantity?.toDoubleOrNull()?.toInt() ?: 0
            dailySnapshots.add(
                DailySnapshot(
                    date = today.date,
                    closePrice = today.close.roundTo2Decimal(),
                    quantity = qty,
                    avgPrice = snapshotHolding?.avgPrice?.toDoubleOrNull() ?: 0.0,
                    tValue = snapshotStatus?.tValue ?: 0.0,
                    starPercent = snapshotStatus?.starPercent ?: 0.0,
                    totalInvested = snapshotHolding?.investedAmount?.toDoubleOrNull() ?: 0.0,
                    portfolioValue = (qty * today.close).roundTo2Decimal(),
                    availableCash = backtestAccountRepo.availableCash.roundTo2Decimal()
                )
            )
        }

        // 7. 결과 집계
        val finalStatus = backtestStatusRepo.get(request.ticker)
        val finalHolding = backtestAccountRepo.getHolding(request.ticker)
        val finalQty = finalHolding?.quantity?.toDoubleOrNull()?.toInt() ?: 0
        val finalAvgPrice = finalHolding?.avgPrice?.toDoubleOrNull() ?: 0.0
        val finalInvested = finalHolding?.investedAmount?.toDoubleOrNull() ?: 0.0
        val lastPrice = priceData.last().close
        val unrealizedProfit = if (finalQty > 0) ((lastPrice - finalAvgPrice) * finalQty).roundTo2Decimal() else 0.0
        val realizedProfit = finalStatus?.realizedTotalProfit ?: 0.0
        val availableCash = backtestAccountRepo.availableCash.roundTo2Decimal()
        val finalPortfolioValue = (availableCash + (finalQty * lastPrice)).roundTo2Decimal()
        val totalReturn = if (request.initialCapital > 0) {
            ((finalPortfolioValue - request.initialCapital) / request.initialCapital * 100).roundTo2Decimal()
        } else 0.0

        val summary = BacktestSummary(
            totalTradingDays = priceData.size,
            totalOrders = totalOrders,
            filledOrders = filledOrders,
            totalInvested = finalInvested,
            finalQuantity = finalQty,
            finalAvgPrice = finalAvgPrice,
            finalTValue = finalStatus?.tValue ?: 0.0,
            realizedProfit = realizedProfit.roundTo2Decimal(),
            unrealizedProfit = unrealizedProfit,
            availableCash = availableCash,
            finalPortfolioValue = finalPortfolioValue,
            totalReturn = totalReturn
        )

        logger.info("[Backtest] 완료 - 총 거래일: ${priceData.size}, 총 주문: $totalOrders, 체결: $filledOrders, 수익률: $totalReturn%")

        return BacktestResult(
            request = request,
            summary = summary,
            dailySnapshots = dailySnapshots,
            trades = backtestTrades
        )
    }

    /**
     * 주문 체결 시뮬레이션
     *
     * LOC BUY: 종가 <= 주문가 → 종가에 체결
     * LOC SELL: 종가 >= 주문가 → 종가에 체결
     * LIMIT SELL: 고가 >= 주문가 → 주문가에 체결
     * MOC: 종가에 무조건 체결
     */
    private fun simulateFills(
        pendingOrders: List<TradeHistory>,
        dayData: StockPriceHistory
    ): List<Pair<TradeHistory, Double>> {
        val fills = mutableListOf<Pair<TradeHistory, Double>>()

        pendingOrders.forEach { order ->
            val filledPrice = when (order.orderType) {
                OrderType.LOC -> {
                    if (order.orderSide == OrderSide.BUY && dayData.close <= order.orderPrice) {
                        dayData.close
                    } else if (order.orderSide == OrderSide.SELL && dayData.close >= order.orderPrice) {
                        dayData.close
                    } else null
                }
                OrderType.LIMIT -> {
                    if (order.orderSide == OrderSide.BUY && dayData.low <= order.orderPrice) {
                        order.orderPrice
                    } else if (order.orderSide == OrderSide.SELL && dayData.high >= order.orderPrice) {
                        order.orderPrice
                    } else null
                }
                OrderType.MOC -> dayData.close
            }

            if (filledPrice != null) {
                fills.add(order to filledPrice.roundTo2Decimal())
            }
        }

        return fills
    }
}
