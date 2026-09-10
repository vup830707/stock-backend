package com.example.stock_backend.dto.timing;

public class TimingBarDto {
    private final String date;
    private final double open;
    private final double high;
    private final double low;
    private final double close;
    private final long volume;

    public TimingBarDto(
            String date,
            double open,
            double high,
            double low,
            double close,
            long volume) {
        this.date = date;
        this.open = open;
        this.high = high;
        this.low = low;
        this.close = close;
        this.volume = volume;
    }

    public String getDate() { return date; }
    public double getOpen() { return open; }
    public double getHigh() { return high; }
    public double getLow() { return low; }
    public double getClose() { return close; }
    public long getVolume() { return volume; }
}
