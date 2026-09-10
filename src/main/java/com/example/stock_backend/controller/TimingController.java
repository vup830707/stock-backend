package com.example.stock_backend.controller;

import com.example.stock_backend.dto.timing.TimingEvaluateRequest;
import com.example.stock_backend.dto.timing.TimingEvaluateResponse;
import com.example.stock_backend.service.TimingService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/timing")
@CrossOrigin
public class TimingController {
    private final TimingService timingService;

    public TimingController(TimingService timingService) {
        this.timingService = timingService;
    }

    @PostMapping("/evaluate")
    public ResponseEntity<?> evaluate(@RequestBody TimingEvaluateRequest request) {
        if (request.getStockNo() == null || request.getStockNo().isBlank()) {
            return ResponseEntity.badRequest().body(Map.of(
                    "reason", "evaluate_failed",
                    "passed", false));
        }

        try {
            TimingEvaluateResponse response = timingService.evaluate(request.getStockNo());
            if ("insufficient_data".equals(response.getReason())) {
                return ResponseEntity.badRequest().body(response);
            }
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return ResponseEntity.status(502)
                    .body(TimingEvaluateResponse.evaluateFailed(request.getStockNo()));
        }
    }
}
