package com.example.stock_backend.service;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

@Component
public class TwseScheduler {

    private final TwseService twseService;

    public TwseScheduler(TwseService twseService) {
        this.twseService = twseService;
    }

    // 每天凌晨 1 點抓多檔股票當月資料
    @Scheduled(cron = "0 0 1 * * *")
    public void fetchStockDaily() {
        String[] stockNos = {"2330", "2317", "2412"}; // 多檔股票
        LocalDate today = LocalDate.now();
        String date = today.format(DateTimeFormatter.ofPattern("yyyyMM01")); // 當月 1 號
        for (String stockNo : stockNos) {
            twseService.fetchAndSave(stockNo, date);
        }
    }
}
