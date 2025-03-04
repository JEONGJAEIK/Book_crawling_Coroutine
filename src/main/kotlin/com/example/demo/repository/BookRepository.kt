package com.example.demo.repository

import com.example.demo.entity.Book
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import org.springframework.stereotype.Repository

/**
 * -- 책 저장소 --
 *
 * @author -- 정재익 --
 * @since -- 1월 27일 --
 */
@Repository
interface BookRepository : JpaRepository<Book, Long> {
    
    @Query("SELECT b.isbn FROM Book b WHERE b.isbn IN :isbns")
    fun findExistingIsbns(@Param("isbns") isbns: List<String>): List<String>

    /**
     * -- 전문검색 인덱싱을 n-gram분석 알고리즘으로 구현하여 제목과 설명에서 단어를 뽑아내어 검색어와 관련있는 책을 반환하는 메서드 --
     *
     * @return -- List<Book> 검색어 결과 --
     *
     * @author -- 정재익 --
     * @since -- 3월 03일 --
     */
    @Query(
        value = """
        SELECT * FROM book 
        WHERE MATCH(title, description) AGAINST(:keyword IN NATURAL LANGUAGE MODE)
        ORDER BY MATCH(title, description) AGAINST(:keyword IN NATURAL LANGUAGE MODE) DESC
        LIMIT 300
    """,
        nativeQuery = true
    )
    fun searchFullText(@Param("keyword") keyword: String): List<Book>

    fun findByIsbn(isbn: String): Book?

    fun findByRankingIsNotNullOrderByRankingAsc(): List<Book>

    @Modifying
    @Query("UPDATE Book b SET b.ranking = NULL")
    fun resetAllRankings()


}