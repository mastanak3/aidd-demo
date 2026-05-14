"use client";

import { useRouter } from "next/navigation";
import { useState } from "react";
import MemberForm from "@/components/MemberForm";
import ErrorMessage from "@/components/ErrorMessage";
import { createMember } from "@/lib/api";

export default function NewMemberPage() {
  const router = useRouter();
  const [error, setError] = useState<string | null>(null);

  return (
    <div>
      <h1>会員登録</h1>
      <ErrorMessage message={error} />
      <MemberForm
        submitLabel="登録"
        onSubmit={async (data) => {
          try {
            setError(null);
            await createMember(data);
            router.push("/members");
          } catch (e) {
            setError(e instanceof Error ? e.message : "登録に失敗しました");
          }
        }}
      />
    </div>
  );
}
