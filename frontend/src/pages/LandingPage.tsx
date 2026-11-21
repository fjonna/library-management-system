import { useEffect, useState } from 'react';
import { BookOpen, LogIn, MessageCircle, Sparkles, UserPlus } from 'lucide-react';
import Button from '../components/Button';
import { keycloak } from '../keycloak';

const FEATURE_CHIPS = [
  { label: 'Track books', icon: BookOpen },
  { label: 'Ask in plain language', icon: MessageCircle },
  { label: 'Recommendations', icon: Sparkles },
];

function fadeClass(revealed: boolean) {
  return `transition-all duration-700 ease-out motion-reduce:transition-none motion-reduce:transform-none ${
    revealed ? 'opacity-100 translate-y-0' : 'opacity-0 translate-y-4'
  }`;
}

export default function LandingPage() {
  const [revealed, setRevealed] = useState(false);

  useEffect(() => {
    const id = requestAnimationFrame(() => setRevealed(true));
    return () => cancelAnimationFrame(id);
  }, []);

  useEffect(() => {
    document.title = 'Library — organize and ask about your books';
    let meta = document.querySelector<HTMLMetaElement>('meta[name="description"]');
    if (!meta) {
      meta = document.createElement('meta');
      meta.name = 'description';
      document.head.appendChild(meta);
    }
    meta.content =
      'Track your reading, ask questions about your collection in plain language, and get personalized book recommendations and insights with Library.';
  }, []);

  function signIn() {
    void keycloak.login({ redirectUri: window.location.origin + '/' });
  }

  function createAccount() {
    void keycloak.register({ redirectUri: window.location.origin + '/' });
  }

  return (
    <div
      className="relative flex min-h-screen flex-col overflow-hidden bg-[linear-gradient(135deg,#e0e7ff_0%,#c7d2fe_50%,#bae6fd_100%)] text-gray-900
        dark:bg-[linear-gradient(135deg,#0b1020_0%,#1e1b4b_50%,#0f172a_100%)] dark:text-gray-100"
    >
      {/* decorative gradient blobs + faint book spines, CSS/SVG only */}
      <div className="pointer-events-none absolute inset-0 overflow-hidden" aria-hidden="true">
        <div
          className="absolute -left-16 -top-16 h-72 w-72 rounded-full bg-indigo-400/40 blur-[60px]
            motion-safe:animate-[float-blob-a_18s_ease-in-out_infinite] dark:bg-indigo-500/30"
        />
        <div
          className="absolute -right-20 top-1/3 h-80 w-80 rounded-full bg-cyan-300/40 blur-[60px]
            motion-safe:animate-[float-blob-b_22s_ease-in-out_infinite] dark:bg-cyan-500/20"
        />
        <div
          className="absolute -bottom-24 left-1/4 h-64 w-64 rounded-full bg-indigo-300/40 blur-[60px]
            motion-safe:animate-[float-blob-c_20s_ease-in-out_infinite] dark:bg-indigo-400/20"
        />
        <svg
          viewBox="0 0 400 120"
          preserveAspectRatio="xMidYMax slice"
          className="absolute bottom-0 left-0 h-28 w-full text-indigo-900/10 dark:text-white/10"
        >
          <rect x="20" y="40" width="24" height="80" rx="4" fill="currentColor" />
          <rect x="52" y="20" width="24" height="100" rx="4" fill="currentColor" />
          <rect x="84" y="55" width="24" height="65" rx="4" fill="currentColor" />
          <rect x="300" y="30" width="24" height="90" rx="4" fill="currentColor" />
          <rect x="332" y="50" width="24" height="70" rx="4" fill="currentColor" />
          <rect x="364" y="15" width="24" height="105" rx="4" fill="currentColor" />
        </svg>
      </div>

      <header className="relative z-10 flex items-center justify-between px-4 py-5 sm:px-6">
        <span className="text-lg font-bold text-indigo-700 dark:text-indigo-300">Library</span>
        <button
          onClick={signIn}
          className="text-sm font-medium text-gray-700 transition-colors hover:text-indigo-700 focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-indigo-500 dark:text-gray-200 dark:hover:text-indigo-300"
        >
          Sign in
        </button>
      </header>

      <main className="relative z-10 flex flex-1 flex-col items-center justify-center px-4 py-10">
        <div
          className={`w-[min(420px,calc(100%-32px))] rounded-[20px] border border-white/60 bg-white/55 p-8 text-center shadow-2xl shadow-indigo-500/10 backdrop-blur-[16px] dark:border-white/10 dark:bg-slate-900/55 dark:shadow-black/40 ${fadeClass(revealed)}`}
        >
          <h1 className="text-2xl font-bold tracking-tight text-gray-900 dark:text-white">Library</h1>
          <p className="mt-2 text-sm text-gray-700 dark:text-gray-300">
            Your personal library, organized and searchable.
          </p>
          <div className="mt-6 flex flex-col gap-3">
            <Button onClick={signIn} icon={<LogIn size={16} />} className="w-full">
              Sign in
            </Button>
            <Button
              onClick={createAccount}
              variant="secondary"
              icon={<UserPlus size={16} />}
              className="w-full bg-white/40 dark:bg-white/5"
            >
              Create account
            </Button>
          </div>
        </div>

        <div className={`mt-6 flex flex-wrap items-center justify-center gap-2 ${fadeClass(revealed)}`}>
          {FEATURE_CHIPS.map((chip) => (
            <span
              key={chip.label}
              className="inline-flex items-center gap-1.5 rounded-full border border-white/60 bg-white/40 px-3 py-1.5 text-xs font-medium text-gray-700 backdrop-blur-sm dark:border-white/10 dark:bg-white/5 dark:text-gray-200"
            >
              <chip.icon size={14} className="text-indigo-600 dark:text-indigo-400" />
              {chip.label}
            </span>
          ))}
        </div>
      </main>
    </div>
  );
}
