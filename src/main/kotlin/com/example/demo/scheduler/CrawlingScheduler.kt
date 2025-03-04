package com.example.demo.scheduler

import com.example.demo.service.CrawlingService
import org.slf4j.LoggerFactory
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component

@Component
class CrawlingScheduler(private val crawlingService: CrawlingService) {
    private val logger = LoggerFactory.getLogger(CrawlingScheduler::class.java)


    @Scheduled(cron = "0 10 * * * *")
    fun scheduledCrawling() {
        logger.info("크롤링 시작: ${java.time.LocalDateTime.now()}")
        crawlingService.main()
        logger.info("크롤링 완료: ${java.time.LocalDateTime.now()}")
    }
}
