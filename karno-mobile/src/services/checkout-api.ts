import { Platform } from 'react-native';

import type { CheckoutPreview, CheckoutStartResponse, CheckoutStatusResponse, PaymentMethod } from '@/types/checkout';

const fallbackHost = Platform.OS === 'android' ? '10.0.2.2' : 'localhost';
const ORDER_API_URL = (process.env.EXPO_PUBLIC_ORDER_API_URL ?? `http://${fallbackHost}:8081`).replace(/\/$/, '');
const CUSTOMER_ID = process.env.EXPO_PUBLIC_CUSTOMER_ID?.trim();
const ADDRESS_ID = process.env.EXPO_PUBLIC_ADDRESS_ID?.trim();

function requireValue(value: string | undefined, variable: string) {
  if (!value) throw new Error(`Chưa cấu hình ${variable}.`);
  return value;
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

export async function previewCheckout(cartItemIds: string[], signal?: AbortSignal): Promise<CheckoutPreview> {
  const customerId = requireValue(CUSTOMER_ID, 'EXPO_PUBLIC_CUSTOMER_ID');
  const addressId = requireValue(ADDRESS_ID, 'EXPO_PUBLIC_ADDRESS_ID');
  const response = await fetch(`${ORDER_API_URL}/api/checkouts/${customerId}/preview`, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({ cartItemIds, addressId }),
    signal,
  });
  if (!response.ok) throw new Error(await readError(response));
  return response.json() as Promise<CheckoutPreview>;
}

export async function createOrder(
  preview: CheckoutPreview,
  cartItemIds: string[],
  paymentMethod: PaymentMethod,
  note: string,
  idempotencyKey: string,
): Promise<CheckoutStartResponse> {
  const customerId = requireValue(CUSTOMER_ID, 'EXPO_PUBLIC_CUSTOMER_ID');
  const addressId = requireValue(ADDRESS_ID, 'EXPO_PUBLIC_ADDRESS_ID');
  const response = await fetch(`${ORDER_API_URL}/api/orders/${customerId}`, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json', 'Idempotency-Key': idempotencyKey },
    body: JSON.stringify({
      cartItemIds,
      addressId,
      shippingSelections: preview.shops.map((shop) => ({
        shopId: shop.shopId,
        shippingMethod: shop.shipping.shippingMethod,
        displayedShippingFee: shop.shipping.shippingFee,
        displayedSubtotal: shop.subtotal,
      })),
      paymentMethod,
      note: note.trim() || null,
      deliverySlotStart: null,
      deliverySlotEnd: null,
    }),
  });
  if (!response.ok) throw new Error(await readError(response));
  return response.json() as Promise<CheckoutStartResponse>;
}

export async function getCheckoutStatus(checkoutId: string): Promise<CheckoutStatusResponse> {
  const customerId = requireValue(CUSTOMER_ID, 'EXPO_PUBLIC_CUSTOMER_ID');
  const response = await fetch(`${ORDER_API_URL}/api/orders/checkouts/${checkoutId}/${customerId}`);
  if (!response.ok) throw new Error(await readError(response));
  return response.json() as Promise<CheckoutStatusResponse>;
}

export function createIdempotencyKey() {
  return `mobile-${Date.now()}-${Math.random().toString(36).slice(2, 12)}`;
}
