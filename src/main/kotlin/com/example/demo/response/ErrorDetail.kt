package com.example.demo.response

/**
 * ErrorDetail
 * 에러 발생 시 클라이언트에게 전달할 에러 상세 정보를 담는 클래스
 * @param field 에러가 발생한 필드
 * @param reason 에러 발생 원인
 * author 이원재
 * since 2025.01.27
 */
@JvmRecord
data class ErrorDetail(val field: String, val reason: String) {
    companion object {
        /**
         * ErrorDetail 생성 팩토리 메서드
         * @param field
         * @param reason
         * @return [ErrorDetail]
         */
        fun of(field: String, reason: String): ErrorDetail {
            return ErrorDetail(field, reason)
        }
    }
}
