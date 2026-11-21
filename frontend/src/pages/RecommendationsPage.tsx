import { useEffect, useState } from 'react';
import { Sparkles } from 'lucide-react';
import { api, errMsg } from '../api';
import Badge from '../components/Badge';
import Card from '../components/Card';
import EmptyState from '../components/EmptyState';
import PageHeader from '../components/PageHeader';
import Spinner from '../components/Spinner';
import { useToast } from '../components/Toast';
import type { RecommendationItem } from '../types';

export default function RecommendationsPage() {
  const toast = useToast();
  const [items, setItems] = useState<RecommendationItem[]>([]);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    api<RecommendationItem[]>('/api/recommendations')
      .then(setItems)
      .catch((e) => toast.show('error', errMsg(e)))
      .finally(() => setLoading(false));
  }, [toast]);

  return (
    <div className="flex flex-col gap-6">
      <PageHeader
        title="Recommended for you"
        description="Based on your genres, authors and readers with similar taste."
      />

      {loading ? (
        <div className="flex justify-center py-12">
          <Spinner size={28} className="text-indigo-500" />
        </div>
      ) : items.length === 0 ? (
        <Card>
          <EmptyState
            icon={<Sparkles size={28} />}
            title="No recommendations yet"
            message="Add a few books to your library and check back for personalized picks."
          />
        </Card>
      ) : (
        <div className="grid grid-cols-1 gap-4 sm:grid-cols-2 lg:grid-cols-3">
          {items.map((item, i) => (
            <Card key={`${item.title}-${item.author}-${i}`} className="flex flex-col gap-2">
              <h3 className="truncate font-semibold" title={item.title}>
                {item.title}
              </h3>
              <p className="truncate text-sm text-gray-500 dark:text-gray-400" title={item.author}>
                {item.author}
                {item.genre ? ` · ${item.genre}` : ''}
              </p>
              <Badge color="indigo" className="self-start">
                {item.reason}
              </Badge>
            </Card>
          ))}
        </div>
      )}
    </div>
  );
}
