import { Platform } from 'react-native';

import type { PageResponse, ProductCategory, ProductDetail, ProductListItem, ProductReview, ProductSearchParams } from '@/types/product';

const fallbackHost = Platform.OS === 'android' ? '10.0.2.2' : 'localhost';
const PRODUCT_API_URL = (process.env.EXPO_PUBLIC_PRODUCT_API_URL ?? `http://${fallbackHost}:8080`).replace(/\/$/, '');

export async function getProducts(params: ProductSearchParams): Promise<PageResponse<ProductListItem>> {
  const query = new URLSearchParams();
  if (params.keyword) query.set('keyword', params.keyword);
  if (params.categoryId) query.set('categoryId', params.categoryId);
  if (params.minPrice !== undefined) query.set('minPrice', String(params.minPrice));
  if (params.maxPrice !== undefined) query.set('maxPrice', String(params.maxPrice));
  if (params.minRating !== undefined) query.set('minRating', String(params.minRating));
  if (params.sort) query.set('sort', params.sort);
  query.set('page', String(params.page ?? 0));
  query.set('size', String(params.size ?? 20));

  const response = await fetch(`${PRODUCT_API_URL}/api/products?${query.toString()}`);
  if (!response.ok) throw new Error(`Không thể tải sản phẩm (HTTP ${response.status}).`);
  return response.json() as Promise<PageResponse<ProductListItem>>;
}

export async function getCategories(signal?: AbortSignal): Promise<ProductCategory[]> {
  const response = await fetch(`${PRODUCT_API_URL}/api/categories`, { signal });
  if (!response.ok) throw new Error(`Không thể tải danh mục (HTTP ${response.status}).`);
  return response.json() as Promise<ProductCategory[]>;
}

export async function getProductReviews(
  productId: string,
  params: { page?: number; size?: number; rating?: number },
  signal?: AbortSignal,
): Promise<PageResponse<ProductReview>> {
  const query = new URLSearchParams();
  query.set('page', String(params.page ?? 0));
  query.set('size', String(params.size ?? 8));
  if (params.rating !== undefined) query.set('rating', String(params.rating));

  const response = await fetch(
    `${PRODUCT_API_URL}/api/product-reviews/${encodeURIComponent(productId)}?${query.toString()}`,
    { signal },
  );
  if (!response.ok) throw new Error(`Không thể tải đánh giá (HTTP ${response.status}).`);
  return response.json() as Promise<PageResponse<ProductReview>>;
}

export async function getProduct(productId: string, signal?: AbortSignal): Promise<ProductDetail> {
  const response = await fetch(`${PRODUCT_API_URL}/api/products/${encodeURIComponent(productId)}`, { signal });
  if (!response.ok) {
    if (response.status === 404) throw new Error('Sản phẩm không còn tồn tại.');
    throw new Error(`Không thể tải chi tiết sản phẩm (HTTP ${response.status}).`);
  }
  return response.json() as Promise<ProductDetail>;
}

export function resolveProductImageUrl(imageUrl: string | null): string | undefined {
  if (!imageUrl) return undefined;
  if (/^https?:\/\//i.test(imageUrl)) return imageUrl;
  return `${PRODUCT_API_URL}/${imageUrl.replace(/^\//, '')}`;
}
