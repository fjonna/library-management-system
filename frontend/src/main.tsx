import { StrictMode } from 'react';
import { createRoot } from 'react-dom/client';
import './index.css';
import App from './App';
import { initAuth } from './keycloak';

const root = createRoot(document.getElementById('root')!);

initAuth()
  .then(() =>
    root.render(
      <StrictMode>
        <App />
      </StrictMode>,
    ),
  )
  .catch(() =>
    root.render(<p className="fatal">Could not reach the login server. Is Keycloak running?</p>),
  );
