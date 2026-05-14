"use client";

import { useRouter } from "next/navigation";
import { useState } from "react";
import BookForm from "@/components/BookForm";
import ErrorMessage from "@/components/ErrorMessage";
import { createBook } from "@/lib/api";

export default function NewBookPage() {
  const router = useRouter();
  const [error, setError] = useState<string | null>(null);

  return (
    <div>
      <h1>書籍登録</h1>
      <ErrorMessage message={error} />
      <BookForm
        submitLabel="登録"
        onSubmit={async (data) => {
          try {
            setError(null);
            await createBook(data);
            router.push("/books");
          } catch (e) {
            setError(e instanceof Error ? e.message : "登録に失敗しました");
          }
        }}
      />
    </div>
  );
}
