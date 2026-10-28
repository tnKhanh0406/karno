export type ProductSort = 'NEWEST' | 'PRICE_ASC' | 'PRICE_DESC' | 'RATING_DESC' | 'SOLD_DESC';

export type ProductListItem = {
  id: string;
  name: string;
  thumbnailImg: string | null;
  minPrice: number;
  rating: number;
  soldCount: number;
};

export type PageResponse<T> = {
  content: T[];
  page: number;
  size: number;
  totalElements: number;
  totalPages: number;
};

export type ProductSearchParams = {
  keyword?: string;
  categoryId?: string;
  minPrice?: number;
  maxPrice?: number;
  minRating?: number;
  sort?: ProductSort;
  page?: number;
  size?: number;
};

export type ProductFilters = Pick<
  ProductSearchParams,
  'categoryId' | 'minPrice' | 'maxPrice' | 'minRating'
>;

export type ProductCategory = {
  id: string;
  parentId: string | null;
  name: string;
  slug: string;
  imageUrl: string | null;
};

export type ProductReview = {
  id: string;
  comment: string | null;
  rating: number;
  createdAt: string;
  updatedAt: string;
  user: {
    id: string;
    name: string;
    avatarUrl: string | null;
  } | null;
};

export type ProductImage = {
  id: string;
  imageType: 'PRODUCT' | 'VARIANT';
  url: string;
};

export type ProductVariant = {
  id: string;
  sku: string;
  name: string;
  price: number;
  primaryImg: string | null;
  availableQuantity: number;
  attributes: string[];
};

export type ProductAttribute = {
  id: string;
  name: string;
  values: { id: string; value: string }[];
};

export type ProductDetail = {
  id: string;
  name: string;
  description: string;
  thumbnailImg: string | null;
  soldCount: number;
  rating: number;
  minPrice: number;
  maxPrice: number;
  categoryBreadcrumbs: { id: string; name: string; slug: string }[];
  images: ProductImage[];
  variants: ProductVariant[];
  attributes: ProductAttribute[];
  shopInformation: {
    id: string;
    name: string;
    description: string;
    logoUrl: string | null;
  };
  ratingSummary: {
    totalReviews: number;
    ratingCounts: { rating: number; count: number }[];
  };
};
