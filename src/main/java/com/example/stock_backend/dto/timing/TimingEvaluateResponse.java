package com.example.stock_backend.dto.timing;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class TimingEvaluateResponse {
    private String stockNo;
    private boolean passed;
    private String reason;
    private Metrics metrics;
    private List<Map<String, Object>> trades;
    private String currentSignal;

    public static TimingEvaluateResponse insufficientData(String stockNo) {
        return failure(stockNo, "insufficient_data");
    }

    public static TimingEvaluateResponse evaluateFailed(String stockNo) {
        return failure(stockNo, "evaluate_failed");
    }

    private static TimingEvaluateResponse failure(String stockNo, String reason) {
        TimingEvaluateResponse response = new TimingEvaluateResponse();
        response.stockNo = stockNo;
        response.passed = false;
        response.reason = reason;
        response.metrics = new Metrics();
        response.trades = new ArrayList<>();
        return response;
    }

    public String getStockNo() { return stockNo; }
    public void setStockNo(String stockNo) { this.stockNo = stockNo; }
    public boolean isPassed() { return passed; }
    public void setPassed(boolean passed) { this.passed = passed; }
    public String getReason() { return reason; }
    public void setReason(String reason) { this.reason = reason; }
    public Metrics getMetrics() { return metrics; }
    public void setMetrics(Metrics metrics) { this.metrics = metrics; }
    public List<Map<String, Object>> getTrades() { return trades; }
    public void setTrades(List<Map<String, Object>> trades) { this.trades = trades; }
    public String getCurrentSignal() { return currentSignal; }
    public void setCurrentSignal(String currentSignal) { this.currentSignal = currentSignal; }

    public static class Metrics {
        private double strategyEndNav;
        private double buyHoldEndNav;
        private int roundTrips;
        private String oosStart = "";
        private String oosEnd = "";
        private int barCount;

        public double getStrategyEndNav() { return strategyEndNav; }
        public void setStrategyEndNav(double strategyEndNav) { this.strategyEndNav = strategyEndNav; }
        public double getBuyHoldEndNav() { return buyHoldEndNav; }
        public void setBuyHoldEndNav(double buyHoldEndNav) { this.buyHoldEndNav = buyHoldEndNav; }
        public int getRoundTrips() { return roundTrips; }
        public void setRoundTrips(int roundTrips) { this.roundTrips = roundTrips; }
        public String getOosStart() { return oosStart; }
        public void setOosStart(String oosStart) { this.oosStart = oosStart; }
        public String getOosEnd() { return oosEnd; }
        public void setOosEnd(String oosEnd) { this.oosEnd = oosEnd; }
        public int getBarCount() { return barCount; }
        public void setBarCount(int barCount) { this.barCount = barCount; }
    }
}
