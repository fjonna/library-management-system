import type { ReactNode } from 'react';
import Card from './Card';

interface Props {
  label: string;
  value: ReactNode;
  icon?: ReactNode;
}

export default function StatTile({ label, value, icon }: Props) {
  return (
    <Card className="p-4 text-center">
      {icon && <div className="mb-1 flex justify-center text-indigo-500 dark:text-indigo-400">{icon}</div>}
      <div className="truncate text-xl font-bold text-indigo-600 dark:text-indigo-400">{value}</div>
      <div className="mt-1 text-xs font-medium uppercase tracking-wide text-gray-500 dark:text-gray-400">{label}</div>
    </Card>
  );
}
