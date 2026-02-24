import { useState } from 'react';
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import { BookOpen, Plus, Pencil, Trash2, Check, X, Search, Filter } from 'lucide-react';
import {
  listJargon,
  createJargon,
  updateJargon,
  deleteJargon,
} from '../../api/normalize';
import type { JargonEntry } from '../../types/normalize';
import { Badge } from '../../components/common/Badge';
import { ConfirmDialog } from '../../components/common/ConfirmDialog';
import { EmptyState } from '../../components/common/EmptyState';
import { LoadingOverlay } from '../../components/common/LoadingSpinner';

// ---- Source badge ----

function SourceBadge({ source }: { source: JargonEntry['source'] }) {
  const map = {
    human: 'blue',
    llm: 'purple',
    seed: 'gray',
  } as const;
  return <Badge variant={map[source]} size="sm">{source}</Badge>;
}

// ---- Create Form ----

interface CreateFormProps {
  onSuccess: () => void;
  onCancel: () => void;
}

function CreateForm({ onSuccess, onCancel }: CreateFormProps) {
  const queryClient = useQueryClient();
  const [acronym, setAcronym] = useState('');
  const [expansion, setExpansion] = useState('');
  const [industry, setIndustry] = useState('');
  const [contextExample, setContextExample] = useState('');
  const [error, setError] = useState<string | null>(null);

  const mutation = useMutation({
    mutationFn: () =>
      createJargon({
        acronym: acronym.trim().toUpperCase(),
        expansion: expansion.trim(),
        industry: industry.trim() || null,
        contextExample: contextExample.trim() || null,
        source: 'human',
        verified: true,
      }),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['jargon'] });
      onSuccess();
    },
    onError: () => {
      setError('Failed to create jargon entry. Please try again.');
    },
  });

  function handleSubmit(e: React.FormEvent) {
    e.preventDefault();
    setError(null);
    if (!acronym.trim()) {
      setError('Acronym is required.');
      return;
    }
    if (!expansion.trim()) {
      setError('Expansion is required.');
      return;
    }
    mutation.mutate();
  }

  return (
    <form
      onSubmit={handleSubmit}
      className="rounded-lg border border-red-200 bg-red-50 p-5 space-y-4"
    >
      <h3 className="text-sm font-semibold text-red-900">Add Jargon Entry</h3>

      <div className="grid grid-cols-1 gap-4 sm:grid-cols-2">
        <div className="space-y-1">
          <label className="block text-xs font-medium text-gray-700">
            Acronym <span className="text-red-500">*</span>
          </label>
          <input
            type="text"
            value={acronym}
            onChange={(e) => setAcronym(e.target.value)}
            placeholder="e.g. VFD"
            className="w-full rounded-md border border-gray-300 px-3 py-2 text-sm focus:outline-none focus:ring-2 focus:ring-red-500 uppercase"
          />
        </div>
        <div className="space-y-1">
          <label className="block text-xs font-medium text-gray-700">
            Expansion <span className="text-red-500">*</span>
          </label>
          <input
            type="text"
            value={expansion}
            onChange={(e) => setExpansion(e.target.value)}
            placeholder="e.g. Variable Frequency Drive"
            className="w-full rounded-md border border-gray-300 px-3 py-2 text-sm focus:outline-none focus:ring-2 focus:ring-red-500"
          />
        </div>
        <div className="space-y-1">
          <label className="block text-xs font-medium text-gray-700">
            Industry (optional)
          </label>
          <input
            type="text"
            value={industry}
            onChange={(e) => setIndustry(e.target.value)}
            placeholder="e.g. Electrical"
            className="w-full rounded-md border border-gray-300 px-3 py-2 text-sm focus:outline-none focus:ring-2 focus:ring-red-500"
          />
        </div>
        <div className="space-y-1">
          <label className="block text-xs font-medium text-gray-700">
            Context Example (optional)
          </label>
          <input
            type="text"
            value={contextExample}
            onChange={(e) => setContextExample(e.target.value)}
            placeholder="e.g. Selling Siemens VFD 5kW"
            className="w-full rounded-md border border-gray-300 px-3 py-2 text-sm focus:outline-none focus:ring-2 focus:ring-red-500"
          />
        </div>
      </div>

      {error && <p className="text-xs text-red-600">{error}</p>}

      <div className="flex justify-end gap-2">
        <button
          type="button"
          onClick={onCancel}
          disabled={mutation.isPending}
          className="rounded-md border border-gray-300 bg-white px-3 py-1.5 text-sm font-medium text-gray-700 hover:bg-gray-50 disabled:opacity-50"
        >
          Cancel
        </button>
        <button
          type="submit"
          disabled={mutation.isPending}
          className="rounded-md bg-red-600 px-3 py-1.5 text-sm font-medium text-white hover:bg-red-700 disabled:opacity-50"
        >
          {mutation.isPending ? 'Adding...' : 'Add Entry'}
        </button>
      </div>
    </form>
  );
}

