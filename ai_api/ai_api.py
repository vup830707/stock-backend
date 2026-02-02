from flask import Flask, request, jsonify
from stock_prediction_lstm import get_stock_data, format_feature, predict_future_prices, predict, get_stock_data_for_model
import os
from flask_cors import CORS
from datetime import datetime, timedelta

app = Flask(__name__)
CORS(app)
SAVE_DIR = "results"

@app.route("/predict_future", methods=["POST"])
def predict_future_stock():
    data = request.json
    ticker = data.get("ticker")
    days = data.get("days", 1)  # 預測天數，預設 1 天
    start_date = data.get("startDate")
    end_date = data.get("endDate")

    if not ticker:
        return jsonify({"error": "ticker is required"}), 400

    try:
        # 🔹 印出 ticker 確認
        print(f"Received ticker: {ticker}")
        print(f"Date range: {start_date} ~ {end_date}")

        # ✅ yfinance end 是 end-exclusive，所以這裡 end + 1 天，等同「包含 end_date」
        end_dt = datetime.strptime(end_date, "%Y-%m-%d") + timedelta(days=1)
        end_inclusive = end_dt.strftime("%Y-%m-%d")
        print(f"Date range (inclusive for fetch): {start_date} ~ {end_inclusive}")

        # 1. 讀取股票資料
        stock_data = get_stock_data_for_model(ticker+ ".TW", start_date, end_inclusive)

        # （可選）印最後一天確認
        try:
            if all(c in stock_data.columns for c in ["Year", "Month", "Day"]):
                print("stock_data last day:", int(stock_data.iloc[-1]["Year"]), int(stock_data.iloc[-1]["Month"]), int(stock_data.iloc[-1]["Day"]))
            else:
                print("stock_data tail:", stock_data.tail(1))
            print("stock_data len:", len(stock_data))
        except Exception as _:
            pass

        stock_features = format_feature(stock_data)

        # 2. 計算歷史測試集指標（accuracy, RMSE, MAE）
        result  = predict(
            ticker_name=ticker,
            stock_data=stock_data,
            stock_features=stock_features,
            save_dir=SAVE_DIR,
            epochs=10
        )

        # 3回傳前端用
        return jsonify({
            "ticker": ticker,
            "historicalPredictions": result["historicalPredictions"],
            "testDates": result.get("testDates", []),
            "metrics": result["metrics"]
        })

    except Exception as e:
        return jsonify({"error": str(e)}), 500


if __name__ == "__main__":
    os.makedirs(SAVE_DIR, exist_ok=True)
    app.run(host="0.0.0.0", port=5000)