import type { ReactNode } from 'react';

interface Props {
  icon?: ReactNode;
  title: string;
  message?: string;
  action?: ReactNode;
}

export default function EmptyState({ icon, title, message, action }: Props) {
  return (
    <div className="flex flex-col items-center gap-2 rounded-xl border border-dashed border-gray-300 px-6 py-12 text-center dark:border-gray-700">
      {icon && <div className="text-gray-400 dark:text-gray-500">{icon}</div>}
      <p className="font-medium text-gray-700 dark:text-gray-200">{title}</p>
      {message && <p className="max-w-sm text-sm text-gray-500 dark:text-gray-400">{message}</p>}
      {action && <div className="mt-2">{action}</div>}
    </div>
  );
}
