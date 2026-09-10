package com.example.stock_backend.repository;

import com.example.stock_backend.model.StockHistorical;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;
import java.util.Optional;

public interface StockHistoricalRepository extends MongoRepository<StockHistorical, String> {
    boolean existsByStockNoAndDate(String stockNo, String date);
    Optional<StockHistorical> findByStockNoAndDate(String stockNo, String date);
    List<StockHistorical> findByStockNo(String stockNo);
}
