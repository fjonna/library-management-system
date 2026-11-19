export type BookStatus = 'TO_READ' | 'READING' | 'COMPLETED';

export const STATUS_LABELS: Record<BookStatus, string> = {
  TO_READ: 'To read',
  READING: 'Reading',
  COMPLETED: 'Completed',
};

export interface Book {
  id: number;
  title: string;
  author: string;
  genre: string | null;
  status: BookStatus;
  price: number | null;
  pages: number | null;
  isbn: string | null;
  ownerId: string;
  ownerUsername: string;
  createdAt: string;
}

export interface UserSummary {
  id: string;
  username: string;
  email: string;
  bookCount: number;
}

export type Cell = string | number | null;

export interface QueryResult {
  intent: string;
  title: string;
  summary: string;
  columns: string[];
  rows: Cell[][];
}
