package com.example.demo.service

import com.example.demo.dto.BookDTO
import com.example.demo.dto.KakaoDTO
import com.example.demo.dto.NaverDTO
import com.fasterxml.jackson.databind.ObjectMapper
import org.springframework.beans.factory.annotation.Value
import org.springframework.core.ParameterizedTypeReference
import org.springframework.http.HttpEntity
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpMethod
import org.springframework.stereotype.Service
import org.springframework.web.client.RestTemplate

@Service
class ApiClientService(
    private val restTemplate: RestTemplate,
    private val objectMapper: ObjectMapper,
    @Value("\${naver.client-id}") val clientId: String,
    @Value("\${naver.client-secret}") val clientSecret: String,
    @Value("\${naver.book-search-url}") val naverUrl: String,
    @Value("\${kakao.key}") val kakaoKey: String,
    @Value("\${kakao.url}") val kakaoUrl: String
) {

    fun requestApi(query: String, apiType: String, naverStart: Int, kakaoPage: Int): List<BookDTO> {
        val (headers, url, responseKey) = getApiRequestParams(query, apiType, naverStart, kakaoPage)
        val entity = HttpEntity<String>(headers)

        val response = restTemplate.exchange(
            url,
            HttpMethod.GET,
            entity,
            object : ParameterizedTypeReference<Map<String, Any>>() {})
        val rawData = (response.body?.get(responseKey) as? List<*>)?.filterNotNull() ?: emptyList()

        return rawData.map { convertToBook(it, apiType) }
    }

    private fun convertToBook(item: Any, apiType: String): BookDTO {
        val bookDto = when (apiType.lowercase()) {
            "kakao" -> objectMapper.convertValue(item, KakaoDTO::class.java)
            else -> objectMapper.convertValue(item, NaverDTO::class.java)
        }
        return BookDTO(
            id = null,
            title = bookDto.title,
            author = bookDto.author,
            description = bookDto.description,
            image = bookDto.image,
            isbn = bookDto.isbn,
            ranking = null,
            favoriteCount = 0
        )
    }

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
}
