import InsightsView from '../components/InsightsView';

export default function InsightsPage() {
  return (
    <section className="card">
      <h2>Your insights</h2>
      <InsightsView basePath="/api/insights" summarizeLabel="Summarize my reading habits" />
    </section>
  );
}
