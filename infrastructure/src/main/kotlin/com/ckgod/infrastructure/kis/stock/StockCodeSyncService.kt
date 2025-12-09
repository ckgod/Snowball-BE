package com.ckgod.infrastructure.kis.stock

import com.ckgod.domain.stock.StockRepository
import com.ckgod.infrastructure.kis.stock.downloader.StockCodeDownloader
import com.ckgod.infrastructure.kis.stock.parser.MstFileParser
import java.io.File

class StockCodeSyncService(
    private val stockRepository: StockRepository,
    private val downloader: StockCodeDownloader,
    private val kospiMasterUrl: String,
    private val outputDir: File
) {
    suspend fun syncKospiStocks() {
        val mstFile = downloader.downloadAndUnzip(kospiMasterUrl, outputDir)
        val stocks = MstFileParser.parse(mstFile)

        stockRepository.deleteAll()
        stockRepository.saveAll(stocks)

        println("KOSPI 종목 ${stocks.size}개 동기화 완료")
    }
}
