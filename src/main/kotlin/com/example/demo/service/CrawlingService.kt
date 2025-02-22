package com.example.demo.service

import com.example.demo.dto.BookDTO
import com.microsoft.playwright.*
import com.microsoft.playwright.options.WaitUntilState
import jakarta.transaction.Transactional
import kotlinx.coroutines.*
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import org.springframework.stereotype.Service

@Service
class CrawlingService(private val bookService: BookService) {

    @Transactional
    fun crawling() {
        val playwright = Playwright.create()
        val browser =
            playwright.chromium().launch(BrowserType.LaunchOptions().setHeadless(false)) // 디버깅을 위해 headless 비활성화)
        val context = browser.newContext()
        context.addInitScript("Object.defineProperty(navigator, 'webdriver', { get: () => false });")


        try {
            val page = context.newPage() // 새 브라우저 탭 열기
            val bookLinks = mutableListOf<String>() // 링크 리스트 생성

            for (bestsellerPage in 1..2) {
                page.navigate("https://store.kyobobook.co.kr/bestseller/realtime?page=$bestsellerPage&per=50") // 교보문고 접속
                page.waitForSelector("div.ml-4 > .prod_link") // 해당 요소가 추가된 후 실행

                val links = page.locator("div.ml-4 > .prod_link").all() // 해당 요소를 추가하고 list<Locator>로 반환
                bookLinks.addAll(links.mapNotNull { it.getAttribute("href") }) // 해당 요소에서 링크만 추출해서 list<String>으로 반환
            }

            println("총 ${bookLinks.size}개의 도서 링크 수집 완료!")

            val bestsellers = mutableListOf<BookDTO>()
            val mutex = Mutex()

            runBlocking {
                bookLinks.chunked(5).map { chunk ->
                    async(Dispatchers.IO) {
                        chunk.mapIndexed { ranking, bookUrl ->
                            async {
                                delay(1000)
                                val bookData = scrapeBookData(context, bookUrl, ranking)



                                if (bookData != null) {
                                    mutex.withLock {
                                        bestsellers.add(bookData)
                                    }
                                }
                            }
                        }.awaitAll()
                    }
                }.awaitAll()
            }

            println("총 ${bestsellers.size}개의 도서 데이터 저장 완료!")
            bookService.saveBestsellers(bestsellers)
        } catch (e: Exception) {
            println("❌ 크롤링 중 오류 발생: ${e.message}")
        } finally {
            browser.close()
            playwright.close()

        }

    }

    private suspend fun scrapeBookData(context: BrowserContext, bookUrl: String, ranking: Int): BookDTO? {
        var page: Page? = null
        var attempt = 0

        while (attempt < 3) {  // ✅ 최대 3번 재시도
            try {
                delay(500)  // ✅ 페이지 생성 간격 조정
                page = context.newPage()  // ✅ 안정적인 페이지 생성

                page.navigate(
                    bookUrl, Page.NavigateOptions().setWaitUntil(WaitUntilState.LOAD)
                )  // ✅ `LOAD` 상태까지 기다리기

                page.waitForTimeout(3000.0)  // ✅ 추가 대기 (API 호출 등 고려)

                val title = getTextOrEmpty(page, ".prod_title")
                val author = getTextOrEmpty(page, ".author")
                val isbn = getTextOrEmpty(
                    page,
                    "#scrollSpyProdInfo > div.product_detail_area.basic_info > div.tbl_row_wrap > table > tbody > tr:nth-child(1) > td"
                )
                val description = getTextOrEmpty(page, ".intro_bottom")
                val image = getAttributeOrEmpty(page, ".portrait_img_box img", "src")

                if (title.isNotBlank() || author.isNotBlank() || isbn.isNotBlank() || description.isNotBlank() || image.isNotBlank()) {
                    return BookDTO(
                        id = 0L,
                        title = title,
                        author = author,
                        description = description,
                        image = image,
                        isbn = isbn,
                        ranking = ranking + 1,
                        favoriteCount = 0
                    )
                }
            } catch (e: PlaywrightException) {
                attempt++  // ✅ 재시도 증가
                println("🚨 [$attempt] $bookUrl, 오류 발생: ${e.message}")
            } finally {
                page?.close()
            }

            delay(1000)  // ✅ 재시도 전 딜레이
        }

        return null
    }


    fun getTextOrEmpty(page: Page, selector: String): String {
        return page.waitForSelector(selector, Page.WaitForSelectorOptions().setTimeout(5000.0)).textContent()?.trim()
            ?.replace("\n", " ") ?: "" // ✅ 첫 번째 요소의 텍스트 가져오기

    }

    fun getAttributeOrEmpty(page: Page, selector: String, attribute: String): String {
        return page.waitForSelector(selector, Page.WaitForSelectorOptions().setTimeout(5000.0)).getAttribute(attribute)
            ?.trim()?.replace("\n", " ") ?: "" // ✅ 첫 번째 요소의 속성(attribute) 값 가져오기
    }
}

