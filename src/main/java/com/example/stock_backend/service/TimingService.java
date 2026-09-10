package com.example.stock_backend.service;

import com.example.stock_backend.dto.timing.TimingBarDto;
import com.example.stock_backend.dto.timing.TimingEvaluateRequest;
import com.example.stock_backend.dto.timing.TimingEvaluateResponse;
import com.example.stock_backend.model.StockHistorical;
import com.example.stock_backend.repository.StockHistoricalRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.Comparator;
import java.util.List;

@Service
public class TimingService {
    private final StockHistoricalRepository repo;
    private final RestTemplate timingRestTemplate;
    private final String url;

    public TimingService(
            StockHistoricalRepository repo,
            RestTemplate timingRestTemplate,
            @Value("${timing.service.url}") String url) {
        this.repo = repo;
        this.timingRestTemplate = timingRestTemplate;
        this.url = url;
    }

    public TimingEvaluateResponse evaluate(String stockNo) {
        List<StockHistorical> validBars = repo.findByStockNo(stockNo).stream()
                .filter(this::isValid)
                .sorted(Comparator.comparing(StockHistorical::getDate))
                .toList();
        if (validBars.isEmpty()) {
            return TimingEvaluateResponse.insufficientData(stockNo);
        }

        List<TimingBarDto> bars = validBars.stream()
                .map(this::toTimingBar)
                .toList();
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<TimingEvaluateRequest> entity =
                new HttpEntity<>(new TimingEvaluateRequest(stockNo, bars), headers);
        return timingRestTemplate.postForEntity(url, entity, TimingEvaluateResponse.class).getBody();
    }

    private boolean isValid(StockHistorical bar) {
        return bar.getOpenPrice() > 0
                && bar.getHighPrice() > 0
                && bar.getLowPrice() > 0
                && bar.getClosePrice() > 0;
    }

    private TimingBarDto toTimingBar(StockHistorical bar) {
        return new TimingBarDto(
                bar.getDate(),
                bar.getOpenPrice(),
                bar.getHighPrice(),
                bar.getLowPrice(),
                bar.getClosePrice(),
                bar.getVolume());
    }
}
