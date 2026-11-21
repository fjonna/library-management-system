import { useEffect, useState } from 'react';
import type { LucideIcon } from 'lucide-react';
import { BookOpen, LayoutDashboard, LogOut, Menu, MessageCircle, ShieldCheck, Sparkles, BarChart3, X } from 'lucide-react';
import { api } from './api';
import { currentUsername, isAdmin, keycloak } from './keycloak';
import AdminPage from './pages/AdminPage';
import AgentPage from './pages/AgentPage';
import BooksPage from './pages/BooksPage';
import DashboardPage from './pages/DashboardPage';
import InsightsPage from './pages/InsightsPage';
import LandingPage from './pages/LandingPage';
import RecommendationsPage from './pages/RecommendationsPage';

export type Tab = 'dashboard' | 'books' | 'agent' | 'recommendations' | 'insights' | 'admin';

const TABS: { id: Tab; label: string; icon: LucideIcon; adminOnly?: boolean }[] = [
  { id: 'dashboard', label: 'Dashboard', icon: LayoutDashboard },
  { id: 'books', label: 'My books', icon: BookOpen },
  { id: 'agent', label: 'Ask the library', icon: MessageCircle },
  { id: 'recommendations', label: 'Recommended', icon: Sparkles },
  { id: 'insights', label: 'Insights', icon: BarChart3 },
  { id: 'admin', label: 'Admin', icon: ShieldCheck, adminOnly: true },
];

const tabButtonClass = (active: boolean) =>
  `inline-flex items-center gap-1.5 whitespace-nowrap rounded-lg px-3 py-2 text-sm font-medium transition-colors focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-indigo-500 ${
    active
      ? 'bg-indigo-50 text-indigo-700 dark:bg-indigo-500/10 dark:text-indigo-300'
      : 'text-gray-600 hover:bg-gray-100 dark:text-gray-300 dark:hover:bg-gray-800'
  }`;

export default function App() {
  const [tab, setTab] = useState<Tab>('dashboard');
  const [menuOpen, setMenuOpen] = useState(false);
  const [pendingQuestion, setPendingQuestion] = useState<string | undefined>(undefined);
  const authenticated = keycloak.authenticated ?? false;
  const admin = isAdmin();
  const visibleTabs = TABS.filter((t) => !t.adminOnly || admin);

  // makes sure the logged-in user exists in the library database
  useEffect(() => {
    if (authenticated) {
      api('/api/me').catch(() => undefined);
    }
  }, [authenticated]);

  function selectTab(t: Tab) {
    setTab(t);
    setMenuOpen(false);
  }

  function askFromDashboard(question: string) {
    setPendingQuestion(question);
    selectTab('agent');
  }

  function logout() {
    void keycloak.logout({ redirectUri: window.location.origin + '/' });
  }

  if (!authenticated) {
    return <LandingPage />;
  }

  return (
    <div className="flex min-h-screen flex-col bg-gray-50 text-gray-900 dark:bg-gray-950 dark:text-gray-100">
      <header className="sticky top-0 z-20 border-b border-gray-200 bg-white/80 backdrop-blur dark:border-gray-800 dark:bg-gray-900/80">
        <div className="mx-auto flex max-w-6xl items-center gap-4 px-4 py-3 sm:px-6">
          <span className="text-lg font-bold text-indigo-600 dark:text-indigo-400">Library</span>

          <nav className="hidden flex-1 items-center gap-1 overflow-x-auto sm:flex" aria-label="Main">
            {visibleTabs.map((t) => (
              <button key={t.id} onClick={() => selectTab(t.id)} className={tabButtonClass(tab === t.id)}>
                <t.icon size={16} />
                {t.label}
              </button>
            ))}
          </nav>

          <div className="hidden items-center gap-3 sm:flex">
            <span className="text-sm text-gray-600 dark:text-gray-300">{currentUsername()}</span>
            {admin && (
              <span className="rounded-full bg-indigo-100 px-2.5 py-0.5 text-xs font-semibold text-indigo-700 dark:bg-indigo-500/20 dark:text-indigo-300">
                ADMIN
              </span>
            )}
            <button
              onClick={logout}
              className="inline-flex items-center gap-1.5 rounded-lg border border-gray-300 px-3 py-1.5 text-sm font-medium text-gray-700 transition-colors hover:bg-gray-100 focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-indigo-500 dark:border-gray-700 dark:text-gray-200 dark:hover:bg-gray-800"
            >
              <LogOut size={16} /> Log out
            </button>
          </div>

          <button
            className="ml-auto inline-flex items-center justify-center rounded-lg p-2 text-gray-600 hover:bg-gray-100 focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-indigo-500 sm:hidden dark:text-gray-300 dark:hover:bg-gray-800"
            aria-label={menuOpen ? 'Close menu' : 'Open menu'}
            aria-expanded={menuOpen}
            onClick={() => setMenuOpen((o) => !o)}
          >
            {menuOpen ? <X size={22} /> : <Menu size={22} />}
          </button>
        </div>

        {menuOpen && (
          <div className="border-t border-gray-200 px-4 py-3 sm:hidden dark:border-gray-800">
            <nav className="flex flex-col gap-1" aria-label="Main">
              {visibleTabs.map((t) => (
                <button
                  key={t.id}
                  onClick={() => selectTab(t.id)}
                  className={`${tabButtonClass(tab === t.id)} text-left`}
                >
                  <t.icon size={16} />
                  {t.label}
                </button>
              ))}
            </nav>
            <div className="mt-3 flex items-center justify-between border-t border-gray-200 pt-3 dark:border-gray-800">
              <div className="flex items-center gap-2 text-sm text-gray-600 dark:text-gray-300">
                <span>{currentUsername()}</span>
                {admin && (
                  <span className="rounded-full bg-indigo-100 px-2.5 py-0.5 text-xs font-semibold text-indigo-700 dark:bg-indigo-500/20 dark:text-indigo-300">
                    ADMIN
                  </span>
                )}
              </div>
              <button
                onClick={logout}
                className="inline-flex items-center gap-1.5 rounded-lg border border-gray-300 px-3 py-1.5 text-sm font-medium text-gray-700 hover:bg-gray-100 dark:border-gray-700 dark:text-gray-200 dark:hover:bg-gray-800"
              >
                <LogOut size={16} /> Log out
              </button>
            </div>
          </div>
        )}
      </header>

      <main className="mx-auto w-full max-w-6xl flex-1 px-4 py-6 sm:px-6">
        {tab === 'dashboard' && (
          <DashboardPage username={currentUsername()} onNavigate={selectTab} onAsk={askFromDashboard} />
        )}
        {tab === 'books' && <BooksPage />}
        {tab === 'agent' && (
          <AgentPage
            initialQuestion={pendingQuestion}
            onConsumeInitialQuestion={() => setPendingQuestion(undefined)}
          />
        )}
        {tab === 'recommendations' && <RecommendationsPage />}
        {tab === 'insights' && <InsightsPage />}
        {tab === 'admin' && admin && <AdminPage />}
      </main>

      <footer className="border-t border-gray-200 py-6 text-center text-xs text-gray-400 dark:border-gray-800 dark:text-gray-500">
        Library
      </footer>
    </div>
  );
}
