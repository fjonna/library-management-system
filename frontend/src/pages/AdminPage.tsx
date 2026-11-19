import { useCallback, useEffect, useState } from 'react';
import { api, errMsg } from '../api';
import { STATUS_LABELS } from '../types';
import type { Book, UserSummary } from '../types';

export default function AdminPage() {
  const [users, setUsers] = useState<UserSummary[]>([]);
  const [books, setBooks] = useState<Book[]>([]);
  const [error, setError] = useState('');

  const load = useCallback(async () => {
    try {
      const [u, b] = await Promise.all([
        api<UserSummary[]>('/api/admin/users'),
        api<Book[]>('/api/admin/books'),
      ]);
      setUsers(u);
      setBooks(b);
      setError('');
    } catch (e) {
      setError(errMsg(e));
    }
  }, []);

  useEffect(() => {
    void load();
  }, [load]);

  async function removeUser(u: UserSummary) {
    if (!window.confirm(`Delete user "${u.username}" and all their books?`)) return;
    try {
      await api(`/api/admin/users/${u.id}`, { method: 'DELETE' });
      await load();
    } catch (e) {
      setError(errMsg(e));
    }
  }

  async function removeBook(b: Book) {
    if (!window.confirm(`Delete "${b.title}" (${b.ownerUsername})?`)) return;
    try {
      await api(`/api/admin/books/${b.id}`, { method: 'DELETE' });
      await load();
    } catch (e) {
      setError(errMsg(e));
    }
  }

  return (
    <>
      {error && <div className="error">{error}</div>}

      <section className="card">
        <h2>Users ({users.length})</h2>
        <div className="table-wrap">
          <table>
            <thead>
              <tr><th>Username</th><th>Email</th><th>Books</th><th></th></tr>
            </thead>
            <tbody>
              {users.map((u) => (
                <tr key={u.id}>
                  <td>{u.username}</td>
                  <td>{u.email}</td>
                  <td>{u.bookCount}</td>
                  <td className="actions">
                    <button className="btn danger small" onClick={() => void removeUser(u)}>Delete</button>
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      </section>

      <section className="card">
        <h2>All books ({books.length})</h2>
        {books.length === 0 ? (
          <p className="empty">No books yet.</p>
        ) : (
          <div className="table-wrap">
            <table>
              <thead>
                <tr>
                  <th>Title</th><th>Author</th><th>Genre</th><th>Status</th>
                  <th>Price</th><th>Owner</th><th></th>
                </tr>
              </thead>
              <tbody>
                {books.map((b) => (
                  <tr key={b.id}>
                    <td>{b.title}</td>
                    <td>{b.author}</td>
                    <td>{b.genre ?? '—'}</td>
                    <td>{STATUS_LABELS[b.status]}</td>
                    <td>{b.price ?? '—'}</td>
                    <td>{b.ownerUsername}</td>
                    <td className="actions">
                      <button className="btn danger small" onClick={() => void removeBook(b)}>Delete</button>
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
