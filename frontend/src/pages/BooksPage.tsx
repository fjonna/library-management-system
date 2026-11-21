import { useCallback, useEffect, useMemo, useState } from 'react';
import { Pencil, Plus, Search, Trash2 } from 'lucide-react';
import { api, errMsg } from '../api';
import BookFormModal from '../components/BookFormModal';
import Button from '../components/Button';
import Card from '../components/Card';
import ConfirmDialog from '../components/ConfirmDialog';
import EmptyState from '../components/EmptyState';
import { inputClass } from '../components/FormField';
import PageHeader from '../components/PageHeader';
import StatusBadge from '../components/StatusBadge';
import { useToast } from '../components/Toast';
import { formatDate, formatPrice } from '../format';
import { STATUS_LABELS } from '../types';
import type { Book, BookStatus } from '../types';

export default function BooksPage() {
  const toast = useToast();
  const [books, setBooks] = useState<Book[]>([]);
  const [loading, setLoading] = useState(true);
  const [modalOpen, setModalOpen] = useState(false);
  const [editingBook, setEditingBook] = useState<Book | null>(null);
  const [search, setSearch] = useState('');
  const [genreFilter, setGenreFilter] = useState('');
  const [statusFilter, setStatusFilter] = useState('');
  const [deleteTarget, setDeleteTarget] = useState<Book | null>(null);
  const [deleting, setDeleting] = useState(false);

  const load = useCallback(async () => {
    setLoading(true);
    try {
      setBooks(await api<Book[]>('/api/books'));
    } catch (e) {
      toast.show('error', errMsg(e));
    } finally {
      setLoading(false);
    }
  }, [toast]);

  useEffect(() => {
    void load();
  }, [load]);

  function openAdd() {
    setEditingBook(null);
    setModalOpen(true);
  }

  function openEdit(b: Book) {
    setEditingBook(b);
    setModalOpen(true);
  }

  async function confirmDelete() {
    if (!deleteTarget) return;
    setDeleting(true);
    try {
      await api(`/api/books/${deleteTarget.id}`, { method: 'DELETE' });
      toast.show('success', `Deleted "${deleteTarget.title}".`);
      setDeleteTarget(null);
      await load();
    } catch (err) {
      toast.show('error', errMsg(err));
    } finally {
      setDeleting(false);
    }
  }

  const genres = useMemo(
    () => [...new Set(books.map((b) => b.genre).filter((g): g is string => !!g))].sort(),
    [books],
  );

  const shown = useMemo(() => {
    const q = search.trim().toLowerCase();
    return books.filter((b) => {
      if (genreFilter && b.genre !== genreFilter) return false;
      if (statusFilter && b.status !== statusFilter) return false;
      if (q && ![b.title, b.author, b.genre ?? ''].some((v) => v.toLowerCase().includes(q))) return false;
      return true;
    });
  }, [books, search, genreFilter, statusFilter]);

  return (
    <div className="flex flex-col gap-6">
      <PageHeader
        title="My books"
        description="Browse, search and manage everything in your library."
        action={
          <Button onClick={openAdd} icon={<Plus size={16} />}>
            Add book
          </Button>
        }
      />

      <Card className="flex flex-col gap-3 sm:flex-row sm:items-center">
        <div className="relative flex-1">
          <Search size={16} className="pointer-events-none absolute left-3 top-1/2 -translate-y-1/2 text-gray-400" />
          <input
            value={search}
            onChange={(e) => setSearch(e.target.value)}
            placeholder="Search by title, author or genre"
            aria-label="Search books"
            className={`${inputClass} pl-9`}
          />
        </div>
        <select
          value={genreFilter}
          onChange={(e) => setGenreFilter(e.target.value)}
          aria-label="Filter by genre"
          className={`${inputClass} sm:w-48`}
        >
          <option value="">All genres</option>
          {genres.map((g) => (
            <option key={g} value={g}>
              {g}
            </option>
          ))}
        </select>
        <select
          value={statusFilter}
          onChange={(e) => setStatusFilter(e.target.value)}
          aria-label="Filter by status"
          className={`${inputClass} sm:w-48`}
        >
          <option value="">All statuses</option>
          {(Object.keys(STATUS_LABELS) as BookStatus[]).map((s) => (
            <option key={s} value={s}>
              {STATUS_LABELS[s]}
            </option>
          ))}
        </select>
      </Card>

      <Card>
        {loading ? (
          <div className="flex flex-col gap-3" aria-hidden="true">
            {[0, 1, 2].map((i) => (
              <div key={i} className="h-14 animate-pulse rounded-lg bg-gray-100 dark:bg-gray-800" />
            ))}
          </div>
        ) : shown.length === 0 ? (
          books.length === 0 ? (
            <EmptyState
              title="No books yet"
              message="Add your first book to start building your library."
              action={
                <Button onClick={openAdd} icon={<Plus size={16} />}>
                  Add book
                </Button>
              }
            />
          ) : (
            <EmptyState title="No books match your filters" message="Try a different search or clear the filters." />
          )
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
                  <th className="px-3 py-2 font-medium">Pages</th>
                  <th className="px-3 py-2 font-medium">Added</th>
                  <th className="px-3 py-2" />
                </tr>
              </thead>
              <tbody className="divide-y divide-gray-100 dark:divide-gray-800">
                {shown.map((b) => (
                  <tr key={b.id} className="transition-colors hover:bg-gray-50 dark:hover:bg-gray-800/40">
                    <td className="max-w-[220px] truncate px-3 py-2.5 font-medium" title={b.title}>
                      {b.title}
                    </td>
                    <td className="max-w-[160px] truncate px-3 py-2.5 text-gray-600 dark:text-gray-300" title={b.author}>
                      {b.author}
                    </td>
                    <td className="px-3 py-2.5 text-gray-600 dark:text-gray-300">{b.genre ?? '—'}</td>
                    <td className="px-3 py-2.5">
                      <StatusBadge status={b.status} />
                    </td>
                    <td className="px-3 py-2.5 text-gray-600 dark:text-gray-300">{formatPrice(b.price)}</td>
                    <td className="px-3 py-2.5 text-gray-600 dark:text-gray-300">{b.pages ?? '—'}</td>
                    <td className="px-3 py-2.5 text-gray-600 dark:text-gray-300">{formatDate(b.createdAt)}</td>
                    <td className="px-3 py-2.5">
                      <div className="flex justify-end gap-1">
                        <button
                          type="button"
                          onClick={() => openEdit(b)}
                          aria-label={`Edit ${b.title}`}
                          title="Edit"
                          className="rounded-lg p-1.5 text-gray-500 hover:bg-gray-100 focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-indigo-500 dark:text-gray-400 dark:hover:bg-gray-800"
                        >
                          <Pencil size={16} />
                        </button>
                        <button
                          type="button"
                          onClick={() => setDeleteTarget(b)}
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

      {modalOpen && <BookFormModal book={editingBook} onClose={() => setModalOpen(false)} onSaved={() => void load()} />}

      {deleteTarget && (
        <ConfirmDialog
          title="Delete book"
          message={`Delete "${deleteTarget.title}"? This cannot be undone.`}
          loading={deleting}
          onConfirm={() => void confirmDelete()}
          onCancel={() => setDeleteTarget(null)}
        />
      )}
    </div>
  );
}
