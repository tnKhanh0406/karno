import { Platform } from 'react-native';

import type { PickupRequest, ReadyShipment } from '@/types/order';

const fallbackHost = Platform.OS === 'android' ? '10.0.2.2' : 'localhost';
const SHIPPING_API_URL = (process.env.EXPO_PUBLIC_SHIPPING_API_URL ?? `http://${fallbackHost}:8084`).replace(/\/$/, '');
const FLEET_API_URL = (process.env.EXPO_PUBLIC_FLEET_API_URL ?? `http://${fallbackHost}:8085`).replace(/\/$/, '');

export const SELLER_ID = process.env.EXPO_PUBLIC_SELLER_ID?.trim();
export const SHOP_ID = process.env.EXPO_PUBLIC_SHOP_ID?.trim();

export function requireSellerIdentity() {
  if (!SELLER_ID || !SHOP_ID) {
    throw new Error('Chưa cấu hình EXPO_PUBLIC_SELLER_ID và EXPO_PUBLIC_SHOP_ID.');
  }
  return { sellerId: SELLER_ID, shopId: SHOP_ID };
}

export async function getReadyShipments(shopId: string, signal?: AbortSignal): Promise<ReadyShipment[]> {
  const response = await fetch(`${SHIPPING_API_URL}/api/shipments/ready-for-pickup?shopId=${encodeURIComponent(shopId)}`, { signal });
  if (!response.ok) throw new Error(await readError(response));
  return response.json() as Promise<ReadyShipment[]>;
}

export async function createPickupRequest(input: {
  shopId: string;
  sellerId: string;
  shipmentIds: string[];
  pickupWindowStart: string;
  pickupWindowEnd: string;
}): Promise<PickupRequest> {
  const response = await fetch(`${FLEET_API_URL}/api/pickup-requests`, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({ ...input, serviceDurationSeconds: 300 }),
  });
  if (!response.ok) throw new Error(await readError(response));
  return response.json() as Promise<PickupRequest>;
}

async function readError(response: Response) {
  const body = await response.text();
  if (!body) return `Yêu cầu thất bại (HTTP ${response.status}).`;
  try {
    const parsed = JSON.parse(body) as { message?: string; detail?: string; error?: string };
    return parsed.message || parsed.detail || parsed.error || body;
  } catch {
    return body;
  }
}
