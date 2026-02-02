package com.example.stock_backend.service;

import com.example.stock_backend.model.StockHistorical;
import com.example.stock_backend.repository.StockHistoricalRepository;
import com.example.stock_backend.repository.CompanyRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

@Service
public class TwseService {

    private final StockHistoricalRepository repo;
    private final CompanyRepository companyRepo;
    private final RestTemplate restTemplate = new RestTemplate();
    private final ObjectMapper mapper = new ObjectMapper();

    public TwseService(StockHistoricalRepository repo, CompanyRepository companyRepo) {
        this.repo = repo;
        this.companyRepo = companyRepo;
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
                String rowDate = row.get(0).asText(); // "112/12/18"
                String[] parts = rowDate.split("/");
                int year = Integer.parseInt(parts[0]) + 1911;
                String formattedDate = year + "/" + parts[1] + "/" + parts[2];

                StockHistorical item = new StockHistorical();
                item.setStockNo(stockNo);
                item.setStockName(stockName);
                item.setDate(formattedDate);
                item.setClosePrice(row.get(6).asDouble());

                try {
                    repo.save(item); // 唯一索引保護重複寫入
                } catch (DuplicateKeyException e) {
                    System.out.println(stockNo + " " + formattedDate + " 已存在，跳過");
                }
            }

            System.out.println(stockNo + " 月資料抓取完成");

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
