export type CheckoutPreviewItem = {
  cartItemId: string;
  productId: string;
  variantId: string;
  productName: string;
  variantName: string;
  sku: string;
  productImageUrl: string | null;
  unitPrice: number;
  quantity: number;
  subtotal: number;
};

export type CheckoutPreviewShop = {
  shopId: string;
  shopName: string;
  shipping: {
    shippingMethod: string;
    shippingFee: number;
    totalWeightGram: number;
    estimatedDeliveryAt: string;
    pickup: {
      name: string;
      phone: string;
      address: string;
      latitude: number;
      longitude: number;
    };
  };
  subtotal: number;
  totalAmount: number;
  items: CheckoutPreviewItem[];
};

export type CheckoutPreview = {
  customerId: string;
  receiver: {
    name: string;
    phone: string;
    address: string;
    latitude: number;
    longitude: number;
  };
  shops: CheckoutPreviewShop[];
  subtotal: number;
  shippingFee: number;
  totalAmount: number;
  preparedAt: string;
};

export type PaymentMethod = 'COD' | 'ONLINE';
export type CheckoutStatus = 'PROCESSING' | 'INVENTORY_RESERVED' | 'ORDER_COMMITTED' | 'COMPENSATING' | 'COMPLETED' | 'FAILED';

export type CheckoutStartResponse = {
  checkoutId: string;
  status: CheckoutStatus;
  reused: boolean;
  message: string;
};

export type CheckoutStatusResponse = {
  checkoutId: string;
  status: CheckoutStatus;
  failureCode: string | null;
  completedAt: string | null;
};
