import os
import pandas as pd
import numpy as np
from tqdm import tqdm
import torch
import torch.nn as nn
import torch.optim as optim
from torch.utils.data import DataLoader, TensorDataset
from sklearn.preprocessing import MinMaxScaler
import pickle
import warnings
from visualization import (
    plot_stock_prediction,
    plot_training_loss,
    plot_cumulative_earnings,
    plot_accuracy_comparison
)
from pymongo import MongoClient
import yfinance as yf

warnings.filterwarnings("ignore", category=FutureWarning)
device = torch.device('cuda' if torch.cuda.is_available() else 'cpu')

class LSTMModel(nn.Module):
    def __init__(self, input_size, hidden_size, num_layers, output_size, dropout=0.2):
        super(LSTMModel, self).__init__()
        self.hidden_size = hidden_size
        self.num_layers = num_layers
        self.lstm = nn.LSTM(input_size, hidden_size, num_layers, batch_first=True, dropout=dropout)
        self.fc = nn.Linear(hidden_size, output_size)

    def forward(self, x):
        h0 = torch.zeros(self.num_layers, x.size(0), self.hidden_size).to(device)
        c0 = torch.zeros(self.num_layers, x.size(0), self.hidden_size).to(device)
        out, _ = self.lstm(x, (h0, c0))
        out = self.fc(out[:, -1, :])
        return out

# def get_stock_data(ticker, data_dir='data'):
#     file_path = os.path.join(data_dir, f'{ticker}.csv')
#     data = pd.read_csv(file_path, index_col='Date', parse_dates=True)
#     return data

def get_stock_data(ticker: str):
    from pymongo import MongoClient
    import pandas as pd

    client = MongoClient("mongodb://mongo:27017")
    db = client["stockDB"]
    col = db["stocks"]

    # 查詢 Date / Close 欄位
    cursor = col.find(
        {"ticker": ticker},
        {"_id": 0, "Date": 1, "Close": 1, "Volume":1, "Year":1, "Month":1, "Day":1,
         "MA5":1,"MA10":1,"MA20":1,"RSI":1,"MACD":1,"VWAP":1,"SMA":1,"Std_dev":1,
         "Upper_band":1,"Lower_band":1,"Relative_Performance":1,"ATR":1,
         "Close_yes":1,"Open_yes":1,"High_yes":1,"Low_yes":1}
    ).sort("Date", 1)

    data = list(cursor)
    if len(data) == 0:
        raise ValueError(f"No data found for ticker {ticker}")

    df = pd.DataFrame(data)

    # LSTM 需要日期是 datetime
    df["Date"] = pd.to_datetime(df["Date"])
    df["Close"] = df["Close"].astype(float)

    return df

def get_stock_data_for_model(ticker, start_date, end_date):
    data = yf.download(ticker, start=start_date, end=end_date)

    if isinstance(data.columns, pd.MultiIndex):
        data.columns = data.columns.get_level_values(0)

    data = calculate_technical_indicators(data, start_date=start_date, end_date=end_date)

    # 重置 index 避免 datetime 當欄位
    data = data.reset_index(drop=True)

    return data

