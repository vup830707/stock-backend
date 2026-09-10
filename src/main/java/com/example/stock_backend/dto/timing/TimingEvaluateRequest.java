package com.example.stock_backend.dto.timing;

import java.util.List;

public class TimingEvaluateRequest {
    private String stockNo;
    private List<TimingBarDto> bars;

    public TimingEvaluateRequest() {}

    public TimingEvaluateRequest(String stockNo, List<TimingBarDto> bars) {
        this.stockNo = stockNo;
        this.bars = bars;
    }

    public String getStockNo() { return stockNo; }
    public void setStockNo(String stockNo) { this.stockNo = stockNo; }
    public List<TimingBarDto> getBars() { return bars; }
    public void setBars(List<TimingBarDto> bars) { this.bars = bars; }
}