// ---- Inline Edit Row ----

interface EditRowProps {
  entry: JargonEntry;
  onDone: () => void;
}

function EditRow({ entry, onDone }: EditRowProps) {
  const queryClient = useQueryClient();
  const [expansion, setExpansion] = useState(entry.expansion);
  const [industry, setIndustry] = useState(entry.industry ?? '');
  const [contextExample, setContextExample] = useState(entry.contextExample ?? '');
  const [verified, setVerified] = useState(entry.verified);
  const [error, setError] = useState<string | null>(null);

  const mutation = useMutation({
    mutationFn: () =>
      updateJargon(entry.id, {
        expansion: expansion.trim(),
        industry: industry.trim() || null,
        contextExample: contextExample.trim() || null,
        verified,
      }),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['jargon'] });
      onDone();
    },
    onError: () => {
      setError('Failed to save changes.');
    },
  });

  function handleSubmit(e: React.FormEvent) {
    e.preventDefault();
    setError(null);
    if (!expansion.trim()) {
      setError('Expansion is required.');
      return;
    }
    mutation.mutate();
  }

  return (
    <tr className="bg-yellow-50 border-b border-yellow-200">
      <td colSpan={7} className="px-4 py-3">
        <form onSubmit={handleSubmit} className="space-y-3">
          <div className="flex flex-wrap items-end gap-3">
            <div>
              <label className="block text-xs font-medium text-gray-700 mb-1">
                Acronym
              </label>
              <input
                type="text"
                value={entry.acronym}
                disabled
                className="rounded-md border border-gray-200 bg-gray-100 px-2.5 py-1.5 text-sm text-gray-500 w-24"
              />
            </div>
            <div>
              <label className="block text-xs font-medium text-gray-700 mb-1">
                Expansion <span className="text-red-500">*</span>
              </label>
              <input
                type="text"
                value={expansion}
                onChange={(e) => setExpansion(e.target.value)}
                className="rounded-md border border-gray-300 px-2.5 py-1.5 text-sm focus:outline-none focus:ring-2 focus:ring-yellow-500 w-52"
              />
            </div>
            <div>
              <label className="block text-xs font-medium text-gray-700 mb-1">
                Industry
              </label>
              <input
                type="text"
                value={industry}
                onChange={(e) => setIndustry(e.target.value)}
                className="rounded-md border border-gray-300 px-2.5 py-1.5 text-sm focus:outline-none focus:ring-2 focus:ring-yellow-500 w-36"
              />
            </div>
            <div>
              <label className="block text-xs font-medium text-gray-700 mb-1">
                Context Example
              </label>
              <input
                type="text"
                value={contextExample}
                onChange={(e) => setContextExample(e.target.value)}
                className="rounded-md border border-gray-300 px-2.5 py-1.5 text-sm focus:outline-none focus:ring-2 focus:ring-yellow-500 w-52"
              />
            </div>
            <div className="flex items-center gap-2 pb-1">
              <input
                id={`verified-${entry.id}`}
                type="checkbox"
                checked={verified}
                onChange={(e) => setVerified(e.target.checked)}
                className="h-4 w-4 rounded border-gray-300 text-blue-600 focus:ring-blue-500"
              />
              <label
                htmlFor={`verified-${entry.id}`}
                className="text-xs font-medium text-gray-700"
              >
                Verified
              </label>
            </div>
            <div className="flex gap-2 pb-1">
              <button
                type="submit"
                disabled={mutation.isPending}
                className="flex items-center gap-1 rounded-md bg-yellow-600 px-3 py-1.5 text-sm font-medium text-white hover:bg-yellow-700 disabled:opacity-50"
              >
                <Check className="h-3.5 w-3.5" />
                {mutation.isPending ? 'Saving...' : 'Save'}
              </button>
              <button
                type="button"
                onClick={onDone}
                disabled={mutation.isPending}
                className="flex items-center gap-1 rounded-md border border-gray-300 bg-white px-3 py-1.5 text-sm font-medium text-gray-700 hover:bg-gray-50 disabled:opacity-50"
              >
                <X className="h-3.5 w-3.5" />
                Cancel
              </button>
            </div>
          </div>
          {error && <p className="text-xs text-red-600">{error}</p>}
        </form>
      </td>
    </tr>
  );
}

