"use client";

import { useEffect, useState } from "react";
import Link from "next/link";
import { Member } from "@/lib/types";
import { getMembers, deleteMember } from "@/lib/api";
import ErrorMessage from "@/components/ErrorMessage";

const memberTypeLabel = { GENERAL: "一般", PREMIUM: "プレミアム" } as const;

export default function MembersPage() {
  const [members, setMembers] = useState<Member[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

  const fetchMembers = async () => {
    try {
      setLoading(true);
      setError(null);
      const data = await getMembers();
      setMembers(data);
    } catch (e) {
      setError(e instanceof Error ? e.message : "会員の取得に失敗しました");
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    fetchMembers();
  }, []);

  const handleDelete = async (id: number) => {
    if (!confirm("この会員を削除しますか？")) return;
    try {
      await deleteMember(id);
      setMembers((prev) => prev.filter((m) => m.id !== id));
    } catch (e) {
      setError(e instanceof Error ? e.message : "削除に失敗しました");
    }
  };

  return (
    <div>
      <div className="page-header">
        <h1>会員管理</h1>
        <Link href="/members/new">
          <button className="btn-primary">会員を登録</button>
        </Link>
      </div>
      <ErrorMessage message={error} />
      {loading ? (
        <div className="loading">読み込み中...</div>
      ) : members.length === 0 ? (
        <div className="empty">会員が登録されていません</div>
      ) : (
        <table>
          <thead>
            <tr>
              <th>ID</th>
              <th>名前</th>
              <th>メールアドレス</th>
              <th>会員種別</th>
              <th>操作</th>
            </tr>
          </thead>
          <tbody>
            {members.map((member) => (
              <tr key={member.id}>
                <td>{member.id}</td>
                <td>{member.name}</td>
                <td>{member.email}</td>
                <td>{memberTypeLabel[member.memberType]}</td>
                <td>
                  <div className="actions">
                    <Link href={`/members/${member.id}/edit`}>
                      <button className="btn-secondary btn-sm">編集</button>
                    </Link>
                    <button
                      className="btn-danger btn-sm"
                      onClick={() => handleDelete(member.id)}
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
