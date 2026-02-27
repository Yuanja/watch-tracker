import { useState } from 'react';
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import { Tag, Factory, Ruler, ShieldCheck, Plus, Pencil, Trash2, Check, X } from 'lucide-react';
import {
  listCategories,
  createCategory,
  updateCategory,
  deleteCategory,
  listManufacturers,
  createManufacturer,
  updateManufacturer,
  deleteManufacturer,
  listUnits,
  createUnit,
  updateUnit,
  deleteUnit,
  listConditions,
  createCondition,
  updateCondition,
  deleteCondition,
} from '../../api/normalize';
import type {
  Category,
  Manufacturer,
  Unit,
  Condition,
} from '../../types/normalize';
import { Badge } from '../../components/common/Badge';
import { ConfirmDialog } from '../../components/common/ConfirmDialog';
import { EmptyState } from '../../components/common/EmptyState';
import { LoadingOverlay } from '../../components/common/LoadingSpinner';

// ---- Type config ----

export type NormalizeType = 'categories' | 'manufacturers' | 'units' | 'conditions';

type NormalizeItem = Category | Manufacturer | Unit | Condition;

interface TypeConfig {
  label: string;
  icon: React.ReactNode;
  iconBg: string;
  iconColor: string;
  queryKey: string;
  listFn: () => Promise<NormalizeItem[]>;
  createFn: (data: Partial<NormalizeItem>) => Promise<NormalizeItem>;
  updateFn: (id: string, data: Partial<NormalizeItem>) => Promise<NormalizeItem>;
  deleteFn: (id: string) => Promise<void>;
  /** Fields shown in the create/edit form */
  fields: FormField[];
  /** Fields shown as columns in the table */
  columns: TableColumn[];
}

interface FormField {
  key: string;
  label: string;
  type: 'text' | 'number';
  required?: boolean;
  placeholder?: string;
}

interface TableColumn {
  key: string;
  label: string;
  render?: (item: NormalizeItem) => React.ReactNode;
}

