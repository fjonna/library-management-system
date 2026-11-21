import { useEffect, useState } from 'react';
import type { FormEvent } from 'react';
import { api, errMsg } from '../api';
import ResultTable from '../components/ResultTable';
import type { QueryResult } from '../types';

export default function AgentPage() {
  const [question, setQuestion] = useState('');
  const [examples, setExamples] = useState<string[]>([]);
  const [result, setResult] = useState<QueryResult | null>(null);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState('');

  useEffect(() => {
    api<string[]>('/api/agent/examples').then(setExamples).catch(() => undefined);
  }, []);

  async function ask(q: string) {
    if (!q.trim()) return;
    setLoading(true);
    setError('');
    try {
      setResult(
        await api<QueryResult>('/api/agent/query', {
          method: 'POST',
          body: JSON.stringify({ question: q }),
        }),
      );
    } catch (e) {
      setError(errMsg(e));
    } finally {
      setLoading(false);
    }
  }

  function onSubmit(e: FormEvent) {
    e.preventDefault();
    void ask(question);
  }

  return (
    <>
      <section className="card">
        <h2>Ask the library</h2>
        <form className="ask" onSubmit={onSubmit}>
          <input
            placeholder="e.g. Show the five most expensive books"
            value={question}
            onChange={(e) => setQuestion(e.target.value)}
            maxLength={300}
          />
          <button className="btn" type="submit" disabled={loading}>
            {loading ? 'Asking…' : 'Ask'}
          </button>
        </form>
        <div className="chips">
          {examples.map((ex) => (
            <button
              key={ex}
              className="chip"
              type="button"
              onClick={() => {
                setQuestion(ex);
                void ask(ex);
              }}
            >
              {ex}
            </button>
          ))}
        </div>
      </section>

      {error && <div className="error">{error}</div>}

      {result && (
        <section className="card">
          {result.understoodAs && <div className="chip understood">{result.understoodAs}</div>}
          <h2>{result.title}</h2>
          <div className="summary">{result.summary}</div>
          {result.intent === 'UNKNOWN' ? (
            <div className="chips">
              {result.rows.map((row) => (
                <button
                  key={String(row[0])}
                  className="chip"
                  type="button"
                  onClick={() => {
                    setQuestion(String(row[0]));
                    void ask(String(row[0]));
                  }}
                >
                  {row[0]}
                </button>
              ))}
            </div>
          ) : (
            result.columns.length > 0 && <ResultTable columns={result.columns} rows={result.rows} />
          )}
        </section>
      )}
    </>
  );
}
