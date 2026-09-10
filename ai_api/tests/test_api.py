from ai_api import app


def test_evaluate_rejects_empty_bars():
    client = app.test_client()
    res = client.post("/evaluate", json={"stockNo": "2330", "bars": []})
    assert res.status_code == 400
