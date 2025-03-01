package com.example.demo.service

import com.example.demo.dto.BookDTO
import com.example.demo.dto.KakaoDTO
import com.example.demo.dto.NaverDTO
import com.example.demo.entity.Book
import com.example.demo.entity.Keyword
import com.example.demo.exception.BookErrorCode
import com.example.demo.exception.BookException
import com.example.demo.repository.BookRepository
import com.example.demo.repository.KeywordRepository
import com.example.demo.util.BookUtil
import com.fasterxml.jackson.databind.ObjectMapper
import jakarta.persistence.EntityManager
import jakarta.persistence.PersistenceContext
import jakarta.transaction.Transactional
import org.springframework.beans.factory.annotation.Value
import org.springframework.core.ParameterizedTypeReference
import org.springframework.data.domain.Page
import org.springframework.http.HttpEntity
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpMethod
import org.springframework.http.ResponseEntity
import org.springframework.http.client.SimpleClientHttpRequestFactory
import org.springframework.stereotype.Service
import org.springframework.web.client.RestTemplate

/**
 * -- 도서 서비스 클래스 --
 *
 * @author -- 정재익 --
 * @since -- 2월 5일 --
 */
@Service
class BookService(
    private val bookRepository: BookRepository,
    private val objectMapper: ObjectMapper,
    private val keywordRepository: KeywordRepository,
    @Value("\${naver.client-id}") val clientId: String,
    @Value("\${naver.client-secret}") val clientSecret: String,
    @Value("\${naver.book-search-url}") val naverUrl: String,
    @Value("\${kakao.key}") val kakaoKey: String,
    @Value("\${kakao.url}") val kakaoUrl: String
) {
    @PersistenceContext
    private lateinit var entityManager: EntityManager


    /**
     * -- 도서 검색 메소드 --
     * 1. 카카오와 네이버 두 Api에 요청
     * 2. Page<BookDto>로 변환하여 반환
     */
    @Transactional
    fun searchBooks(query: String?, page: Int, size: Int): Page<BookDTO> {
        if (query.isNullOrBlank()) {
            throw BookException(BookErrorCode.QUERY_EMPTY)
        }

        var bookList = searchBooksDB(query)

        if (keywordRepository.existsByKeyword(query)) {
            return BookUtil.pagingBooks(page, size, bookList)
        }

        keywordRepository.save(Keyword(query))


        var start = 0
        val end = 3
        while (bookList.size < 200 && start < end) {

            val apiBooks = mutableListOf<BookDTO>()

            when (start) {
                0 -> {
                    apiBooks += requestApi(query, "naver", 1, 0)
                    apiBooks += requestApi(query, "kakao", 0, 2)
                }

                1 -> {
                    apiBooks += requestApi(query, "naver", 100, 0)
                    apiBooks += requestApi(query, "kakao", 0, 4)
                }

                2 -> {
                    apiBooks += requestApi(query, "kakao", 0, 10)
                }
            }
            saveBooks(apiBooks)
            bookList = searchBooksDB(query)

            if (bookList.size >= 200) {
                break
            }
            start++
        }
        return BookUtil.pagingBooks(page, size, bookList)
    }

    /**
     * -- API 요청 메소드 --
     */
    private fun requestApi(query: String, apiType: String, naverStart: Int, kakaoPage: Int): List<BookDTO> {
        val restTemplate = RestTemplate(SimpleClientHttpRequestFactory())
        val (headers, url, responseKey) = getApiRequestParams(query, apiType, naverStart, kakaoPage)

        val entity = HttpEntity<String>(headers)
        val response: ResponseEntity<Map<String, Any>> = restTemplate.exchange(
            url, HttpMethod.GET, entity, object : ParameterizedTypeReference<Map<String, Any>>() {}
        )

        val rawData = (response.body?.get(responseKey) as? List<*>)?.filterNotNull()
            ?: throw BookException(BookErrorCode.BOOK_NOT_FOUND)

        return rawData.map { convertToBook(it, apiType) }
    }


    // api 설정 메소드
    private fun getApiRequestParams(
        query: String,
        apiType: String,
        naverStart: Int,
        kakaoPage: Int
    ): Triple<HttpHeaders, String, String> {
        val headers = HttpHeaders()

        return when (apiType.lowercase()) {
            "kakao" -> {
                headers["Authorization"] = "KakaoAK $kakaoKey"
                Triple(headers, "$kakaoUrl?query=$query&target=author&page=$kakaoPage&size=50", "documents")
            }

            else -> {
                headers["X-Naver-Client-Id"] = clientId
                headers["X-Naver-Client-Secret"] = clientSecret
                Triple(headers, "$naverUrl?query=$query&display=100&start=$naverStart", "items")
            }
        }
    }


    // 데이터를 책DTO로 바꾸는 메서드
    private fun convertToBook(item: Any, apiType: String): BookDTO {
        val bookDto = when (apiType.lowercase()) {
            "kakao" -> objectMapper.convertValue(item, KakaoDTO::class.java)
            else -> objectMapper.convertValue(item, NaverDTO::class.java)
        }
        return BookDTO(
            id = 0L,
            title = bookDto.title,
            author = bookDto.author,
            description = bookDto.description,
            image = bookDto.image,
            isbn = bookDto.isbn,
            ranking = null,
            favoriteCount = 0
        )
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
        val uniqueBooks = BookUtil.removeDuplicateBooks(books)
        val existingIsbns = bookRepository.findExistingIsbns(uniqueBooks.mapNotNull { it.isbn }).toSet()


        val booksToSave = uniqueBooks.filterNot { existingIsbns.contains(it.isbn) }.map { dto ->
            Book(
                id = null,
                title = dto.title ?: "제목 정보가 없습니다",
                author = dto.author ?: "작가 정보가 없습니다",
                description = dto.description ?: "설명 정보가 없습니다",
                image = dto.image ?: "이미지 파일이 없습니다",
                isbn = dto.isbn ?: "isbn 정보가 없습니다",
                ranking = null,
                favoriteCount = dto.favoriteCount
            )
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
        books.forEach { dto ->
            val existingBook = bookRepository.findByIsbn(dto.isbn ?: "")

            if (existingBook != null) {
                existingBook.ranking = dto.ranking
                entityManager.merge(existingBook)
            } else {
                val book = Book(
                    id = null,
                    title = dto.title ?: "제목 정보가 없습니다",
                    author = dto.author ?: "작가 정보가 없습니다",
                    description = dto.description ?: "설명 정보가 없습니다",
                    image = dto.image ?: "이미지 파일이 없습니다",
                    isbn = dto.isbn ?: "ISBN 정보가 없습니다",
                    ranking = dto.ranking,
                    favoriteCount = 0
                )
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
        return bookRepository.findByTitleOrAuthor(query)
            .take(200)
            .map { BookUtil.entityToDTO(it) }
    }
}