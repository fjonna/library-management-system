import { keycloak } from './keycloak';

export function errMsg(e: unknown): string {
  return e instanceof Error ? e.message : String(e);
}

export async function api<T>(path: string, options: RequestInit = {}): Promise<T> {
  try {
    await keycloak.updateToken(30);
  } catch {
    await keycloak.login();
  }

  const res = await fetch(path, {
    ...options,
    headers: {
      'Content-Type': 'application/json',
      Authorization: `Bearer ${keycloak.token}`,
      ...options.headers,
    },
  });

  if (!res.ok) {
    let message = `${res.status} ${res.statusText}`;
    try {
      const body = await res.json();
      message = body.detail || body.message || body.error || message;
    } catch {
      /* no JSON body */
    }
    throw new Error(message);
  }

  if (res.status === 204) return undefined as T;
  return res.json() as Promise<T>;
}
