package com.ckgod.kis.websokets

/**
 * @see <a href="https://apiportal.koreainvestment.com/apiservice-apiservice?/tryitout/H0GSCNI0">KIS 해외 실시간 체결 통보</>
 */
data class RealtimeExecutionNotification(
    val orderNumber: String,            // 주문 번호
    val sellBuyType: String,            // 01: 매도, 02: 매수, 03:전매도, 04: 환매수
    val correctionType: String,         // 0:정상, 1:정정, 2:취소
    val orderType2: String,             // 1:시장가 2:지정자 6:단주시장가 7:단주지정가 A:MOO B:LOO C:MOC D:LOC
    val stockShortCode: String,         // 주식 단축 종목 코드
    val executionQuantity: String,      // 체결통보인 경우 해당 위치에 체결수량이 출력 ex: 0000000002
    val executionPrice: String,         // 000292900 -> 29.29달러 4번째 자리에 소수점을 찍어 해석한다.
    val stockExecutionTime: String,     // 체결 시간
    val rejectionFlag: String,          // 거부여부 0:정상 1:거부
    val executionFlag: String,          // 체결여부 1:주문,정정,취소,거부 2:체결
    val branchNumber: String,           // 접수여부 1:주문접수 2:확인 3:취소(FOK/IOC)
    val orderQuantity: String,          // 주문 통보인 경우 미출력, 체결 통보의 경우 해당 위치에 주문수량이 출력
    val executionStockName: String,     // 체결 종목명
    val overseasStockType: String,      // 해외 종목 구분  4:홍콩(HKD) 5:상해B(USD) 6:NASDAQ 7:NYSE 8:AMEX 9:OTCB C:홍콩(CNY) A:상해A(CNY) B:심천B(HKD) D:도쿄 E:하노이 F:호치민
) {

    companion object {
        fun from(fields: List<String>): RealtimeExecutionNotification {
            require(fields.size >= 24) { "필드 개수가 부족합니다. 필요: 24개, 실제: ${fields.size}개" }

            return RealtimeExecutionNotification(
                orderNumber = fields[2],
                sellBuyType = fields[4],
                correctionType = fields[5],
                orderType2 = fields[6],
                stockShortCode = fields[7],
                executionQuantity = fields[8],
                executionPrice = fields[9],
                stockExecutionTime = fields[10],
                rejectionFlag = fields[11],
                executionFlag = fields[12],
                branchNumber = fields[14],
                orderQuantity = fields[15],
                executionStockName = fields[17],
                overseasStockType = fields[18],
            )
        }
    }
}