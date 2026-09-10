package com.example.stock_backend.model;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.index.CompoundIndexes;
import org.springframework.data.mongodb.core.mapping.Document;

@Document(collection = "stock_history")
@CompoundIndexes({
        @CompoundIndex(name = "stock_date_idx", def = "{'stockNo' : 1, 'date': 1}", unique = true)
})
public class StockHistorical {
    @Id
    private String id;
    private String stockNo;
    private String stockName;
    private String date;        // "2025/12/18"
    private double openPrice;
    private double highPrice;
    private double lowPrice;
    private double closePrice;
    private long volume;

    public StockHistorical() {}

    // getters & setters
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getStockNo() { return stockNo; }
    public void setStockNo(String stockNo) { this.stockNo = stockNo; }
    public String getStockName() { return stockName; }
    public void setStockName(String stockName) { this.stockName = stockName; }
    public String getDate() { return date; }
    public void setDate(String date) { this.date = date; }
    public double getOpenPrice() { return openPrice; }
    public void setOpenPrice(double openPrice) { this.openPrice = openPrice; }
    public double getHighPrice() { return highPrice; }
    public void setHighPrice(double highPrice) { this.highPrice = highPrice; }
    public double getLowPrice() { return lowPrice; }
    public void setLowPrice(double lowPrice) { this.lowPrice = lowPrice; }
    public double getClosePrice() { return closePrice; }
    public void setClosePrice(double closePrice) { this.closePrice = closePrice; }
    public long getVolume() { return volume; }
    public void setVolume(long volume) { this.volume = volume; }
}
