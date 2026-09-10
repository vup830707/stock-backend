import numpy as np
import pandas as pd
from timing.bars import Bar

FEATURE_COLS = [
    "ret_1", "ret_5", "ret_10", "ret_20",
    "sma5_rel", "sma20_rel", "sma60_rel",
    "vol_20", "volume_rel", "rsi_14", "atr_rel",
]


def _feature_columns_only(bars: list[Bar]) -> pd.DataFrame:
    df = pd.DataFrame([b.__dict__ for b in bars])
    close = df["close"]
    df["ret_1"] = close.pct_change(1)
    df["ret_5"] = close.pct_change(5)
    df["ret_10"] = close.pct_change(10)
    df["ret_20"] = close.pct_change(20)
    df["sma5_rel"] = close / close.rolling(5).mean() - 1
    df["sma20_rel"] = close / close.rolling(20).mean() - 1
    df["sma60_rel"] = close / close.rolling(60).mean() - 1
    df["vol_20"] = df["ret_1"].rolling(20).std()
    df["volume_rel"] = df["volume"] / df["volume"].rolling(20).mean() - 1
    delta = close.diff()
    gain = delta.clip(lower=0).rolling(14).mean()
    loss = (-delta.clip(upper=0)).rolling(14).mean()
    rs = gain / loss.replace(0, np.nan)
    df["rsi_14"] = 100 - (100 / (1 + rs))
    df.loc[loss.eq(0) & gain.gt(0), "rsi_14"] = 100.0
    df.loc[loss.eq(0) & gain.eq(0), "rsi_14"] = 50.0
    high_low = df["high"] - df["low"]
    high_close = (df["high"] - close.shift(1)).abs()
    low_close = (df["low"] - close.shift(1)).abs()
    tr = pd.concat([high_low, high_close, low_close], axis=1).max(axis=1)
    df["atr_rel"] = tr.rolling(14).mean() / close
    return df


def build_feature_frame(bars: list[Bar]) -> pd.DataFrame:
    df = _feature_columns_only(bars)
    close = df["close"]
    df["y"] = close.pct_change().shift(-1)
    out = df.dropna().reset_index(drop=True)
    return out


def build_inference_row(bars: list[Bar]) -> pd.Series:
    df = _feature_columns_only(bars)
    return df.dropna(subset=FEATURE_COLS).iloc[-1]
