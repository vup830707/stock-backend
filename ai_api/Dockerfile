# Step 1: 選 Python base image
FROM python:3.11-slim

# Step 2: 設定工作目錄
WORKDIR /app

# Step 3: 複製 requirements.txt（你的 Python 套件清單）
COPY requirements.txt .

# Step 4: 安裝套件
RUN pip install --no-cache-dir -r requirements.txt

# Step 5: 複製 ai_api 目錄所有程式碼到 /app
COPY . .

# Step 6: 設定容器啟動指令
# 假設你的 Flask 程式是 ai_api.py
ENV FLASK_APP=ai_api.py
ENV FLASK_RUN_HOST=0.0.0.0
EXPOSE 5000
CMD ["flask", "run"]
