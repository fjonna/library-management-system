import type { ReactNode } from 'react';

type Color = 'gray' | 'indigo' | 'green' | 'amber' | 'red' | 'blue';

interface Props {
  color?: Color;
  children: ReactNode;
  className?: string;
}

const colors: Record<Color, string> = {
  gray: 'bg-gray-100 text-gray-700 dark:bg-gray-800 dark:text-gray-300',
  indigo: 'bg-indigo-100 text-indigo-700 dark:bg-indigo-500/20 dark:text-indigo-300',
  green: 'bg-emerald-100 text-emerald-700 dark:bg-emerald-500/20 dark:text-emerald-300',
  amber: 'bg-amber-100 text-amber-700 dark:bg-amber-500/20 dark:text-amber-300',
  red: 'bg-red-100 text-red-700 dark:bg-red-500/20 dark:text-red-300',
  blue: 'bg-blue-100 text-blue-700 dark:bg-blue-500/20 dark:text-blue-300',
};

export default function Badge({ color = 'gray', children, className = '' }: Props) {
  return (
    <span className={`inline-flex items-center rounded-full px-2.5 py-0.5 text-xs font-medium ${colors[color]} ${className}`}>
      {children}
    </span>
  );
}
