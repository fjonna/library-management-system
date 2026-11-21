import { useCallback, useEffect, useMemo, useState } from 'react';
import { BookOpen, Sparkles, Trash2, Users } from 'lucide-react';
import { api, errMsg } from '../api';
import Card from '../components/Card';
import ConfirmDialog from '../components/ConfirmDialog';
import EmptyState from '../components/EmptyState';
import { inputClass } from '../components/FormField';
import InsightsView from '../components/InsightsView';
import Modal from '../components/Modal';
import PageHeader from '../components/PageHeader';
import StatTile from '../components/StatTile';
import StatusBadge from '../components/StatusBadge';
import { useToast } from '../components/Toast';
import { formatPrice } from '../format';
import { keycloak } from '../keycloak';
import type { Book, UserSummary } from '../types';

export default function AdminPage() {
  const toast = useToast();
  const myId = keycloak.tokenParsed?.sub as string | undefined;

  const [users, setUsers] = useState<UserSummary[]>([]);
  const [books, setBooks] = useState<Book[]>([]);
  const [loading, setLoading] = useState(true);
  const [userSearch, setUserSearch] = useState('');
  const [bookSearch, setBookSearch] = useState('');
  const [insightsUser, setInsightsUser] = useState<UserSummary | null>(null);
  const [deleteUserTarget, setDeleteUserTarget] = useState<UserSummary | null>(null);
  const [deleteBookTarget, setDeleteBookTarget] = useState<Book | null>(null);
  const [deleting, setDeleting] = useState(false);

  const load = useCallback(async () => {
    setLoading(true);
    try {
      const [u, b] = await Promise.all([
        api<UserSummary[]>('/api/admin/users'),
        api<Book[]>('/api/admin/books'),
      ]);
      setUsers(u);
      setBooks(b);
    } catch (e) {
      toast.show('error', errMsg(e));
    } finally {
      setLoading(false);
    }
  }, [toast]);

  useEffect(() => {
    void load();
  }, [load]);

  async function confirmDeleteUser() {
    if (!deleteUserTarget) return;
    setDeleting(true);
    try {
      await api(`/api/admin/users/${deleteUserTarget.id}`, { method: 'DELETE' });
      toast.show('success', `Deleted user "${deleteUserTarget.username}".`);
      setDeleteUserTarget(null);
      await load();
    } catch (e) {
      toast.show('error', errMsg(e));
    } finally {
      setDeleting(false);
    }
  }

  async function confirmDeleteBook() {
    if (!deleteBookTarget) return;
    setDeleting(true);
    try {
      await api(`/api/admin/books/${deleteBookTarget.id}`, { method: 'DELETE' });
      toast.show('success', `Deleted "${deleteBookTarget.title}".`);
      setDeleteBookTarget(null);
      await load();
    } catch (e) {
      toast.show('error', errMsg(e));
    } finally {
      setDeleting(false);
    }
  }

  const shownUsers = useMemo(() => {
    const q = userSearch.trim().toLowerCase();
    if (!q) return users;
    return users.filter((u) => [u.username, u.email].some((v) => v.toLowerCase().includes(q)));
  }, [users, userSearch]);

  const shownBooks = useMemo(() => {
    const q = bookSearch.trim().toLowerCase();
    if (!q) return books;
    return books.filter((b) =>
      [b.title, b.author, b.genre ?? '', b.ownerUsername].some((v) => v.toLowerCase().includes(q)),
    );
  }, [books, bookSearch]);

  const averagePrice = useMemo(() => {
    const priced = books.map((b) => b.price).filter((p): p is number => p !== null);
    if (priced.length === 0) return null;
    return priced.reduce((a, b) => a + b, 0) / priced.length;
  }, [books]);

  return (
    <div className="flex flex-col gap-6">
      <PageHeader title="Admin" description="Manage every user and book in the library." />

      <div className="grid grid-cols-2 gap-3 sm:grid-cols-3">
        <StatTile icon={<Users size={18} />} label="Users" value={users.length} />
        <StatTile icon={<BookOpen size={18} />} label="Books" value={books.length} />
        <StatTile icon={<Sparkles size={18} />} label="Avg. price" value={averagePrice === null ? '—' : formatPrice(averagePrice)} />
      </div>

      <Card>
        <div className="mb-3 flex flex-wrap items-center justify-between gap-3">
          <h2 className="text-lg font-semibold">Users ({shownUsers.length})</h2>
          <input
            value={userSearch}
            onChange={(e) => setUserSearch(e.target.value)}
            placeholder="Search users"
            aria-label="Search users"
            className={`${inputClass} max-w-xs`}
          />
        </div>
        {loading ? (
          <SkeletonRows />
        ) : shownUsers.length === 0 ? (
          <EmptyState title="No users found" />
        ) : (
          <div className="overflow-x-auto">
            <table className="w-full text-left text-sm">
              <thead className="text-xs uppercase tracking-wide text-gray-500 dark:text-gray-400">
                <tr className="border-b border-gray-200 dark:border-gray-800">
                  <th className="px-3 py-2 font-medium">Username</th>
                  <th className="px-3 py-2 font-medium">Email</th>
                  <th className="px-3 py-2 font-medium">Books</th>
                  <th className="px-3 py-2" />
                </tr>
              </thead>
              <tbody className="divide-y divide-gray-100 dark:divide-gray-800">
                {shownUsers.map((u) => {
                  const isSelf = u.id === myId;
                  return (
                    <tr key={u.id} className="transition-colors hover:bg-gray-50 dark:hover:bg-gray-800/40">
                      <td className="px-3 py-2.5 font-medium">{u.username}</td>
                      <td
                        className="max-w-[220px] truncate px-3 py-2.5 text-gray-600 dark:text-gray-300"
                        title={u.email}
                      >
                        {u.email}
                      </td>
                      <td className="px-3 py-2.5 text-gray-600 dark:text-gray-300">{u.bookCount}</td>
                      <td className="px-3 py-2.5">
                        <div className="flex justify-end gap-1">
                          <button
                            type="button"
                            onClick={() => setInsightsUser(u)}
                            className="rounded-lg px-2.5 py-1.5 text-xs font-medium text-indigo-600 hover:bg-indigo-50 focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-indigo-500 dark:text-indigo-400 dark:hover:bg-indigo-500/10"
                          >
                            Insights
                          </button>
                          <button
                            type="button"
                            onClick={() => setDeleteUserTarget(u)}
                            disabled={isSelf}
                            title={isSelf ? 'You cannot delete yourself' : 'Delete user'}
                            aria-label={`Delete ${u.username}`}
                            className="rounded-lg p-1.5 text-red-500 hover:bg-red-50 focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-red-500 disabled:cursor-not-allowed disabled:opacity-40 disabled:hover:bg-transparent dark:text-red-400 dark:hover:bg-red-950/40"
                          >
                            <Trash2 size={16} />
                          </button>
                        </div>
                      </td>
                    </tr>
                  );
                })}
              </tbody>
            </table>
          </div>
        )}
      </Card>

      <Card>
        <div className="mb-3 flex flex-wrap items-center justify-between gap-3">
          <h2 className="text-lg font-semibold">All books ({shownBooks.length})</h2>
          <input
            value={bookSearch}
            onChange={(e) => setBookSearch(e.target.value)}
            placeholder="Search books"
            aria-label="Search books"
            className={`${inputClass} max-w-xs`}
          />
        </div>
        {loading ? (
          <SkeletonRows />
        ) : shownBooks.length === 0 ? (
          <EmptyState title="No books found" />
        ) : (
          <div className="overflow-x-auto">
            <table className="w-full text-left text-sm">
              <thead className="text-xs uppercase tracking-wide text-gray-500 dark:text-gray-400">
                <tr className="border-b border-gray-200 dark:border-gray-800">
                  <th className="px-3 py-2 font-medium">Title</th>
                  <th className="px-3 py-2 font-medium">Author</th>
                  <th className="px-3 py-2 font-medium">Genre</th>
                  <th className="px-3 py-2 font-medium">Status</th>
                  <th className="px-3 py-2 font-medium">Price</th>
                  <th className="px-3 py-2 font-medium">Owner</th>
                  <th className="px-3 py-2" />
                </tr>
              </thead>
              <tbody className="divide-y divide-gray-100 dark:divide-gray-800">
                {shownBooks.map((b) => (
                  <tr key={b.id} className="transition-colors hover:bg-gray-50 dark:hover:bg-gray-800/40">
                    <td className="max-w-[200px] truncate px-3 py-2.5 font-medium" title={b.title}>
                      {b.title}
                    </td>
                    <td
                      className="max-w-[160px] truncate px-3 py-2.5 text-gray-600 dark:text-gray-300"
                      title={b.author}
                    >
                      {b.author}
                    </td>
                    <td className="px-3 py-2.5 text-gray-600 dark:text-gray-300">{b.genre ?? '—'}</td>
                    <td className="px-3 py-2.5">
                      <StatusBadge status={b.status} />
                    </td>
                    <td className="px-3 py-2.5 text-gray-600 dark:text-gray-300">{formatPrice(b.price)}</td>
                    <td className="px-3 py-2.5 text-gray-600 dark:text-gray-300">{b.ownerUsername}</td>
                    <td className="px-3 py-2.5">
                      <div className="flex justify-end">
                        <button
                          type="button"
                          onClick={() => setDeleteBookTarget(b)}
                          aria-label={`Delete ${b.title}`}
                          title="Delete"
                          className="rounded-lg p-1.5 text-red-500 hover:bg-red-50 focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-red-500 dark:text-red-400 dark:hover:bg-red-950/40"
                        >
                          <Trash2 size={16} />
                        </button>
                      </div>
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        )}
      </Card>

      {insightsUser && (
        <Modal title={`${insightsUser.username}'s insights`} onClose={() => setInsightsUser(null)} widthClass="max-w-2xl">
          <InsightsView
            basePath={`/api/admin/insights/${insightsUser.id}`}
            summarizeLabel={`Summarize ${insightsUser.username}'s reading habits`}
          />
        </Modal>
      )}

      {deleteUserTarget && (
        <ConfirmDialog
          title="Delete user"
          message={`Delete user "${deleteUserTarget.username}" and all their books? This cannot be undone.`}
          loading={deleting}
          onConfirm={() => void confirmDeleteUser()}
          onCancel={() => setDeleteUserTarget(null)}
        />
      )}

      {deleteBookTarget && (
        <ConfirmDialog
          title="Delete book"
          message={`Delete "${deleteBookTarget.title}" (${deleteBookTarget.ownerUsername})? This cannot be undone.`}
          loading={deleting}
          onConfirm={() => void confirmDeleteBook()}
          onCancel={() => setDeleteBookTarget(null)}
        />
      )}
    </div>
  );
}

function SkeletonRows() {
  return (
    <div className="flex flex-col gap-3" aria-hidden="true">
      {[0, 1, 2].map((i) => (
        <div key={i} className="h-10 animate-pulse rounded-lg bg-gray-100 dark:bg-gray-800" />
      ))}
    </div>
  );
}
