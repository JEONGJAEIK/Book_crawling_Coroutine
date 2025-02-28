package com.example.demo.service

import okhttp3.OkHttpClient
import okhttp3.Request

private val client = OkHttpClient()

fun main() {
    getLinks()
}

fun getLinks() {
    val request = Request.Builder()
        .url("https://store.kyobobook.co.kr/bestseller/realtime?page=1&per=50")
        .header(
            "User-Agent",
            "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/133.0.0.0 Safari/537.36"
        )
        .build()

    val response = client.newCall(request).execute()
    println(response.body?.string())
}
