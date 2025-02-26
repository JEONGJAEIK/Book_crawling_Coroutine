package com.example.demo.controller

import com.example.demo.dto.BookDTO
import com.example.demo.service.BookService
import com.example.demo.service.CrawlingService
import org.springframework.data.domain.Page
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*

/**
 * -- 도서 컨트롤러 --
 *
 * @author -- 정재익 --
 * @since -- 2월 5일 --
 */
@RestController
@RequestMapping("/book")
class BookController(private val bookService: BookService, private val crawlingService: CrawlingService) {

    @GetMapping
    fun mainPage(): ResponseEntity<List<BookDTO>> {
        val bestSellers = bookService.searchRankedBooks()
        return ResponseEntity.ok(bestSellers)
    }

    /**
     * -- 도서 검색 --
     * api의 정보를 바탕으로 도서를 검색
     * 작가, 제목을 통합 검색
     *
     * @param -- query(검색어)
     * @param -- page 페이지 --
     * @param -- size 한 페이지에 보여주는 책 수량 --
     * @return -- ResponseEntity<<Page<BookDTO>>> --
     * @author -- 정재익 --
     * @since -- 2월 10일 --
     */
    @GetMapping("/search")
    fun searchBooks(
        @RequestParam(name = "query") query: String?,
        @RequestParam(name = "page") page: Int = 0,
        @RequestParam(name = "size") size: Int = 10
    ): ResponseEntity<Page<BookDTO>> {
        val books = bookService.searchBooks(query, page, size)
        return ResponseEntity.ok(books)
    }

    /**
     * -- 도서 상세 검색 --
     * book id로 DB의 정보를 가져옴
     *
     * @param -- id 책 아이디 --
     * @return -- ResponseEntity<<Page<BookDTO>>> --
     * @author -- 정재익 --
     * @since -- 2월 11일 --
     */
    @GetMapping("/search/{id}")
    fun searchDetailBooks(@PathVariable(name = "id") id: Long):
            ResponseEntity<BookDTO> {
        val detailBook = bookService.searchDetailBooks(id)
        return ResponseEntity.ok(detailBook)
    }

    @GetMapping("/test")
    fun test() {
        crawlingService.crawling()
    }
}