import { Platform } from 'react-native';

import type { FirstMilePreview, FirstMileRequest, NewPickupRequest, PickupConfirmation, PickupManifest, PickupSession, Vehicle } from '@/types/fleet';

const fallbackHost = Platform.OS === 'android' ? '10.0.2.2' : 'localhost';
const FLEET_API_URL = (process.env.EXPO_PUBLIC_FLEET_API_URL ?? `http://${fallbackHost}:8085`).replace(/\/$/, '');
export const SHIPPER_ID = process.env.EXPO_PUBLIC_SHIPPER_ID?.trim();

function requireShipperId() {
  if (!SHIPPER_ID) throw new Error('Chưa cấu hình EXPO_PUBLIC_SHIPPER_ID.');
  return SHIPPER_ID;
}

export async function getAvailableVehicles(signal?: AbortSignal): Promise<Vehicle[]> {
  const shipperId = requireShipperId();
  return request(`/api/first-mile/vehicles?shipperId=${encodeURIComponent(shipperId)}`, { signal });
}

export async function previewFirstMile(input: FirstMileRequest): Promise<FirstMilePreview> {
  const shipperId = requireShipperId();
  return request(`/api/first-mile/preview?shipperId=${encodeURIComponent(shipperId)}`, {
    method: 'POST', headers: { 'Content-Type': 'application/json' }, body: JSON.stringify(input),
  });
}

export async function startFirstMileSession(input: FirstMileRequest): Promise<PickupSession> {
  const shipperId = requireShipperId();
  return request(`/api/first-mile/sessions/start?shipperId=${encodeURIComponent(shipperId)}`, {
    method: 'POST', headers: { 'Content-Type': 'application/json' }, body: JSON.stringify(input),
  });
}

export async function getActivePickupSession(signal?: AbortSignal): Promise<PickupSession | null> {
  const shipperId = requireShipperId();
  const response = await fetch(`${FLEET_API_URL}/api/first-mile/sessions/active?shipperId=${encodeURIComponent(shipperId)}`, { signal });
  if (response.status === 204) return null;
  if (!response.ok) throw new Error(await readError(response));
  return response.json() as Promise<PickupSession>;
}

export async function replanFirstMileSession(sessionId: string, input: FirstMileRequest): Promise<PickupSession> {
  const shipperId = requireShipperId();
  return request(`/api/first-mile/sessions/${encodeURIComponent(sessionId)}/replan?shipperId=${encodeURIComponent(shipperId)}`, {
    method: 'POST', headers: { 'Content-Type': 'application/json' }, body: JSON.stringify(input),
  });
}

export async function getNewPickupRequests(sessionId: string): Promise<NewPickupRequest[]> {
  const shipperId = requireShipperId();
  return request(`/api/first-mile/sessions/${encodeURIComponent(sessionId)}/new-pickup-requests?shipperId=${encodeURIComponent(shipperId)}`);
}

export async function getMapPickupRequests(signal?: AbortSignal): Promise<NewPickupRequest[]> {
  const shipperId = requireShipperId();
  return request(`/api/first-mile/map-pickup-requests?shipperId=${encodeURIComponent(shipperId)}`, { signal });
}

export async function addPickupRequestToSession(sessionId: string, pickupRequestId: string): Promise<PickupSession> {
  const shipperId = requireShipperId();
  return request(`/api/first-mile/sessions/${encodeURIComponent(sessionId)}/pickup-requests/${encodeURIComponent(pickupRequestId)}?shipperId=${encodeURIComponent(shipperId)}`, { method: 'POST' });
}

export async function deferPickupRequest(sessionId: string, pickupRequestId: string): Promise<void> {
  const shipperId = requireShipperId();
  const response = await fetch(`${FLEET_API_URL}/api/first-mile/sessions/${encodeURIComponent(sessionId)}/pickup-requests/${encodeURIComponent(pickupRequestId)}/defer?shipperId=${encodeURIComponent(shipperId)}`, { method: 'POST' });
  if (!response.ok) throw new Error(await readError(response));
}

export async function cancelPickupSession(sessionId: string): Promise<PickupSession> {
  const shipperId = requireShipperId();
  return request(`/api/first-mile/sessions/${encodeURIComponent(sessionId)}/cancel?shipperId=${encodeURIComponent(shipperId)}`, { method: 'POST' });
}

export async function completePickupSession(sessionId: string): Promise<PickupSession> {
  const shipperId = requireShipperId();
  return request(`/api/first-mile/sessions/${encodeURIComponent(sessionId)}/complete?shipperId=${encodeURIComponent(shipperId)}`, { method: 'POST' });
}

export async function getPickupManifest(pickupRequestId: string): Promise<PickupManifest> {
  const shipperId = requireShipperId();
  return request(`/api/first-mile/pickup-requests/${encodeURIComponent(pickupRequestId)}/manifest?shipperId=${encodeURIComponent(shipperId)}`);
}

export async function confirmPickup(pickupRequestId: string): Promise<PickupConfirmation> {
  const shipperId = requireShipperId();
  return request(`/api/first-mile/pickup-requests/${encodeURIComponent(pickupRequestId)}/confirm-pickup?shipperId=${encodeURIComponent(shipperId)}`, { method: 'POST' });
}

async function request<T>(path: string, init?: RequestInit): Promise<T> {
  const response = await fetch(`${FLEET_API_URL}${path}`, init);
  if (!response.ok) throw new Error(await readError(response));
  return response.json() as Promise<T>;
}

async function readError(response: Response) {
  const body = await response.text();
  if (!body) return `Yêu cầu thất bại (HTTP ${response.status}).`;
  try {
    const value = JSON.parse(body) as { message?: string; detail?: string; error?: string };
    return value.message || value.detail || value.error || body;
  } catch {
    return body;
  }
}