const TYPE_CONFIG: Record<NormalizeType, TypeConfig> = {
  categories: {
    label: 'Categories',
    icon: <Tag className="h-5 w-5" />,
    iconBg: 'bg-teal-100',
    iconColor: 'text-teal-600',
    queryKey: 'normalizeCategories',
    listFn: listCategories as () => Promise<NormalizeItem[]>,
    createFn: createCategory as (d: Partial<NormalizeItem>) => Promise<NormalizeItem>,
    updateFn: updateCategory as (id: string, d: Partial<NormalizeItem>) => Promise<NormalizeItem>,
    deleteFn: deleteCategory,
    fields: [
      { key: 'name', label: 'Name', type: 'text', required: true, placeholder: 'e.g. Electrical' },
      { key: 'sortOrder', label: 'Sort Order', type: 'number', placeholder: '0' },
    ],
    columns: [
      { key: 'name', label: 'Name' },
      {
        key: 'parentName',
        label: 'Parent',
        render: (item) => (item as Category).parentName ?? <span className="text-gray-400">—</span>,
      },
      {
        key: 'sortOrder',
        label: 'Sort Order',
        render: (item) => (item as Category).sortOrder,
      },
      {
        key: 'listingCount',
        label: 'Listings',
        render: (item) => (item as Category).listingCount?.toLocaleString() ?? '—',
      },
    ],
  },
  manufacturers: {
    label: 'Manufacturers',
    icon: <Factory className="h-5 w-5" />,
    iconBg: 'bg-orange-100',
    iconColor: 'text-orange-600',
    queryKey: 'normalizeManufacturers',
    listFn: listManufacturers as () => Promise<NormalizeItem[]>,
    createFn: createManufacturer as (d: Partial<NormalizeItem>) => Promise<NormalizeItem>,
    updateFn: updateManufacturer as (id: string, d: Partial<NormalizeItem>) => Promise<NormalizeItem>,
    deleteFn: deleteManufacturer,
    fields: [
      { key: 'name', label: 'Name', type: 'text', required: true, placeholder: 'e.g. Siemens' },
      { key: 'website', label: 'Website', type: 'text', placeholder: 'https://...' },
    ],
    columns: [
      { key: 'name', label: 'Name' },
      {
        key: 'aliases',
        label: 'Aliases',
        render: (item) => {
          const aliases = (item as Manufacturer).aliases;
          if (!aliases?.length) return <span className="text-gray-400">—</span>;
          return (
            <div className="flex flex-wrap gap-1">
              {aliases.map((a) => (
                <Badge key={a} variant="gray" size="sm">
                  {a}
                </Badge>
              ))}
            </div>
          );
        },
      },
      {
        key: 'website',
        label: 'Website',
        render: (item) => {
          const site = (item as Manufacturer).website;
          if (!site) return <span className="text-gray-400">—</span>;
          return (
            <a
              href={site}
              target="_blank"
              rel="noopener noreferrer"
              className="text-blue-600 hover:underline text-xs"
            >
              {site}
            </a>
          );
        },
      },
      {
        key: 'listingCount',
        label: 'Listings',
        render: (item) => (item as Manufacturer).listingCount?.toLocaleString() ?? '—',
      },
    ],
  },
  units: {
    label: 'Units',
    icon: <Ruler className="h-5 w-5" />,
    iconBg: 'bg-yellow-100',
    iconColor: 'text-yellow-600',
    queryKey: 'normalizeUnits',
    listFn: listUnits as () => Promise<NormalizeItem[]>,
    createFn: createUnit as (d: Partial<NormalizeItem>) => Promise<NormalizeItem>,
    updateFn: updateUnit as (id: string, d: Partial<NormalizeItem>) => Promise<NormalizeItem>,
    deleteFn: deleteUnit,
    fields: [
      { key: 'name', label: 'Name', type: 'text', required: true, placeholder: 'e.g. Kilogram' },
      { key: 'abbreviation', label: 'Abbreviation', type: 'text', required: true, placeholder: 'e.g. kg' },
    ],
    columns: [
      { key: 'name', label: 'Name' },
      { key: 'abbreviation', label: 'Abbreviation' },
    ],
  },
  conditions: {
    label: 'Conditions',
    icon: <ShieldCheck className="h-5 w-5" />,
    iconBg: 'bg-indigo-100',
    iconColor: 'text-indigo-600',
    queryKey: 'normalizeConditions',
    listFn: listConditions as () => Promise<NormalizeItem[]>,
    createFn: createCondition as (d: Partial<NormalizeItem>) => Promise<NormalizeItem>,
    updateFn: updateCondition as (id: string, d: Partial<NormalizeItem>) => Promise<NormalizeItem>,
    deleteFn: deleteCondition,
    fields: [
      { key: 'name', label: 'Name', type: 'text', required: true, placeholder: 'e.g. New' },
      { key: 'abbreviation', label: 'Abbreviation', type: 'text', placeholder: 'e.g. N' },
      { key: 'sortOrder', label: 'Sort Order', type: 'number', placeholder: '0' },
    ],
    columns: [
      { key: 'name', label: 'Name' },
      {
        key: 'abbreviation',
        label: 'Abbreviation',
        render: (item) =>
          (item as Condition).abbreviation ?? <span className="text-gray-400">—</span>,
      },
      {
        key: 'sortOrder',
        label: 'Sort Order',
        render: (item) => (item as Condition).sortOrder,
      },
    ],
  },
};

// ---- Inline Create Form ----

interface InlineCreateFormProps {
  config: TypeConfig;
  onSuccess: () => void;
  onCancel: () => void;
}

