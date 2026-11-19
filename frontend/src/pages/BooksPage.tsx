import { useCallback, useEffect, useState } from 'react';
import type { FormEvent } from 'react';
import { api, errMsg } from '../api';
import { STATUS_LABELS } from '../types';
import type { Book, BookStatus } from '../types';

const emptyForm = {
  title: '',
  author: '',
  genre: '',
  status: 'TO_READ' as BookStatus,
  price: '',
  pages: '',
  isbn: '',
};

export default function BooksPage() {
  const [books, setBooks] = useState<Book[]>([]);
  const [form, setForm] = useState(emptyForm);
  const [editId, setEditId] = useState<number | null>(null);
  const [filter, setFilter] = useState('');
  const [error, setError] = useState('');

  const load = useCallback(async () => {
    try {
      setBooks(await api<Book[]>('/api/books'));
      setError('');
    } catch (e) {
      setError(errMsg(e));
    }
  }, []);

  useEffect(() => {
    void load();
  }, [load]);

  function set<K extends keyof typeof emptyForm>(key: K, value: (typeof emptyForm)[K]) {
    setForm((f) => ({ ...f, [key]: value }));
  }

  function startEdit(b: Book) {
    setEditId(b.id);
    setForm({
      title: b.title,
      author: b.author,
      genre: b.genre ?? '',
      status: b.status,
      price: b.price === null ? '' : String(b.price),
      pages: b.pages === null ? '' : String(b.pages),
      isbn: b.isbn ?? '',
    });
    window.scrollTo({ top: 0, behavior: 'smooth' });
  }

  function reset() {
    setEditId(null);
    setForm(emptyForm);
  }

  async function submit(e: FormEvent) {
    e.preventDefault();
    const body = {
      title: form.title,
      author: form.author,
      genre: form.genre || null,
      status: form.status,
      price: form.price === '' ? null : Number(form.price),
      pages: form.pages === '' ? null : Number(form.pages),
      isbn: form.isbn || null,
    };
    try {
      if (editId === null) {
        await api('/api/books', { method: 'POST', body: JSON.stringify(body) });
      } else {
        await api(`/api/books/${editId}`, { method: 'PUT', body: JSON.stringify(body) });
      }
      reset();
      await load();
    } catch (err) {
      setError(errMsg(err));
    }
  }

  async function remove(b: Book) {
    if (!window.confirm(`Delete "${b.title}"?`)) return;
    try {
      await api(`/api/books/${b.id}`, { method: 'DELETE' });
      await load();
    } catch (err) {
      setError(errMsg(err));
    }
  }

  const q = filter.trim().toLowerCase();
  const shown = q
    ? books.filter((b) =>
        [b.title, b.author, b.genre ?? ''].some((v) => v.toLowerCase().includes(q)),
      )
    : books;

  return (
    <>
      {error && <div className="error">{error}</div>}

      <form className="card" onSubmit={submit}>
        <h2>{editId === null ? 'Add a book' : 'Edit book'}</h2>
        <div className="form-grid">
          <label>
            Title *
            <input value={form.title} onChange={(e) => set('title', e.target.value)} required />
          </label>
          <label>
            Author *
            <input value={form.author} onChange={(e) => set('author', e.target.value)} required />
          </label>
          <label>
            Genre
            <input value={form.genre} onChange={(e) => set('genre', e.target.value)} />
          </label>
          <label>
            Status
            <select value={form.status} onChange={(e) => set('status', e.target.value as BookStatus)}>
              {(Object.keys(STATUS_LABELS) as BookStatus[]).map((s) => (
                <option key={s} value={s}>
                  {STATUS_LABELS[s]}
                </option>
              ))}
            </select>
          </label>
          <label>
            Price
            <input type="number" min="0" step="0.01" value={form.price}
              onChange={(e) => set('price', e.target.value)} />
          </label>
          <label>
            Pages
            <input type="number" min="1" value={form.pages}
              onChange={(e) => set('pages', e.target.value)} />
          </label>
          <label>
            ISBN
            <input value={form.isbn} onChange={(e) => set('isbn', e.target.value)} />
          </label>
        </div>
        <div className="form-actions">
          <button className="btn" type="submit">
            {editId === null ? 'Add book' : 'Save changes'}
          </button>
          {editId !== null && (
            <button className="btn secondary" type="button" onClick={reset}>
              Cancel
            </button>
          )}
        </div>
      </form>

      <section className="card">
        <h2>My books ({books.length})</h2>
        <input className="filter" placeholder="Filter by title, author or genre"
          value={filter} onChange={(e) => setFilter(e.target.value)} />
        {shown.length === 0 ? (
          <p className="empty">No books yet.</p>
        ) : (
          <div className="table-wrap">
            <table>
              <thead>
                <tr>
                  <th>Title</th><th>Author</th><th>Genre</th><th>Status</th>
                  <th>Price</th><th>Pages</th><th></th>
                </tr>
              </thead>
              <tbody>
                {shown.map((b) => (
                  <tr key={b.id}>
                    <td>{b.title}</td>
                    <td>{b.author}</td>
                    <td>{b.genre ?? '—'}</td>
                    <td>{STATUS_LABELS[b.status]}</td>
                    <td>{b.price ?? '—'}</td>
                    <td>{b.pages ?? '—'}</td>
                    <td className="actions">
                      <button className="btn secondary small" onClick={() => startEdit(b)}>Edit</button>
                      <button className="btn danger small" onClick={() => void remove(b)}>Delete</button>
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        )}
      </section>
    </>
  );
}
