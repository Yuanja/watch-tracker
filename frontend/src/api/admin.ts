import { apiClient } from './client';
import type { User, UserRole } from '../types/user';
import type { WhatsappGroup } from '../types/message';
import type { PagedResponse } from '../types/message';

export interface AuditLogEntry {
  id: string;
  actorId: string | null;
  actorEmail?: string | null;
  action: string;
  targetType: string | null;
  targetId: string | null;
  oldValues: Record<string, unknown> | null;
  newValues: Record<string, unknown> | null;
  ipAddress: string | null;
  createdAt: string;
}

export interface AllUserCostRow {
  userId: string;
  email: string;
  displayName: string | null;
  totalInputTokens: number;
  totalOutputTokens: number;
  totalCostUsd: number;
  sessionCount: number;
}

// ---- User Management ----

export async function getAllUsers(): Promise<User[]> {
  const response = await apiClient.get('/admin/users', { params: { size: 1000 } });
  // Backend returns Page<UserDTO>; unwrap .content array
  return response.data.content ?? response.data;
}

export async function updateUserRole(
  userId: string,
  role: UserRole
): Promise<User> {
  const response = await apiClient.put<User>(`/admin/users/${userId}/role`, {
    role,
  });
  return response.data;
}

export async function setUserActive(
  userId: string,
  isActive: boolean
): Promise<User> {
  const response = await apiClient.put<User>(
    `/admin/users/${userId}/active`,
    { active: isActive }
  );
  return response.data;
}

export async function getUserChats(userId: string, page = 0, size = 20) {
  const response = await apiClient.get(`/admin/users/${userId}/chats`, {
    params: { page, size },
  });
  return response.data;
}

// ---- Cost Reports ----

export async function getAllCosts(): Promise<AllUserCostRow[]> {
  const response = await apiClient.get<AllUserCostRow[]>('/admin/costs');
  return response.data;
}

export async function exportCostsCsv(): Promise<Blob> {
  const response = await apiClient.get('/admin/costs/export', {
    responseType: 'blob',
  });
  return response.data;
}

// ---- Audit Log ----

export async function getAuditLog(
  params: { page?: number; size?: number } = {}
): Promise<PagedResponse<AuditLogEntry>> {
  const response = await apiClient.get<PagedResponse<AuditLogEntry>>(
    '/admin/audit',
    { params }
  );
  return response.data;
}

// ---- WhatsApp Group Management ----

export async function getAdminGroups(): Promise<WhatsappGroup[]> {
  const response = await apiClient.get<WhatsappGroup[]>('/admin/groups');
  return response.data;
}

export async function addGroup(
  data: Partial<WhatsappGroup>
): Promise<WhatsappGroup> {
  const response = await apiClient.post<WhatsappGroup>('/admin/groups', data);
  return response.data;
}

export async function updateGroup(
  id: string,
  data: Partial<WhatsappGroup>
): Promise<WhatsappGroup> {
  const response = await apiClient.put<WhatsappGroup>(
    `/admin/groups/${id}`,
    data
  );
  return response.data;
}

export async function deleteGroup(id: string): Promise<void> {
  await apiClient.delete(`/admin/groups/${id}`);
}
