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
  id: string;
  name: string;
  email: string;
  memberType: MemberType;
}

export interface MemberRequest {
  id: string;
  name: string;
  email: string;
  memberType: MemberType;
}

export interface Loan {
  id: number;
  bookId: number;
  memberId: string;
  loanDate: string;
  dueDate: string;
  returnDate: string | null;
  overdueFee: number;
  extended: boolean;
}

export interface LoanRequest {
  memberId: string;
  bookId: number;
}
