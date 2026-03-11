package com.example.stock.scheduler;

import com.example.stock.service.StockTaggingService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDate;

@Component
public class StockTaggingScheduler {

    private static final Logger log = LoggerFactory.getLogger(StockTaggingScheduler.class);

    @Autowired
    private StockTaggingService stockTaggingService;

    @Scheduled(fixedRate = 600000, initialDelay = 20000)
    public void run() {
        try {
            stockTaggingService.tagByDate(LocalDate.now());
        } catch (Exception e) {
            log.error("stock tagging scheduler failed", e);
        }
    }
}
