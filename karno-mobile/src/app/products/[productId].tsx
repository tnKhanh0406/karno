import { Image } from 'expo-image';
import { router, useLocalSearchParams } from 'expo-router';
import { StatusBar } from 'expo-status-bar';
import { useCallback, useEffect, useMemo, useState } from 'react';
import {
  ActivityIndicator,
  Alert,
  NativeScrollEvent,
  NativeSyntheticEvent,
  Pressable,
  ScrollView,
  StyleSheet,
  Text,
  useWindowDimensions,
  View,
} from 'react-native';
import { SafeAreaView, useSafeAreaInsets } from 'react-native-safe-area-context';

import { addToCart } from '@/services/cart-api';
import { getProduct, getProductReviews, resolveProductImageUrl } from '@/services/product-api';
import type { ProductDetail, ProductReview, ProductVariant } from '@/types/product';

const ORANGE = '#EE4D2D';
const formatPrice = (price: number) => `${Math.round(price).toLocaleString('vi-VN')}đ`;
const formatCount = (count: number) => count >= 1_000 ? `${(count / 1_000).toFixed(1).replace('.0', '')}k` : count.toString();

export default function ProductDetailScreen() {
  const { productId } = useLocalSearchParams<{ productId: string }>();
  const { width } = useWindowDimensions();
  const insets = useSafeAreaInsets();
  const galleryWidth = Math.min(width, 720);
  const [product, setProduct] = useState<ProductDetail | null>(null);
  const [selectedVariant, setSelectedVariant] = useState<ProductVariant | null>(null);
  const [activeImage, setActiveImage] = useState(0);
  const [quantity, setQuantity] = useState(1);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [addingToCart, setAddingToCart] = useState(false);
  const [reviews, setReviews] = useState<ProductReview[]>([]);
  const [reviewRating, setReviewRating] = useState<number>();
  const [reviewPage, setReviewPage] = useState(0);
  const [reviewTotalPages, setReviewTotalPages] = useState(0);
  const [reviewsLoading, setReviewsLoading] = useState(true);
  const [reviewsLoadingMore, setReviewsLoadingMore] = useState(false);
  const [reviewsError, setReviewsError] = useState<string | null>(null);

  const loadProduct = useCallback(async (signal?: AbortSignal) => {
    if (!productId) {
      setError('Mã sản phẩm không hợp lệ.');
      setLoading(false);
      return;
    }
    setLoading(true);
    setError(null);
    try {
      const response = await getProduct(productId, signal);
      setProduct(response);
      setSelectedVariant(response.variants[0] ?? null);
      setQuantity(1);
      setActiveImage(0);
    } catch (cause) {
      if (cause instanceof Error && cause.name === 'AbortError') return;
      setError(cause instanceof Error ? cause.message : 'Không thể tải chi tiết sản phẩm.');
    } finally {
      if (!signal?.aborted) setLoading(false);
    }
  }, [productId]);

  useEffect(() => {
    const controller = new AbortController();
    const task = setTimeout(() => void loadProduct(controller.signal), 0);
    return () => {
      clearTimeout(task);
      controller.abort();
    };
  }, [loadProduct]);

  const loadReviews = useCallback(async (nextPage = 0, append = false, signal?: AbortSignal) => {
    if (!productId) return;
    if (append) setReviewsLoadingMore(true);
    else setReviewsLoading(true);
    setReviewsError(null);
    try {
      const response = await getProductReviews(productId, { page: nextPage, size: 8, rating: reviewRating }, signal);
      setReviews((current) => append ? [...current, ...response.content.filter((review) => !current.some((item) => item.id === review.id))] : response.content);
      setReviewPage(response.page);
      setReviewTotalPages(response.totalPages);
    } catch (cause) {
      if (cause instanceof Error && cause.name === 'AbortError') return;
      setReviewsError(cause instanceof Error ? cause.message : 'Không thể tải đánh giá sản phẩm.');
    } finally {
      if (!signal?.aborted) {
        setReviewsLoading(false);
        setReviewsLoadingMore(false);
      }
    }
  }, [productId, reviewRating]);

  useEffect(() => {
    const controller = new AbortController();
    const task = setTimeout(() => void loadReviews(0, false, controller.signal), 0);
    return () => {
      clearTimeout(task);
      controller.abort();
    };
  }, [loadReviews]);

  const galleryImages = useMemo(() => {
    if (!product) return [];
    const urls = [selectedVariant?.primaryImg, product.thumbnailImg, ...product.images.map((image) => image.url)]
      .filter((url): url is string => Boolean(url));
    return [...new Set(urls)];
  }, [product, selectedVariant]);

  const selectVariant = (variant: ProductVariant) => {
    setSelectedVariant(variant);
    setQuantity(1);
    setActiveImage(0);
  };

  const changeQuantity = (step: number) => {
    const stock = selectedVariant?.availableQuantity ?? 0;
    setQuantity((current) => Math.min(Math.max(current + step, 1), Math.max(stock, 1)));
  };

  const handleAddToCart = async () => {
    if (!selectedVariant || currentStock === 0 || addingToCart) return;
    setAddingToCart(true);
    try {
      await addToCart(selectedVariant.id, quantity);
      Alert.alert('Đã thêm vào giỏ hàng', `${quantity} × ${selectedVariant.name}`, [
        { text: 'Tiếp tục mua', style: 'cancel' },
        { text: 'Xem giỏ hàng', onPress: () => router.push('/cart') },
      ]);
    } catch (cause) {
      Alert.alert('Không thể thêm vào giỏ', cause instanceof Error ? cause.message : 'Vui lòng thử lại.');
    } finally {
      setAddingToCart(false);
    }
  };

  if (loading) {
    return (
      <View style={styles.centerState}>
        <StatusBar style="dark" />
        <ActivityIndicator color={ORANGE} size="large" />
        <Text style={styles.stateMessage}>Đang tải thông tin sản phẩm...</Text>
      </View>
    );
  }

  if (!product || error) {
    return (
      <SafeAreaView style={styles.centerState}>
        <StatusBar style="dark" />
        <Text style={styles.errorIcon}>!</Text>
        <Text style={styles.errorTitle}>Không thể mở sản phẩm</Text>
        <Text style={styles.stateMessage}>{error}</Text>
        <View style={styles.errorActions}>
          <Pressable onPress={() => router.back()} style={styles.secondaryButton}><Text style={styles.secondaryButtonText}>Quay lại</Text></Pressable>
          <Pressable onPress={() => void loadProduct()} style={styles.primaryButton}><Text style={styles.primaryButtonText}>Thử lại</Text></Pressable>
        </View>
      </SafeAreaView>
    );
  }

  const currentPrice = selectedVariant?.price ?? product.minPrice;
  const currentStock = selectedVariant?.availableQuantity ?? 0;

  return (
    <View style={styles.screen}>
      <StatusBar style="dark" />
      <SafeAreaView edges={['top']} style={styles.topSafeArea}>
        <View style={styles.topBar}>
          <Pressable accessibilityLabel="Quay lại" hitSlop={10} onPress={() => router.back()} style={styles.circleButton}><Text style={styles.backIcon}>‹</Text></Pressable>
          <Text numberOfLines={1} style={styles.topTitle}>Chi tiết sản phẩm</Text>
          <View style={styles.topActions}>
            <Pressable accessibilityLabel="Chia sẻ" style={styles.circleButton}><Text style={styles.actionIcon}>↗</Text></Pressable>
            <Pressable accessibilityLabel="Giỏ hàng" onPress={() => router.push('/cart')} style={styles.circleButton}><Text style={styles.cartIcon}>🛒</Text></Pressable>
          </View>
        </View>
      </SafeAreaView>

      <ScrollView contentContainerStyle={[styles.scrollContent, { paddingBottom: 96 + insets.bottom }]} showsVerticalScrollIndicator={false}>
        <View style={styles.gallerySection}>
          {galleryImages.length > 0 ? (
            <ScrollView
              horizontal
              pagingEnabled
              showsHorizontalScrollIndicator={false}
              onMomentumScrollEnd={(event: NativeSyntheticEvent<NativeScrollEvent>) => setActiveImage(Math.round(event.nativeEvent.contentOffset.x / galleryWidth))}>
              {galleryImages.map((url) => (
                <Image key={url} contentFit="cover" source={resolveProductImageUrl(url)} style={{ width: galleryWidth, height: galleryWidth }} transition={180} />
              ))}
            </ScrollView>
          ) : <View style={[styles.imagePlaceholder, { height: galleryWidth }]}><Text style={styles.imagePlaceholderText}>Không có ảnh</Text></View>}
          {galleryImages.length > 1 && <View style={styles.imageCounter}><Text style={styles.imageCounterText}>{activeImage + 1}/{galleryImages.length}</Text></View>}
        </View>

        <View style={styles.mainInfo}>
          <Text style={styles.price}>{formatPrice(currentPrice)}</Text>
          {product.minPrice !== product.maxPrice && !selectedVariant && <Text style={styles.priceRange}>{formatPrice(product.minPrice)} - {formatPrice(product.maxPrice)}</Text>}
          <Text style={styles.productName}>{product.name}</Text>
          <View style={styles.productMeta}>
            <Text style={styles.rating}>★ {product.rating.toFixed(1)}</Text>
            <View style={styles.divider} />
            <Text style={styles.metaText}>{product.ratingSummary.totalReviews} đánh giá</Text>
            <View style={styles.divider} />
            <Text style={styles.metaText}>Đã bán {formatCount(product.soldCount)}</Text>
          </View>
        </View>

        {product.variants.length > 0 && (
          <View style={styles.section}>
            <View style={styles.sectionTitleRow}><Text style={styles.sectionTitle}>Phân loại</Text><Text style={[styles.stockText, currentStock === 0 && styles.outOfStockText]}>{currentStock > 0 ? `Còn ${currentStock} sản phẩm` : 'Hết hàng'}</Text></View>
            <View style={styles.variantList}>
              {product.variants.map((variant) => {
                const selected = variant.id === selectedVariant?.id;
                const unavailable = variant.availableQuantity === 0;
                return (
                  <Pressable key={variant.id} disabled={unavailable} onPress={() => selectVariant(variant)} style={[styles.variantChip, selected && styles.variantChipSelected, unavailable && styles.variantChipDisabled]}>
                    {variant.primaryImg && <Image source={resolveProductImageUrl(variant.primaryImg)} style={styles.variantImage} />}
                    <View><Text style={[styles.variantName, selected && styles.variantNameSelected]}>{variant.name}</Text><Text style={styles.variantPrice}>{formatPrice(variant.price)}</Text></View>
                  </Pressable>
                );
              })}
            </View>
            <View style={styles.quantityRow}>
              <Text style={styles.quantityLabel}>Số lượng</Text>
              <View style={styles.stepper}>
                <Pressable disabled={quantity <= 1} onPress={() => changeQuantity(-1)} style={styles.stepButton}><Text style={styles.stepText}>−</Text></Pressable>
                <Text style={styles.quantityValue}>{quantity}</Text>
                <Pressable disabled={quantity >= currentStock} onPress={() => changeQuantity(1)} style={styles.stepButton}><Text style={styles.stepText}>+</Text></Pressable>
              </View>
            </View>
          </View>
        )}

        <View style={styles.section}>
          <View style={styles.shopRow}>
            <Image contentFit="cover" source={resolveProductImageUrl(product.shopInformation.logoUrl)} style={styles.shopLogo} />
            <View style={styles.shopInfo}><Text style={styles.shopName}>{product.shopInformation.name}</Text><Text numberOfLines={1} style={styles.shopDescription}>{product.shopInformation.description || 'Gian hàng trên Karno'}</Text></View>
            <Pressable style={styles.shopButton}><Text style={styles.shopButtonText}>Xem shop</Text></Pressable>
          </View>
        </View>

        <View style={styles.section}>
          <Text style={styles.sectionTitle}>Chi tiết sản phẩm</Text>
          {product.categoryBreadcrumbs.length > 0 && <View style={styles.detailRow}><Text style={styles.detailLabel}>Danh mục</Text><Text style={styles.detailValue}>{product.categoryBreadcrumbs.map((category) => category.name).join('  ›  ')}</Text></View>}
          {product.attributes.map((attribute) => <View key={attribute.id} style={styles.detailRow}><Text style={styles.detailLabel}>{attribute.name}</Text><Text style={styles.detailValue}>{attribute.values.map((value) => value.value).join(', ')}</Text></View>)}
        </View>

        <View style={styles.section}>
          <Text style={styles.sectionTitle}>Mô tả sản phẩm</Text>
          <Text style={styles.description}>{product.description || 'Chưa có mô tả cho sản phẩm này.'}</Text>
        </View>

        <View style={styles.section}>
          <View style={styles.sectionTitleRow}><Text style={styles.sectionTitle}>Đánh giá sản phẩm</Text><Text style={styles.reviewCount}>{product.ratingSummary.totalReviews} đánh giá</Text></View>
          <View style={styles.ratingSummary}>
            <View style={styles.ratingOverview}><Text style={styles.ratingNumber}>{product.rating.toFixed(1)}</Text><Text style={styles.ratingStars}>★★★★★</Text><Text style={styles.ratingOutOf}>trên 5</Text></View>
            <View style={styles.ratingBars}>
              {[5, 4, 3, 2, 1].map((score) => {
                const count = product.ratingSummary.ratingCounts.find((item) => item.rating === score)?.count ?? 0;
                const ratio = product.ratingSummary.totalReviews ? count / product.ratingSummary.totalReviews : 0;
                return <View key={score} style={styles.ratingBarRow}><Text style={styles.ratingBarLabel}>{score}★</Text><View style={styles.ratingTrack}><View style={[styles.ratingFill, { width: `${ratio * 100}%` }]} /></View><Text style={styles.ratingBarCount}>{count}</Text></View>;
              })}
            </View>
          </View>
          <ScrollView horizontal showsHorizontalScrollIndicator={false} contentContainerStyle={styles.reviewFilters}>
            {[undefined, 5, 4, 3, 2, 1].map((score) => {
              const selected = score === reviewRating;
              return (
                <Pressable key={score ?? 'all'} onPress={() => setReviewRating(score)} style={[styles.reviewFilterChip, selected && styles.reviewFilterChipSelected]}>
                  <Text style={[styles.reviewFilterText, selected && styles.reviewFilterTextSelected]}>{score ? `${score} sao` : 'Tất cả'}</Text>
                </Pressable>
              );
            })}
          </ScrollView>
          {reviewsLoading ? (
            <ActivityIndicator color={ORANGE} style={styles.reviewsLoader} />
          ) : reviewsError ? (
            <Pressable onPress={() => void loadReviews()} style={styles.reviewError}><Text style={styles.reviewErrorText}>{reviewsError}</Text><Text style={styles.reviewRetry}>Thử lại</Text></Pressable>
          ) : reviews.length === 0 ? (
            <Text style={styles.noReviews}>Chưa có đánh giá phù hợp.</Text>
          ) : (
            <View style={styles.reviewList}>
              {reviews.map((review) => (
                <View key={review.id} style={styles.reviewItem}>
                  <View style={styles.reviewerRow}>
                    {review.user?.avatarUrl ? (
                      <Image contentFit="cover" source={resolveProductImageUrl(review.user.avatarUrl)} style={styles.reviewerAvatar} />
                    ) : (
                      <View style={styles.reviewerAvatarFallback}><Text style={styles.reviewerInitial}>{review.user?.name?.charAt(0).toUpperCase() || '?'}</Text></View>
                    )}
                    <View style={styles.reviewerInfo}>
                      <Text style={styles.reviewerName}>{review.user?.name || 'Người dùng Karno'}</Text>
                      <Text style={styles.reviewStars}>{'★'.repeat(review.rating)}{'☆'.repeat(Math.max(0, 5 - review.rating))}</Text>
                    </View>
                    <Text style={styles.reviewDate}>{new Date(review.createdAt).toLocaleDateString('vi-VN')}</Text>
                  </View>
                  <Text style={styles.reviewComment}>{review.comment?.trim() || 'Người mua không để lại bình luận.'}</Text>
                </View>
              ))}
              {reviewPage + 1 < reviewTotalPages && (
                <Pressable disabled={reviewsLoadingMore} onPress={() => void loadReviews(reviewPage + 1, true)} style={styles.moreReviewsButton}>
                  {reviewsLoadingMore ? <ActivityIndicator color={ORANGE} /> : <Text style={styles.moreReviewsText}>Xem thêm đánh giá</Text>}
                </Pressable>
              )}
            </View>
          )}
        </View>
      </ScrollView>

      <View style={[styles.bottomBar, { paddingBottom: Math.max(insets.bottom, 10) }]}>
        <Pressable disabled={currentStock === 0 || addingToCart} onPress={() => void handleAddToCart()} style={[styles.addToCartButton, (currentStock === 0 || addingToCart) && styles.disabledButton]}>{addingToCart ? <ActivityIndicator color={ORANGE} /> : <Text style={styles.addToCartText}>🛒  Thêm vào giỏ</Text>}</Pressable>
        <Pressable disabled={currentStock === 0} onPress={() => Alert.alert('Mua ngay', 'Luồng checkout sẽ được nối ở màn hình tiếp theo.')} style={[styles.buyButton, currentStock === 0 && styles.disabledButton]}><Text style={styles.buyButtonText}>{currentStock > 0 ? 'Mua ngay' : 'Hết hàng'}</Text></Pressable>
      </View>
    </View>
  );
}

