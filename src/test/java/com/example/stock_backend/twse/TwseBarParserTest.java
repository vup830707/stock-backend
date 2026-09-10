package com.example.stock_backend.twse;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TwseBarParserTest {

    private final ObjectMapper mapper = new ObjectMapper();

    @Test
    void parseRow_convertsRocDateAndStripsCommas() throws Exception {
        var row = mapper.readTree(
                "[\"112/12/18\",\"23,456,789\",\"1,000\",\"500.00\",\"510.50\",\"495.00\",\"505.00\",\"+5.00\",\"1,234\"]");
        Optional<TwseBar> parsed = TwseBarParser.parseRow(row);
        assertTrue(parsed.isPresent());
        TwseBar bar = parsed.get();
        assertEquals("2023/12/18", bar.dateSlash());
        assertEquals(23456789L, bar.volume());
        assertEquals(500.00, bar.open(), 1e-6);
        assertEquals(510.50, bar.high(), 1e-6);
        assertEquals(495.00, bar.low(), 1e-6);
        assertEquals(505.00, bar.close(), 1e-6);
    }

    @Test
    void parseRow_skipsMissingClose() throws Exception {
        var row = mapper.readTree(
                "[\"112/12/18\",\"100\",\"1\",\"500.00\",\"510.00\",\"495.00\",\"--\",\"0\",\"1\"]");
        assertTrue(TwseBarParser.parseRow(row).isEmpty());
    }
}
