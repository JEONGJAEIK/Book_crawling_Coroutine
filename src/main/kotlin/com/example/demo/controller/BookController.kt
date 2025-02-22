package com.example.demo.controller

import com.example.demo.dto.BookDTO
import com.example.demo.service.BookService
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
class BookController(private val bookService: BookService) {

    @GetMapping
    fun mainPage() : ResponseEntity<List<BookDTO>> {
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

//    /**
//     * 도서 찜하기,취소하기 기능
//     *
//     * @param -- isbn --
//     * @param -- 프론트에 있는 bookdto
//     * @param -- customUserDetails 로그인한 사용자 정보 --
//     * @return -- <String>
//     * @author -- 정재익, 김남우 --
//     * @since -- 2월 9일 --
//     */
//    @PostMapping("/{isbn}/favorite")
//    @Operation(summary = "도서 찜하기 / 찜취소하기")
//    public ResponseEntity<<String>> favoriteBook(
//    @PathVariable(name = "isbn") String isbn,
//    @RequestBody BookDTO bookDto,
//    @AuthenticationPrincipal CustomUserDetails customUserDetails) {
//
//        bookDto.setIsbn(isbn);
//        boolean isFavorited = bookService.favoriteBook(bookDto, customUserDetails.getUsername());
//
//        return isFavorited
//        ? ResponseEntity.status(HttpStatus.CREATED).body(.of("찜한 도서가 추가되었습니다."))
//        : ResponseEntity.ok(.of("찜한 도서가 취소되었습니다."));
//    }
//
//    /**
//     * -- 찜 도서 목록 확인 메소드 --
//     * 로그인한 사용자의 정보를 통해 favoriteRepository에서 찜한 도서 목록 조회
//     *
//     * @param customUserDetails 로그인한 사용자 정보
//     * @param page 페이지 번호 (기본값: 0)
//     * @param size 페이지 크기 (기본값: 10)
//     * @return <Page<BookDTO>>
//     * @author 김남우
//     * @since 2월 10일
//     */
//    @GetMapping("/favorite")
//    @Operation(summary = "도서 찜 목록")
//    public ResponseEntity<<Page<BookDTO>>> getFavoriteBooks(
//    @AuthenticationPrincipal CustomUserDetails customUserDetails,
//    @RequestParam(name = "page", defaultValue = "1") int page,
//    @RequestParam(name = "size", defaultValue = "10") int size) {
//
//        Page<BookDTO> favoriteBooks = bookService.getFavoriteBooks(customUserDetails.getUsername(), page, size);
//        return ResponseEntity.ok(.of(favoriteBooks, "찜한 도서 목록입니다."));
//    }
}
