package com.example.stock_backend.service;

import com.example.stock_backend.dto.fetch.FetchMonthResponse;
import com.example.stock_backend.twse.TwseBar;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.client.RestTemplate;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TwseMonthFetchServiceTest {

    @Mock
    StockHistoryWriter writer;
    @Mock
    RestTemplate timingRestTemplate;

    @Test
    void fetchMonth_statNotOk_returnsSoftFailure() {
        when(timingRestTemplate.getForObject(contains("20210301"), eq(String.class)))
                .thenReturn("{\"stat\":\"很抱歉，沒有符合條件的資料!\"}");
        TwseMonthFetchService service = new TwseMonthFetchService(writer, timingRestTemplate);

        FetchMonthResponse result = service.fetchMonth("2330", "202103");

        assertEquals("2330", result.getStockNo());
        assertEquals("202103", result.getYearMonth());
        assertEquals("", result.getStockName());
        assertEquals(0, result.getUpserted());
        assertFalse(result.isTwseOk());
        verify(writer, never()).upsert(any(), any(), any());
    }

    @Test
    void fetchMonth_okRow_upsertsAndReturnsName() {
        String json = "{"
                + "\"stat\":\"OK\","
                + "\"title\":\"2330 台積電 日成交資訊\","
                + "\"data\":[[\"110/03/02\",\"1,000\",\"1\",\"600.00\",\"610.00\",\"590.00\",\"605.00\",\"+5.00\",\"10\"]]"
                + "}";
        when(timingRestTemplate.getForObject(contains("20210301"), eq(String.class)))
                .thenReturn(json);
        TwseMonthFetchService service = new TwseMonthFetchService(writer, timingRestTemplate);

        FetchMonthResponse result = service.fetchMonth("2330", "202103");

        assertTrue(result.isTwseOk());
        assertEquals(1, result.getUpserted());
        assertEquals("台積電", result.getStockName());
        verify(writer).upsert(eq("2330"), eq("台積電"), any(TwseBar.class));
    }
}
