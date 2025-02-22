package com.example.demo.exception

import org.springframework.http.HttpStatus

/**
 * BookErrorCode
 * Book Controller에서 발생하는 예외 코드를 정의하는 Enum 클래스
 * @author 정재익
 * @since 2025.01.31
 */
enum class BookErrorCode(val status: HttpStatus, val code: String, val message: String) {
    QUERY_EMPTY(HttpStatus.BAD_REQUEST, "QUERY_EMPTY", "잘못된 검색어 입니다."),
    BOOK_NOT_FOUND(HttpStatus.NOT_FOUND, "BOOK_NOT_FOUND", "해당 도서를 찾을 수 없습니다."),
}