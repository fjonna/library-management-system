import { useEffect, useState } from 'react';
import { api, errMsg } from '../api';
import type { RecommendationItem } from '../types';

export default function RecommendationsPage() {
  const [items, setItems] = useState<RecommendationItem[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');

  useEffect(() => {
    api<RecommendationItem[]>('/api/recommendations')
      .then(setItems)
      .catch((e) => setError(errMsg(e)))
      .finally(() => setLoading(false));
  }, []);

  return (
    <section className="card">
      <h2>Recommended for you</h2>
      {loading && <p className="empty">Loading…</p>}
      {error && <div className="error">{error}</div>}
      {!loading && !error && items.length === 0 && <p className="empty">No recommendations yet.</p>}
      {items.length > 0 && (
        <div className="rec-grid">
          {items.map((item, i) => (
            <div className="rec-card" key={`${item.title}-${item.author}-${i}`}>
              <h3>{item.title}</h3>
              <div className="rec-meta">
                {item.author}
                {item.genre ? ` · ${item.genre}` : ''}
              </div>
              <div className="rec-reason">{item.reason}</div>
            </div>
          ))}
        </div>
      )}
    </section>
  );
}
