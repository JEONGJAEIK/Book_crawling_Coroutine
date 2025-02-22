package com.example.demo.service

import com.example.demo.dto.BookDTO
import com.microsoft.playwright.Playwright
import com.microsoft.playwright.options.LoadState
import jakarta.transaction.Transactional
import org.springframework.stereotype.Service

@Service
class CrawlingService(private val bookService: BookService) {

    @Transactional
    fun crawling() {
        val playwright = Playwright.create()
        val browser = playwright.chromium().launch()
        val page = browser.newPage()

        val bookLinks = mutableListOf<String>()

        for (bestsellerPage in 1..2) {
            page.navigate("https://store.kyobobook.co.kr/bestseller/realtime?page=$bestsellerPage&per=50")
            page.waitForLoadState(LoadState.DOMCONTENTLOADED)
            page.waitForSelector("div.ml-4 > .prod_link")

            val links = page.locator("div.ml-4 > .prod_link").all()
            links.forEach { link ->
                link.getAttribute("href")?.let { href ->
                    bookLinks.add(href)
                }
            }
        }

        val bestsellers = mutableListOf<BookDTO>()

        bookLinks.forEachIndexed { ranking, bookUrl ->
            page.navigate(bookUrl)
            page.waitForLoadState(LoadState.DOMCONTENTLOADED)

            val title = getTextOrEmpty(page, ".prod_title")
            val author = getTextOrEmpty(page, ".author")
            val isbn = getTextOrEmpty(
                page,
                "#scrollSpyProdInfo > div.product_detail_area.basic_info > div.tbl_row_wrap > table > tbody > tr:nth-child(1) > td"
            )
            val description = getTextOrEmpty(page, ".intro_bottom")
            val image = getAttributeOrEmpty(page, ".portrait_img_box img", "src")
            if (title.isNotBlank() || author.isNotBlank() || isbn.isNotBlank() || description.isNotBlank() || image.isNotBlank()) {
                val book = BookDTO(
                    id = 0L,
                    title = title,
                    author = author,
                    description = description,
                    image = image,
                    isbn = isbn,
                    ranking = ranking + 1,
                    favoriteCount = 0
                )
                bestsellers.add(book)
            }
        }
        browser.close()
        playwright.close()
        bookService.saveBestsellers(bestsellers)
    }

    fun getTextOrEmpty(page: com.microsoft.playwright.Page, selector: String): String {
        return if (page.locator(selector).count() > 0) {
            page.locator(selector).first().textContent()?.trim()?.replace("\n", " ") ?: ""
        } else {
            ""
        }
    }

    fun getAttributeOrEmpty(page: com.microsoft.playwright.Page, selector: String, attribute: String): String {
        return if (page.locator(selector).count() > 0) {
            page.locator(selector).first().getAttribute(attribute)?.trim()?.replace("\n", " ") ?: ""
        } else {
            ""
        }
    }
}