function InlineCreateForm({ config, onSuccess, onCancel }: InlineCreateFormProps) {
  const queryClient = useQueryClient();
  const [values, setValues] = useState<Record<string, string>>({});
  const [error, setError] = useState<string | null>(null);

  const mutation = useMutation({
    mutationFn: () => {
      const payload: Record<string, unknown> = {};
      config.fields.forEach((f) => {
        const v = values[f.key];
        if (v !== undefined && v !== '') {
          payload[f.key] = f.type === 'number' ? Number(v) : v;
        }
      });
      return config.createFn(payload as Partial<NormalizeItem>);
    },
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: [config.queryKey] });
      onSuccess();
    },
    onError: () => {
      setError('Failed to create item. Please try again.');
    },
  });

  function handleSubmit(e: React.FormEvent) {
    e.preventDefault();
    setError(null);
    for (const field of config.fields) {
      if (field.required && !values[field.key]?.trim()) {
        setError(`${field.label} is required.`);
        return;
      }
    }
    mutation.mutate();
  }

  return (
    <form
      onSubmit={handleSubmit}
      className="flex flex-wrap items-end gap-3 rounded-lg border border-dashed border-gray-300 bg-gray-50 p-4"
    >
      {config.fields.map((field) => (
        <div key={field.key} className="flex flex-col gap-1 min-w-32">
          <label className="text-xs font-medium text-gray-700">
            {field.label}
            {field.required && <span className="text-red-500 ml-0.5">*</span>}
          </label>
          <input
            type={field.type}
            value={values[field.key] ?? ''}
            onChange={(e) =>
              setValues((prev) => ({ ...prev, [field.key]: e.target.value }))
            }
            placeholder={field.placeholder}
            className="rounded-md border border-gray-300 px-2.5 py-1.5 text-sm focus:outline-none focus:ring-2 focus:ring-blue-500 w-40"
          />
        </div>
      ))}

      {error && (
        <p className="w-full text-xs text-red-600">{error}</p>
      )}

      <div className="flex gap-2">
        <button
          type="submit"
          disabled={mutation.isPending}
          className="flex items-center gap-1 rounded-md bg-blue-600 px-3 py-1.5 text-sm font-medium text-white hover:bg-blue-700 disabled:opacity-50"
        >
          <Check className="h-3.5 w-3.5" />
          {mutation.isPending ? 'Saving...' : 'Add'}
        </button>
        <button
          type="button"
          onClick={onCancel}
          disabled={mutation.isPending}
          className="flex items-center gap-1 rounded-md border border-gray-300 bg-white px-3 py-1.5 text-sm font-medium text-gray-700 hover:bg-gray-50 disabled:opacity-50"
        >
          <X className="h-3.5 w-3.5" />
          Cancel
        </button>
      </div>
    </form>
  );
}

// ---- Inline Edit Row ----

interface InlineEditRowProps {
  config: TypeConfig;
  item: NormalizeItem;
  onDone: () => void;
}

function InlineEditRow({ config, item, onDone }: InlineEditRowProps) {
  const queryClient = useQueryClient();
  const [values, setValues] = useState<Record<string, string>>(() => {
    const initial: Record<string, string> = {};
    const itemRecord = item as unknown as Record<string, unknown>;
    config.fields.forEach((f) => {
      const val = itemRecord[f.key];
      initial[f.key] = val != null ? String(val) : '';
    });
    return initial;
  });
  const [error, setError] = useState<string | null>(null);

  const mutation = useMutation({
    mutationFn: () => {
      const payload: Record<string, unknown> = {};
      config.fields.forEach((f) => {
        const v = values[f.key];
        if (v !== undefined && v !== '') {
          payload[f.key] = f.type === 'number' ? Number(v) : v;
        }
      });
      return config.updateFn(item.id, payload as Partial<NormalizeItem>);
    },
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: [config.queryKey] });
      onDone();
    },
    onError: () => {
      setError('Failed to save changes.');
    },
  });

  function handleSubmit(e: React.FormEvent) {
    e.preventDefault();
    setError(null);
    for (const field of config.fields) {
      if (field.required && !values[field.key]?.trim()) {
        setError(`${field.label} is required.`);
        return;
      }
    }
    mutation.mutate();
  }

  // Total columns = data columns + isActive + actions
  const colSpan = config.columns.length + 2;

  return (
    <tr className="bg-yellow-50 border-b border-yellow-200">
      <td colSpan={colSpan} className="px-4 py-3">
        <form onSubmit={handleSubmit} className="flex flex-wrap items-end gap-3">
          {config.fields.map((field) => (
            <div key={field.key} className="flex flex-col gap-1">
              <label className="text-xs font-medium text-gray-700">
                {field.label}
                {field.required && <span className="text-red-500 ml-0.5">*</span>}
              </label>
              <input
                type={field.type}
                value={values[field.key] ?? ''}
                onChange={(e) =>
                  setValues((prev) => ({ ...prev, [field.key]: e.target.value }))
                }
                className="rounded-md border border-gray-300 px-2.5 py-1.5 text-sm focus:outline-none focus:ring-2 focus:ring-yellow-500 w-40"
              />
            </div>
          ))}

          {error && <p className="w-full text-xs text-red-600">{error}</p>}

          <div className="flex gap-2">
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
        </form>
      </td>
    </tr>
  );
}

