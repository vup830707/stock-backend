package com.example.stock_backend.controller;

import com.example.stock_backend.dto.fetch.FetchMonthRequest;
import com.example.stock_backend.service.StockHistoryWriter;
import com.example.stock_backend.service.TwseMonthFetchService;
import com.example.stock_backend.twse.TwseBarParser;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestTemplate;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/manual")
@CrossOrigin(origins = "*")
public class ManualFetchController {

    private final StockHistoryWriter writer;
    private final TwseMonthFetchService monthFetchService;
    private final RestTemplate restTemplate = new RestTemplate();
    private final ObjectMapper mapper = new ObjectMapper();

    public ManualFetchController(StockHistoryWriter writer, TwseMonthFetchService monthFetchService) {
        this.writer = writer;
        this.monthFetchService = monthFetchService;
    }

    /**
     * 手動抓特定股票特定區間資料
     * @param stockNo 股票代號
     * @param startDate yyyy-MM-dd
     * @param endDate yyyy-MM-dd
     */
    @PostMapping("/fetch-month")
    public ResponseEntity<?> fetchMonth(@RequestBody FetchMonthRequest request) {
        String stockNo = request == null ? null : request.getStockNo();
        String yearMonth = request == null ? null : request.getYearMonth();
        if (stockNo == null || stockNo.isBlank() || yearMonth == null || !yearMonth.matches("\\d{6}")) {
            return ResponseEntity.badRequest().body(Map.of("reason", "bad_request"));
        }
        return ResponseEntity.ok(monthFetchService.fetchMonth(stockNo.trim(), yearMonth));
    }

    @GetMapping("/fetch-range")
    public String fetchStockManualRange(@RequestParam String stockNo,
                                        @RequestParam String startDate,
                                        @RequestParam String endDate) {
        try {
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");
            LocalDate start = LocalDate.parse(startDate, formatter);
            LocalDate end = LocalDate.parse(endDate, formatter);

            List<String> months = generateMonthList(startDate, endDate);

            for (String month : months) {
                fetchSingleMonth(stockNo, month, start, end); // ✅ 傳入範圍
            }

            return "抓取完成：" + stockNo;
        } catch (Exception e) {
            e.printStackTrace();
            return "抓取失敗：" + e.getMessage();
        }
    }

    private void fetchSingleMonth(String stockNo, String monthDate,
                                  LocalDate start, LocalDate end) throws Exception {
        String url = String.format(
                "https://www.twse.com.tw/exchangeReport/STOCK_DAY?response=json&date=%s&stockNo=%s",
                monthDate, stockNo
        );

        String response = restTemplate.getForObject(url, String.class);
        JsonNode root = mapper.readTree(response);

        if (!root.path("stat").asText().equals("OK")) {
            System.out.println("TWSE API 無資料：" + monthDate);
            return;
        }

        String stockTitle = root.path("title").asText();
        String stockName = stockTitle.replaceAll("^\\d+\\s+", "").replace(" 日成交資訊", "");

        JsonNode data = root.path("data");
        for (JsonNode row : data) {
            TwseBarParser.parseRow(row).ifPresent(bar -> {
                LocalDate d = bar.date();
                if (d.isBefore(start) || d.isAfter(end)) return;
                writer.upsert(stockNo, stockName, bar);
            });
        }
    }


    // 產生月份清單，例如 2025-01-01 ~ 2025-03-01 => ["20250101","20250201","20250301"]
    private List<String> generateMonthList(String startDate, String endDate) {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");
        LocalDate start = LocalDate.parse(startDate, formatter).withDayOfMonth(1);
        LocalDate end = LocalDate.parse(endDate, formatter).withDayOfMonth(1);

        List<String> months = new ArrayList<>();
        DateTimeFormatter monthFormatter = DateTimeFormatter.ofPattern("yyyyMMdd");

        while (!start.isAfter(end)) {
            months.add(start.format(monthFormatter));
            start = start.plusMonths(1);
        }

        return months;
    }
}