def calculate_technical_indicators(data, start_date=None, end_date=None):
    """
    计算股票的技术指标
    
    参数:
        data: DataFrame, 包含OHLCV数据的DataFrame
        start_date: str, 开始日期 (可选，用于相对表现计算)
        end_date: str, 结束日期 (可选，用于相对表现计算)
    
    返回:
        DataFrame: 添加了技术指标的数据
    """
    # 添加日期特征
    data['Year'] = data.index.year
    data['Month'] = data.index.month
    data['Day'] = data.index.day
    
    # 移动平均线
    data['MA5'] = data['Close'].shift(1).rolling(window=5).mean()
    data['MA10'] = data['Close'].shift(1).rolling(window=10).mean()
    data['MA20'] = data['Close'].shift(1).rolling(window=20).mean()
    
    # RSI指标
    delta = data['Close'].diff()
    gain = delta.clip(lower=0)
    loss = -delta.clip(upper=0)
    avg_gain = gain.rolling(window=14).mean()
    avg_loss = loss.rolling(window=14).mean()
    rs = avg_gain / avg_loss
    data['RSI'] = 100 - (100 / (1 + rs))
    
    # MACD指标
    exp1 = data['Close'].ewm(span=12, adjust=False).mean()
    exp2 = data['Close'].ewm(span=26, adjust=False).mean()
    data['MACD'] = exp1 - exp2
    data['Signal_Line'] = data['MACD'].ewm(span=9, adjust=False).mean()
    data['MACD_Histogram'] = data['MACD'] - data['Signal_Line']
    
    # VWAP指标
    data['VWAP'] = (data['Close'] * data['Volume']).cumsum() / data['Volume'].cumsum()
    
    # 布林带
    period = 20
    data['SMA'] = data['Close'].rolling(window=period).mean()
    data['Std_dev'] = data['Close'].rolling(window=period).std()
    data['Upper_band'] = data['SMA'] + 2 * data['Std_dev']
    data['Lower_band'] = data['SMA'] - 2 * data['Std_dev']
    
    # 相对大盘表现
    # if start_date and end_date:
    #     benchmark_data = yf.download('SPY', start=start_date, end=end_date)['Close']
    #     data['Relative_Performance'] = (data['Close'] / benchmark_data.values) * 100
    
    # # ROC指标
    # data['ROC'] = data['Close'].pct_change(periods=1) * 100

    if start_date and end_date:
        benchmark = yf.download('SPY', start=start_date, end=end_date)

    # ✅ 關鍵 1：攤平 SPY 的 MultiIndex
    if isinstance(benchmark.columns, pd.MultiIndex):
        benchmark.columns = benchmark.columns.get_level_values(0)

    benchmark = benchmark[['Close']].rename(columns={'Close': 'SPY_Close'})

    # ✅ 關鍵 2：用 index 對齊
    data = data.join(benchmark, how='inner')

    # ✅ 關鍵 3：Series / Series
    data['Relative_Performance'] = (data['Close'] / data['SPY_Close']) * 100

    
    # ATR指标
    high_low_range = data['High'] - data['Low']
    high_close_range = abs(data['High'] - data['Close'].shift(1))
    low_close_range = abs(data['Low'] - data['Close'].shift(1))
    true_range = pd.concat([high_low_range, high_close_range, low_close_range], axis=1).max(axis=1)
    data['ATR'] = true_range.rolling(window=14).mean()
    
    # 前一天数据
    data[['Close_yes', 'Open_yes', 'High_yes', 'Low_yes']] = data[['Close', 'Open', 'High', 'Low']].shift(1)
    
    # 删除缺失值
    data = data.dropna()
    
    return data

# def format_feature(data):
#     features = [
#         'Volume', 'Year', 'Month', 'Day', 'MA5', 'MA10', 'MA20', 'RSI', 'MACD',
#         'VWAP', 'SMA', 'Std_dev', 'Upper_band', 'Lower_band', 'Relative_Performance', 'ATR',
#         'Close_yes', 'Open_yes', 'High_yes', 'Low_yes'
#     ]
#     X = data[features].iloc[1:]
#     y = data['Close'].pct_change().iloc[1:]
#     return X, y

def format_feature(data):
    features = [
        'Volume', 'Year', 'Month', 'Day', 'MA5', 'MA10', 'MA20', 'RSI', 'MACD',
        'VWAP', 'SMA', 'Std_dev', 'Upper_band', 'Lower_band', 'Relative_Performance', 'ATR',
        'Close_yes', 'Open_yes', 'High_yes', 'Low_yes'
    ]
    X = data[features].iloc[1:]
    y = data['Close'].pct_change().iloc[1:].astype(float)
    return X, y


def prepare_data(data, n_steps):
    X, y = [], []
    for i in range(len(data) - n_steps):
        X.append(data[i:i + n_steps])
        y.append(data[i + n_steps])
    return np.array(X), np.array(y)

def visualize_predictions(ticker, data, predict_result, test_indices, predictions, actual_percentages, save_dir):
    actual_prices = data['Close'].loc[test_indices].values
    predicted_prices = np.array(predictions)
    
    mse = np.mean((predicted_prices - actual_prices) ** 2)
    rmse = np.sqrt(mse)
    mae = np.mean(np.abs(predicted_prices - actual_prices))
    accuracy = 1 - np.mean(np.abs(predicted_prices - actual_prices) / actual_prices)
    
    metrics = {'rmse': rmse, 'mae': mae, 'accuracy': accuracy}
    plot_stock_prediction(ticker, test_indices, actual_prices, predicted_prices, metrics, save_dir)
    
    return metrics

