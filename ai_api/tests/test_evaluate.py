from datetime import date, timedelta

import pandas as pd

from timing.bars import Bar
from timing.evaluate import evaluate


def make_bars(n=630):
    start = date(2018, 1, 2)
    bars = []
    for i in range(n):
        d = start + timedelta(days=i)
        c = 100.0
        bars.append(Bar(d.strftime("%Y/%m/%d"), c, c, c, c, 1000.0))
    return bars


def test_underperform_clears_trades_and_signal():
    bars = make_bars()

    def predict_oos(X, y, dates):
        vals = [0.01 if (i // 2) % 2 == 0 else -0.01 for i in range(len(dates))]
        return pd.Series(vals, index=dates.index)

    result = evaluate("2330", bars, predict_oos=predict_oos)

    assert result["passed"] is False
    assert result["reason"] == "after_cost_underperformed_buy_hold"
    assert result["metrics"]["roundTrips"] >= 3
    assert result["metrics"]["oosEnd"] == bars[-1].date
    assert result["metrics"]["barCount"] == 67
    assert result["trades"] == []
    assert result["currentSignal"] is None


def test_insufficient_data_does_not_predict():
    called = False

    def predict_oos(X, y, dates):
        nonlocal called
        called = True
        return pd.Series(dtype=float)

    result = evaluate("2330", make_bars(626), predict_oos=predict_oos)

    assert result["passed"] is False
    assert result["reason"] == "insufficient_data"
    assert result["metrics"] == {
        "strategyEndNav": 0,
        "buyHoldEndNav": 0,
        "roundTrips": 0,
        "oosStart": "",
        "oosEnd": "",
        "barCount": 626,
    }
    assert called is False


def test_too_few_round_trips_takes_precedence():
    bars = make_bars()

    def predict_oos(X, y, dates):
        return pd.Series(0.01, index=dates.index)

    result = evaluate("2330", bars, predict_oos=predict_oos)

    assert result["passed"] is False
    assert result["reason"] == "too_few_round_trips"
    assert result["metrics"]["roundTrips"] == 0
    assert result["trades"] == []
    assert result["currentSignal"] is None


def test_passed_evaluation_includes_trades_and_current_signal():
    start = date(2018, 1, 2)
    closes = [100.0, 110.0, 110.0, 100.0]
    bars = []
    previous_close = closes[0]
    for i in range(630):
        close = closes[i % len(closes)]
        bars.append(
            Bar(
                (start + timedelta(days=i)).strftime("%Y/%m/%d"),
                previous_close,
                max(previous_close, close),
                min(previous_close, close),
                close,
                1000.0,
            )
        )
        previous_close = close
    by_date = {bar.date: i for i, bar in enumerate(bars)}

    def predict_oos(X, y, dates):
        values = [
            0.01
            if bars[by_date[signal_date] + 1].close
            > bars[by_date[signal_date]].close
            else -0.01
            for signal_date in dates
        ]
        return pd.Series(values, index=dates.index)

    result = evaluate("2330", bars, predict_oos=predict_oos)

    assert result["passed"] is True
    assert result["reason"] == "passed"
    assert result["trades"]
    assert result["currentSignal"] in {"long", "flat"}
