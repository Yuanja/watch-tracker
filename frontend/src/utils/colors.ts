/**
 * A palette of visually distinct, accessible colors for sender name display.
 * These are chosen to be readable on a white background.
 */
const SENDER_COLOR_PALETTE: string[] = [
  '#1d4ed8', // blue-700
  '#7c3aed', // violet-600
  '#be185d', // pink-700
  '#b45309', // amber-700
  '#047857', // emerald-700
  '#0369a1', // sky-700
  '#c2410c', // orange-700
  '#4338ca', // indigo-700
  '#0f766e', // teal-700
  '#9333ea', // purple-600
  '#b91c1c', // red-700
  '#15803d', // green-700
  '#d97706', // amber-600
  '#0284c7', // sky-600
  '#6d28d9', // violet-700
];

/**
 * Compute a simple numeric hash for a string.
 * Uses the djb2 algorithm for a consistent, well-distributed hash.
 */
function hashString(str: string): number {
  let hash = 5381;
  for (let i = 0; i < str.length; i++) {
    hash = (hash * 33) ^ str.charCodeAt(i);
    hash = hash >>> 0; // Convert to unsigned 32-bit integer
  }
  return hash;
}

/**
 * Get a consistent color for a sender name.
 * The same name always maps to the same color within a session,
 * using a hash of the name to pick from the palette.
 *
 * @param senderName  The sender's display name
 * @returns           A CSS color string (hex)
 */
export function getSenderColor(senderName: string | null | undefined): string {
  if (!senderName) return SENDER_COLOR_PALETTE[0];
  const index = hashString(senderName) % SENDER_COLOR_PALETTE.length;
  return SENDER_COLOR_PALETTE[index];
}

/**
 * Get an avatar background color for a sender.
 * Used when no avatar image is available.
 */
export function getSenderAvatarBg(senderName: string | null | undefined): string {
  const avatarColors: string[] = [
    '#dbeafe', // blue-100
    '#ede9fe', // violet-100
    '#fce7f3', // pink-100
    '#fef3c7', // amber-100
    '#d1fae5', // emerald-100
    '#e0f2fe', // sky-100
    '#ffedd5', // orange-100
    '#e0e7ff', // indigo-100
    '#ccfbf1', // teal-100
    '#f3e8ff', // purple-100
  ];

  if (!senderName) return avatarColors[0];
  const index = hashString(senderName) % avatarColors.length;
  return avatarColors[index];
}

/**
 * Get the sender's initials for avatar display (up to 2 characters).
 */
export function getSenderInitials(
  senderName: string | null | undefined
): string {
  if (!senderName) return '?';
  const parts = senderName.trim().split(/\s+/);
  if (parts.length === 1) return parts[0].slice(0, 2).toUpperCase();
  return (parts[0][0] + parts[parts.length - 1][0]).toUpperCase();
}