// ---- Data Row ----

interface DataRowProps {
  config: TypeConfig;
  item: NormalizeItem;
}

function DataRow({ config, item }: DataRowProps) {
  const queryClient = useQueryClient();
  const [isEditing, setIsEditing] = useState(false);
  const [deleteTarget, setDeleteTarget] = useState<string | null>(null);

  const deleteMutation = useMutation({
    mutationFn: () => config.deleteFn(item.id),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: [config.queryKey] });
      setDeleteTarget(null);
    },
  });

  const toggleMutation = useMutation({
    mutationFn: () =>
      config.updateFn(item.id, {
        isActive: !(item as { isActive: boolean }).isActive,
      } as Partial<NormalizeItem>),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: [config.queryKey] });
    },
  });

  const isActive = (item as { isActive: boolean }).isActive;

  if (isEditing) {
    return (
      <InlineEditRow
        config={config}
        item={item}
        onDone={() => setIsEditing(false)}
      />
    );
  }

  return (
    <>
      <tr className="border-b border-gray-100 hover:bg-gray-50">
        {config.columns.map((col) => (
          <td key={col.key} className="px-4 py-3 text-sm text-gray-700">
            {col.render ? col.render(item) : String((item as unknown as Record<string, unknown>)[col.key] ?? '—')}
          </td>
        ))}

        {/* Active toggle */}
        <td className="px-4 py-3 text-center">
          <button
            type="button"
            role="switch"
            aria-checked={isActive}
            aria-label={isActive ? 'Deactivate item' : 'Activate item'}
            onClick={() => toggleMutation.mutate()}
            disabled={toggleMutation.isPending}
            title={isActive ? 'Deactivate' : 'Activate'}
            className={`relative inline-flex h-5 w-9 cursor-pointer rounded-full transition-colors focus:outline-none focus:ring-2 focus:ring-blue-500 disabled:opacity-50 ${
              isActive ? 'bg-green-500' : 'bg-gray-300'
            }`}
          >
            <span
              className={`inline-block h-4 w-4 transform rounded-full bg-white shadow transition-transform mt-0.5 ${
                isActive ? 'translate-x-4.5' : 'translate-x-0.5'
              }`}
            />
          </button>
        </td>

        {/* Actions */}
        <td className="px-4 py-3">
          <div className="flex items-center gap-1">
            <button
              type="button"
              onClick={() => setIsEditing(true)}
              title="Edit"
              aria-label="Edit item"
              className="rounded-md p-1.5 text-gray-400 hover:text-blue-600"
            >
              <Pencil className="h-4 w-4" />
            </button>
            <button
              type="button"
              onClick={() => setDeleteTarget(item.id)}
              title="Delete"
              aria-label="Delete item"
              className="rounded-md p-1.5 text-gray-400 hover:text-red-600"
            >
              <Trash2 className="h-4 w-4" />
            </button>
          </div>
        </td>
      </tr>

      <ConfirmDialog
        isOpen={deleteTarget === item.id}
        title={`Delete ${config.label.replace(/s$/, '').toLowerCase()}`}
        message="This action cannot be undone. Any listings using this value will be affected."
        confirmLabel="Delete"
        variant="danger"
        onConfirm={() => deleteMutation.mutate()}
        onCancel={() => setDeleteTarget(null)}
        isLoading={deleteMutation.isPending}
      />
    </>
  );
}

