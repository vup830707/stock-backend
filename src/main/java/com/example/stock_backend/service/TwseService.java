package com.example.stock_backend.service;

import com.example.stock_backend.repository.CompanyRepository;
import com.example.stock_backend.twse.TwseBarParser;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

@Service
public class TwseService {

    private final CompanyRepository companyRepo;
    private final StockHistoryWriter writer;
    private final RestTemplate restTemplate = new RestTemplate();
    private final ObjectMapper mapper = new ObjectMapper();

    public TwseService(CompanyRepository companyRepo, StockHistoryWriter writer) {
        this.companyRepo = companyRepo;
        this.writer = writer;
    }

    public void fetchAndSave(String stockNo, String date) {
        try {
            String url = String.format(
                    "https://www.twse.com.tw/exchangeReport/STOCK_DAY?response=json&date=%s&stockNo=%s",
                    date, stockNo
            );

            String response = restTemplate.getForObject(url, String.class);
            JsonNode root = mapper.readTree(response);

            if (!root.path("stat").asText().equals("OK")) {
                System.out.println(stockNo + " 資料抓取失敗");
                return;
            }

            JsonNode data = root.path("data");
            String stockName = companyRepo.findByStockNo(stockNo)
                    .map(c -> c.getStockName())
                    .orElse(stockNo);

            for (JsonNode row : data) {
                TwseBarParser.parseRow(row)
                        .ifPresent(bar -> writer.upsert(stockNo, stockName, bar));
            }

            System.out.println(stockNo + " 月資料抓取完成");

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
