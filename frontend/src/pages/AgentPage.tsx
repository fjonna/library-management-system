import { useEffect, useRef, useState } from 'react';
import type { FormEvent } from 'react';
import { Search, Send } from 'lucide-react';
import { api, errMsg } from '../api';
import Badge from '../components/Badge';
import Card from '../components/Card';
import PageHeader from '../components/PageHeader';
import ResultTable from '../components/ResultTable';
import Spinner from '../components/Spinner';
import { useToast } from '../components/Toast';
import type { QueryResult } from '../types';

interface Props {
  initialQuestion?: string;
  onConsumeInitialQuestion?: () => void;
}

export default function AgentPage({ initialQuestion, onConsumeInitialQuestion }: Props) {
  const toast = useToast();
  const [question, setQuestion] = useState('');
  const [examples, setExamples] = useState<string[]>([]);
  const [result, setResult] = useState<QueryResult | null>(null);
  const [loading, setLoading] = useState(false);
  const inputRef = useRef<HTMLInputElement>(null);

  useEffect(() => {
    api<string[]>('/api/agent/examples').then(setExamples).catch(() => undefined);
  }, []);

  // arriving from the Dashboard's "Ask the library" shortcut with a question to run immediately
  useEffect(() => {
    if (initialQuestion) {
      setQuestion(initialQuestion);
      void ask(initialQuestion);
      onConsumeInitialQuestion?.();
    }
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, []);

  async function ask(q: string) {
    if (!q.trim()) return;
    setLoading(true);
    try {
      setResult(
        await api<QueryResult>('/api/agent/query', {
          method: 'POST',
          body: JSON.stringify({ question: q }),
        }),
      );
    } catch (e) {
      toast.show('error', errMsg(e));
    } finally {
      setLoading(false);
    }
  }

  function onSubmit(e: FormEvent) {
    e.preventDefault();
    void ask(question);
  }

  function askExample(ex: string) {
    setQuestion(ex);
    void ask(ex);
    inputRef.current?.focus();
  }

  return (
    <div className="flex flex-col gap-6">
      <PageHeader title="Ask the library" description="Ask a question in plain English or Albanian about your books." />

      <Card>
        <form
          onSubmit={onSubmit}
          className="flex items-center gap-2 rounded-2xl border border-gray-200 bg-gray-50 p-2 focus-within:border-indigo-400 focus-within:ring-2 focus-within:ring-indigo-500/30 dark:border-gray-700 dark:bg-gray-800"
        >
          <Search size={18} className="ml-2 shrink-0 text-gray-400" />
          <input
            ref={inputRef}
            value={question}
            onChange={(e) => setQuestion(e.target.value)}
            placeholder="e.g. Show the five most expensive books"
            maxLength={300}
            aria-label="Ask the library"
            className="flex-1 bg-transparent px-1 py-2 text-sm text-gray-900 placeholder:text-gray-400 focus:outline-none dark:text-gray-100 dark:placeholder:text-gray-500"
          />
          <button
            type="submit"
            disabled={loading || !question.trim()}
            aria-label="Ask"
            className="inline-flex shrink-0 items-center justify-center gap-1.5 rounded-xl bg-indigo-600 px-4 py-2 text-sm font-medium text-white transition-colors hover:bg-indigo-700 focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-indigo-500 focus-visible:ring-offset-1 disabled:cursor-not-allowed disabled:opacity-50"
          >
            {loading ? <Spinner size={16} className="text-white" /> : <Send size={16} />}
            {loading ? 'Asking…' : 'Ask'}
          </button>
        </form>

        {examples.length > 0 && (
          <div className="mt-3 flex flex-wrap gap-2">
            {examples.map((ex) => (
              <button
                key={ex}
                type="button"
                onClick={() => askExample(ex)}
                className="rounded-full border border-gray-200 bg-white px-3 py-1.5 text-xs font-medium text-gray-600 transition-colors hover:border-indigo-300 hover:text-indigo-600 focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-indigo-500 dark:border-gray-700 dark:bg-gray-900 dark:text-gray-300 dark:hover:border-indigo-500 dark:hover:text-indigo-400"
              >
                {ex}
              </button>
            ))}
          </div>
        )}
      </Card>

      {result && (
        <Card className="flex flex-col gap-3">
          {result.understoodAs && (
            <Badge color="indigo" className="self-start">
              {result.understoodAs}
            </Badge>
          )}
          <h2 className="text-lg font-semibold">{result.title}</h2>
          <p className="rounded-lg bg-indigo-50 px-3 py-2 text-sm text-indigo-800 dark:bg-indigo-500/10 dark:text-indigo-200">
            {result.summary}
          </p>
          {result.suggestedQuestions ? (
            <div className="flex flex-wrap gap-2">
              {result.rows.map((row) => (
                <button
                  key={String(row[0])}
                  type="button"
                  onClick={() => askExample(String(row[0]))}
                  className="rounded-full border border-gray-200 bg-white px-3 py-1.5 text-xs font-medium text-gray-600 transition-colors hover:border-indigo-300 hover:text-indigo-600 dark:border-gray-700 dark:bg-gray-900 dark:text-gray-300 dark:hover:border-indigo-500 dark:hover:text-indigo-400"
                >
                  {row[0]}
                </button>
              ))}
            </div>
          ) : (
            result.columns.length > 0 && <ResultTable columns={result.columns} rows={result.rows} />
          )}
        </Card>
      )}
    </div>
  );
}
