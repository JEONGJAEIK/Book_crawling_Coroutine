package com.example.demo.service

import com.example.demo.dto.BookDTO
import com.microsoft.playwright.Page
import com.microsoft.playwright.Playwright
import com.microsoft.playwright.PlaywrightException
import jakarta.transaction.Transactional
import org.springframework.stereotype.Service

@Service
class CrawlingService(private val bookService: BookService) {

    @Transactional
    fun crawling() {
        Playwright.create().use { playwright ->  // playwright 객체 생성
            val browser = playwright.chromium().launch() //크롬 브라우저 실행
            val page = browser.newPage() // 새 브라우저 탭 열기

            val bookLinks = mutableListOf<String>() // 링크 리스트 생성

            for (bestsellerPage in 1..2) {
                page.navigate("https://store.kyobobook.co.kr/bestseller/realtime?page=$bestsellerPage&per=50") // 교보문고 접속
                page.waitForSelector("div.ml-4 > .prod_link") // 해당 요소가 추가된 후 실행

                val links = page.locator("div.ml-4 > .prod_link").all() // 해당 요소를 추가하고 list<Locator>로 반환
                bookLinks.addAll(links.mapNotNull { it.getAttribute("href") }) // 해당 요소에서 링크만 추출해서 list<String>으로 반환
            }

            println("총 ${bookLinks.size}개의 도서 링크 수집 완료!")

            val bestsellers = mutableListOf<BookDTO>()

            bookLinks.forEachIndexed { ranking, bookUrl -> //랭킹과 url 반복
                page.navigate(bookUrl)                     // url 페이지 열기

                val title = getTextOrEmpty(page, ".prod_title")
                val author = getTextOrEmpty(page, ".author")
                val isbn = getTextOrEmpty(page, "#scrollSpyProdInfo > div.product_detail_area.basic_info > div.tbl_row_wrap > table > tbody > tr:nth-child(1) > td")
                val description = getTextOrEmpty(page, ".intro_bottom")
                val image = getAttributeOrEmpty(page, ".portrait_img_box img", "src")

                if (title.isNotBlank() || author.isNotBlank() || isbn.isNotBlank() || description.isNotBlank() || image.isNotBlank()) {
                    bestsellers.add(
                        BookDTO(
                            id = 0L,
                            title = title,
                            author = author,
                            description = description,
                            image = image,
                            isbn = isbn,
                            ranking = ranking + 1,
                            favoriteCount = 0
                        )
                    )
                }
            }
            println("총 ${bestsellers.size}개의 도서 데이터 저장 완료!")
            bookService.saveBestsellers(bestsellers)
        }
    }

    fun getTextOrEmpty(page: Page, selector: String): String {
        return try {
            val element = page.waitForSelector(selector, Page.WaitForSelectorOptions().setTimeout(3000.0))
            element?.textContent()?.trim()?.replace("\n", "") ?: ""
        } catch (e: PlaywrightException) {
            if (e.message?.contains("Timeout") == true) {
                ""
            } else {
                throw e
            }
        }
    }

    fun getAttributeOrEmpty(page: Page, selector: String, attribute: String): String {
        return try {
            val element = page.waitForSelector(selector, Page.WaitForSelectorOptions().setTimeout(3000.0))
            element?.getAttribute(attribute)?.trim()?.replace("\n", "") ?: ""
        } catch (e: PlaywrightException) {
            if (e.message?.contains("Timeout") == true) {
                ""
            } else {
                throw e
            }
        }
    }
}
