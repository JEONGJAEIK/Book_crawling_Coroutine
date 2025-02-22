package com.example.demo.entity

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.Id
import jakarta.persistence.Table

@Entity
@Table(name = "keyword")
class Keyword(
    @Id
    @Column(unique = true, nullable = false)
    val keyword: String
)
