package com.example.stock_backend.service;

import com.example.stock_backend.dto.PredictRequest;
import com.example.stock_backend.dto.PredictResponse;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

@Service
public class StockPredictionService {

    private final RestTemplate restTemplate = new RestTemplate();
    private static final String FLASK_URL = "http://localhost:5000/predict_future";

    public PredictResponse predict(PredictRequest request) {

        if (request.getDays() == null) {
            request.setDays(5);
        }

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        HttpEntity<PredictRequest> entity = new HttpEntity<>(request, headers);

        ResponseEntity<PredictResponse> response =
                restTemplate.postForEntity(FLASK_URL, entity, PredictResponse.class);

        return response.getBody();
    }
}
