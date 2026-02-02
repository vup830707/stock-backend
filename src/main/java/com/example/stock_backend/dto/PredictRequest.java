package com.example.stock_backend.dto;

public class PredictRequest {
    private String ticker;
    private Integer days;

    public String getTicker() { return ticker; }
    public void setTicker(String ticker) { this.ticker = ticker; }

    public Integer getDays() { return days; }
    public void setDays(Integer days) { this.days = days; }
}
