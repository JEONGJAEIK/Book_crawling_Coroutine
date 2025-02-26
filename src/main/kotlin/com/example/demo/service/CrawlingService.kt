package com.example.demo.service

import com.example.demo.dto.BookDTO
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.microsoft.playwright.Browser
import com.microsoft.playwright.BrowserType
import com.microsoft.playwright.Page
import com.microsoft.playwright.Playwright
import com.microsoft.playwright.options.WaitUntilState
import jakarta.transaction.Transactional
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.runBlocking
import org.springframework.stereotype.Service

@Service
class CrawlingService(private val bookService: BookService) {

    @Transactional
    fun crawling() {
        val playwright = Playwright.create()
        val browser = playwright.chromium().launch(BrowserType.LaunchOptions().setHeadless(true))
        val bookLinks = getBookLinks(browser)

        runBlocking {
            val bestsellers = bookLinks.mapIndexed { ranking, bookLink ->
                async { scrapeBookData(browser, bookLink, ranking) }
            }.awaitAll().filterNotNull()
            bookService.saveBestsellers(bestsellers)
        }
        printWithThread("데이터 저장 완료")

        browser.close()
        playwright.close()
    }

    private fun getBookLinks(browser: Browser): List<String> {
        val page = browser.newPage()
        val bookLinks = mutableListOf<String>()

        for (bestsellerPage in 1..2) {
            page.navigate("https://store.kyobobook.co.kr/bestseller/realtime?page=$bestsellerPage&per=50")
            page.waitForSelector("div.ml-4 > .prod_link")

            val links = page.locator("div.ml-4 > .prod_link").all()
            bookLinks.addAll(links.mapNotNull { it.getAttribute("href") })
        }

        page.close()
        printWithThread("✅ 총 ${bookLinks.size}개의 도서 링크 수집 완료!")
        return bookLinks
    }

    private suspend fun scrapeBookData(browser: Browser, bookUrl: String, ranking: Int): BookDTO? {
        val page = browser.newPage()

        page.navigate(bookUrl, Page.NavigateOptions().setWaitUntil(WaitUntilState.DOMCONTENTLOADED))
        printWithThread("${bookUrl}에 접근 완료")


        val data = page.evaluate(
            """ () => JSON.stringify({
        title: document.querySelector('.prod_title')?.innerText?.trim() || '',
        author: document.querySelector('.author')?.innerText?.trim() || '',
        isbn: document.querySelector('#scrollSpyProdInfo .product_detail_area.basic_info table tbody tr:nth-child(1) td')?.innerText?.trim() || '',
        description: document.querySelector('.intro_bottom')?.innerText?.trim() || '',
        image: document.querySelector('.portrait_img_box img')?.getAttribute('src') || ''}) """
        ).toString()

        val type = object : TypeToken<Map<String, String>>() {}.type
        val json: Map<String, String> = Gson().fromJson(data, type)

        page.close()
        printWithThread("${bookUrl}의 데이터 파싱 완료")
        if (json.values.all { it.isBlank() }) {
            return null
        }

        return BookDTO(
            id = 0L,
            title = json["title"] ?: "",
            author = json["author"] ?: "",
            description = json["description"] ?: "",
            image = json["image"] ?: "",
            isbn = json["isbn"] ?: "",
            ranking = ranking + 1,
            favoriteCount = 0
        )
    }

    private fun printWithThread(str: Any) {
        println("[${Thread.currentThread().name}] $str")
    }

}


