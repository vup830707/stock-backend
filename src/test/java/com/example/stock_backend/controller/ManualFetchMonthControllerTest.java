package com.example.stock_backend.controller;

import com.example.stock_backend.dto.fetch.FetchMonthRequest;
import com.example.stock_backend.dto.fetch.FetchMonthResponse;
import com.example.stock_backend.service.TwseMonthFetchService;
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
class ManualFetchMonthControllerTest {

    @Mock
    TwseMonthFetchService fetchService;

    @Test
    void fetchMonth_blankStock_returnsBadRequest() {
        ManualFetchController controller = new ManualFetchController(null, fetchService);
        FetchMonthRequest request = new FetchMonthRequest();
        request.setStockNo(" ");
        request.setYearMonth("202103");

        ResponseEntity<?> response = controller.fetchMonth(request);

        assertEquals(400, response.getStatusCode().value());
        assertEquals("bad_request", ((Map<?, ?>) response.getBody()).get("reason"));
    }

    @Test
    void fetchMonth_statMiss_stillOkHttp() {
        ManualFetchController controller = new ManualFetchController(null, fetchService);
        FetchMonthRequest request = new FetchMonthRequest();
        request.setStockNo("9999");
        request.setYearMonth("202103");
        FetchMonthResponse body = new FetchMonthResponse("9999", "", "202103", 0, false);
        when(fetchService.fetchMonth("9999", "202103")).thenReturn(body);

        ResponseEntity<?> response = controller.fetchMonth(request);

        assertEquals(200, response.getStatusCode().value());
        assertSame(body, response.getBody());
    }
}
