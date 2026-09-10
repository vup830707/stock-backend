package com.example.stock_backend.dto.fetch;

public class FetchMonthRequest {
    private String stockNo;
    private String yearMonth;

    public FetchMonthRequest() {}

    public String getStockNo() { return stockNo; }
    public void setStockNo(String stockNo) { this.stockNo = stockNo; }
    public String getYearMonth() { return yearMonth; }
    public void setYearMonth(String yearMonth) { this.yearMonth = yearMonth; }
}
