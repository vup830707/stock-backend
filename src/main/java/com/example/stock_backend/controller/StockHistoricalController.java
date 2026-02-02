package com.example.stock_backend.controller;

import com.example.stock_backend.model.StockHistorical;
import com.example.stock_backend.repository.StockHistoricalRepository;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/stock-history")
@CrossOrigin(origins = "*")
public class StockHistoricalController {

    private final StockHistoricalRepository repo;

    public StockHistoricalController(StockHistoricalRepository repo) {
        this.repo = repo;
    }

    // 查單一股票歷史資料
    @GetMapping
    public List<StockHistorical> getStockHistory(@RequestParam String stockNo) {
        return repo.findByStockNo(stockNo);
    }

    // 查所有股票（完整資料）
    @GetMapping("/all")
    public List<StockHistorical> getAllStocks() {
        return repo.findAll();
    }

    // 查所有股票代號 + 公司名稱（去重）
    @GetMapping("/codes-with-name")
    public List<StockCodeName> getAllStockCodesWithName() {
        return repo.findAll()
                .stream()
                .collect(
                        Collectors.groupingBy(
                                StockHistorical::getStockNo,
                                Collectors.mapping(StockHistorical::getStockName, Collectors.toList())
                        )
                )
                .entrySet()
                .stream()
                .map(e -> new StockCodeName(e.getKey(), e.getValue().get(0)))
                .toList();
    }

    // DTO
    public static class StockCodeName {
        private String stockNo;
        private String stockName;

        public StockCodeName(String stockNo, String stockName) {
            this.stockNo = stockNo;
            this.stockName = stockName;
        }

        public String getStockNo() { return stockNo; }
        public void setStockNo(String stockNo) { this.stockNo = stockNo; }

        public String getStockName() { return stockName; }
        public void setStockName(String stockName) { this.stockName = stockName; }
    }
}
