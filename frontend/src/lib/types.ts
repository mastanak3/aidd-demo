export type MemberType = "GENERAL" | "PREMIUM";

export interface Book {
  id: number;
  title: string;
  author: string;
  isbn: string;
  available: boolean;
}

export interface BookRequest {
  title: string;
  author: string;
  isbn: string;
}

export interface Member {
  id: number;
  name: string;
  email: string;
  memberType: MemberType;
}

export interface MemberRequest {
  name: string;
  email: string;
  memberType: MemberType;
}

export interface Loan {
  id: number;
  bookId: number;
  memberId: number;
  loanDate: string;
  dueDate: string;
  returnDate: string | null;
  overdueFee: number;
}

export interface LoanRequest {
  memberId: number;
  bookId: number;
}
