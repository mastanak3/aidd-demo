"use client";

import { useEffect, useState } from "react";
import { useParams, useRouter } from "next/navigation";
import MemberForm from "@/components/MemberForm";
import ErrorMessage from "@/components/ErrorMessage";
import { getMember, updateMember } from "@/lib/api";
import { Member } from "@/lib/types";

export default function EditMemberPage() {
  const params = useParams<{ id: string }>();
  const router = useRouter();
  const [member, setMember] = useState<Member | null>(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    const fetchMember = async () => {
      try {
        const data = await getMember(params.id);
        setMember(data);
      } catch (e) {
        setError(e instanceof Error ? e.message : "会員の取得に失敗しました");
      } finally {
        setLoading(false);
      }
    };
    fetchMember();
  }, [params.id]);

  if (loading) return <div className="loading">読み込み中...</div>;

  return (
    <div>
      <h1>会員編集</h1>
      <ErrorMessage message={error} />
      {member && (
        <MemberForm
          initialData={{
            id: member.id,
            name: member.name,
            email: member.email,
            memberType: member.memberType,
          }}
          idReadOnly
          submitLabel="更新"
          onSubmit={async (data) => {
            try {
              setError(null);
              await updateMember(member.id, data);
              router.push("/members");
            } catch (e) {
              setError(e instanceof Error ? e.message : "更新に失敗しました");
            }
          }}
        />
      )}
    </div>
  );
}
