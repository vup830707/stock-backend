package com.example.stock_backend.controller;

import com.example.stock_backend.model.StockHistorical;
import com.example.stock_backend.repository.StockHistoricalRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestTemplate;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

@RestController
@RequestMapping("/api/manual")
@CrossOrigin(origins = "*")
public class ManualFetchController {

    private final StockHistoricalRepository repo;
    private final RestTemplate restTemplate = new RestTemplate();
    private final ObjectMapper mapper = new ObjectMapper();

    public ManualFetchController(StockHistoricalRepository repo) {
        this.repo = repo;
    }

    /**
     * 手動抓特定股票特定區間資料
     * @param stockNo 股票代號
     * @param startDate yyyy-MM-dd
     * @param endDate yyyy-MM-dd
     */
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
            try {
                String taiwanDate = row.get(0).asText();
                if (taiwanDate.equals("--") || taiwanDate.isEmpty()) continue;

                // 解析民國年 -> 西元 LocalDate
                String[] parts = taiwanDate.split("/");
                int year = Integer.parseInt(parts[0]) + 1911;
                int month = Integer.parseInt(parts[1]);
                int day = Integer.parseInt(parts[2]);
                LocalDate d = LocalDate.of(year, month, day);

                // ✅ 範圍裁切：不在區間內就跳過（這就是你缺的）
                if (d.isBefore(start) || d.isAfter(end)) continue;

                StockHistorical item = new StockHistorical();
                item.setStockNo(stockNo);
                item.setStockName(stockName);

                // 你原本存字串日期：保持同格式
                String formattedDate = String.format("%04d/%02d/%02d", year, month, day);
                item.setDate(formattedDate);

                String closeStr = row.get(6).asText().replace(",", "");
                double closePrice = (!closeStr.equals("--") && !closeStr.isEmpty())
                        ? Double.parseDouble(closeStr) : 0;
                item.setClosePrice(closePrice);

                repo.save(item);
            } catch (Exception e) {
                System.out.println("跳過資料：" + row.toString());
            }
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