// ---- NormalizedValuePage ----

interface NormalizedValuePageProps {
  type: NormalizeType;
}

export function NormalizedValuePage({ type }: NormalizedValuePageProps) {
  const [showCreateForm, setShowCreateForm] = useState(false);
  const config = TYPE_CONFIG[type];

  const { data: items, isLoading, isError } = useQuery({
    queryKey: [config.queryKey],
    queryFn: config.listFn,
  });

  return (
    <div className="flex flex-col h-full">
      {/* Header */}
      <div className="border-b border-gray-200 bg-white px-6 py-4">
        <div className="flex items-center justify-between">
          <div className="flex items-center gap-3">
            <div
              className={`flex h-9 w-9 items-center justify-center rounded-lg ${config.iconBg} ${config.iconColor}`}
            >
              {config.icon}
            </div>
            <div>
              <h1 className="text-lg font-semibold text-gray-900">
                {config.label}
              </h1>
              <p className="text-xs text-gray-500">
                Manage normalized {config.label.toLowerCase()} for listing extraction
              </p>
            </div>
          </div>
          {!showCreateForm && (
            <button
              onClick={() => setShowCreateForm(true)}
              className="flex items-center gap-1.5 rounded-md bg-blue-600 px-3 py-2 text-sm font-medium text-white hover:bg-blue-700"
            >
              <Plus className="h-4 w-4" />
              Add {config.label.replace(/s$/, '')}
            </button>
          )}
        </div>
      </div>

      {/* Content */}
      <div className="flex-1 overflow-auto p-6 space-y-4">
        {showCreateForm && (
          <InlineCreateForm
            config={config}
            onSuccess={() => setShowCreateForm(false)}
            onCancel={() => setShowCreateForm(false)}
          />
        )}

        {isLoading && <LoadingOverlay message={`Loading ${config.label.toLowerCase()}...`} />}

        {isError && (
          <div className="rounded-md border border-red-200 bg-red-50 p-4 text-sm text-red-700">
            Failed to load {config.label.toLowerCase()}. Please refresh the page.
          </div>
        )}

        {!isLoading && !isError && items?.length === 0 && !showCreateForm && (
          <EmptyState
            icon={<span className={config.iconColor}>{config.icon}</span>}
            title={`No ${config.label.toLowerCase()} yet`}
            description={`Add ${config.label.toLowerCase()} to improve listing extraction accuracy.`}
            action={
              <button
                onClick={() => setShowCreateForm(true)}
                className="flex items-center gap-1.5 rounded-md bg-blue-600 px-3 py-2 text-sm font-medium text-white hover:bg-blue-700"
              >
                <Plus className="h-4 w-4" />
                Add first {config.label.replace(/s$/, '').toLowerCase()}
              </button>
            }
          />
        )}

        {items && items.length > 0 && (
          <div className="overflow-hidden rounded-lg border border-gray-200 bg-white shadow-sm">
            <table className="w-full text-left text-sm">
              <thead className="border-b border-gray-200 bg-gray-50 text-xs font-semibold text-gray-600 uppercase tracking-wide">
                <tr>
                  {config.columns.map((col) => (
                    <th key={col.key} className="px-4 py-3">
                      {col.label}
                    </th>
                  ))}
                  <th className="px-4 py-3 text-center">Active</th>
                  <th className="px-4 py-3" />
                </tr>
              </thead>
              <tbody>
                {items.map((item) => (
                  <DataRow key={item.id} config={config} item={item} />
                ))}
              </tbody>
            </table>
          </div>
        )}
      </div>
    </div>
  );
}