const styles = StyleSheet.create({
  screen: { flex: 1, backgroundColor: '#F4F4F4' },
  topSafeArea: { backgroundColor: '#FFF' },
  topBar: { height: 54, paddingHorizontal: 12, flexDirection: 'row', alignItems: 'center', borderBottomWidth: StyleSheet.hairlineWidth, borderBottomColor: '#E8E8E8' },
  topTitle: { flex: 1, marginHorizontal: 12, color: '#222', fontSize: 16, fontWeight: '700' },
  topActions: { flexDirection: 'row', gap: 8 },
  circleButton: { width: 38, height: 38, borderRadius: 19, backgroundColor: '#F5F5F5', alignItems: 'center', justifyContent: 'center' },
  backIcon: { color: '#222', fontSize: 34, lineHeight: 35, marginTop: -3 },
  actionIcon: { color: '#333', fontSize: 20 },
  cartIcon: { fontSize: 17 },
  scrollContent: { width: '100%', maxWidth: 720, alignSelf: 'center' },
  gallerySection: { position: 'relative', backgroundColor: '#FFF' },
  imagePlaceholder: { alignItems: 'center', justifyContent: 'center', backgroundColor: '#EEE' },
  imagePlaceholderText: { color: '#999' },
  imageCounter: { position: 'absolute', right: 14, bottom: 14, backgroundColor: 'rgba(0,0,0,0.55)', borderRadius: 13, paddingHorizontal: 9, paddingVertical: 5 },
  imageCounterText: { color: '#FFF', fontSize: 11, fontWeight: '700' },
  mainInfo: { backgroundColor: '#FFF', paddingHorizontal: 15, paddingTop: 14, paddingBottom: 16 },
  price: { color: ORANGE, fontSize: 26, fontWeight: '800' },
  priceRange: { color: '#999', fontSize: 12, marginTop: 2 },
  productName: { color: '#222', fontSize: 17, lineHeight: 24, fontWeight: '600', marginTop: 8 },
  productMeta: { flexDirection: 'row', alignItems: 'center', marginTop: 12 },
  rating: { color: '#F0A500', fontSize: 12, fontWeight: '800' },
  divider: { width: 1, height: 12, backgroundColor: '#DDD', marginHorizontal: 11 },
  metaText: { color: '#666', fontSize: 12 },
  section: { backgroundColor: '#FFF', marginTop: 9, padding: 15 },
  sectionTitleRow: { flexDirection: 'row', alignItems: 'center', justifyContent: 'space-between' },
  sectionTitle: { color: '#222', fontSize: 16, fontWeight: '800' },
  stockText: { color: '#777', fontSize: 11 },
  outOfStockText: { color: '#C33' },
  variantList: { flexDirection: 'row', flexWrap: 'wrap', gap: 9, marginTop: 13 },
  variantChip: { minWidth: '47%', minHeight: 54, flexDirection: 'row', alignItems: 'center', gap: 8, padding: 7, borderRadius: 7, borderWidth: 1, borderColor: '#DDD', backgroundColor: '#FFF' },
  variantChipSelected: { borderColor: ORANGE, backgroundColor: '#FFF5F2' },
  variantChipDisabled: { opacity: 0.38 },
  variantImage: { width: 38, height: 38, borderRadius: 4, backgroundColor: '#EEE' },
  variantName: { color: '#444', fontSize: 12, fontWeight: '600' },
  variantNameSelected: { color: ORANGE },
  variantPrice: { color: '#888', fontSize: 10, marginTop: 2 },
  quantityRow: { flexDirection: 'row', justifyContent: 'space-between', alignItems: 'center', marginTop: 18 },
  quantityLabel: { color: '#444', fontSize: 13 },
  stepper: { height: 34, flexDirection: 'row', borderWidth: 1, borderColor: '#DDD', borderRadius: 5, overflow: 'hidden' },
  stepButton: { width: 36, alignItems: 'center', justifyContent: 'center', backgroundColor: '#F8F8F8' },
  stepText: { color: '#444', fontSize: 20 },
  quantityValue: { minWidth: 42, textAlign: 'center', textAlignVertical: 'center', color: '#222', borderLeftWidth: 1, borderRightWidth: 1, borderColor: '#DDD', paddingTop: 7 },
  shopRow: { flexDirection: 'row', alignItems: 'center' },
  shopLogo: { width: 54, height: 54, borderRadius: 27, backgroundColor: '#EEE' },
  shopInfo: { flex: 1, marginLeft: 12, marginRight: 8 },
  shopName: { color: '#222', fontSize: 14, fontWeight: '800' },
  shopDescription: { color: '#888', fontSize: 11, marginTop: 4 },
  shopButton: { borderWidth: 1, borderColor: ORANGE, borderRadius: 4, paddingHorizontal: 11, paddingVertical: 7 },
  shopButtonText: { color: ORANGE, fontSize: 11, fontWeight: '700' },
  detailRow: { flexDirection: 'row', paddingTop: 14 },
  detailLabel: { width: 94, color: '#999', fontSize: 12, lineHeight: 18 },
  detailValue: { flex: 1, color: '#444', fontSize: 12, lineHeight: 18 },
  description: { color: '#444', fontSize: 13, lineHeight: 21, marginTop: 13 },
  reviewCount: { color: ORANGE, fontSize: 11 },
  ratingSummary: { flexDirection: 'row', alignItems: 'center', marginTop: 18 },
  ratingOverview: { width: 105, alignItems: 'center' },
  ratingNumber: { color: ORANGE, fontSize: 30, fontWeight: '900' },
  ratingStars: { color: '#F0A500', fontSize: 13, letterSpacing: 1, marginTop: 2 },
  ratingOutOf: { color: '#999', fontSize: 10, marginTop: 3 },
  ratingBars: { flex: 1, gap: 5 },
  ratingBarRow: { flexDirection: 'row', alignItems: 'center', gap: 7 },
  ratingBarLabel: { width: 20, color: '#777', fontSize: 10 },
  ratingTrack: { flex: 1, height: 5, borderRadius: 3, backgroundColor: '#EEE', overflow: 'hidden' },
  ratingFill: { height: '100%', backgroundColor: '#F0A500' },
  ratingBarCount: { width: 24, color: '#999', fontSize: 9, textAlign: 'right' },
  reviewFilters: { gap: 8, paddingTop: 18, paddingBottom: 5 },
  reviewFilterChip: { minWidth: 62, borderRadius: 6, borderWidth: 1, borderColor: '#DDD', backgroundColor: '#FAFAFA', paddingHorizontal: 12, paddingVertical: 8, alignItems: 'center' },
  reviewFilterChipSelected: { borderColor: ORANGE, backgroundColor: '#FFF1ED' },
  reviewFilterText: { color: '#666', fontSize: 11, fontWeight: '600' },
  reviewFilterTextSelected: { color: ORANGE, fontWeight: '800' },
  reviewsLoader: { marginVertical: 28 },
  reviewError: { alignItems: 'center', paddingVertical: 24 },
  reviewErrorText: { color: '#777', fontSize: 12, textAlign: 'center' },
  reviewRetry: { color: ORANGE, fontSize: 12, fontWeight: '800', marginTop: 8 },
  noReviews: { color: '#888', fontSize: 12, textAlign: 'center', paddingVertical: 28 },
  reviewList: { marginTop: 10 },
  reviewItem: { borderTopWidth: StyleSheet.hairlineWidth, borderTopColor: '#E8E8E8', paddingVertical: 15 },
  reviewerRow: { flexDirection: 'row', alignItems: 'center' },
  reviewerAvatar: { width: 38, height: 38, borderRadius: 19, backgroundColor: '#EEE' },
  reviewerAvatarFallback: { width: 38, height: 38, borderRadius: 19, backgroundColor: '#FFF0EC', alignItems: 'center', justifyContent: 'center' },
  reviewerInitial: { color: ORANGE, fontSize: 15, fontWeight: '900' },
  reviewerInfo: { flex: 1, marginLeft: 10 },
  reviewerName: { color: '#333', fontSize: 12, fontWeight: '700' },
  reviewStars: { color: '#F0A500', fontSize: 11, letterSpacing: 1, marginTop: 3 },
  reviewDate: { color: '#999', fontSize: 10 },
  reviewComment: { color: '#444', fontSize: 12, lineHeight: 19, marginTop: 10, paddingLeft: 48 },
  moreReviewsButton: { height: 42, borderRadius: 6, borderWidth: 1, borderColor: ORANGE, alignItems: 'center', justifyContent: 'center', marginTop: 4 },
  moreReviewsText: { color: ORANGE, fontSize: 12, fontWeight: '800' },
  bottomBar: { position: 'absolute', left: 0, right: 0, bottom: 0, flexDirection: 'row', gap: 9, paddingTop: 10, paddingHorizontal: 12, backgroundColor: '#FFF', borderTopWidth: StyleSheet.hairlineWidth, borderTopColor: '#DDD' },
  addToCartButton: { flex: 1, height: 48, borderRadius: 7, borderWidth: 1, borderColor: ORANGE, backgroundColor: '#FFF4F0', alignItems: 'center', justifyContent: 'center' },
  addToCartText: { color: ORANGE, fontSize: 13, fontWeight: '800' },
  buyButton: { flex: 1, height: 48, borderRadius: 7, backgroundColor: ORANGE, alignItems: 'center', justifyContent: 'center' },
  buyButtonText: { color: '#FFF', fontSize: 14, fontWeight: '800' },
  disabledButton: { opacity: 0.45 },
  centerState: { flex: 1, backgroundColor: '#FFF', alignItems: 'center', justifyContent: 'center', padding: 30 },
  stateMessage: { color: '#777', fontSize: 13, lineHeight: 19, textAlign: 'center', marginTop: 12 },
  errorIcon: { width: 54, height: 54, borderRadius: 27, textAlign: 'center', color: ORANGE, backgroundColor: '#FFF0EC', fontSize: 36, fontWeight: '800' },
  errorTitle: { color: '#222', fontSize: 18, fontWeight: '800', marginTop: 16 },
  errorActions: { flexDirection: 'row', gap: 10, marginTop: 22 },
  secondaryButton: { borderWidth: 1, borderColor: '#CCC', borderRadius: 7, paddingHorizontal: 18, paddingVertical: 11 },
  secondaryButtonText: { color: '#555', fontWeight: '700' },
  primaryButton: { borderRadius: 7, backgroundColor: ORANGE, paddingHorizontal: 18, paddingVertical: 11 },
  primaryButtonText: { color: '#FFF', fontWeight: '700' },
});
