import { useState } from 'react';
import { FileText, Video, Music, Image, Download } from 'lucide-react';
import type { ReplayMessage } from '../../types/message';

interface MediaPreviewProps {
  message: ReplayMessage;
}

export function MediaPreview({ message }: MediaPreviewProps) {
  const [imageError, setImageError] = useState(false);

  const { messageType, mediaUrl, mediaMimeType } = message;

  if (!mediaUrl) return null;

  if (messageType === 'image' && !imageError) {
    return (
      <div className="mb-2 overflow-hidden rounded-lg">
        <img
          src={mediaUrl}
          alt="Message image"
          className="max-h-48 w-auto max-w-full rounded-lg object-cover cursor-pointer"
          onError={() => setImageError(true)}
          loading="lazy"
        />
      </div>
    );
  }

  if (messageType === 'video') {
    return (
      <div className="mb-2 flex items-center gap-2 rounded-lg bg-gray-100 px-3 py-2">
        <Video className="h-5 w-5 shrink-0 text-gray-500" />
        <span className="truncate text-sm text-gray-700">Video</span>
        <a
          href={mediaUrl}
          target="_blank"
          rel="noopener noreferrer"
          className="ml-auto shrink-0 text-blue-600 hover:text-blue-800"
          aria-label="Open video"
        >
          <Download className="h-4 w-4" />
        </a>
      </div>
    );
  }

  if (messageType === 'audio') {
    return (
      <div className="mb-2 flex items-center gap-2 rounded-lg bg-gray-100 px-3 py-2">
        <Music className="h-5 w-5 shrink-0 text-gray-500" />
        <audio
          controls
          src={mediaUrl}
          className="h-8 w-full"
          preload="none"
        >
          Your browser does not support audio.
        </audio>
      </div>
    );
  }

  if (messageType === 'document') {
    const fileName = mediaUrl.split('/').pop() ?? 'Document';
    const mimeLabel = mediaMimeType
      ? mediaMimeType.split('/').pop()?.toUpperCase()
      : 'FILE';

    return (
      <a
        href={mediaUrl}
        target="_blank"
        rel="noopener noreferrer"
        className="mb-2 flex items-center gap-2 rounded-lg bg-gray-100 px-3 py-2 transition-colors hover:bg-gray-200"
      >
        <FileText className="h-5 w-5 shrink-0 text-gray-500" />
        <div className="min-w-0 flex-1">
          <p className="truncate text-sm font-medium text-gray-700">
            {fileName}
          </p>
          {mimeLabel && (
            <p className="text-xs text-gray-400">{mimeLabel}</p>
          )}
        </div>
        <Download className="h-4 w-4 shrink-0 text-gray-400" />
      </a>
    );
  }

  // Fallback for unknown media types
  return (
    <div className="mb-2 flex items-center gap-2 rounded-lg bg-gray-100 px-3 py-2">
      <Image className="h-5 w-5 shrink-0 text-gray-500" />
      <span className="text-sm text-gray-600">Media</span>
      <a
        href={mediaUrl}
        target="_blank"
        rel="noopener noreferrer"
        aria-label="Download media"
        className="ml-auto text-blue-600 hover:text-blue-800"
      >
        <Download className="h-4 w-4" />
      </a>
    </div>
  );
}
