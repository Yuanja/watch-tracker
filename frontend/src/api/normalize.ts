import { apiClient } from './client';
import type {
  Category,
  Manufacturer,
  Unit,
  Condition,
  JargonEntry,
} from '../types/normalize';

// ---- Categories ----

export async function listCategories(): Promise<Category[]> {
  const response = await apiClient.get<Category[]>('/normalize/categories');
  return response.data;
}

export async function createCategory(
  data: Partial<Category>
): Promise<Category> {
  const response = await apiClient.post<Category>('/normalize/categories', data);
  return response.data;
}

export async function updateCategory(
  id: string,
  data: Partial<Category>
): Promise<Category> {
  const response = await apiClient.put<Category>(
    `/normalize/categories/${id}`,
    data
  );
  return response.data;
}

export async function deleteCategory(id: string): Promise<void> {
  await apiClient.delete(`/normalize/categories/${id}`);
}

// ---- Manufacturers ----

export async function listManufacturers(): Promise<Manufacturer[]> {
  const response = await apiClient.get<Manufacturer[]>(
    '/normalize/manufacturers'
  );
  return response.data;
}

export async function createManufacturer(
  data: Partial<Manufacturer>
): Promise<Manufacturer> {
  const response = await apiClient.post<Manufacturer>(
    '/normalize/manufacturers',
    data
  );
  return response.data;
}

export async function updateManufacturer(
  id: string,
  data: Partial<Manufacturer>
): Promise<Manufacturer> {
  const response = await apiClient.put<Manufacturer>(
    `/normalize/manufacturers/${id}`,
    data
  );
  return response.data;
}

export async function deleteManufacturer(id: string): Promise<void> {
  await apiClient.delete(`/normalize/manufacturers/${id}`);
}

// ---- Units ----

export async function listUnits(): Promise<Unit[]> {
  const response = await apiClient.get<Unit[]>('/normalize/units');
  return response.data;
}

export async function createUnit(data: Partial<Unit>): Promise<Unit> {
  const response = await apiClient.post<Unit>('/normalize/units', data);
  return response.data;
}

export async function updateUnit(
  id: string,
  data: Partial<Unit>
): Promise<Unit> {
  const response = await apiClient.put<Unit>(`/normalize/units/${id}`, data);
  return response.data;
}

export async function deleteUnit(id: string): Promise<void> {
  await apiClient.delete(`/normalize/units/${id}`);
}

// ---- Conditions ----

export async function listConditions(): Promise<Condition[]> {
  const response = await apiClient.get<Condition[]>('/normalize/conditions');
  return response.data;
}

export async function createCondition(
  data: Partial<Condition>
): Promise<Condition> {
  const response = await apiClient.post<Condition>(
    '/normalize/conditions',
    data
  );
  return response.data;
}

export async function updateCondition(
  id: string,
  data: Partial<Condition>
): Promise<Condition> {
  const response = await apiClient.put<Condition>(
    `/normalize/conditions/${id}`,
    data
  );
  return response.data;
}

export async function deleteCondition(id: string): Promise<void> {
  await apiClient.delete(`/normalize/conditions/${id}`);
}

// ---- Jargon ----

export async function listJargon(params?: {
  search?: string;
  verified?: boolean;
}): Promise<JargonEntry[]> {
  const response = await apiClient.get<JargonEntry[]>('/jargon', { params });
  return response.data;
}

export async function createJargon(
  data: Partial<JargonEntry>
): Promise<JargonEntry> {
  const response = await apiClient.post<JargonEntry>('/jargon', data);
  return response.data;
}

export async function updateJargon(
  id: string,
  data: Partial<JargonEntry>
): Promise<JargonEntry> {
  const response = await apiClient.put<JargonEntry>(`/jargon/${id}`, data);
  return response.data;
}

export async function deleteJargon(id: string): Promise<void> {
  await apiClient.delete(`/jargon/${id}`);
}
