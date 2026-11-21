import InsightsView from '../components/InsightsView';
import PageHeader from '../components/PageHeader';

export default function InsightsPage() {
  return (
    <div className="flex flex-col gap-6">
      <PageHeader title="Your insights" description="A quick look at your reading habits." />
      <InsightsView basePath="/api/insights" summarizeLabel="Summarize my reading habits" />
    </div>
  );
}
