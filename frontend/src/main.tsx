import { StrictMode } from 'react';
import { createRoot } from 'react-dom/client';
import './index.css';
import App from './App';
import { ToastProvider } from './components/Toast';
import { initAuth } from './keycloak';

const root = createRoot(document.getElementById('root')!);

initAuth()
  .then(() =>
    root.render(
      <StrictMode>
        <ToastProvider>
          <App />
        </ToastProvider>
      </StrictMode>,
    ),
  )
  .catch(() =>
    root.render(
      <p className="p-10 text-center text-red-600 dark:text-red-400">
        Could not reach the login server. Is Keycloak running?
      </p>,
    ),
  );
