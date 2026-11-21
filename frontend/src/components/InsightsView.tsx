import { useEffect, useState } from 'react';
import { api, errMsg } from '../api';
import type { InsightResponse, InsightSummaryResponse } from '../types';

interface Props {
  basePath: string;
  summarizeLabel: string;
}

export default function InsightsView({ basePath, summarizeLabel }: Props) {
  const [data, setData] = useState<InsightResponse | null>(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');
  const [summary, setSummary] = useState('');
  const [summarizing, setSummarizing] = useState(false);

  useEffect(() => {
    setLoading(true);
    setData(null);
    setSummary('');
    setError('');
    api<InsightResponse>(basePath)
      .then(setData)
      .catch((e) => setError(errMsg(e)))
      .finally(() => setLoading(false));
  }, [basePath]);

  async function summarize() {
    setSummarizing(true);
    try {
      const res = await api<InsightSummaryResponse>(`${basePath}/summary`, { method: 'POST' });
      setSummary(res.summary);
    } catch (e) {
      setError(errMsg(e));
    } finally {
      setSummarizing(false);
    }
  }

  if (loading) return <p className="empty">Loading…</p>;
  if (error) return <div className="error">{error}</div>;
  if (!data) return null;

  const { stats, sentences } = data;

  return (
    <>
      <div className="tiles">
        <div className="tile">
          <div className="tile-value">{stats.totalBooks}</div>
          <div className="tile-label">Total books</div>
        </div>
        <div className="tile">
          <div className="tile-value">{stats.finished}</div>
          <div className="tile-label">Finished</div>
        </div>
        <div className="tile">
          <div className="tile-value">{stats.reading}</div>
          <div className="tile-label">Reading</div>
        </div>
        <div className="tile">
          <div className="tile-value">{stats.toRead}</div>
          <div className="tile-label">To read</div>
        </div>
        <div className="tile">
          <div className="tile-value">{stats.averagePrice ?? '—'}</div>
          <div className="tile-label">Avg. price</div>
        </div>
        <div className="tile">
          <div className="tile-value">{stats.averagePages ?? '—'}</div>
          <div className="tile-label">Avg. pages</div>
        </div>
        <div className="tile">
          <div className="tile-value">{stats.topGenre ?? '—'}</div>
          <div className="tile-label">Top genre</div>
        </div>
      </div>

      <ul className="insight-list">
        {sentences.map((s) => (
          <li key={s}>{s}</li>
        ))}
      </ul>

      <button className="btn secondary small" type="button" onClick={() => void summarize()} disabled={summarizing}>
        {summarizing ? 'Summarizing…' : summarizeLabel}
      </button>
      {summary && <div className="summary">{summary}</div>}
    </>
  );
}
