import Badge from './Badge';
import { STATUS_LABELS } from '../types';
import type { BookStatus } from '../types';

const COLOR: Record<BookStatus, 'gray' | 'blue' | 'green'> = {
  TO_READ: 'gray',
  READING: 'blue',
  COMPLETED: 'green',
};

export default function StatusBadge({ status }: { status: BookStatus }) {
  return <Badge color={COLOR[status]}>{STATUS_LABELS[status]}</Badge>;
}
