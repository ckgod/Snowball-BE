package com.ckgod

import io.ktor.server.application.*
import io.ktor.server.engine.embeddedServer
import io.ktor.server.netty.Netty

fun main() {
    // Netty 엔진을 사용해서 8080 포트로 서버를 띄움
    embeddedServer(Netty, port = 8080, host = "0.0.0.0", module = Application::module).start(wait = true)
}

fun Application.module() {
    // 초기화 코드
    configureSerialization() // JSON 설정 로드
    configureRouting()       // URL 라우팅 로드
}
