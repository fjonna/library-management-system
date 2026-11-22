import type { QueryResult } from './types';

/**
 * Deterministic "related question" chips shown after an answer. Pure string heuristics over the
 * result's intent label and its "understood as" text (genre=/author=/status= filters) - no backend
 * involvement, so the same result always produces the same three suggestions.
 */

const INTENT_FOLLOW_UPS: Record<string, string[]> = {
  CHEAPEST: ['Show the five most expensive books', 'What is the average price of my books?'],
  TOP_EXPENSIVE: ['Show my cheapest 3 books', 'What is the total value of my books?'],
  LONGEST: ['Show the 3 shortest books', 'How many pages do all my books have together?'],
  AVERAGE_PRICE: ['What is the total value of my books?', 'Show the five most expensive books'],
  TOTAL_VALUE: ['What is the average price of my books?', 'How many books have I completed?'],
  COUNT_BOOKS: ['How many books per genre?', 'Show my most recent books'],
  GENRE_BREAKDOWN: ['Which author has the most books?', 'What is the average price of my books?'],
  TOP_AUTHOR: ['How many books per genre?', 'Show my longest books'],
  RECENT_BOOKS: ['Show my longest books', 'Show the five most expensive books'],
  SEARCH_TITLE: ['Show my most recent books', 'How many books have I completed?'],
  BOOKS_UNDER_PRICE: ['Show books over 300 pages', 'What is the average price of my books?'],
  LIST_BOOKS: ['How many books have I completed?', 'How many books per genre?'],
  MOST_BOOKS_OWNER: ['Which is the most popular book?', 'How many books does each user have?'],
  MOST_POPULAR_BOOK: ['Who owns the most books?', 'Show the five most expensive books'],
};

function byAggregateKeyword(intent: string): string[] {
  if (intent.includes('SUM')) return ['What is the average price of my books?'];
  if (intent.includes('AVG')) return ['What is the total value of my books?'];
  if (intent.includes('_BY_USER')) return ['Who owns the most books?'];
  if (intent.includes('GROUP')) return ['Show the five most expensive books'];
  return ['Show the five most expensive books', 'How many books have I completed?'];
}

/** "genre=fantasy" / "author=orwell" / "status=COMPLETED" inside the understoodAs sentence. */
function extractFilter(understoodAs: string, key: 'genre' | 'author' | 'status'): string | null {
  const match = new RegExp(`${key}=([^,)]+)`, 'i').exec(understoodAs);
  return match ? match[1].trim() : null;
}

export function generateFollowUps(question: string, result: QueryResult, examples: string[]): string[] {
  const intent = result.intent ?? '';
  const base = INTENT_FOLLOW_UPS[intent] ?? byAggregateKeyword(intent);
  const suggestions: string[] = [...base];

  // "add the same filter to another field": reuse the genre filter for a pages-oriented question.
  const genre = extractFilter(result.understoodAs ?? '', 'genre');
  if (genre) {
    suggestions.push(`Average pages of my ${genre} books`);
  } else {
    const author = extractFilter(result.understoodAs ?? '', 'author');
    if (author) suggestions.push(`Show my longest books by ${author}`);
  }

  for (const ex of examples) {
    if (suggestions.length >= 3) break;
    suggestions.push(ex);
  }

  const seen = new Set<string>([question.trim().toLowerCase()]);
  const result3: string[] = [];
  for (const s of suggestions) {
    const key = s.trim().toLowerCase();
    if (seen.has(key)) continue;
    seen.add(key);
    result3.push(s);
    if (result3.length === 3) break;
  }
  return result3;
}
