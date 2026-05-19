export type MemberType = "GENERAL" | "PREMIUM";

export interface Book {
  id: number;
  title: string;
  author: string;
  isbn: string;
  available: boolean;
  newRelease: boolean;
}

export interface BookRequest {
  title: string;
  author: string;
  isbn: string;
  newRelease: boolean;
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
  rentalFee: number;
  extended: boolean;
}

export interface LoanRequest {
  memberId: string;
  bookId: number;
}
