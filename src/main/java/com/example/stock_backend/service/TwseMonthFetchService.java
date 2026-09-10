package com.example.stock_backend.service;

import com.example.stock_backend.dto.fetch.FetchMonthResponse;
import com.example.stock_backend.twse.TwseBarParser;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
public class TwseMonthFetchService {
    private static final DateTimeFormatter YM = DateTimeFormatter.ofPattern("yyyyMM");
    private static final DateTimeFormatter TWSE_DAY = DateTimeFormatter.ofPattern("yyyyMMdd");
    private static final Pattern STOCK_NAME = Pattern.compile(
            "\\d{4}\\s+(.+?)\\s+(?:各日成交資訊|日成交資訊)");

    private final StockHistoryWriter writer;
    private final RestTemplate timingRestTemplate;
    private final ObjectMapper mapper = new ObjectMapper();

    public TwseMonthFetchService(StockHistoryWriter writer, RestTemplate timingRestTemplate) {
        this.writer = writer;
        this.timingRestTemplate = timingRestTemplate;
    }

    public FetchMonthResponse fetchMonth(String stockNo, String yearMonth) {
        try {
            YearMonth ym = YearMonth.parse(yearMonth, YM);
            String twseDate = ym.atDay(1).format(TWSE_DAY);
            String url = String.format(
                    "https://www.twse.com.tw/exchangeReport/STOCK_DAY?response=json&date=%s&stockNo=%s",
                    twseDate, stockNo);
            String body = timingRestTemplate.getForObject(url, String.class);
            if (body == null || body.isBlank()) {
                return miss(stockNo, yearMonth);
            }
            JsonNode root = mapper.readTree(body);
            if (!"OK".equals(root.path("stat").asText())) {
                return miss(stockNo, yearMonth);
            }
            String stockTitle = root.path("title").asText();
            String stockName = parseStockName(stockTitle);
            int upserted = 0;
            for (JsonNode row : root.path("data")) {
                var parsed = TwseBarParser.parseRow(row);
                if (parsed.isPresent()) {
                    writer.upsert(stockNo, stockName, parsed.get());
                    upserted++;
                }
            }
            if (upserted == 0) {
                return miss(stockNo, yearMonth);
            }
            return new FetchMonthResponse(stockNo, stockName, yearMonth, upserted, true);
        } catch (Exception e) {
            return miss(stockNo, yearMonth);
        }
    }

    private static String parseStockName(String title) {
        if (title == null || title.isBlank()) {
            return "";
        }
        Matcher matcher = STOCK_NAME.matcher(title);
        return matcher.find() ? matcher.group(1).trim() : "";
    }

    private static FetchMonthResponse miss(String stockNo, String yearMonth) {
        return new FetchMonthResponse(stockNo, "", yearMonth, 0, false);
    }
}
