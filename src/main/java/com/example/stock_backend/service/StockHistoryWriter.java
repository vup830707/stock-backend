package com.example.stock_backend.service;

import com.example.stock_backend.model.StockHistorical;
import com.example.stock_backend.repository.StockHistoricalRepository;
import com.example.stock_backend.twse.TwseBar;
import org.springframework.stereotype.Service;

@Service
public class StockHistoryWriter {
    private final StockHistoricalRepository repo;

    public StockHistoryWriter(StockHistoricalRepository repo) {
        this.repo = repo;
    }

    public void upsert(String stockNo, String stockName, TwseBar bar) {
        String date = bar.dateSlash();
        StockHistorical item = repo.findByStockNoAndDate(stockNo, date).orElseGet(StockHistorical::new);
        item.setStockNo(stockNo);
        item.setStockName(stockName);
        item.setDate(date);
        item.setOpenPrice(bar.open());
        item.setHighPrice(bar.high());
        item.setLowPrice(bar.low());
        item.setClosePrice(bar.close());
        item.setVolume(bar.volume());
        repo.save(item);
    }
}
