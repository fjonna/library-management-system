export function formatPrice(price: number | string | null | undefined): string {
  if (price === null || price === undefined || price === '') return '—';
  const n = typeof price === 'string' ? Number(price) : price;
  if (Number.isNaN(n)) return '—';
  return new Intl.NumberFormat(undefined, { style: 'currency', currency: 'USD' }).format(n);
}

export function formatDate(iso: string | null | undefined): string {
  if (!iso) return '—';
  const d = new Date(iso);
  if (Number.isNaN(d.getTime())) return '—';
  return new Intl.DateTimeFormat(undefined, { dateStyle: 'medium' }).format(d);
}