// ---- Jargon Row ----

function JargonRow({ entry }: { entry: JargonEntry }) {
  const queryClient = useQueryClient();
  const [isEditing, setIsEditing] = useState(false);
  const [deleteTarget, setDeleteTarget] = useState<string | null>(null);

  const verifyMutation = useMutation({
    mutationFn: () => updateJargon(entry.id, { verified: !entry.verified }),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['jargon'] });
    },
  });

  const deleteMutation = useMutation({
    mutationFn: () => deleteJargon(entry.id),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['jargon'] });
      setDeleteTarget(null);
    },
  });

  if (isEditing) {
    return <EditRow entry={entry} onDone={() => setIsEditing(false)} />;
  }

  return (
    <>
      <tr className="border-b border-gray-100 hover:bg-gray-50">
        <td className="px-4 py-3">
          <span className="font-mono text-sm font-semibold text-gray-900">
            {entry.acronym}
          </span>
        </td>
        <td className="px-4 py-3 text-sm text-gray-700">{entry.expansion}</td>
        <td className="px-4 py-3 text-sm text-gray-500">
          {entry.industry ?? <span className="text-gray-300">—</span>}
        </td>
        <td className="px-4 py-3">
          <SourceBadge source={entry.source} />
        </td>
        <td className="px-4 py-3 text-center">
          <button
            onClick={() => verifyMutation.mutate()}
            disabled={verifyMutation.isPending}
            title={entry.verified ? 'Mark unverified' : 'Verify'}
            className="disabled:opacity-50"
          >
            {entry.verified ? (
              <Check className="h-4 w-4 text-green-500 mx-auto" />
            ) : (
              <X className="h-4 w-4 text-gray-300 mx-auto" />
            )}
          </button>
        </td>
        <td className="px-4 py-3 text-right text-sm tabular-nums text-gray-500">
          {entry.usageCount.toLocaleString()}
        </td>
        <td className="px-4 py-3">
          <div className="flex items-center gap-1">
            <button
              onClick={() => setIsEditing(true)}
              title="Edit"
              className="rounded-md p-1.5 text-gray-400 hover:text-blue-600"
            >
              <Pencil className="h-4 w-4" />
            </button>
            <button
              onClick={() => setDeleteTarget(entry.id)}
              title="Delete"
              className="rounded-md p-1.5 text-gray-400 hover:text-red-600"
            >
              <Trash2 className="h-4 w-4" />
            </button>
          </div>
        </td>
      </tr>

      <ConfirmDialog
        isOpen={deleteTarget === entry.id}
        title="Delete jargon entry"
        message={`Delete "${entry.acronym}" (${entry.expansion})? This cannot be undone.`}
        confirmLabel="Delete"
        variant="danger"
        onConfirm={() => deleteMutation.mutate()}
        onCancel={() => setDeleteTarget(null)}
        isLoading={deleteMutation.isPending}
      />
    </>
  );
}

// ---- JargonPage ----

