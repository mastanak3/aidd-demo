import random

from fastapi import FastAPI, Query
from fastapi.responses import JSONResponse

app = FastAPI(title="ISBN Lookup Service")

BOOKS: dict[str, str] = {
    "テスト駆動開発": "978-4274217883",
    "リファクタリング": "978-4274224546",
    "Clean Code": "978-0132350884",
    "Clean Architecture": "978-0134494166",
    "Effective Java": "978-0134685991",
    "達人プログラマー": "978-4274226298",
    "リーダブルコード": "978-4873115658",
    "エリック・エヴァンスのドメイン駆動設計": "978-4798121963",
    "レガシーコード改善ガイド": "978-4798116839",
    "オブジェクト指向における再利用のためのデザインパターン": "978-4797311129",
    "マイクロサービスアーキテクチャ": "978-4873117607",
    "アジャイルサムライ": "978-4274068560",
    "エクストリームプログラミング": "978-4274217623",
    "継続的デリバリー": "978-4048930581",
    "人月の神話": "978-4621066089",
    "CODE COMPLETE": "978-4891004552",
    "プログラミング作法": "978-4048930529",
    "情熱プログラマー": "978-4274067938",
    "Head Firstデザインパターン": "978-4873119762",
    "Java言語で学ぶデザインパターン入門": "978-4815609801",
}

FAILURE_RATE = 0.3


@app.get("/health")
def health():
    return {"status": "ok"}


@app.get("/api/isbn-lookup")
def isbn_lookup(title: str = Query(...)):
    if random.random() < FAILURE_RATE:
        return JSONResponse(
            status_code=503,
            content={"error": "Service Unavailable"},
        )

    isbn = BOOKS.get(title)
    if isbn is None:
        return JSONResponse(
            status_code=404,
            content={"error": "Book not found"},
        )

    return {"title": title, "isbn": isbn}
