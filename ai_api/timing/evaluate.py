import pandas as pd
from sklearn.ensemble import HistGradientBoostingRegressor

from timing.backtest import BacktestResult, backtest
from timing.constants import MIN_FEATURE_ROWS
from timing.features import FEATURE_COLS, build_feature_frame, build_inference_row
from timing.walkforward import fold_ranges


def _model():
    return HistGradientBoostingRegressor(
        max_depth=3,
        max_iter=100,
        learning_rate=0.05,
        random_state=42,
    )


def to_response(
    stock_no,
    passed,
    reason,
    bt,
    current_signal,
    bar_count,
    trades,
):
    return {
        "stockNo": stock_no,
        "passed": passed,
        "reason": reason,
        "metrics": {
            "strategyEndNav": bt.strategy_end_nav,
            "buyHoldEndNav": bt.buy_hold_end_nav,
            "roundTrips": bt.round_trips,
            "oosStart": bt.oos_start,
            "oosEnd": bt.oos_end,
            "barCount": bar_count,
        },
        "trades": trades if passed else [],
        "currentSignal": current_signal if passed else None,
    }


def _oos_positions(row_count):
    return [
        i
        for _, _, test_start, test_end in fold_ranges(row_count)
        for i in range(test_start, test_end)
    ]


def _default_oos_predictions(X, y):
    predictions = pd.Series(index=X.index, dtype=float)
    for train_start, train_end, test_start, test_end in fold_ranges(len(X)):
        model = _model()
        model.fit(X.iloc[train_start:train_end], y.iloc[train_start:train_end])
        predictions.iloc[test_start:test_end] = model.predict(
            X.iloc[test_start:test_end]
        )
    return predictions


def evaluate(stock_no, bars, predict_oos=None):
    df = build_feature_frame(bars)
    if len(df) < MIN_FEATURE_ROWS:
        empty = BacktestResult(0, 0, 0, [], "", "")
        return to_response(
            stock_no, False, "insufficient_data", empty, None, len(bars), []
        )

    X = df[FEATURE_COLS]
    y = df["y"]
    dates = df["date"]
    predictions = (
        predict_oos(X, y, dates)
        if predict_oos is not None
        else _default_oos_predictions(X, y)
    )

    oos_positions = _oos_positions(len(df))
    signal_by_date = {
        dates.iloc[i]: "long" if predictions.loc[X.index[i]] > 0 else "flat"
        for i in oos_positions
    }
    first_date = dates.iloc[oos_positions[0]]
    last_date = dates.iloc[oos_positions[-1]]
    first_bar = next(i for i, bar in enumerate(bars) if bar.date == first_date)
    last_bar = next(i for i, bar in enumerate(bars) if bar.date == last_date)
    oos_bars = bars[first_bar:min(last_bar + 2, len(bars))]

    bt = backtest(oos_bars, signal_by_date)
    if bt.round_trips < 3:
        reason = "too_few_round_trips"
    elif bt.strategy_end_nav <= bt.buy_hold_end_nav:
        reason = "after_cost_underperformed_buy_hold"
    else:
        model = _model()
        model.fit(X, y)
        row = build_inference_row(bars)
        inference_X = pd.DataFrame(
            [[row[column] for column in FEATURE_COLS]],
            columns=FEATURE_COLS,
        )
        current_signal = "long" if model.predict(inference_X)[0] > 0 else "flat"
        return to_response(
            stock_no,
            True,
            "passed",
            bt,
            current_signal,
            len(oos_bars),
            bt.trades,
        )

    return to_response(
        stock_no, False, reason, bt, None, len(oos_bars), bt.trades
    )
