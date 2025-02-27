package com.example.demo.scheduler

import com.example.demo.service.CrawlingService
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component

@Component
class CrawlingScheduler(private val crawlingService: CrawlingService) {

    @Scheduled(cron = "0 10 * * * *")
    fun scheduledCrawling() {
        println("크롤링 시작: ${java.time.LocalDateTime.now()}")
        crawlingService.main()
        println("크롤링 완료: ${java.time.LocalDateTime.now()}")
    }
}