def train_and_predict_lstm(ticker, data, X, y, save_dir, n_steps=60, num_epochs=500, batch_size=32, learning_rate=0.001):
    print("len(data):", len(data))
    print("len(X):", len(X), "len(y):", len(y))
    print("last data y/m/d:", int(data.iloc[-1]["Year"]), int(data.iloc[-1]["Month"]), int(data.iloc[-1]["Day"]))


    # 数据归一化和准备部分
    scaler_y = MinMaxScaler()
    scaler_X = MinMaxScaler()
    scaler_y.fit(y.values.reshape(-1, 1))
    y_scaled = scaler_y.transform(y.values.reshape(-1, 1))
    X_scaled = scaler_X.fit_transform(X)

    X_train, y_train = prepare_data(X_scaled, n_steps)
    y_train = y_scaled[n_steps-1:-1]

    train_per = 0.8
    split_index = int(train_per * len(X_train))
    X_val = X_train[split_index-n_steps+1:]
    y_val = y_train[split_index-n_steps+1:]
    X_train = X_train[:split_index]
    y_train = y_train[:split_index]

    # PyTorch数据准备
    X_train_tensor = torch.tensor(X_train, dtype=torch.float32).to(device)
    y_train_tensor = torch.tensor(y_train, dtype=torch.float32).to(device)
    X_val_tensor = torch.tensor(X_val, dtype=torch.float32).to(device)
    y_val_tensor = torch.tensor(y_val, dtype=torch.float32).to(device)

    train_dataset = TensorDataset(X_train_tensor, y_train_tensor)
    val_dataset = TensorDataset(X_val_tensor, y_val_tensor)
    train_loader = DataLoader(train_dataset, batch_size=batch_size, shuffle=False)
    val_loader = DataLoader(val_dataset, batch_size=batch_size, shuffle=False)

    model = LSTMModel(input_size=X_train.shape[2], hidden_size=50, num_layers=2, output_size=1).to(device)
    criterion = nn.MSELoss()
    optimizer = optim.Adam(model.parameters(), lr=learning_rate)
    scheduler = optim.lr_scheduler.StepLR(optimizer, step_size=50, gamma=0.1)

    train_losses = []
    val_losses = []

    with tqdm(total=num_epochs, desc=f"Training {ticker}", unit="epoch") as pbar:
        for epoch in range(num_epochs):
            # 训练和验证循环
            model.train()
            epoch_train_loss = 0
            for inputs, targets in train_loader:
                outputs = model(inputs)
                loss = criterion(outputs, targets)
                optimizer.zero_grad()
                loss.backward()
                optimizer.step()
                epoch_train_loss += loss.item()

            avg_train_loss = epoch_train_loss / len(train_loader)
            train_losses.append(avg_train_loss)

            model.eval()
            epoch_val_loss = 0
            with torch.no_grad():
                for inputs, targets in val_loader:
                    outputs = model(inputs)
                    val_loss = criterion(outputs, targets)
                    epoch_val_loss += val_loss.item()

            avg_val_loss = epoch_val_loss / len(val_loader)
            val_losses.append(avg_val_loss)

            pbar.set_postfix({"Train Loss": avg_train_loss, "Val Loss": avg_val_loss})
            pbar.update(1)
            scheduler.step()

    # 使用可视化工具绘制损失曲线
    plot_training_loss(ticker, train_losses, val_losses, save_dir)

    # 预测
    model.eval()
    predictions = []
    test_indices = []
    predict_percentages = []
    actual_percentages = []

    # with torch.no_grad():
    #     for i in range(1 + split_index, len(X_scaled) + 1):
    #         x_input = torch.tensor(X_scaled[i - n_steps:i].reshape(1, n_steps, X_train.shape[2]), 
    #                              dtype=torch.float32).to(device)
    #         y_pred = model(x_input)
    #         y_pred = scaler_y.inverse_transform(y_pred.cpu().numpy().reshape(-1, 1))
    #         predictions.append((1 + y_pred[0][0]) * data['Close'].iloc[i - 2])
    #         test_indices.append(data.index[i - 1])
    #         predict_percentages.append(y_pred[0][0] * 100)
    #         actual_percentages.append(y[i - 1] * 100)

    # ✅ y_scaled 長度應該跟 y 一樣
    # ✅ y_true_percent 對應 day t 的變動（相對於 t-1）
    y_true_percent = y.values  # 這裡假設 y 是「報酬率/變動」(ex: close/prev - 1)

    with torch.no_grad():
        # 這裡的 t 是「要預測的那一天索引」
        # 我們要預測 t 的變動（相對 t-1），所以需要 X 的窗口 ending at t
        # 需要 X_scaled[t-n_steps : t]
        for t in range(split_index, len(X_scaled)):   # ✅ t 會跑到最後一天(len-1)
            x_input = torch.tensor(
                X_scaled[t - n_steps + 1 : t + 1].reshape(1, n_steps, X_train.shape[2]),
                dtype=torch.float32
            ).to(device)

            y_pred = model(x_input)
            y_pred = scaler_y.inverse_transform(y_pred.cpu().numpy().reshape(-1, 1))  # 預測變動
            change = float(y_pred[0][0])

            # ✅ 用 t-1 的 Close 還原 t 的 Close（這樣最後一天一定能算）
            base_close = float(data["Close"].iloc[t - 1])
            pred_close = (1.0 + change) * base_close

            predictions.append(pred_close)
            test_indices.append(int(data.index[t]))      # t 對應的日期
            predict_percentages.append(change * 100)

            # ✅ 實際變動（t 的變動）
            actual_percentages.append(float(y_true_percent[t]) * 100)

    # 使用可视化工具绘制累积收益率曲线
    plot_cumulative_earnings(ticker, test_indices, actual_percentages, predict_percentages, save_dir)

    predict_result = {str(date): pred / 100 for date, pred in zip(test_indices, predict_percentages)}
    return predict_result, test_indices, predictions, actual_percentages

