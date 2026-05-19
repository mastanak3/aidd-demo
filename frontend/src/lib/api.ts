import {
  Book,
  BookRequest,
  Member,
  MemberRequest,
  Loan,
  LoanRequest,
} from "./types";

export class ApiError extends Error {
  status: number;
  constructor(message: string, status: number) {
    super(message);
    this.status = status;
  }
}

async function request<T>(path: string, options?: RequestInit): Promise<T> {
  const res = await fetch(path, {
    headers: { "Content-Type": "application/json" },
    ...options,
  });
  if (!res.ok) {
    const body = await res.json().catch(() => null);
    const message =
      body?.error || body?.message || `エラーが発生しました (${res.status})`;
    throw new ApiError(message, res.status);
  }
  if (res.status === 204) return undefined as T;
  return res.json();
}

// Books
export const getBooks = () => request<Book[]>("/api/books");
export const getBook = (id: number) => request<Book>(`/api/books/${id}`);
export const createBook = (data: BookRequest) =>
  request<Book>("/api/books", {
    method: "POST",
    body: JSON.stringify(data),
  });
export const updateBook = (id: number, data: BookRequest) =>
  request<Book>(`/api/books/${id}`, {
    method: "PUT",
    body: JSON.stringify(data),
  });
export const deleteBook = (id: number) =>
  request<void>(`/api/books/${id}`, { method: "DELETE" });

// Members
export const getMembers = () => request<Member[]>("/api/members");
export const getMember = (id: string) =>
  request<Member>(`/api/members/${id}`);
export const createMember = (data: MemberRequest) =>
  request<Member>("/api/members", {
    method: "POST",
    body: JSON.stringify(data),
  });
export const updateMember = (id: string, data: MemberRequest) =>
  request<Member>(`/api/members/${id}`, {
    method: "PUT",
    body: JSON.stringify(data),
  });
export const deleteMember = (id: string) =>
  request<void>(`/api/members/${id}`, { method: "DELETE" });

// Loans
export const getLoans = () => request<Loan[]>("/api/loans");
export const borrowBook = (data: LoanRequest) =>
  request<Loan>("/api/loans", {
    method: "POST",
    body: JSON.stringify(data),
  });
export const returnBook = (loanId: number) =>
  request<Loan>(`/api/loans/${loanId}/return`, { method: "POST" });
export const extendLoan = (loanId: number) =>
  request<Loan>(`/api/loans/${loanId}/extend`, { method: "POST" });
