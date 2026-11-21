import { useState } from 'react';
import type { FormEvent } from 'react';
import { api, errMsg } from '../api';
import Button from './Button';
import FormField, { errorInputClass, inputClass } from './FormField';
import Modal from './Modal';
import { useToast } from './Toast';
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

type FormValues = typeof emptyForm;
type FormErrors = Partial<Record<keyof FormValues, string>>;

interface Props {
  book?: Book | null;
  onClose: () => void;
  onSaved: () => void;
}

/** Add/edit book modal, shared by My books and the Dashboard's "Add book" shortcut. */
export default function BookFormModal({ book, onClose, onSaved }: Props) {
  const toast = useToast();
  const editId = book?.id ?? null;
  const [form, setForm] = useState<FormValues>(
    book
      ? {
          title: book.title,
          author: book.author,
          genre: book.genre ?? '',
          status: book.status,
          price: book.price === null ? '' : String(book.price),
          pages: book.pages === null ? '' : String(book.pages),
          isbn: book.isbn ?? '',
        }
      : emptyForm,
  );
  const [errors, setErrors] = useState<FormErrors>({});
  const [saving, setSaving] = useState(false);

  function set<K extends keyof FormValues>(key: K, value: FormValues[K]) {
    setForm((f) => ({ ...f, [key]: value }));
    setErrors((e) => ({ ...e, [key]: undefined }));
  }

  function validate(): boolean {
    const next: FormErrors = {};
    if (!form.title.trim()) next.title = 'Title is required.';
    if (!form.author.trim()) next.author = 'Author is required.';
    if (form.price !== '' && (Number.isNaN(Number(form.price)) || Number(form.price) < 0)) {
      next.price = 'Enter a valid, non-negative price.';
    }
    if (form.pages !== '' && (!Number.isInteger(Number(form.pages)) || Number(form.pages) < 1)) {
      next.pages = 'Enter a whole number of pages.';
    }
    setErrors(next);
    return Object.keys(next).length === 0;
  }

  async function submit(e: FormEvent) {
    e.preventDefault();
    if (!validate()) return;
    const body = {
      title: form.title.trim(),
      author: form.author.trim(),
      genre: form.genre.trim() || null,
      status: form.status,
      price: form.price === '' ? null : Number(form.price),
      pages: form.pages === '' ? null : Number(form.pages),
      isbn: form.isbn.trim() || null,
    };
    setSaving(true);
    try {
      if (editId === null) {
        await api('/api/books', { method: 'POST', body: JSON.stringify(body) });
        toast.show('success', `Added "${body.title}".`);
      } else {
        await api(`/api/books/${editId}`, { method: 'PUT', body: JSON.stringify(body) });
        toast.show('success', `Saved changes to "${body.title}".`);
      }
      onSaved();
      onClose();
    } catch (err) {
      toast.show('error', errMsg(err));
    } finally {
      setSaving(false);
    }
  }

  return (
    <Modal
      title={editId === null ? 'Add a book' : 'Edit book'}
      onClose={onClose}
      footer={
        <>
          <Button variant="secondary" onClick={onClose} disabled={saving}>
            Cancel
          </Button>
          <Button onClick={submit} loading={saving}>
            {editId === null ? 'Add book' : 'Save changes'}
          </Button>
        </>
      }
    >
      <form onSubmit={submit} className="grid grid-cols-1 gap-4 sm:grid-cols-2">
        <FormField label="Title" htmlFor="title" required error={errors.title}>
          <input
            id="title"
            className={`${inputClass} ${errors.title ? errorInputClass : ''}`}
            value={form.title}
            onChange={(e) => set('title', e.target.value)}
          />
        </FormField>
        <FormField label="Author" htmlFor="author" required error={errors.author}>
          <input
            id="author"
            className={`${inputClass} ${errors.author ? errorInputClass : ''}`}
            value={form.author}
            onChange={(e) => set('author', e.target.value)}
          />
        </FormField>
        <FormField label="Genre" htmlFor="genre">
          <input id="genre" className={inputClass} value={form.genre} onChange={(e) => set('genre', e.target.value)} />
        </FormField>
        <FormField label="Status" htmlFor="status">
          <select
            id="status"
            className={inputClass}
            value={form.status}
            onChange={(e) => set('status', e.target.value as BookStatus)}
          >
            {(Object.keys(STATUS_LABELS) as BookStatus[]).map((s) => (
              <option key={s} value={s}>
                {STATUS_LABELS[s]}
              </option>
            ))}
          </select>
        </FormField>
        <FormField label="Price" htmlFor="price" error={errors.price}>
          <input
            id="price"
            type="number"
            min="0"
            step="0.01"
            className={`${inputClass} ${errors.price ? errorInputClass : ''}`}
            value={form.price}
            onChange={(e) => set('price', e.target.value)}
          />
        </FormField>
        <FormField label="Pages" htmlFor="pages" error={errors.pages}>
          <input
            id="pages"
            type="number"
            min="1"
            className={`${inputClass} ${errors.pages ? errorInputClass : ''}`}
            value={form.pages}
            onChange={(e) => set('pages', e.target.value)}
          />
        </FormField>
        <FormField label="ISBN" htmlFor="isbn">
          <input id="isbn" className={inputClass} value={form.isbn} onChange={(e) => set('isbn', e.target.value)} />
        </FormField>
      </form>
    </Modal>
  );
}
