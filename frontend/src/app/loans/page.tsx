"use client";

import { useEffect, useState } from "react";
import { Book, Loan, Member } from "@/lib/types";
import { getLoans, getBooks, getMembers, borrowBook, returnBook } from "@/lib/api";
import ErrorMessage from "@/components/ErrorMessage";

export default function LoansPage() {
  const [loans, setLoans] = useState<Loan[]>([]);
  const [books, setBooks] = useState<Book[]>([]);
  const [members, setMembers] = useState<Member[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

  const [selectedMemberId, setSelectedMemberId] = useState("");
  const [selectedBookId, setSelectedBookId] = useState("");
  const [submitting, setSubmitting] = useState(false);

  const fetchAll = async () => {
    try {
      setLoading(true);
      setError(null);
      const [loansData, booksData, membersData] = await Promise.all([
        getLoans(),
        getBooks(),
        getMembers(),
      ]);
      setLoans(loansData);
      setBooks(booksData);
      setMembers(membersData);
    } catch (e) {
      setError(e instanceof Error ? e.message : "データの取得に失敗しました");
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    fetchAll();
  }, []);

  const bookName = (id: number) => books.find((b) => b.id === id)?.title ?? `書籍ID: ${id}`;
  const memberName = (id: number) => members.find((m) => m.id === id)?.name ?? `会員ID: ${id}`;

  const availableBooks = books.filter((b) => b.available);

  const handleBorrow = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!selectedMemberId || !selectedBookId) return;
    setSubmitting(true);
    try {
      setError(null);
      await borrowBook({
        memberId: Number(selectedMemberId),
        bookId: Number(selectedBookId),
      });
      setSelectedMemberId("");
      setSelectedBookId("");
      await fetchAll();
    } catch (e) {
      setError(e instanceof Error ? e.message : "貸出に失敗しました");
    } finally {
      setSubmitting(false);
    }
  };

  const handleReturn = async (loanId: number) => {
    try {
      setError(null);
      await returnBook(loanId);
      await fetchAll();
    } catch (e) {
      setError(e instanceof Error ? e.message : "返却に失敗しました");
    }
  };

  const isOverdue = (loan: Loan) => {
    if (loan.returnDate) return false;
    return new Date(loan.dueDate) < new Date();
  };

  const activeLoans = loans.filter((l) => !l.returnDate);
  const returnedLoans = loans.filter((l) => l.returnDate);

  if (loading) return <div className="loading">読み込み中...</div>;

  return (
    <div>
      <h1>貸出管理</h1>
      <ErrorMessage message={error} />

      <section style={{ marginBottom: 32 }}>
        <h2 style={{ fontSize: "1.1rem", marginBottom: 12 }}>新規貸出</h2>
        {members.length === 0 || availableBooks.length === 0 ? (
          <p style={{ color: "var(--text-muted)", fontSize: "0.875rem" }}>
            {members.length === 0
              ? "会員が登録されていません。先に会員を登録してください。"
              : "貸出可能な書籍がありません。"}
          </p>
        ) : (
          <form onSubmit={handleBorrow} style={{ display: "flex", gap: 8, alignItems: "flex-end", flexWrap: "wrap" }}>
            <div className="form-group" style={{ marginBottom: 0 }}>
              <label>会員</label>
              <select
                value={selectedMemberId}
                onChange={(e) => setSelectedMemberId(e.target.value)}
                required
                style={{ width: 200 }}
              >
                <option value="">選択してください</option>
                {members.map((m) => (
                  <option key={m.id} value={m.id}>
                    {m.name}
                  </option>
                ))}
              </select>
            </div>
            <div className="form-group" style={{ marginBottom: 0 }}>
              <label>書籍</label>
              <select
                value={selectedBookId}
                onChange={(e) => setSelectedBookId(e.target.value)}
                required
                style={{ width: 300 }}
              >
                <option value="">選択してください</option>
                {availableBooks.map((b) => (
                  <option key={b.id} value={b.id}>
                    {b.title}
                  </option>
                ))}
              </select>
            </div>
            <button type="submit" className="btn-primary" disabled={submitting}>
              {submitting ? "処理中..." : "貸出"}
            </button>
          </form>
        )}
      </section>

      <section style={{ marginBottom: 32 }}>
        <h2 style={{ fontSize: "1.1rem", marginBottom: 12 }}>貸出中一覧</h2>
        {activeLoans.length === 0 ? (
          <div className="empty">貸出中の書籍はありません</div>
        ) : (
          <table>
            <thead>
              <tr>
                <th>ID</th>
                <th>書籍</th>
                <th>会員</th>
                <th>貸出日</th>
                <th>返却期限</th>
                <th>状態</th>
                <th>操作</th>
              </tr>
            </thead>
            <tbody>
              {activeLoans.map((loan) => (
                <tr key={loan.id}>
                  <td>{loan.id}</td>
                  <td>{bookName(loan.bookId)}</td>
                  <td>{memberName(loan.memberId)}</td>
                  <td>{loan.loanDate}</td>
                  <td>{loan.dueDate}</td>
                  <td>
                    {isOverdue(loan) ? (
                      <span className="badge badge-overdue">延滞</span>
                    ) : (
                      <span className="badge badge-available">貸出中</span>
                    )}
                  </td>
                  <td>
                    <button
                      className="btn-success btn-sm"
                      onClick={() => handleReturn(loan.id)}
                    >
                      返却
                    </button>
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        )}
      </section>

      <section>
        <h2 style={{ fontSize: "1.1rem", marginBottom: 12 }}>返却済み履歴</h2>
        {returnedLoans.length === 0 ? (
          <div className="empty">返却済みの記録はありません</div>
        ) : (
          <table>
            <thead>
              <tr>
                <th>ID</th>
                <th>書籍</th>
                <th>会員</th>
                <th>貸出日</th>
                <th>返却期限</th>
                <th>返却日</th>
                <th>延滞料金</th>
              </tr>
            </thead>
            <tbody>
              {returnedLoans.map((loan) => (
                <tr key={loan.id}>
                  <td>{loan.id}</td>
                  <td>{bookName(loan.bookId)}</td>
                  <td>{memberName(loan.memberId)}</td>
                  <td>{loan.loanDate}</td>
                  <td>{loan.dueDate}</td>
                  <td>{loan.returnDate}</td>
                  <td>{loan.overdueFee}円</td>
                </tr>
              ))}
            </tbody>
          </table>
        )}
      </section>
    </div>
  );
}
