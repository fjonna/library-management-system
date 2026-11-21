import { useEffect, useState } from 'react';
import { api } from './api';
import { currentUsername, isAdmin, keycloak } from './keycloak';
import AdminPage from './pages/AdminPage';
import AgentPage from './pages/AgentPage';
import BooksPage from './pages/BooksPage';
import InsightsPage from './pages/InsightsPage';
import RecommendationsPage from './pages/RecommendationsPage';

type Tab = 'books' | 'agent' | 'recommendations' | 'insights' | 'admin';

export default function App() {
  const [tab, setTab] = useState<Tab>('books');
  const admin = isAdmin();

  // makes sure the logged-in user exists in the library database
  useEffect(() => {
    api('/api/me').catch(() => undefined);
  }, []);

  return (
    <>
      <header className="header">
        <span className="brand">Library</span>
        <nav className="nav">
          <button className={tab === 'books' ? 'active' : ''} onClick={() => setTab('books')}>
            My books
          </button>
          <button className={tab === 'agent' ? 'active' : ''} onClick={() => setTab('agent')}>
            Ask the library
          </button>
          <button
            className={tab === 'recommendations' ? 'active' : ''}
            onClick={() => setTab('recommendations')}
          >
            Recommended for you
          </button>
          <button className={tab === 'insights' ? 'active' : ''} onClick={() => setTab('insights')}>
            Insights
          </button>
          {admin && (
            <button className={tab === 'admin' ? 'active' : ''} onClick={() => setTab('admin')}>
              Admin
            </button>
          )}
        </nav>
        <div className="who">
          <span>{currentUsername()}</span>
          {admin && <span className="badge">ADMIN</span>}
          <button
            className="btn secondary small"
            onClick={() => keycloak.logout({ redirectUri: window.location.origin + '/' })}
          >
            Log out
          </button>
        </div>
      </header>
      <main>
        {tab === 'books' && <BooksPage />}
        {tab === 'agent' && <AgentPage />}
        {tab === 'recommendations' && <RecommendationsPage />}
        {tab === 'insights' && <InsightsPage />}
        {tab === 'admin' && admin && <AdminPage />}
      </main>
    </>
  );
}
