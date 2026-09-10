package com.example.stock_backend.service;

import com.example.stock_backend.model.StockHistorical;
import com.example.stock_backend.repository.StockHistoricalRepository;
import com.example.stock_backend.twse.TwseBar;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class StockHistoryWriterTest {

    @Mock
    StockHistoricalRepository repo;
    @InjectMocks
    StockHistoryWriter writer;

    @Test
    void upsert_updatesExistingBar() {
        StockHistorical existing = new StockHistorical();
        existing.setStockNo("2330");
        existing.setDate("2023/12/18");
        existing.setClosePrice(1);
        when(repo.findByStockNoAndDate("2330", "2023/12/18")).thenReturn(Optional.of(existing));

        TwseBar bar = new TwseBar(LocalDate.of(2023, 12, 18), 500, 510, 495, 505, 1000L);
        writer.upsert("2330", "台積電", bar);

        ArgumentCaptor<StockHistorical> cap = ArgumentCaptor.forClass(StockHistorical.class);
        verify(repo).save(cap.capture());
        assertEquals(505, cap.getValue().getClosePrice(), 1e-6);
        assertEquals(500, cap.getValue().getOpenPrice(), 1e-6);
        assertEquals(510, cap.getValue().getHighPrice(), 1e-6);
        assertEquals(495, cap.getValue().getLowPrice(), 1e-6);
        assertEquals(1000L, cap.getValue().getVolume());
        assertEquals("台積電", cap.getValue().getStockName());
    }

    @Test
    void upsert_savesNewBarWhenNoRecordExists() {
        when(repo.findByStockNoAndDate("2330", "2023/12/18")).thenReturn(Optional.empty());

        TwseBar bar = new TwseBar(LocalDate.of(2023, 12, 18), 500, 510, 495, 505, 1000L);
        writer.upsert("2330", "台積電", bar);

        ArgumentCaptor<StockHistorical> cap = ArgumentCaptor.forClass(StockHistorical.class);
        verify(repo).save(cap.capture());
        StockHistorical saved = cap.getValue();
        assertEquals("2330", saved.getStockNo());
        assertEquals("台積電", saved.getStockName());
        assertEquals("2023/12/18", saved.getDate());
        assertEquals(500, saved.getOpenPrice(), 1e-6);
        assertEquals(510, saved.getHighPrice(), 1e-6);
        assertEquals(495, saved.getLowPrice(), 1e-6);
        assertEquals(505, saved.getClosePrice(), 1e-6);
        assertEquals(1000L, saved.getVolume());
    }
}
