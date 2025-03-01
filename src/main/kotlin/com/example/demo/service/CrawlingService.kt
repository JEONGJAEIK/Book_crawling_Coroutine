package com.example.demo.service

import com.example.demo.dto.BookDTO
import kotlinx.coroutines.*
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import org.jsoup.safety.Safelist
import org.springframework.stereotype.Service
import java.text.SimpleDateFormat
import java.util.*

@Service
class CrawlingService(private val bookService: BookService) {

    fun main() = runBlocking {
        printWithThread("크롤링 시작")
        val bestSellerMaps = getBestSellerMaps()
        val bestSellerBookDTOs = getBestSellerBookDTOs(bestSellerMaps)
        bookService.saveBestsellers(bestSellerBookDTOs)
        printWithThread("크롤링 완료")
    }

    private fun getBestSellerMaps(): Map<Int, String> {
        val targetUrl = "https://www.yes24.com/Product/Category/RealTimeBestSeller?categoryNumber=001"
        val baseUrl = "https://www.yes24.com"

        val doc: Document = Jsoup.connect(targetUrl).get()
        val bestSellerLinks =
            doc.select("#yesBestList > li > div > div.item_info > div.info_row.info_name > a.gd_name").eachAttr("href")

        return bestSellerLinks.mapIndexed { index, href ->
            (index + 1) to "$baseUrl$href"
        }.toMap().also {
            printWithThread("${bestSellerLinks.size}개 링크 크롤링 완료")
        }
    }

    private suspend fun getBestSellerBookDTOs(bestSellersMaps: Map<Int, String>): List<BookDTO> =
        coroutineScope {
            bestSellersMaps.map { (ranking, link) ->
                async(Dispatchers.IO) {
                    getBestSellerBookDTO(ranking, link)
                }
            }.awaitAll()
        }

    private fun getBestSellerBookDTO(ranking: Int, link: String): BookDTO {
        val doc: Document = Jsoup.connect(link).get()

        val title = doc.selectFirst("#yDetailTopWrap > div.topColRgt > div.gd_infoTop > div > h2")?.text() ?: "제목 없음"
        val author = doc.selectFirst("#yDetailTopWrap > div.topColRgt > div.gd_infoTop > span.gd_pubArea > span.gd_auth > a:nth-child(1)")?.text() ?: "저자 없음"
        val image = doc.selectFirst("#yDetailTopWrap > div.topColLft > div > div.gd_3dGrp.gdImgLoadOn > div > span.gd_img > em > img")?.attr("src") ?: "이미지 없음"
        val isbn = doc.selectFirst("#infoset_specific > div.infoSetCont_wrap > div > table > tbody > tr:nth-child(3) > td")?.text() ?: "ISBN 없음"
        val rawDescription = doc.selectFirst("#infoset_introduce > div.infoSetCont_wrap > div.infoWrap_txt")?.text() ?: "설명 없음"
        val description = Jsoup.clean(rawDescription, Safelist.none()).trim()

        return BookDTO(
            id = null,
            title = title,
            author = author,
            description = description,
            image = image,
            isbn = isbn,
            ranking = ranking,
            favoriteCount = 0
        ).also {
            printWithThread("${ranking}위 책 크롤링 완료")
        }
    }

    private fun printWithThread(str: Any) {
        val time = System.currentTimeMillis()
        val formattedTime = SimpleDateFormat("mm분 ss초 SSS", Locale.getDefault()).format(Date(time))
        println("$str $formattedTime")
    }
}
