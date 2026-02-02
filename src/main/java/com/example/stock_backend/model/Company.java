package com.example.stock_backend.model;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

@Document(collection = "company")
public class Company {
    @Id
    private String id;
    private String stockNo;
    private String stockName;

    public Company() {} // 必須有無參數建構子

    public Company(String stockNo, String stockName) {
        this.stockNo = stockNo;
        this.stockName = stockName;
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getStockNo() { return stockNo; }
    public void setStockNo(String stockNo) { this.stockNo = stockNo; }

    public String getStockName() { return stockName; }
    public void setStockName(String stockName) { this.stockName = stockName; }
}
