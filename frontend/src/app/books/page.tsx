"use client";

import { useEffect, useState } from "react";
import Link from "next/link";
import { Book } from "@/lib/types";
import { getBooks, deleteBook } from "@/lib/api";
import ErrorMessage from "@/components/ErrorMessage";

export default function BooksPage() {
  const [books, setBooks] = useState<Book[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

  const fetchBooks = async () => {
    try {
      setLoading(true);
      setError(null);
      const data = await getBooks();
      setBooks(data);
    } catch (e) {
      setError(e instanceof Error ? e.message : "書籍の取得に失敗しました");
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    fetchBooks();
  }, []);

  const handleDelete = async (id: number) => {
    if (!confirm("この書籍を削除しますか？")) return;
    try {
      await deleteBook(id);
      setBooks((prev) => prev.filter((b) => b.id !== id));
    } catch (e) {
      setError(e instanceof Error ? e.message : "削除に失敗しました");
    }
  };

  return (
    <div>
      <div className="page-header">
        <h1>書籍管理</h1>
        <Link href="/books/new">
          <button className="btn-primary">書籍を登録</button>
        </Link>
      </div>
      <ErrorMessage message={error} />
      {loading ? (
        <div className="loading">読み込み中...</div>
      ) : books.length === 0 ? (
        <div className="empty">書籍が登録されていません</div>
      ) : (
        <table>
          <thead>
            <tr>
              <th>ID</th>
              <th>タイトル</th>
              <th>著者</th>
              <th>ISBN</th>
              <th>状態</th>
              <th>操作</th>
            </tr>
          </thead>
          <tbody>
            {books.map((book) => (
              <tr key={book.id}>
                <td>{book.id}</td>
                <td>
                  {book.title}
                  {book.newRelease && (
                    <span className="badge badge-overdue" style={{ marginLeft: 8 }}>新刊</span>
                  )}
                </td>
                <td>{book.author}</td>
                <td>{book.isbn}</td>
                <td>
                  <span
                    className={`badge ${book.available ? "badge-available" : "badge-unavailable"}`}
                  >
                    {book.available ? "貸出可" : "貸出中"}
                  </span>
                </td>
                <td>
                  <div className="actions">
                    <Link href={`/books/${book.id}/edit`}>
                      <button className="btn-secondary btn-sm">編集</button>
                    </Link>
                    <button
                      className="btn-danger btn-sm"
                      onClick={() => handleDelete(book.id)}
                    >
                      削除
                    </button>
                  </div>
                </td>
              </tr>
            ))}
          </tbody>
        </table>
      )}
    </div>
  );
}
