"use client";

import { useState } from "react";
import { BookRequest } from "@/lib/types";

interface Props {
  initialData?: BookRequest;
  onSubmit: (data: BookRequest) => Promise<void>;
  submitLabel: string;
}

export default function BookForm({ initialData, onSubmit, submitLabel }: Props) {
  const [title, setTitle] = useState(initialData?.title ?? "");
  const [author, setAuthor] = useState(initialData?.author ?? "");
  const [isbn, setIsbn] = useState(initialData?.isbn ?? "");
  const [submitting, setSubmitting] = useState(false);

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    setSubmitting(true);
    try {
      await onSubmit({ title, author, isbn });
    } finally {
      setSubmitting(false);
    }
  };

  return (
    <form onSubmit={handleSubmit}>
      <div className="form-group">
        <label>タイトル</label>
        <input value={title} onChange={(e) => setTitle(e.target.value)} required />
      </div>
      <div className="form-group">
        <label>著者</label>
        <input value={author} onChange={(e) => setAuthor(e.target.value)} required />
      </div>
      <div className="form-group">
        <label>ISBN</label>
        <input value={isbn} onChange={(e) => setIsbn(e.target.value)} required />
      </div>
      <div className="form-actions">
        <button type="submit" className="btn-primary" disabled={submitting}>
          {submitting ? "処理中..." : submitLabel}
        </button>
        <a href="/books"><button type="button" className="btn-secondary">キャンセル</button></a>
      </div>
    </form>
  );
}