export function JargonPage() {
  const [showCreateForm, setShowCreateForm] = useState(false);
  const [search, setSearch] = useState('');
  const [verifiedFilter, setVerifiedFilter] = useState<boolean | undefined>(undefined);

  const { data: entries, isLoading, isError } = useQuery({
    queryKey: ['jargon', search, verifiedFilter],
    queryFn: () =>
      listJargon({
        search: search.trim() || undefined,
        verified: verifiedFilter,
      }),
  });

  return (
    <div className="flex flex-col h-full">
      {/* Header */}
      <div className="border-b border-gray-200 bg-white px-6 py-4">
        <div className="flex items-center justify-between gap-4">
          <div className="flex items-center gap-3">
            <div className="flex h-9 w-9 items-center justify-center rounded-lg bg-red-100">
              <BookOpen className="h-5 w-5 text-red-600" />
            </div>
            <div>
              <h1 className="text-lg font-semibold text-gray-900">
                Jargon Dictionary
              </h1>
              <p className="text-xs text-gray-500">
                Industry acronyms and their expansions
              </p>
            </div>
          </div>
          {!showCreateForm && (
            <button
              onClick={() => setShowCreateForm(true)}
              className="flex items-center gap-1.5 rounded-md bg-red-600 px-3 py-2 text-sm font-medium text-white hover:bg-red-700"
            >
              <Plus className="h-4 w-4" />
              Add Entry
            </button>
          )}
        </div>
      </div>

      {/* Filters */}
      <div className="border-b border-gray-100 bg-gray-50 px-6 py-2.5 flex items-center gap-3">
        <div className="relative flex-1 max-w-xs">
          <Search className="absolute left-2.5 top-1/2 -translate-y-1/2 h-3.5 w-3.5 text-gray-400" />
          <input
            type="search"
            value={search}
            onChange={(e) => setSearch(e.target.value)}
            placeholder="Search acronym or expansion..."
            className="w-full rounded-md border border-gray-300 bg-white pl-8 pr-3 py-1.5 text-sm focus:outline-none focus:ring-2 focus:ring-red-500"
          />
        </div>
        <div className="flex items-center gap-1.5">
          <Filter className="h-3.5 w-3.5 text-gray-400" />
          <select
            value={verifiedFilter === undefined ? '' : String(verifiedFilter)}
            onChange={(e) => {
              const v = e.target.value;
              setVerifiedFilter(v === '' ? undefined : v === 'true');
            }}
            className="rounded-md border border-gray-300 bg-white px-2 py-1.5 text-xs focus:outline-none focus:ring-2 focus:ring-red-500"
          >
            <option value="">All</option>
            <option value="true">Verified only</option>
            <option value="false">Unverified only</option>
          </select>
        </div>
        {entries && (
          <p className="ml-auto text-xs text-gray-500">
            {entries.length.toLocaleString()} entries
          </p>
        )}
      </div>

      {/* Content */}
      <div className="flex-1 overflow-auto p-6 space-y-4">
        {showCreateForm && (
          <CreateForm
            onSuccess={() => setShowCreateForm(false)}
            onCancel={() => setShowCreateForm(false)}
          />
        )}

        {isLoading && <LoadingOverlay message="Loading jargon dictionary..." />}

        {isError && (
          <div className="rounded-md border border-red-200 bg-red-50 p-4 text-sm text-red-700">
            Failed to load jargon entries. Please refresh the page.
          </div>
        )}

        {!isLoading && !isError && entries?.length === 0 && !showCreateForm && (
          <EmptyState
            icon={<BookOpen className="h-8 w-8" />}
            title="No jargon entries"
            description={
              search
                ? 'No entries match your search.'
                : 'Add industry acronyms to improve LLM extraction accuracy.'
            }
            action={
              !search ? (
                <button
                  onClick={() => setShowCreateForm(true)}
                  className="flex items-center gap-1.5 rounded-md bg-red-600 px-3 py-2 text-sm font-medium text-white hover:bg-red-700"
                >
                  <Plus className="h-4 w-4" />
                  Add first entry
                </button>
              ) : undefined
            }
          />
        )}

        {entries && entries.length > 0 && (
          <div className="overflow-hidden rounded-lg border border-gray-200 bg-white shadow-sm">
            <table className="w-full text-left text-sm">
              <thead className="border-b border-gray-200 bg-gray-50 text-xs font-semibold text-gray-600 uppercase tracking-wide">
                <tr>
                  <th className="px-4 py-3">Acronym</th>
                  <th className="px-4 py-3">Expansion</th>
                  <th className="px-4 py-3">Industry</th>
                  <th className="px-4 py-3">Source</th>
                  <th className="px-4 py-3 text-center">Verified</th>
                  <th className="px-4 py-3 text-right">Uses</th>
                  <th className="px-4 py-3" />
                </tr>
              </thead>
              <tbody>
                {entries.map((entry) => (
                  <JargonRow key={entry.id} entry={entry} />
                ))}
              </tbody>
            </table>
          </div>
        )}
      </div>
    </div>
  );
}