def save_predictions_with_indices(ticker, test_indices, predictions, save_dir):
    df = pd.DataFrame({
        'Date': test_indices,
        'Prediction': predictions
    })

    file_path = os.path.join(save_dir, 'predictions', f'{ticker}_predictions.pkl')
    os.makedirs(os.path.dirname(file_path), exist_ok=True)
    with open(file_path, 'wb') as file:
        pickle.dump(df, file)

    print(f'Saved predictions for {ticker} to {file_path}')

def _to_yyyymmdd_slash(x):
    # x 可能是 Timestamp / datetime / string
    dt = pd.to_datetime(x)
    return dt.strftime("%Y/%m/%d")

def predict(ticker_name, stock_data, stock_features, save_dir, epochs=50, batch_size=32, learning_rate=0.001):
    all_predictions_lstm = {}
    prediction_metrics = {}

    print(f"\nProcessing {ticker_name}")
    data = stock_data
    X, y = stock_features

    print("y type:", type(y))
    try:
        print("y index head:", y.index[:3])
    except:
        pass
        
    predict_result, test_indices, predictions, actual_percentages = train_and_predict_lstm(
        ticker_name, data, X, y, save_dir, num_epochs=epochs, batch_size=batch_size, learning_rate=learning_rate
    )

    print("test_indices type:", type(test_indices))
    try:
        print("test_indices sample:", list(test_indices)[:5])
    except:
        pass
    print("data.index type:", type(stock_data.index), "head:", stock_data.index[:3])
    print("columns:", list(stock_data.columns)[:10])

    all_predictions_lstm[ticker_name] = predict_result
    
    metrics = visualize_predictions(ticker_name, data, predict_result, test_indices, predictions, actual_percentages, save_dir)
    prediction_metrics[ticker_name] = metrics
    
    save_predictions_with_indices(ticker_name, test_indices, predictions, save_dir)

    # =====================
    # ✅ 新增：把 test_indices 轉成 testDates（給前端對齊用）
    # =====================
    test_dates = []
    try:
        test_indices_list = test_indices.tolist() if hasattr(test_indices, "tolist") else list(test_indices)

        for ridx in test_indices_list:
            row = data.iloc[int(ridx)]   # ✅ 直接用原始 row index
            yy = int(row["Year"])
            mm = int(row["Month"])
            dd = int(row["Day"])
            test_dates.append(f"{yy:04d}/{mm:02d}/{dd:02d}")

        print(f"test_dates len={len(test_dates)}, predictions len={len(predictions)}")
    except Exception as e:
        print("[WARN] build test_dates failed:", e)
        test_dates = []

    # 保存预测指标
    os.makedirs(os.path.join(save_dir, 'output'), exist_ok=True)
    metrics_df = pd.DataFrame(prediction_metrics).T
    metrics_df.to_csv(os.path.join(save_dir, 'output', f'{ticker_name}_prediction_metrics.csv'))
    print("\nPrediction metrics summary:")
    print(metrics_df.describe())

    # 使用可视化工具绘制准确度对比图
    plot_accuracy_comparison(prediction_metrics, save_dir)

    # 生成汇总报告
    summary = {
        'Average Accuracy': np.mean([m['accuracy'] * 100 for m in prediction_metrics.values()]),
        'Best Stock': max(prediction_metrics.items(), key=lambda x: x[1]['accuracy'])[0],
        'Worst Stock': min(prediction_metrics.items(), key=lambda x: x[1]['accuracy'])[0],
        'Average RMSE': metrics_df['rmse'].mean(),
        'Average MAE': metrics_df['mae'].mean()
    }

    # 保存汇总报告
    with open(os.path.join(save_dir, 'output', f'{ticker_name}_prediction_summary.txt'), 'w') as f:
        for key, value in summary.items():
            f.write(f'{key}: {value}\n')

    print("\nPrediction Summary:")
    for key, value in summary.items():
        print(f"{key}: {value}")

    # return metrics
    # 🔹 這裡回傳未來股價 + 指標
    return {
        "historicalPredictions": [float(p) for p in predictions],   # 歷史預測股價
        "testDates": test_dates,
        "metrics": metrics
    }

