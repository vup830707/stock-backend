package com.example.stock_backend.controller;

import com.example.stock_backend.dto.timing.TimingEvaluateRequest;
import com.example.stock_backend.dto.timing.TimingEvaluateResponse;
import com.example.stock_backend.service.TimingService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.ResponseEntity;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TimingControllerTest {

    @Mock
    TimingService timingService;

    @Test
    void evaluate_returnsBadRequestForBlankStockNumber() {
        TimingController controller = new TimingController(timingService);
        TimingEvaluateRequest request = new TimingEvaluateRequest();
        request.setStockNo(" ");

        ResponseEntity<?> response = controller.evaluate(request);

        assertEquals(400, response.getStatusCode().value());
        assertEquals("evaluate_failed", ((Map<?, ?>) response.getBody()).get("reason"));
    }

    @Test
    void evaluate_returnsBadRequestForInsufficientData() {
        TimingController controller = new TimingController(timingService);
        TimingEvaluateRequest request = new TimingEvaluateRequest();
        request.setStockNo("2330");
        TimingEvaluateResponse result = TimingEvaluateResponse.insufficientData("2330");
        when(timingService.evaluate("2330")).thenReturn(result);

        ResponseEntity<?> response = controller.evaluate(request);

        assertEquals(400, response.getStatusCode().value());
        assertSame(result, response.getBody());
    }

    @Test
    void evaluate_returnsBadGatewayWhenPythonFails() {
        TimingController controller = new TimingController(timingService);
        TimingEvaluateRequest request = new TimingEvaluateRequest();
        request.setStockNo("2330");
        when(timingService.evaluate("2330")).thenThrow(new RuntimeException("timeout"));

        ResponseEntity<?> response = controller.evaluate(request);

        assertEquals(502, response.getStatusCode().value());
        TimingEvaluateResponse body = (TimingEvaluateResponse) response.getBody();
        assertEquals("2330", body.getStockNo());
        assertEquals("evaluate_failed", body.getReason());
    }
}
