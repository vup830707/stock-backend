package com.example.stock_backend.dto;
import java.util.List;

public class PredictResponse {
    private String ticker;
    private List<Double> futurePrices;
    private double accuracy;
    private double rmse;
    private double mae;

    public String getTicker() { return ticker; }
    public void setTicker(String ticker) { this.ticker = ticker; }

    public List<Double> getFuturePrices() { return futurePrices; }
    public void setFuturePrices(List<Double> futurePrices) { this.futurePrices = futurePrices; }

    public double getAccuracy() { return accuracy; }
    public void setAccuracy(double accuracy) { this.accuracy = accuracy; }

    public double getRmse() { return rmse; }
    public void setRmse(double rmse) { this.rmse = rmse; }

    public double getMae() { return mae; }
    public void setMae(double mae) { this.mae = mae; }
}
