import type { Cell } from '../types';

interface Props {
  columns: string[];
  rows: Cell[][];
}

export default function ResultTable({ columns, rows }: Props) {
  if (rows.length === 0) return <p className="empty">No rows.</p>;
  return (
    <div className="table-wrap">
      <table>
        <thead>
          <tr>
            {columns.map((c) => (
              <th key={c}>{c}</th>
            ))}
          </tr>
        </thead>
        <tbody>
          {rows.map((row, i) => (
            <tr key={i}>
              {row.map((cell, j) => (
                <td key={j}>{cell === null ? '—' : String(cell)}</td>
              ))}
            </tr>
          ))}
        </tbody>
      </table>
    </div>
  );
}
