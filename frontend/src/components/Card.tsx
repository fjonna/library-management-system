import type { HTMLAttributes } from 'react';

export default function Card({ className = '', ...rest }: HTMLAttributes<HTMLDivElement>) {
  return (
    <div
      className={`rounded-xl border border-gray-200 bg-white p-5 shadow-sm transition-shadow duration-150 hover:shadow-md dark:border-gray-800 dark:bg-gray-900 ${className}`}
      {...rest}
    />
  );
}
