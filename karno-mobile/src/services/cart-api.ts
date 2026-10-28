import { Platform } from 'react-native';

import type { AddToCartResponse, Cart } from '@/types/cart';

const fallbackHost = Platform.OS === 'android' ? '10.0.2.2' : 'localhost';
const ORDER_API_URL = (process.env.EXPO_PUBLIC_ORDER_API_URL ?? `http://${fallbackHost}:8081`).replace(/\/$/, '');
const CUSTOMER_ID = process.env.EXPO_PUBLIC_CUSTOMER_ID?.trim();

function requireCustomerId() {
  if (!CUSTOMER_ID) throw new Error('Chưa cấu hình EXPO_PUBLIC_CUSTOMER_ID cho người mua.');
  return CUSTOMER_ID;
}

async function readError(response: Response) {
  try {
    const body = await response.json() as { message?: string };
    return body.message || `Yêu cầu thất bại (HTTP ${response.status}).`;
  } catch {
    return `Yêu cầu thất bại (HTTP ${response.status}).`;
  }
}

export async function getCart(signal?: AbortSignal): Promise<Cart> {
  const response = await fetch(`${ORDER_API_URL}/api/carts/${requireCustomerId()}`, { signal });
  if (!response.ok) throw new Error(await readError(response));
  return response.json() as Promise<Cart>;
}

export async function addToCart(variantId: string, quantity: number): Promise<AddToCartResponse> {
  const response = await fetch(`${ORDER_API_URL}/api/carts/${requireCustomerId()}/items`, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({ variantId, quantity }),
  });
  if (!response.ok) throw new Error(await readError(response));
  return response.json() as Promise<AddToCartResponse>;
}

export async function updateCartItem(cartItemId: string, quantity: number) {
  const response = await fetch(`${ORDER_API_URL}/api/carts/${requireCustomerId()}/items/${cartItemId}`, {
    method: 'PUT',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({ quantity }),
  });
  if (!response.ok) throw new Error(await readError(response));
  return response.json() as Promise<{ cartItemId: string; quantity: number }>;
}

export async function removeCartItem(cartItemId: string) {
  const response = await fetch(`${ORDER_API_URL}/api/carts/${requireCustomerId()}/items/${cartItemId}`, { method: 'DELETE' });
  if (!response.ok) throw new Error(await readError(response));
}
