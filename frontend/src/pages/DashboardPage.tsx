import { useEffect, useMemo, useState } from 'react';
import type { FormEvent } from 'react';
import { ArrowRight, Bookmark, BookOpen, CheckCircle2, Clock, Plus, Search, ShieldCheck, Sparkles, Send, Users } from 'lucide-react';
import { api, errMsg } from '../api';
import type { Tab } from '../App';
import Badge from '../components/Badge';
import BookFormModal from '../components/BookFormModal';
import Button from '../components/Button';
import Card from '../components/Card';
import EmptyState from '../components/EmptyState';
import StatTile from '../components/StatTile';
import StatusBadge from '../components/StatusBadge';
import { useToast } from '../components/Toast';
import { isAdmin } from '../keycloak';
import type { Book, InsightResponse, RecommendationItem, UserSummary } from '../types';

interface Props {
  username: string;
  onNavigate: (tab: Tab) => void;
  onAsk: (question: string) => void;
}

function Skeleton({ className = '' }: { className?: string }) {
  return <div className={`animate-pulse rounded-lg bg-gray-100 dark:bg-gray-800 ${className}`} />;
}

export default function DashboardPage({ username, onNavigate, onAsk }: Props) {
  const toast = useToast();
  const admin = isAdmin();

  const [books, setBooks] = useState<Book[]>([]);
  const [insights, setInsights] = useState<InsightResponse | null>(null);
  const [loadingCore, setLoadingCore] = useState(true);
  const [addOpen, setAddOpen] = useState(false);

  const [examples, setExamples] = useState<string[]>([]);
  const [question, setQuestion] = useState('');

  const [recs, setRecs] = useState<RecommendationItem[]>([]);
  const [loadingRecs, setLoadingRecs] = useState(true);

  const [adminUsers, setAdminUsers] = useState<UserSummary[]>([]);
  const [adminBooks, setAdminBooks] = useState<Book[]>([]);
  const [loadingAdmin, setLoadingAdmin] = useState(admin);

  async function loadCore() {
    setLoadingCore(true);
    try {
      const [b, i] = await Promise.all([api<Book[]>('/api/books'), api<InsightResponse>('/api/insights')]);
      setBooks(b);
      setInsights(i);
    } catch (e) {
      toast.show('error', errMsg(e));
    } finally {
      setLoadingCore(false);
    }
  }

  useEffect(() => {
    void loadCore();
    api<string[]>('/api/agent/examples').then(setExamples).catch(() => undefined);

    setLoadingRecs(true);
    api<RecommendationItem[]>('/api/recommendations')
      .then(setRecs)
      .catch(() => undefined)
      .finally(() => setLoadingRecs(false));

    if (admin) {
      setLoadingAdmin(true);
      Promise.all([api<UserSummary[]>('/api/admin/users'), api<Book[]>('/api/admin/books')])
        .then(([u, b]) => {
          setAdminUsers(u);
          setAdminBooks(b);
        })
        .catch(() => undefined)
        .finally(() => setLoadingAdmin(false));
    }
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, []);

  const stats = insights?.stats;
  const greetingSentence = insights?.sentences[0];

  const statusPct = useMemo(() => {
    if (!stats || stats.totalBooks === 0) return { finished: 0, reading: 0, toRead: 0 };
    return {
      finished: Math.round((stats.finished / stats.totalBooks) * 100),
      reading: Math.round((stats.reading / stats.totalBooks) * 100),
      toRead: Math.round((stats.toRead / stats.totalBooks) * 100),
    };
  }, [stats]);

  const topGenres = useMemo(() => {
    const counts = new Map<string, number>();
    for (const b of books) {
      if (!b.genre) continue;
      counts.set(b.genre, (counts.get(b.genre) ?? 0) + 1);
    }
    return [...counts.entries()]
      .sort((a, b) => b[1] - a[1] || a[0].localeCompare(b[0]))
      .slice(0, 5)
      .map(([genre, count]) => ({ genre, count }));
  }, [books]);

  const maxGenreCount = topGenres[0]?.count ?? 1;
  const recent = books.slice(0, 5);

  const libraryTopGenre = useMemo(() => {
    const counts = new Map<string, number>();
    for (const b of adminBooks) {
      if (!b.genre) continue;
      counts.set(b.genre, (counts.get(b.genre) ?? 0) + 1);
    }
    let best: string | null = null;
    let bestCount = 0;
    for (const [genre, count] of [...counts.entries()].sort((a, b) => a[0].localeCompare(b[0]))) {
      if (count > bestCount) {
        best = genre;
        bestCount = count;
      }
    }
    return best;
  }, [adminBooks]);

  const mostPopularBook = useMemo(() => {
    const groups = new Map<string, { title: string; owners: Set<string> }>();
    for (const b of adminBooks) {
      const key = `${b.title.toLowerCase()}|${b.author.toLowerCase()}`;
      const g = groups.get(key) ?? { title: b.title, owners: new Set<string>() };
      g.owners.add(b.ownerId);
      groups.set(key, g);
    }
    let best: { title: string; owners: number } | null = null;
    for (const g of [...groups.values()].sort((a, b) => a.title.localeCompare(b.title))) {
      const owners = g.owners.size;
      if (!best || owners > best.owners) best = { title: g.title, owners };
    }
    return best?.title ?? null;
  }, [adminBooks]);

  function submitAsk(e: FormEvent) {
    e.preventDefault();
    if (!question.trim()) return;
    onAsk(question);
  }

  return (
    <div className="flex flex-col gap-6">
      <Card className="flex flex-wrap items-center justify-between gap-4">
        <div className="min-w-0">
          <h1 className="text-2xl font-semibold">Welcome back, {username}</h1>
          {loadingCore ? (
            <Skeleton className="mt-2 h-4 w-64" />
          ) : (
            <p className="mt-1 text-sm text-gray-500 dark:text-gray-400">
              {greetingSentence ?? 'Add a book to start getting personalized insights.'}
            </p>
          )}
        </div>
        <Button size="sm" onClick={() => setAddOpen(true)} icon={<Plus size={16} />}>
          Add book
        </Button>
      </Card>

      {loadingCore ? (
        <div className="grid grid-cols-2 gap-3 sm:grid-cols-4">
          {[0, 1, 2, 3].map((i) => (
            <Skeleton key={i} className="h-24" />
          ))}
        </div>
      ) : !stats || stats.totalBooks === 0 ? (
        <Card>
          <EmptyState
            icon={<BookOpen size={28} />}
            title="Your library is empty"
            message="Add your first book to see stats, genres and recommendations here."
            action={
              <Button onClick={() => setAddOpen(true)} icon={<Plus size={16} />}>
                Add book
              </Button>
            }
          />
        </Card>
      ) : (
        <>
          <div className="grid grid-cols-2 gap-3 sm:grid-cols-4">
            <StatTile icon={<BookOpen size={18} />} label="Total books" value={stats.totalBooks} />
            <StatTile icon={<CheckCircle2 size={18} />} label="Finished" value={stats.finished} />
            <StatTile icon={<Clock size={18} />} label="Reading" value={stats.reading} />
            <StatTile icon={<Bookmark size={18} />} label="To read" value={stats.toRead} />
          </div>

          <Card>
            <h2 className="mb-3 text-lg font-semibold">Reading progress</h2>
            <div className="flex h-3 w-full overflow-hidden rounded-full bg-gray-100 dark:bg-gray-800">
              <div className="bg-emerald-500" style={{ width: `${statusPct.finished}%` }} />
              <div className="bg-blue-500" style={{ width: `${statusPct.reading}%` }} />
              <div className="bg-gray-300 dark:bg-gray-600" style={{ width: `${statusPct.toRead}%` }} />
            </div>
            <div className="mt-3 flex flex-wrap gap-4 text-xs text-gray-600 dark:text-gray-300">
              <span className="flex items-center gap-1.5">
                <span className="h-2.5 w-2.5 rounded-full bg-emerald-500" /> Finished {statusPct.finished}%
              </span>
              <span className="flex items-center gap-1.5">
                <span className="h-2.5 w-2.5 rounded-full bg-blue-500" /> Reading {statusPct.reading}%
              </span>
              <span className="flex items-center gap-1.5">
                <span className="h-2.5 w-2.5 rounded-full bg-gray-300 dark:bg-gray-600" /> To read {statusPct.toRead}%
              </span>
            </div>
          </Card>

          <div className="grid grid-cols-1 gap-6 lg:grid-cols-2">
            <Card>
              <h2 className="mb-3 text-lg font-semibold">Top genres</h2>
              {topGenres.length === 0 ? (
                <p className="text-sm text-gray-500 dark:text-gray-400">No genres recorded yet.</p>
              ) : (
                <div className="flex flex-col gap-3">
                  {topGenres.map((g) => (
                    <div key={g.genre}>
                      <div className="mb-1 flex items-center justify-between text-sm">
                        <span className="truncate font-medium">{g.genre}</span>
                        <span className="text-gray-500 dark:text-gray-400">{g.count}</span>
                      </div>
                      <div className="h-2 w-full rounded-full bg-gray-100 dark:bg-gray-800">
                        <div
                          className="h-2 rounded-full bg-indigo-500"
                          style={{ width: `${(g.count / maxGenreCount) * 100}%` }}
                        />
                      </div>
                    </div>
                  ))}
                </div>
              )}
            </Card>

            <Card>
              <h2 className="mb-3 text-lg font-semibold">Recently added</h2>
              <ul className="divide-y divide-gray-100 dark:divide-gray-800">
                {recent.map((b) => (
                  <li key={b.id} className="flex items-center justify-between gap-3 py-2 text-sm">
                    <div className="min-w-0">
                      <p className="truncate font-medium" title={b.title}>
                        {b.title}
                      </p>
                      <p className="truncate text-gray-500 dark:text-gray-400" title={b.author}>
                        {b.author}
                      </p>
                    </div>
                    <StatusBadge status={b.status} />
                  </li>
                ))}
              </ul>
            </Card>
          </div>
        </>
      )}

      <Card>
        <h2 className="mb-3 text-lg font-semibold">Ask the library</h2>
        <form
          onSubmit={submitAsk}
          className="flex items-center gap-2 rounded-2xl border border-gray-200 bg-gray-50 p-2 focus-within:border-indigo-400 focus-within:ring-2 focus-within:ring-indigo-500/30 dark:border-gray-700 dark:bg-gray-800"
        >
          <Search size={18} className="ml-2 shrink-0 text-gray-400" />
          <input
            value={question}
            onChange={(e) => setQuestion(e.target.value)}
            placeholder="e.g. Show the five most expensive books"
            maxLength={300}
            aria-label="Ask the library"
            className="flex-1 bg-transparent px-1 py-2 text-sm text-gray-900 placeholder:text-gray-400 focus:outline-none dark:text-gray-100 dark:placeholder:text-gray-500"
          />
          <button
            type="submit"
            disabled={!question.trim()}
            aria-label="Ask"
            className="inline-flex shrink-0 items-center justify-center gap-1.5 rounded-xl bg-indigo-600 px-4 py-2 text-sm font-medium text-white transition-colors hover:bg-indigo-700 focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-indigo-500 focus-visible:ring-offset-1 disabled:cursor-not-allowed disabled:opacity-50"
          >
            <Send size={16} />
          </button>
        </form>
        {examples.length > 0 && (
          <div className="mt-3 flex flex-wrap gap-2">
            {examples.slice(0, 3).map((ex) => (
              <button
                key={ex}
                type="button"
                onClick={() => onAsk(ex)}
                className="rounded-full border border-gray-200 bg-white px-3 py-1.5 text-xs font-medium text-gray-600 transition-colors hover:border-indigo-300 hover:text-indigo-600 focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-indigo-500 dark:border-gray-700 dark:bg-gray-900 dark:text-gray-300 dark:hover:border-indigo-500 dark:hover:text-indigo-400"
              >
                {ex}
              </button>
            ))}
          </div>
        )}
      </Card>

      <Card>
        <div className="mb-3 flex items-center justify-between gap-3">
          <h2 className="text-lg font-semibold">Recommended for you</h2>
          <button
            type="button"
            onClick={() => onNavigate('recommendations')}
            className="inline-flex items-center gap-1 text-sm font-medium text-indigo-600 hover:underline dark:text-indigo-400"
          >
            View all <ArrowRight size={14} />
          </button>
        </div>
        {loadingRecs ? (
          <div className="grid grid-cols-1 gap-3 sm:grid-cols-3">
            {[0, 1, 2].map((i) => (
              <Skeleton key={i} className="h-20" />
            ))}
          </div>
        ) : recs.length === 0 ? (
          <EmptyState icon={<Sparkles size={24} />} title="No recommendations yet" />
        ) : (
          <div className="grid grid-cols-1 gap-3 sm:grid-cols-3">
            {recs.slice(0, 3).map((item, i) => (
              <div
                key={`${item.title}-${item.author}-${i}`}
                className="flex flex-col gap-1.5 rounded-lg border border-gray-200 p-3 dark:border-gray-800"
              >
                <p className="truncate text-sm font-semibold" title={item.title}>
                  {item.title}
                </p>
                <p className="truncate text-xs text-gray-500 dark:text-gray-400" title={item.author}>
                  {item.author}
                  {item.genre ? ` · ${item.genre}` : ''}
                </p>
                <Badge color="indigo" className="self-start">
                  {item.reason}
                </Badge>
              </div>
            ))}
          </div>
        )}
      </Card>

      {admin && (
        <Card>
          <div className="mb-3 flex items-center justify-between gap-3">
            <h2 className="flex items-center gap-2 text-lg font-semibold">
              <ShieldCheck size={18} className="text-indigo-500" /> Library overview
            </h2>
            <button
              type="button"
              onClick={() => onNavigate('admin')}
              className="inline-flex items-center gap-1 text-sm font-medium text-indigo-600 hover:underline dark:text-indigo-400"
            >
              Go to Admin <ArrowRight size={14} />
            </button>
          </div>
          {loadingAdmin ? (
            <div className="grid grid-cols-2 gap-3 sm:grid-cols-4">
              {[0, 1, 2, 3].map((i) => (
                <Skeleton key={i} className="h-24" />
              ))}
            </div>
          ) : adminBooks.length === 0 && adminUsers.length === 0 ? (
            <EmptyState title="No library data yet" />
          ) : (
            <div className="grid grid-cols-2 gap-3 sm:grid-cols-4">
              <StatTile icon={<Users size={18} />} label="Users" value={adminUsers.length} />
              <StatTile icon={<BookOpen size={18} />} label="Books" value={adminBooks.length} />
              <StatTile label="Top genre" value={libraryTopGenre ?? '—'} />
              <StatTile label="Most popular" value={mostPopularBook ?? '—'} />
            </div>
          )}
        </Card>
      )}

      {addOpen && <BookFormModal onClose={() => setAddOpen(false)} onSaved={() => void loadCore()} />}
    </div>
  );
}
