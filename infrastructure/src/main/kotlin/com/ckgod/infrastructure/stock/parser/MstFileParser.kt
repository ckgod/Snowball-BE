package com.ckgod.infrastructure.stock.parser

import com.ckgod.domain.stock.Stock
import java.io.BufferedReader
import java.io.File
import java.io.FileInputStream
import java.io.InputStreamReader
import java.nio.charset.Charset

object MstFileParser {
    private val CP949 = Charset.forName("CP949")

    private val part2FieldSpecs = listOf(
        // [0~4] 그룹코드(2), 시가총액규모(1), 지수업종대분류(4), 지수업종중분류(4), 지수업종소분류(4)
        2, 1, 4, 4, 4,
        // [5~9] 제조업(1), 저유동성(1), 지배구조지수종목(1), KOSPI200섹터업종(1), KOSPI100(1)
        1, 1, 1, 1, 1,
        // [10~14] KOSPI50(1), KRX(1), ETP(1), ELW발행(1), KRX100(1)
        1, 1, 1, 1, 1,
        // [15~19] KRX자동차(1), KRX반도체(1), KRX바이오(1), KRX은행(1), SPAC(1)
        1, 1, 1, 1, 1,
        // [20~24] KRX에너지화학(1), KRX철강(1), 단기과열(1), KRX미디어통신(1), KRX건설(1)
        1, 1, 1, 1, 1,
        // [25~29] Non1(1), KRX증권(1), KRX선박(1), KRX섹터_보험(1), KRX섹터_운송(1)
        1, 1, 1, 1, 1,
        // [30~34] SRI(1), 기준가(9), 매매수량단위(5), 시간외수량단위(5), 거래정지(1)
        1, 9, 5, 5, 1,
        // [35~39] 정리매매(1), 관리종목(1), 시장경고(2), 경고예고(1), 불성실공시(1)
        1, 1, 2, 1, 1,
        // [40~44] 우회상장(1), 락구분(2), 액면변경(2), 증자구분(2), 증거금비율(3)
        1, 2, 2, 2, 3,
        // [45~49] 신용가능(1), 신용기간(3), 전일거래량(12), 액면가(12), 상장일자(8)
        1, 3, 12, 12, 8,
        // [50~54] 상장주수(15), 자본금(21), 결산월(2), 공모가(7), 우선주(1)
        15, 21, 2, 7, 1,
        // [55~59] 공매도과열(1), 이상급등(1), KRX300(1), KOSPI(1), 매출액(9)
        1, 1, 1, 1, 9,
        // [60~64] 영업이익(9), 경상이익(9), 당기순이익(5), ROE(9), 기준년월(8)
        9, 9, 5, 9, 8,
        // [65~69] 시가총액(9), 그룹사코드(3), 회사신용한도초과(1), 담보대출가능(1), 대주가능(1)
        9, 3, 1, 1, 1
    )

    private const val PART_LENGTH = 227

    fun parse(file: File): List<Stock> {
        val stockData = mutableListOf<Stock>()

        BufferedReader(InputStreamReader(FileInputStream(file), CP949)).use { reader ->
            reader.forEachLine { line ->
                try {
                    if (line.length <= PART_LENGTH) return@forEachLine

                    val part1 = line.substring(0, line.length - PART_LENGTH)
                    val part2 = line.substring(line.length - PART_LENGTH)

                    val shortCode = part1.substring(0, 9).trim()
                    val standardCode = part1.substring(9, 21).trim()
                    val name = part1.substring(21).trim()

                    val part2Values = parseFixedFieldString(part2, part2FieldSpecs)

                    stockData.add(
                        Stock(
                            shortCode = shortCode,
                            standardCode = standardCode,
                            name = name,
                            groupCode = part2Values.getOrNull(0) ?: "",
                            marketCapScale = part2Values.getOrNull(1) ?: "",
                            sectorLarge = part2Values.getOrNull(2) ?: "",
                            basePrice = part2Values.getOrNull(31)?.toLongOrNull(),
                            isTradingHalted = parseYN(part2Values.getOrNull(34)),
                            isManaged = parseYN(part2Values.getOrNull(36)),
                            marketWarning = part2Values.getOrNull(37) ?: ""
                        )
                    )
                } catch (e: Exception) {
                    println("Error parsing $line: ${e.message}")
                }
            }
        }

        return stockData
    }

    private fun parseYN(value: String?): Boolean {
        return value == "Y"
    }

    private fun parseFixedFieldString(source: String, widths: List<Int>): List<String> {
        val result = mutableListOf<String>()
        var currentIndex = 0

        for (width in widths) {
            if (currentIndex + width > source.length) break
            val value = source.substring(currentIndex, currentIndex + width)
            result.add(value)
            currentIndex += width
        }
        return result
    }
}
