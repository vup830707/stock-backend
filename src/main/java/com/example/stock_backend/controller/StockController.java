package com.example.stock_backend.controller;

import com.example.stock_backend.model.Stock;
import com.example.stock_backend.repository.StockRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/stocks")
@CrossOrigin(origins = "*")
public class StockController {

    @Autowired
    private StockRepository stockRepository;

    @GetMapping
    public List<Stock> getAllStocks() {
        return stockRepository.findAll();
    }

    @GetMapping("/{id}")
    public Stock getStockById(@PathVariable String id) {
        return stockRepository.findById(id).orElse(null);
    }

    @PostMapping
    public Stock createStock(@RequestBody Stock stock) {
        return stockRepository.save(stock);
    }

    @PutMapping("/{id}")
    public Stock updateStock(@PathVariable String id, @RequestBody Stock stockDetails) {
        return stockRepository.findById(id).map(stock -> {
            stock.setCode(stockDetails.getCode());
            stock.setName(stockDetails.getName());
            stock.setPrice(stockDetails.getPrice());
            stock.setChange(stockDetails.getChange());
            return stockRepository.save(stock);
        }).orElse(null);
    }

    @DeleteMapping("/{id}")
    public void deleteStock(@PathVariable String id) {
        stockRepository.deleteById(id);
    }
}
