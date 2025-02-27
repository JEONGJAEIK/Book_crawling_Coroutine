package com.example.demo.service

import com.example.demo.dto.BookDTO
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.microsoft.playwright.Browser
import com.microsoft.playwright.BrowserType
import com.microsoft.playwright.Page
import com.microsoft.playwright.Playwright
import com.microsoft.playwright.options.LoadState
import com.microsoft.playwright.options.WaitUntilState
import jakarta.transaction.Transactional
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import org.springframework.stereotype.Service
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.CountDownLatch
import kotlin.concurrent.thread

@Service
class CrawlingService(private val bookService: BookService) {

    private val latch = CountDownLatch(2)
    private val completionLatch = CountDownLatch(4)
    private var bookLinks: MutableList<String> = mutableListOf()
    private var bestSellers: MutableList<BookDTO> = mutableListOf()

    @Transactional
    fun main() {
        repeat(4) { threadIndex ->
            thread { startCrawling(threadIndex) }
        }
    }

    fun startCrawling(threadIndex: Int) {
        printWithThread("크롤링 시작", threadIndex)
        runBlocking {
            withContext(Dispatchers.IO) {
                val playwright = Playwright.create()
                val browser = playwright.chromium().launch(BrowserType.LaunchOptions().setHeadless(true))
                getBookLinks(browser, threadIndex)
                scrapeBookData(browser, bookLinks, threadIndex)

                printWithThread("📌 bestSellers 크기: ${bestSellers.size}", threadIndex)
                completionLatch.countDown()

                browser.close()
                playwright.close()
                printWithThread("크롤링 완료", threadIndex)
            }
        }
        if (threadIndex == 0) {
            completionLatch.await()
            printWithThread("데이터 저장 시작", threadIndex)
            bookService.saveBestsellers(bestSellers)
            printWithThread("데이터 저장 완료", threadIndex)
        }
    }




    private fun getBookLinks(browser: Browser, threadIndex: Int) {
        val page = browser.newPage()

        if (threadIndex == 1 || threadIndex == 2) {
            val bestSellerPage = threadIndex

            page.navigate("https://store.kyobobook.co.kr/bestseller/realtime?page=$bestSellerPage&per=50")
            page.waitForLoadState(LoadState.NETWORKIDLE)
            page.waitForSelector("div.ml-4 > .prod_link")

            val links = page.locator("div.ml-4 > .prod_link").all()
            bookLinks.addAll(links.mapNotNull { it.getAttribute("href") })
            printWithThread("${threadIndex}페이지 ${bookLinks.size}개의 도서 링크 수집 완료", threadIndex)

            latch.countDown()
        }

        page.close()
        latch.await()
    }


    private fun scrapeBookData(browser: Browser, bookLinks: List<String>, threadIndex: Int) {
        runBlocking {
            bookLinks.mapIndexedNotNull { ranking, bookLink ->
                if (ranking % 4 == threadIndex) {
                    launch {
                        val page = browser.newPage()
                        printWithThread("${ranking}, ${bookLink} 접근 시작", threadIndex)
                        page.navigate(bookLink, Page.NavigateOptions().setWaitUntil(WaitUntilState.COMMIT))
                        printWithThread("${ranking}, ${bookLink} 접근 완료", threadIndex)

                        val data = page.evaluate(
                            """ () => JSON.stringify({
                            title: document.querySelector('.prod_title')?.innerText?.trim() || '',
                            author: document.querySelector('.author')?.innerText?.trim() || '',
                            isbn: document.querySelector('#scrollSpyProdInfo .product_detail_area.basic_info table tbody tr:nth-child(1) td')?.innerText?.trim() || '',
                            description: document.querySelector('.intro_bottom')?.innerText?.trim() || '',
                            image: document.querySelector('.portrait_img_box img')?.getAttribute('src') || ''
                        }) """
                        ).toString()

                        val type = object : TypeToken<Map<String, String>>() {}.type
                        val json: Map<String, String> = Gson().fromJson(data, type)

                        page.close()
                        printWithThread("${ranking} 데이터 파싱 완료", threadIndex)

                        if (json.values.all { it.isBlank() }) {
                            null
                        } else {
                            bestSellers.add(BookDTO(
                                id = 0L,
                                title = json["title"] ?: "",
                                author = json["author"] ?: "",
                                description = json["description"] ?: "",
                                image = json["image"] ?: "",
                                isbn = json["isbn"] ?: "",
                                ranking = ranking + 1,
                                favoriteCount = 0
                            ))
                        }
                    }
                } else {
                    null
                }
            }
        }
    }


    private fun printWithThread(str: Any, threadIndex: Int) {
        val time = System.currentTimeMillis()
        val formattedTime = SimpleDateFormat("mm분 ss초 SSS", Locale.getDefault()).format(Date(time))

        println("Thread[$threadIndex] $str $formattedTime")
    }
}
