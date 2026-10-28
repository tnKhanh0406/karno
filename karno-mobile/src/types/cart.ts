export type CartItem = {
  cartItemId: string;
  productId: string;
  productName: string | null;
  thumbnailUrl: string | null;
  variantId: string;
  variantName: string | null;
  primaryImg: string | null;
  price: number | null;
  quantity: number;
  availableStock: number;
  attributeValues: {
    attributeId: string;
    attributeName: string;
    attributeValueId: string;
    attributeValueName: string;
  }[];
  available: boolean;
  unavailableReason: 'PRODUCT_NOT_FOUND' | 'PRODUCT_UNAVAILABLE' | 'OUT_OF_STOCK' | 'INSUFFICIENT_STOCK' | null;
};

export type CartShop = {
  shopId: string;
  shopName: string | null;
  shopLogo: string | null;
  items: CartItem[];
};

export type Cart = {
  cartId: string | null;
  totalItems: number;
  shops: CartShop[];
};

export type AddToCartResponse = {
  productId: string;
  productName: string;
  thumbnailImg: string | null;
  variantId: string;
  price: number;
  primaryImg: string | null;
  quantity: number;
};
