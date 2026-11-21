import type { Cell } from '../types';

interface Props {
  columns: string[];
  rows: Cell[][];
}

export default function ResultTable({ columns, rows }: Props) {
  if (rows.length === 0) {
    return <p className="py-3 text-sm text-gray-500 dark:text-gray-400">No rows.</p>;
  }
  return (
    <div className="overflow-x-auto rounded-lg border border-gray-200 dark:border-gray-800">
      <table className="w-full text-left text-sm">
        <thead className="bg-gray-50 text-xs uppercase tracking-wide text-gray-500 dark:bg-gray-800/60 dark:text-gray-400">
          <tr>
            {columns.map((c) => (
              <th key={c} className="whitespace-nowrap px-3 py-2 font-medium">
                {c}
              </th>
            ))}
          </tr>
        </thead>
        <tbody className="divide-y divide-gray-100 dark:divide-gray-800">
          {rows.map((row, i) => (
            <tr key={i} className="transition-colors hover:bg-gray-50 dark:hover:bg-gray-800/40">
              {row.map((cell, j) => (
                <td
                  key={j}
                  title={cell === null ? undefined : String(cell)}
                  className="max-w-[240px] truncate px-3 py-2 text-gray-700 dark:text-gray-200"
                >
                  {cell === null ? '—' : String(cell)}
                </td>
              ))}
            </tr>
          ))}
        </tbody>
      </table>
    </div>
  );
}
