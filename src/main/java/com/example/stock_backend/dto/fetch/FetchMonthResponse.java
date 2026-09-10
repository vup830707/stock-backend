package com.example.stock_backend.dto.fetch;

public class FetchMonthResponse {
    private String stockNo;
    private String stockName;
    private String yearMonth;
    private int upserted;
    private boolean twseOk;

    public FetchMonthResponse() {}

    public FetchMonthResponse(String stockNo, String stockName, String yearMonth, int upserted, boolean twseOk) {
        this.stockNo = stockNo;
        this.stockName = stockName;
        this.yearMonth = yearMonth;
        this.upserted = upserted;
        this.twseOk = twseOk;
    }

    public String getStockNo() { return stockNo; }
    public void setStockNo(String stockNo) { this.stockNo = stockNo; }
    public String getStockName() { return stockName; }
    public void setStockName(String stockName) { this.stockName = stockName; }
    public String getYearMonth() { return yearMonth; }
    public void setYearMonth(String yearMonth) { this.yearMonth = yearMonth; }
    public int getUpserted() { return upserted; }
    public void setUpserted(int upserted) { this.upserted = upserted; }
    public boolean isTwseOk() { return twseOk; }
    public void setTwseOk(boolean twseOk) { this.twseOk = twseOk; }
}
