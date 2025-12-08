package com.ckgod.infrastructure.stock.parser

import org.junit.Test
import java.io.File

class MstFileParserTest {

    @Test
    fun `mst 파일 전체 파싱 테스트`() {
        val resourcePath = this::class.java.classLoader.getResource("kospi_code.mst")
        val mstFile = if (resourcePath != null) {
            File(resourcePath.toURI())
        } else {
            return
        }

        if (!mstFile.exists()) {
            println("파일이 없습니다: ${mstFile.absolutePath}")
            return
        }

        println("=== MST 파일 파싱 테스트 ===")
        println("파일 경로: ${mstFile.absolutePath}")
        println("파일 크기: ${mstFile.length()} bytes")
        println()

        val stocks = MstFileParser.parse(mstFile)

        println("총 ${stocks.size}개 종목 파싱 완료")
        println()
        println("=".repeat(120))

        stocks.forEachIndexed { index, stock ->
            println("[$index] 종목 정보:")
            println("  단축코드      : ${stock.shortCode}")
            println("  표준코드      : ${stock.standardCode}")
            println("  한글명        : ${stock.name}")
            println("  그룹코드      : ${stock.groupCode}")
            println("  시가총액규모  : ${stock.marketCapScale}")
            println("  지수업종대분류: ${stock.sectorLarge}")
            println("  기준가        : ${stock.basePrice ?: "null"}")
            println("-".repeat(120))
        }

        println()
        println("=== 통계 ===")
        println("총 종목 수: ${stocks.size}")
        println("기준가 있는 종목: ${stocks.count { it.basePrice != null }}")
    }
}
