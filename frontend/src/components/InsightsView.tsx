import { useEffect, useState } from 'react';
import { Lightbulb } from 'lucide-react';
import { api, errMsg } from '../api';
import { formatPrice } from '../format';
import Button from './Button';
import Card from './Card';
import Spinner from './Spinner';
import StatTile from './StatTile';
import { useToast } from './Toast';
import type { InsightResponse, InsightSummaryResponse } from '../types';

interface Props {
  basePath: string;
  summarizeLabel: string;
}

export default function InsightsView({ basePath, summarizeLabel }: Props) {
  const toast = useToast();
  const [data, setData] = useState<InsightResponse | null>(null);
  const [loading, setLoading] = useState(true);
  const [summary, setSummary] = useState('');
  const [summarizing, setSummarizing] = useState(false);

  useEffect(() => {
    setLoading(true);
    setData(null);
    setSummary('');
    api<InsightResponse>(basePath)
      .then(setData)
      .catch((e) => toast.show('error', errMsg(e)))
      .finally(() => setLoading(false));
  }, [basePath, toast]);

  async function summarize() {
    setSummarizing(true);
    try {
      const res = await api<InsightSummaryResponse>(`${basePath}/summary`, { method: 'POST' });
      setSummary(res.summary);
    } catch (e) {
      toast.show('error', errMsg(e));
    } finally {
      setSummarizing(false);
    }
  }

  if (loading) {
    return (
      <div className="flex justify-center py-10">
        <Spinner size={28} className="text-indigo-500" />
      </div>
    );
  }
  if (!data) return null;

  const { stats, sentences } = data;

  return (
    <div className="flex flex-col gap-6">
      <div className="grid grid-cols-2 gap-3 sm:grid-cols-4">
        <StatTile label="Total books" value={stats.totalBooks} />
        <StatTile label="Finished" value={stats.finished} />
        <StatTile label="Reading" value={stats.reading} />
        <StatTile label="To read" value={stats.toRead} />
        <StatTile label="Avg. price" value={stats.averagePrice === null ? '—' : formatPrice(stats.averagePrice)} />
        <StatTile label="Avg. pages" value={stats.averagePages === null ? '—' : Math.round(stats.averagePages)} />
        <StatTile label="Top genre" value={stats.topGenre ?? '—'} />
      </div>

      <ul className="flex flex-col gap-2">
        {sentences.map((s) => (
          <li
            key={s}
            className="flex items-start gap-2 rounded-lg bg-gray-50 px-3 py-2 text-sm text-gray-700 dark:bg-gray-800/60 dark:text-gray-200"
          >
            <Lightbulb size={16} className="mt-0.5 shrink-0 text-indigo-500" />
            <span>{s}</span>
          </li>
        ))}
      </ul>

      <div>
        <Button variant="secondary" onClick={() => void summarize()} loading={summarizing}>
          {summarizeLabel}
        </Button>
        {summary && (
          <Card className="mt-3 bg-indigo-50 text-sm text-indigo-900 dark:bg-indigo-500/10 dark:text-indigo-100">
            {summary}
          </Card>
        )}
      </div>
    </div>
  );
}
