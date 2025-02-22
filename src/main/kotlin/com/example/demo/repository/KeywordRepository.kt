package com.example.demo.repository

import com.example.demo.entity.Keyword
import org.springframework.data.jpa.repository.JpaRepository

interface KeywordRepository : JpaRepository<Keyword, String> {
    fun existsByKeyword(keyword: String): Boolean
}
