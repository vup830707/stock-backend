package com.example.stock_backend.controller;

import com.example.stock_backend.dto.PredictRequest;
import com.example.stock_backend.dto.PredictResponse;
import com.example.stock_backend.service.StockPredictionService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/predict")
@CrossOrigin
public class StockPredictionController {

    private final StockPredictionService predictionService;

    public StockPredictionController(StockPredictionService predictionService) {
        this.predictionService = predictionService;
    }

    @PostMapping
    public ResponseEntity<?> predict(@RequestBody PredictRequest request) {
        if (request.getTicker() == null || request.getTicker().isEmpty()) {
            return ResponseEntity.badRequest().body("ticker is required");
        }

        PredictResponse response = predictionService.predict(request);
        return ResponseEntity.ok(response);
    }
}
