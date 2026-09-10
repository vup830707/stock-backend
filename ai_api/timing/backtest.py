from dataclasses import dataclass

from timing.bars import Bar
from timing.constants import BUY_FEE, SELL_FEE, SELL_TAX


@dataclass
class BacktestResult:
    strategy_end_nav: float
    buy_hold_end_nav: float
    round_trips: int
    trades: list
    oos_start: str
    oos_end: str


def backtest(bars: list[Bar], signal_by_date: dict) -> BacktestResult:
    if not bars:
        return BacktestResult(1.0, 1.0, 0, [], "", "")

    cash = 1.0
    shares = 0.0
    position = "flat"
    trades = []
    round_trips = 0
    opened = False

    for i in range(1, len(bars)):
        target = signal_by_date.get(bars[i - 1].date, position)
        fill = bars[i].open
        if target != position:
            if target == "long" and position == "flat":
                shares = cash * (1 - BUY_FEE) / fill
                cash = 0.0
                trades.append({"date": bars[i].date, "side": "buy"})
                opened = True
            elif target == "flat" and position == "long":
                cash = shares * fill * (1 - SELL_FEE - SELL_TAX)
                shares = 0.0
                trades.append({"date": bars[i].date, "side": "sell"})
                if opened:
                    round_trips += 1
                    opened = False
            position = target

    last_close = bars[-1].close
    strategy_nav = cash + shares * last_close

    bh_shares = (1.0 * (1 - BUY_FEE)) / bars[0].open
    bh_nav = bh_shares * last_close

    return BacktestResult(
        strategy_end_nav=strategy_nav,
        buy_hold_end_nav=bh_nav,
        round_trips=round_trips,
        trades=trades,
        oos_start=bars[0].date,
        oos_end=bars[-1].date,
    )
