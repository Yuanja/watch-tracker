import { useState, useRef, useEffect } from 'react';
import { X, Send, Loader2, CheckCircle } from 'lucide-react';
import { assistByListing, resolveReviewItem, getReviewQueue } from '../../api/review';
import type { AssistResponse, ExtractionResult } from '../../api/review';

interface Message {
  role: 'user' | 'assistant';
  content: string;
  extraction?: ExtractionResult;
}

interface AssistDialogProps {
  listingId: string;
  onClose: () => void;
  onResolved: () => void;
}

function ExtractionCard({ extraction }: { extraction: ExtractionResult }) {
  const item = extraction.items?.[0];
  return (
    <div className="rounded-lg border border-gray-200 bg-gray-50 p-3 text-xs">
      <div className="mb-1.5 flex items-center justify-between">
        <span className="font-semibold text-gray-700">Extraction Result</span>
        {extraction.confidence != null && (
          <span className={`rounded-full px-2 py-0.5 text-xs font-medium ${
            extraction.confidence >= 0.8
              ? 'bg-green-100 text-green-700'
              : extraction.confidence >= 0.5
                ? 'bg-yellow-100 text-yellow-700'
                : 'bg-red-100 text-red-700'
          }`}>
            {(extraction.confidence * 100).toFixed(0)}% confidence
          </span>
        )}
      </div>
      {extraction.intent && (
        <div className="mb-1"><span className="text-gray-500">Intent:</span> {extraction.intent}</div>
      )}
      {item && (
        <div className="space-y-0.5">
          {item.description && <div><span className="text-gray-500">Description:</span> {item.description}</div>}
          {item.category && <div><span className="text-gray-500">Category:</span> {item.category}</div>}
          {item.manufacturer && <div><span className="text-gray-500">Manufacturer:</span> {item.manufacturer}</div>}
          {item.part_number && <div><span className="text-gray-500">Part #:</span> {item.part_number}</div>}
          {item.quantity != null && <div><span className="text-gray-500">Qty:</span> {item.quantity} {item.unit ?? ''}</div>}
          {item.price != null && <div><span className="text-gray-500">Price:</span> {item.currency ?? ''} {item.price}</div>}
          {item.condition && <div><span className="text-gray-500">Condition:</span> {item.condition}</div>}
        </div>
      )}
    </div>
  );
}

export function AssistDialog({ listingId, onClose, onResolved }: AssistDialogProps) {
  const [messages, setMessages] = useState<Message[]>([]);
  const [input, setInput] = useState('');
  const [loading, setLoading] = useState(false);
  const [resolving, setResolving] = useState(false);
  const [originalText, setOriginalText] = useState<string | null>(null);
  const [latestExtraction, setLatestExtraction] = useState<ExtractionResult | null>(null);
  const [error, setError] = useState<string | null>(null);
  const bottomRef = useRef<HTMLDivElement>(null);
  const inputRef = useRef<HTMLInputElement>(null);

  useEffect(() => {
    bottomRef.current?.scrollIntoView({ behavior: 'smooth' });
  }, [messages]);

  useEffect(() => {
    inputRef.current?.focus();
  }, [loading]);

  const handleSend = async () => {
    const hint = input.trim();
    if (!hint || loading) return;

    setInput('');
    setError(null);
    setMessages((prev) => [...prev, { role: 'user', content: hint }]);
    setLoading(true);

    try {
      const res: AssistResponse = await assistByListing(listingId, hint);
      if (!originalText && res.originalText) {
        setOriginalText(res.originalText);
      }
      setLatestExtraction(res.extraction);
      setMessages((prev) => [
        ...prev,
        {
          role: 'assistant',
          content: 'Here is the refined extraction:',
          extraction: res.extraction,
        },
      ]);
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Request failed');
    } finally {
      setLoading(false);
    }
  };

  const handleResolve = async () => {
    if (!latestExtraction || resolving) return;
    setResolving(true);
    setError(null);

    const item = latestExtraction.items?.[0];
    try {
      // Find the review queue item for this listing, then resolve with extraction values
      const queue = await getReviewQueue({ page: 0, size: 100 });
      const reviewItem = queue.content.find((r) => r.listingId === listingId);
      if (!reviewItem) {
        setError('Could not find review item for this listing');
        setResolving(false);
        return;
      }
      await resolveReviewItem(reviewItem.id, {
        itemDescription: item?.description,
        categoryName: item?.category,
        manufacturerName: item?.manufacturer,
        partNumber: item?.part_number,
        quantity: item?.quantity,
        unit: item?.unit,
        price: item?.price,
        condition: item?.condition,
        intent: latestExtraction.intent,
      });
      onResolved();
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Failed to resolve');
    } finally {
      setResolving(false);
    }
  };

  return (
    <div className="fixed inset-0 z-50 flex items-center justify-center bg-black/50">
      <div className="flex h-[600px] w-full max-w-lg flex-col rounded-xl bg-white shadow-xl">
        {/* Header */}
        <div className="flex items-center justify-between border-b border-gray-200 px-5 py-3">
          <h3 className="text-sm font-semibold text-gray-900">Agent-Assisted Review</h3>
          <button
            type="button"
            onClick={onClose}
            className="rounded p-1 text-gray-400 hover:bg-gray-100 hover:text-gray-600"
            aria-label="Close"
          >
            <X className="h-4 w-4" />
          </button>
        </div>

        {/* Original text */}
        {originalText && (
          <div className="border-b border-gray-100 bg-gray-50 px-5 py-2">
            <p className="text-xs font-medium text-gray-500">Original Message</p>
            <p className="mt-0.5 line-clamp-3 text-xs text-gray-700">{originalText}</p>
          </div>
        )}

        {/* Messages */}
        <div className="flex-1 space-y-3 overflow-y-auto px-5 py-4">
          {messages.length === 0 && (
            <p className="text-center text-xs text-gray-400">
              Type a hint to guide the LLM re-extraction (e.g. &ldquo;The manufacturer is Siemens, not ABB&rdquo;)
            </p>
          )}
          {messages.map((msg, i) => (
            <div key={i} className={`flex ${msg.role === 'user' ? 'justify-end' : 'justify-start'}`}>
              <div
                className={`max-w-[85%] rounded-lg px-3 py-2 text-sm ${
                  msg.role === 'user'
                    ? 'bg-blue-600 text-white'
                    : 'bg-gray-100 text-gray-800'
                }`}
              >
                <p>{msg.content}</p>
                {msg.extraction && (
                  <div className="mt-2">
                    <ExtractionCard extraction={msg.extraction} />
                  </div>
                )}
              </div>
            </div>
          ))}
          {loading && (
            <div className="flex justify-start">
              <div className="flex items-center gap-2 rounded-lg bg-gray-100 px-3 py-2 text-sm text-gray-500">
                <Loader2 className="h-3 w-3 animate-spin" /> Analyzing...
              </div>
            </div>
          )}
          <div ref={bottomRef} />
        </div>

        {/* Error */}
        {error && (
          <div className="border-t border-red-100 bg-red-50 px-5 py-2 text-xs text-red-600">
            {error}
          </div>
        )}

        {/* Accept & Resolve button */}
        {latestExtraction && !resolving && (
          <div className="border-t border-gray-100 px-5 py-2">
            <button
              type="button"
              onClick={handleResolve}
              className="flex w-full items-center justify-center gap-2 rounded-lg bg-green-600 px-4 py-2 text-sm font-medium text-white hover:bg-green-700"
            >
              <CheckCircle className="h-4 w-4" />
              Accept &amp; Resolve
            </button>
          </div>
        )}
        {resolving && (
          <div className="border-t border-gray-100 px-5 py-2">
            <div className="flex w-full items-center justify-center gap-2 rounded-lg bg-green-100 px-4 py-2 text-sm text-green-700">
              <Loader2 className="h-4 w-4 animate-spin" /> Resolving...
            </div>
          </div>
        )}

        {/* Input */}
        <div className="border-t border-gray-200 px-5 py-3">
          <form
            onSubmit={(e) => {
              e.preventDefault();
              handleSend();
            }}
            className="flex items-center gap-2"
          >
            <input
              ref={inputRef}
              type="text"
              value={input}
              onChange={(e) => setInput(e.target.value)}
              placeholder="Type a hint..."
              disabled={loading}
              className="flex-1 rounded-lg border border-gray-300 px-3 py-2 text-sm focus:border-blue-500 focus:outline-none focus:ring-1 focus:ring-blue-500 disabled:bg-gray-50"
            />
            <button
              type="submit"
              disabled={!input.trim() || loading}
              className="rounded-lg bg-blue-600 p-2 text-white hover:bg-blue-700 disabled:opacity-50"
              aria-label="Send hint"
            >
              <Send className="h-4 w-4" />
            </button>
          </form>
        </div>
      </div>
    </div>
  );
}
