import { Platform } from 'react-native';

import type { CustomerOrderPage, OrderStatus } from '@/types/order';

const fallbackHost = Platform.OS === 'android' ? '10.0.2.2' : 'localhost';
const ORDER_API_URL = (process.env.EXPO_PUBLIC_ORDER_API_URL ?? `http://${fallbackHost}:8081`).replace(/\/$/, '');
const CUSTOMER_ID = process.env.EXPO_PUBLIC_CUSTOMER_ID?.trim();

export async function getCustomerOrders(
  params: { status?: OrderStatus; keyword?: string; page?: number; size?: number },
  signal?: AbortSignal,
): Promise<CustomerOrderPage> {
  if (!CUSTOMER_ID) throw new Error('Chưa cấu hình EXPO_PUBLIC_CUSTOMER_ID.');
  const query = new URLSearchParams();
  if (params.status) query.set('status', params.status);
  if (params.keyword) query.set('keyword', params.keyword);
  query.set('page', String(params.page ?? 0));
  query.set('size', String(params.size ?? 10));

  const response = await fetch(
    `${ORDER_API_URL}/api/orders/customers/${encodeURIComponent(CUSTOMER_ID)}?${query.toString()}`,
    { signal },
  );
  if (!response.ok) {
    const body = await response.text();
    throw new Error(body || `Không thể tải đơn hàng (HTTP ${response.status}).`);
  }
  return response.json() as Promise<CustomerOrderPage>;
}

export async function getShopOrders(
  shopId: string,
  params: { status?: OrderStatus; keyword?: string; page?: number; size?: number },
  signal?: AbortSignal,
): Promise<CustomerOrderPage> {
  const query = new URLSearchParams();
  if (params.status) query.set('status', params.status);
  if (params.keyword) query.set('keyword', params.keyword);
  query.set('page', String(params.page ?? 0));
  query.set('size', String(params.size ?? 20));
  const response = await fetch(`${ORDER_API_URL}/api/orders/shops/${encodeURIComponent(shopId)}?${query}`, { signal });
  if (!response.ok) throw new Error(await readError(response));
  return response.json() as Promise<CustomerOrderPage>;
}

export async function packOrder(orderId: string, shopId: string, sellerId: string) {
  const response = await fetch(`${ORDER_API_URL}/api/orders/${encodeURIComponent(orderId)}/pack`, {
    method: 'PATCH',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({ shopId, sellerId }),
  });
  if (!response.ok) throw new Error(await readError(response));
  return response.json() as Promise<{ orderId: string; status: 'PACKED'; idempotent: boolean }>;
}

async function readError(response: Response) {
  const body = await response.text();
  if (!body) return `Yêu cầu thất bại (HTTP ${response.status}).`;
  try {
    const parsed = JSON.parse(body) as { message?: string };
    return parsed.message || body;
  } catch {
    return body;
  }
}
