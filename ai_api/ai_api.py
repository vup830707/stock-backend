from flask import Flask, request, jsonify
from flask_cors import CORS
from timing.bars import Bar
from timing.evaluate import evaluate

app = Flask(__name__)
CORS(app)


@app.route("/evaluate", methods=["POST"])
def evaluate_timing():
    data = request.get_json(silent=True) or {}
    stock_no = data.get("stockNo")
    raw_bars = data.get("bars")
    if not stock_no or not raw_bars:
        return jsonify({"error": "stockNo and bars are required"}), 400
    try:
        bars = [
            Bar(
                date=b["date"],
                open=float(b["open"]),
                high=float(b["high"]),
                low=float(b["low"]),
                close=float(b["close"]),
                volume=float(b["volume"]),
            )
            for b in raw_bars
        ]
        return jsonify(evaluate(stock_no, bars))
    except Exception as e:
        return jsonify({"error": str(e), "reason": "evaluate_failed"}), 500


if __name__ == "__main__":
    app.run(host="0.0.0.0", port=5000)
