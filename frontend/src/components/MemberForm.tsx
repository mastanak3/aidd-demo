"use client";

import { useState } from "react";
import { MemberRequest, MemberType } from "@/lib/types";

interface Props {
  initialData?: MemberRequest;
  onSubmit: (data: MemberRequest) => Promise<void>;
  submitLabel: string;
}

export default function MemberForm({ initialData, onSubmit, submitLabel }: Props) {
  const [name, setName] = useState(initialData?.name ?? "");
  const [email, setEmail] = useState(initialData?.email ?? "");
  const [memberType, setMemberType] = useState<MemberType>(initialData?.memberType ?? "GENERAL");
  const [submitting, setSubmitting] = useState(false);

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    setSubmitting(true);
    try {
      await onSubmit({ name, email, memberType });
    } finally {
      setSubmitting(false);
    }
  };

  return (
    <form onSubmit={handleSubmit}>
      <div className="form-group">
        <label>名前</label>
        <input value={name} onChange={(e) => setName(e.target.value)} required />
      </div>
      <div className="form-group">
        <label>メールアドレス</label>
        <input type="email" value={email} onChange={(e) => setEmail(e.target.value)} required />
      </div>
      <div className="form-group">
        <label>会員種別</label>
        <select value={memberType} onChange={(e) => setMemberType(e.target.value as MemberType)}>
          <option value="GENERAL">一般</option>
          <option value="PREMIUM">プレミアム</option>
        </select>
      </div>
      <div className="form-actions">
        <button type="submit" className="btn-primary" disabled={submitting}>
          {submitting ? "処理中..." : submitLabel}
        </button>
        <a href="/members"><button type="button" className="btn-secondary">キャンセル</button></a>
      </div>
    </form>
  );
}
