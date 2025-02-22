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
     * -- 검색어와 관련이 있는 작가와 DB를 반환 --
     *
     * @author -- 정재익 --
     * @since -- 2월 11일 --
     */
    @Query(
        "SELECT b FROM Book b WHERE LOWER(b.title) LIKE LOWER(CONCAT('%', :keyword, '%')) " +
                "OR LOWER(b.author) LIKE LOWER(CONCAT('%', :keyword, '%'))"
    )
    fun findByTitleOrAuthor(@Param("keyword") keyword: String): List<Book>

    fun findByIsbn(isbn: String): Book?

    fun findByRankingIsNotNullOrderByRankingAsc(): List<Book>

    @Modifying
    @Query("UPDATE Book b SET b.ranking = NULL")
    fun resetAllRankings() // ✅ 모든 책의 ranking을 null로 설정


}