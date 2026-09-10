package com.example.stock_backend.service;

import com.example.stock_backend.dto.timing.TimingEvaluateRequest;
import com.example.stock_backend.dto.timing.TimingEvaluateResponse;
import com.example.stock_backend.model.StockHistorical;
import com.example.stock_backend.repository.StockHistoricalRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpEntity;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestTemplate;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TimingServiceTest {

    @Mock
    StockHistoricalRepository repo;
    @Mock
    RestTemplate restTemplate;

    @Test
    void evaluate_skipsPythonWhenOhlcvMissing() {
        StockHistorical row = new StockHistorical();
        row.setStockNo("2330");
        row.setDate("2020/01/02");
        row.setClosePrice(100);
        when(repo.findByStockNo("2330")).thenReturn(List.of(row));
        TimingService service = new TimingService(repo, restTemplate, "http://localhost/evaluate");

        TimingEvaluateResponse res = service.evaluate("2330");

        assertEquals("2330", res.getStockNo());
        assertEquals("insufficient_data", res.getReason());
        assertFalse(res.isPassed());
        verifyNoInteractions(restTemplate);
    }

    @Test
    void evaluate_sendsValidBarsSortedWithPythonFieldNames() {
        StockHistorical later = bar("2330", "2020/01/03", 101, 103, 100, 102, 2000);
        StockHistorical invalid = bar("2330", "2020/01/02", 0, 102, 99, 101, 1500);
        StockHistorical earlier = bar("2330", "2020/01/01", 99, 101, 98, 100, 1000);
        when(repo.findByStockNo("2330")).thenReturn(List.of(later, invalid, earlier));
        TimingEvaluateResponse pythonResponse = TimingEvaluateResponse.insufficientData("2330");
        when(restTemplate.postForEntity(
                eq("http://localhost/evaluate"),
                any(HttpEntity.class),
                eq(TimingEvaluateResponse.class)))
                .thenReturn(ResponseEntity.ok(pythonResponse));
        TimingService service = new TimingService(repo, restTemplate, "http://localhost/evaluate");

        TimingEvaluateResponse result = service.evaluate("2330");

        assertSame(pythonResponse, result);
        @SuppressWarnings("unchecked")
        ArgumentCaptor<HttpEntity<TimingEvaluateRequest>> entityCaptor =
                ArgumentCaptor.forClass((Class) HttpEntity.class);
        verify(restTemplate).postForEntity(
                eq("http://localhost/evaluate"),
                entityCaptor.capture(),
                eq(TimingEvaluateResponse.class));
        TimingEvaluateRequest payload = entityCaptor.getValue().getBody();
        assertEquals("2330", payload.getStockNo());
        assertEquals(2, payload.getBars().size());
        assertEquals("2020/01/01", payload.getBars().get(0).getDate());
        assertEquals(99, payload.getBars().get(0).getOpen(), 1e-6);
        assertEquals(101, payload.getBars().get(0).getHigh(), 1e-6);
        assertEquals(98, payload.getBars().get(0).getLow(), 1e-6);
        assertEquals(100, payload.getBars().get(0).getClose(), 1e-6);
        assertEquals(1000, payload.getBars().get(0).getVolume());
        assertEquals("2020/01/03", payload.getBars().get(1).getDate());
    }

    private StockHistorical bar(
            String stockNo,
            String date,
            double open,
            double high,
            double low,
            double close,
            long volume) {
        StockHistorical bar = new StockHistorical();
        bar.setStockNo(stockNo);
        bar.setDate(date);
        bar.setOpenPrice(open);
        bar.setHighPrice(high);
        bar.setLowPrice(low);
        bar.setClosePrice(close);
        bar.setVolume(volume);
        return bar;
    }
}
