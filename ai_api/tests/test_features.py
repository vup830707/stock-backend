from datetime import date, timedelta
from timing.bars import Bar
from timing.features import build_feature_frame


def make_bars(n=80):
    start = date(2020, 1, 2)
    bars = []
    for i in range(n):
        d = start + timedelta(days=i)
        c = 100.0 + i
        bars.append(Bar(d.strftime("%Y/%m/%d"), c, c + 1, c - 1, c, 1000.0))
    return bars


def test_ret_1_matches_adjacent_closes_not_next():
    bars = make_bars(80)
    df = build_feature_frame(bars)
    last = df.iloc[-1]
    today_date = last["date"]
    today_bar = next(b for b in bars if b.date == today_date)
    idx = bars.index(today_bar)
    expected = (bars[idx].close - bars[idx - 1].close) / bars[idx - 1].close
    assert abs(last["ret_1"] - expected) < 1e-9
    leak = (bars[idx + 1].close - bars[idx].close) / bars[idx].close
    assert abs(last["ret_1"] - leak) > 1e-6
    assert abs(last["y"] - leak) < 1e-9
