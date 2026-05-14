"use client";

import { useEffect, useState } from "react";
import { useParams, useRouter } from "next/navigation";
import BookForm from "@/components/BookForm";
import ErrorMessage from "@/components/ErrorMessage";
import { getBook, updateBook } from "@/lib/api";
import { Book } from "@/lib/types";

export default function EditBookPage() {
  const params = useParams<{ id: string }>();
  const router = useRouter();
  const [book, setBook] = useState<Book | null>(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    const fetchBook = async () => {
      try {
        const data = await getBook(Number(params.id));
        setBook(data);
      } catch (e) {
        setError(e instanceof Error ? e.message : "書籍の取得に失敗しました");
      } finally {
        setLoading(false);
      }
    };
    fetchBook();
  }, [params.id]);

  if (loading) return <div className="loading">読み込み中...</div>;

  return (
    <div>
      <h1>書籍編集</h1>
      <ErrorMessage message={error} />
      {book && (
        <BookForm
          initialData={{ title: book.title, author: book.author, isbn: book.isbn }}
          submitLabel="更新"
          onSubmit={async (data) => {
            try {
              setError(null);
              await updateBook(book.id, data);
              router.push("/books");
            } catch (e) {
              setError(e instanceof Error ? e.message : "更新に失敗しました");
            }
          }}
        />
      )}
    </div>
  );
}
