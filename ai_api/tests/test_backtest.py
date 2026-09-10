from timing.bars import Bar
from timing.backtest import backtest


def test_round_trip_same_price_applies_fees_and_tax():
    bars = [
        Bar("2020/01/02", 100, 100, 100, 100, 1),
        Bar("2020/01/03", 100, 100, 100, 100, 1),
        Bar("2020/01/06", 100, 100, 100, 100, 1),
        Bar("2020/01/07", 100, 100, 100, 100, 1),
    ]
    # signal on day0 -> buy next open (day1); signal on day1 -> sell next open (day2)
    signals = {"2020/01/02": "long", "2020/01/03": "flat", "2020/01/06": "flat"}
    result = backtest(bars, signals)
    expected = (1 - 0.001425) * (1 - 0.001425 - 0.003)
    assert abs(result.strategy_end_nav - expected) < 1e-9
    assert result.round_trips == 1
    assert result.trades == [
        {"date": "2020/01/03", "side": "buy"},
        {"date": "2020/01/06", "side": "sell"},
    ]
