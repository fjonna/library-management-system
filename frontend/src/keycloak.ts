import Keycloak from 'keycloak-js';

// dev server: Keycloak on :8081; production build (behind nginx): same origin under /auth
const defaultUrl = import.meta.env.PROD
  ? `${window.location.origin}/auth`
  : 'http://localhost:8081/auth';

export const keycloak = new Keycloak({
  url: import.meta.env.VITE_KEYCLOAK_URL ?? defaultUrl,
  realm: import.meta.env.VITE_KEYCLOAK_REALM ?? 'library',
  clientId: import.meta.env.VITE_KEYCLOAK_CLIENT ?? 'library-frontend',
});

// init() may only run once (React StrictMode would call it twice)
let started: Promise<boolean> | null = null;

export function initAuth(): Promise<boolean> {
  if (!started) {
    started = keycloak.init({
      onLoad: 'check-sso',
      pkceMethod: 'S256',
      checkLoginIframe: false,
    });
  }
  return started;
}

export const isAdmin = (): boolean =>
  keycloak.tokenParsed?.realm_access?.roles?.includes('ADMIN') ?? false;

export const currentUsername = (): string =>
  (keycloak.tokenParsed?.preferred_username as string | undefined) ?? '';