def predict_future_prices(stock_data, stock_features, n_future=5, n_steps=60):
    """
    使用訓練好的 LSTM 模型，從最後一個可用序列預測未來 n_future 天股價
    """
    X, y = stock_features

    # 載入模型 (假設你每次都是重新訓練)
    model = LSTMModel(input_size=X.shape[1], hidden_size=50, num_layers=2, output_size=1).to(device)
    # 如果你有儲存的模型，可以 load_state_dict 這裡

    # 將資料標準化
    scaler_X = MinMaxScaler()
    scaler_y = MinMaxScaler()
    X_scaled = scaler_X.fit_transform(X)
    y_scaled = scaler_y.fit_transform(y.values.reshape(-1, 1))

    # 取最後 n_steps 當作初始序列
    last_sequence = X_scaled[-n_steps:].reshape(1, n_steps, X_scaled.shape[1])
    last_sequence = torch.tensor(last_sequence, dtype=torch.float32).to(device)

    model.eval()
    future_prices = []
    current_sequence = last_sequence.clone()

    with torch.no_grad():
        for _ in range(n_future):
            pred = model(current_sequence)
            pred_price = scaler_y.inverse_transform(pred.cpu().numpy().reshape(-1, 1))[0][0]
            future_prices.append(pred_price)

            # 更新 sequence: 滾動 window
            new_feature = current_sequence.cpu().numpy()[0, 1:, :]
            # 這裡僅示範把預測值放回 Close 欄位，其餘欄位可以補 0
            next_input = np.zeros((1, X_scaled.shape[1]))
            next_input[0, 0] = pred_price  # 假設第一個欄位是 Close
            new_feature = np.vstack([new_feature, next_input])
            current_sequence = torch.tensor(new_feature.reshape(1, n_steps, X_scaled.shape[1]), dtype=torch.float32).to(device)

    return future_prices

if __name__ == "__main__":
    # tickers = [
    #     'AAPL', 'MSFT', 'GOOGL', 'AMZN', 'TSLA',       # 科技
    #     'JPM', 'BAC', 'C', 'WFC', 'GS',                # 金融
    #     'JNJ', 'PFE', 'MRK', 'ABBV', 'BMY',            # 医药
    #     'XOM', 'CVX', 'COP', 'SLB', 'BKR',             # 能源
    #     'DIS', 'NFLX', 'CMCSA', 'NKE', 'SBUX',         # 消费
    #     'CAT', 'DE', 'MMM', 'GE', 'HON'                # 工业
    # ]
    tickers = [
        '2330.TW'
    ]

    save_dir = 'results'  # 设置保存目录
    for ticker_name in tickers:
        stock_data = get_stock_data(ticker_name)
        stock_features = format_feature(stock_data)
        predict(
            ticker_name=ticker_name,
            stock_data=stock_data,
            stock_features=stock_features,
            save_dir=save_dir
        )