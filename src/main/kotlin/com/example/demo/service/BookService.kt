package com.example.demo.service

import com.example.demo.dto.BookDTO
import com.example.demo.entity.Book
import com.example.demo.exception.BookErrorCode
import com.example.demo.exception.BookException
import com.example.demo.repository.BookRepository
import com.example.demo.repository.RedisRepository
import com.example.demo.util.BookUtil
import jakarta.persistence.EntityManager
import jakarta.persistence.PersistenceContext
import jakarta.transaction.Transactional
import org.springframework.data.domain.Page
import org.springframework.stereotype.Service

/**
 * -- 도서 서비스 클래스 --
 *
 * @author -- 정재익 --
 * @since -- 2월 5일 --
 */
@Service
class BookService(
    private val bookRepository: BookRepository,
    private val redisRepository: RedisRepository,
    private val apiClientService: ApiClientService,
) {
    @PersistenceContext
    private lateinit var entityManager: EntityManager

    /**
     * -- 도서 검색 메소드 --
     * 1. 입력된 적이 있는 검색어 인지 Redis 이용 판단
     * 2. 입력된 적이 있으면 DB 기반 통합 검색 시행
     * 3. 입력된 적이 없을 경우 DB 기반 통합 검색 시행하고 데이터가 200건보다 적을경우만 API 요청
     * 4. 처음엔 소량을 요청하고 그래도 200권이 안되면 수량을 늘려 요청
     * 5. 3회까지 요청하고도 200권이 안되면 요청 종료
     * 6. Page<BookDto>로 변환하여 반환
     *
     * @param -- query(검색어)
     * @param -- page 페이지 --
     * @param -- size 한 페이지에 보여주는 책 수량 --
     * @return -- Page<BookDTO> --
     * @author -- 정재익 --
     * @since -- 3월 03일 --
     */
    @Transactional
    fun searchBooks(query: String, page: Int, size: Int): Page<BookDTO> {
        if (query.isBlank()) {
            throw BookException(BookErrorCode.QUERY_EMPTY)
        }

        var bookList = searchBooksDB(query)

        if (redisRepository.existKeyword(query)) {
            return BookUtil.pagingBooks(page, size, bookList)
        }

        redisRepository.saveKeyword(query)

        var start = 0
        val end = 3
        while (bookList.size < 300 && start < end) {

            val apiBooks = mutableListOf<BookDTO>()

            when (start) {
                0 -> {
                    apiBooks += apiClientService.requestApi(query, "naver", 1, 0)
                    apiBooks += apiClientService.requestApi(query, "kakao", 0, 2)
                }

                1 -> {
                    apiBooks += apiClientService.requestApi(query, "naver", 100, 0)
                    apiBooks += apiClientService.requestApi(query, "kakao", 0, 4)
                }

                2 -> {
                    apiBooks += apiClientService.requestApi(query, "kakao", 0, 10)
                }
            }
            saveBooks(apiBooks)
            bookList = searchBooksDB(query)

            if (bookList.size >= 300) {
                break
            }
            start++
        }
        return BookUtil.pagingBooks(page, size, bookList)
    }


    /**
     * -- 도서 상세 검색 메소드 --
     */
    fun searchDetailBooks(id: Long): BookDTO {
        return bookRepository.findById(id)
            .map { BookUtil.entityToDTO(it) }
            .orElseThrow { BookException(BookErrorCode.BOOK_NOT_FOUND) }
    }

    fun searchBestSellersDB(page: Int, size: Int): Page<BookDTO> {
        val bestSellers = bookRepository.findByRankingIsNotNullOrderByRankingAsc()
            .map { BookUtil.entityToDTO(it) }
        return BookUtil.pagingBooks(page, size, bestSellers)
    }

    /**
     * -- DB 저장 메소드 --
     */
    @Transactional
    fun saveBooks(books: List<BookDTO>) {
        val uniqueBooks = BookUtil.removeDuplicateBooks(books).filter { it.isbn.isNotBlank() }
        val existingIsbns = bookRepository.findExistingIsbns(uniqueBooks.map { it.isbn }).toSet()

        val booksToSave = uniqueBooks.filterNot { existingIsbns.contains(it.isbn) }.map { dto ->
            Book(null, dto.title, dto.author, dto.description, dto.image, dto.isbn, null, dto.favoriteCount)
        }
        if (booksToSave.isNotEmpty()) {
            booksToSave.chunked(1000).forEach { batch ->
                batch.forEach { entityManager.persist(it) }
                entityManager.flush()
                entityManager.clear()
            }
        }
    }

    @Transactional
    fun saveBestsellers(books: List<BookDTO>) {
        bookRepository.resetAllRankings()

        books.filter { it.isbn.isNotBlank() }
            .forEach { dto ->
                val existingBook = bookRepository.findByIsbn(dto.isbn)

                if (existingBook != null) {
                    existingBook.ranking = dto.ranking
                    entityManager.merge(existingBook)
                } else {
                    val book = Book(null, dto.title, dto.author, dto.description, dto.image, dto.isbn, dto.ranking, 0)
                    entityManager.persist(book)
                }
            }
        entityManager.flush()
        entityManager.clear()
    }


    /**
     * -- DB에서 관련 검색 데이터를 찾는 메소드 --
     */
    fun searchBooksDB(query: String): List<BookDTO> {
        return bookRepository.searchFullText(query)
            .map { BookUtil.entityToDTO(it) }
    }
}