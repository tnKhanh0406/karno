export type OrderStatus =
  | 'PENDING'
  | 'CONFIRMED'
  | 'PREPARING'
  | 'PACKED'
  | 'READY_FOR_PICKUP'
  | 'PICKED_UP'
  | 'IN_TRANSIT'
  | 'OUT_FOR_DELIVERY'
  | 'DELIVERED'
  | 'CANCELLED'
  | 'RETURN_REQUESTED'
  | 'RETURNING'
  | 'RETURNED';

export type CustomerOrderItem = {
  id: string;
  orderId: string;
  productId: string;
  variantId: string;
  productName: string;
  variantName: string;
  productImageUrl: string | null;
  unitPrice: number;
  quantity: number;
  subtotal: number;
};

export type CustomerOrder = {
  id: string;
  checkoutId: string;
  shopId: string;
  shopName: string;
  status: OrderStatus;
  subtotal: number;
  shippingFee: number;
  totalAmount: number;
  paymentMethod: 'COD' | 'ONLINE';
  paymentStatus: string;
  createdAt: string;
  totalItems: number;
  items: CustomerOrderItem[];
};

export type CustomerOrderPage = {
  content: CustomerOrder[];
  page: number;
  size: number;
  totalElements: number;
  totalPages: number;
};
