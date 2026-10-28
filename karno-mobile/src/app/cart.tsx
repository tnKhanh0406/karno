import { Image } from 'expo-image';
import { router, useFocusEffect } from 'expo-router';
import { StatusBar } from 'expo-status-bar';
import { useCallback, useMemo, useState } from 'react';
import {
  ActivityIndicator,
  Alert,
  Pressable,
  RefreshControl,
  ScrollView,
  StyleSheet,
  Text,
  View,
} from 'react-native';
import { SafeAreaView, useSafeAreaInsets } from 'react-native-safe-area-context';

import { getCart, removeCartItem, updateCartItem } from '@/services/cart-api';
import { resolveProductImageUrl } from '@/services/product-api';
import type { Cart, CartItem, CartShop } from '@/types/cart';

const ORANGE = '#EE4D2D';
const formatPrice = (price: number) => `${Math.round(price).toLocaleString('vi-VN')}đ`;

const unavailableMessages: Record<NonNullable<CartItem['unavailableReason']>, string> = {
  PRODUCT_NOT_FOUND: 'Sản phẩm không còn tồn tại',
  PRODUCT_UNAVAILABLE: 'Sản phẩm đã ngừng bán',
  OUT_OF_STOCK: 'Sản phẩm đã hết hàng',
  INSUFFICIENT_STOCK: 'Số lượng trong giỏ vượt quá tồn kho',
};

export default function CartScreen() {
  const insets = useSafeAreaInsets();
  const [cart, setCart] = useState<Cart | null>(null);
  const [selectedIds, setSelectedIds] = useState<Set<string>>(new Set());
  const [busyIds, setBusyIds] = useState<Set<string>>(new Set());
  const [loading, setLoading] = useState(true);
  const [refreshing, setRefreshing] = useState(false);
  const [error, setError] = useState<string | null>(null);

  const loadCart = useCallback(async (signal?: AbortSignal, showLoader = true) => {
    if (showLoader) setLoading(true);
    setError(null);
    try {
      const response = await getCart(signal);
      setCart(response);
      const currentIds = new Set(response.shops.flatMap((shop) => shop.items.map((item) => item.cartItemId)));
      setSelectedIds((current) => new Set([...current].filter((id) => currentIds.has(id))));
    } catch (cause) {
      if (cause instanceof Error && cause.name === 'AbortError') return;
      setError(cause instanceof Error ? cause.message : 'Không thể tải giỏ hàng.');
    } finally {
      if (!signal?.aborted) {
        setLoading(false);
        setRefreshing(false);
      }
    }
  }, []);

  useFocusEffect(useCallback(() => {
    const controller = new AbortController();
    const task = setTimeout(() => void loadCart(controller.signal), 0);
    return () => {
      clearTimeout(task);
      controller.abort();
    };
  }, [loadCart]));

  const availableItems = useMemo(
    () => cart?.shops.flatMap((shop) => shop.items).filter((item) => item.available) ?? [],
    [cart],
  );
  const selectedItems = useMemo(
    () => availableItems.filter((item) => selectedIds.has(item.cartItemId)),
    [availableItems, selectedIds],
  );
  const total = selectedItems.reduce((sum, item) => sum + (item.price ?? 0) * item.quantity, 0);
  const allSelected = availableItems.length > 0 && selectedItems.length === availableItems.length;

  const setBusy = (cartItemId: string, busy: boolean) => {
    setBusyIds((current) => {
      const next = new Set(current);
      if (busy) next.add(cartItemId); else next.delete(cartItemId);
      return next;
    });
  };

  const toggleItem = (item: CartItem) => {
    if (!item.available) return;
    setSelectedIds((current) => {
      const next = new Set(current);
      if (next.has(item.cartItemId)) next.delete(item.cartItemId); else next.add(item.cartItemId);
      return next;
    });
  };

  const toggleShop = (shop: CartShop) => {
    const ids = shop.items.filter((item) => item.available).map((item) => item.cartItemId);
    const selected = ids.length > 0 && ids.every((id) => selectedIds.has(id));
    setSelectedIds((current) => {
      const next = new Set(current);
      ids.forEach((id) => selected ? next.delete(id) : next.add(id));
      return next;
    });
  };

  const toggleAll = () => {
    setSelectedIds(allSelected ? new Set() : new Set(availableItems.map((item) => item.cartItemId)));
  };

  const changeQuantity = async (item: CartItem, quantity: number) => {
    if (quantity < 1 || quantity > item.availableStock || busyIds.has(item.cartItemId)) return;
    setBusy(item.cartItemId, true);
    try {
      await updateCartItem(item.cartItemId, quantity);
      setCart((current) => current ? {
        ...current,
        shops: current.shops.map((shop) => ({
          ...shop,
          items: shop.items.map((candidate) => candidate.cartItemId === item.cartItemId
            ? { ...candidate, quantity, available: true, unavailableReason: null }
            : candidate),
        })),
      } : current);
    } catch (cause) {
      Alert.alert('Không thể cập nhật số lượng', cause instanceof Error ? cause.message : 'Vui lòng thử lại.');
    } finally {
      setBusy(item.cartItemId, false);
    }
  };

  const confirmRemove = (item: CartItem) => {
    Alert.alert('Xóa sản phẩm?', 'Sản phẩm sẽ được xóa khỏi giỏ hàng của bạn.', [
      { text: 'Hủy', style: 'cancel' },
      { text: 'Xóa', style: 'destructive', onPress: () => void performRemove(item) },
    ]);
  };

  const performRemove = async (item: CartItem) => {
    setBusy(item.cartItemId, true);
    try {
      await removeCartItem(item.cartItemId);
      setSelectedIds((current) => {
        const next = new Set(current);
        next.delete(item.cartItemId);
        return next;
      });
      setCart((current) => {
        if (!current) return current;
        const shops = current.shops
          .map((shop) => ({ ...shop, items: shop.items.filter((candidate) => candidate.cartItemId !== item.cartItemId) }))
          .filter((shop) => shop.items.length > 0);
        return { ...current, totalItems: Math.max(0, current.totalItems - 1), shops };
      });
    } catch (cause) {
      Alert.alert('Không thể xóa sản phẩm', cause instanceof Error ? cause.message : 'Vui lòng thử lại.');
    } finally {
      setBusy(item.cartItemId, false);
    }
  };

  const handleCheckout = () => {
    if (selectedItems.length === 0) return;
    router.push({
      pathname: '/checkout',
      params: { cartItemIds: selectedItems.map((item) => item.cartItemId).join(',') },
    });
  };

  return (
    <View style={styles.screen}>
      <StatusBar style="dark" />
      <SafeAreaView edges={['top']} style={styles.topSafeArea}>
        <View style={styles.header}>
          <Pressable accessibilityLabel="Quay lại" hitSlop={10} onPress={() => router.back()} style={styles.backButton}><Text style={styles.backIcon}>‹</Text></Pressable>
          <Text style={styles.headerTitle}>Giỏ hàng {cart?.totalItems ? `(${cart.totalItems})` : ''}</Text>
          <View style={styles.headerSpacer} />
        </View>
      </SafeAreaView>

      {loading ? (
        <View style={styles.centerState}><ActivityIndicator color={ORANGE} size="large" /><Text style={styles.stateMessage}>Đang tải giỏ hàng...</Text></View>
      ) : error && !cart ? (
        <View style={styles.centerState}>
          <Text style={styles.errorIcon}>!</Text><Text style={styles.emptyTitle}>Không thể tải giỏ hàng</Text><Text style={styles.stateMessage}>{error}</Text>
          <Pressable onPress={() => void loadCart()} style={styles.retryButton}><Text style={styles.retryText}>Thử lại</Text></Pressable>
        </View>
      ) : !cart || cart.shops.length === 0 ? (
        <View style={styles.centerState}>
          <View style={styles.emptyCartIcon}><Text style={styles.emptyCartEmoji}>🛒</Text></View>
          <Text style={styles.emptyTitle}>Giỏ hàng đang trống</Text>
          <Text style={styles.stateMessage}>Hãy khám phá các sản phẩm phù hợp với bạn.</Text>
          <Pressable onPress={() => router.replace('/')} style={styles.shoppingButton}><Text style={styles.shoppingButtonText}>Mua sắm ngay</Text></Pressable>
        </View>
      ) : (
        <ScrollView
          contentContainerStyle={[styles.cartContent, { paddingBottom: 90 + insets.bottom }]}
          refreshControl={<RefreshControl colors={[ORANGE]} refreshing={refreshing} tintColor={ORANGE} onRefresh={() => { setRefreshing(true); void loadCart(undefined, false); }} />}
          showsVerticalScrollIndicator={false}>
          {cart.shops.map((shop) => {
            const selectableIds = shop.items.filter((item) => item.available).map((item) => item.cartItemId);
            const shopSelected = selectableIds.length > 0 && selectableIds.every((id) => selectedIds.has(id));
            return (
              <View key={shop.shopId} style={styles.shopCard}>
                <View style={styles.shopHeader}>
                  <CheckBox checked={shopSelected} disabled={selectableIds.length === 0} onPress={() => toggleShop(shop)} />
                  <Image contentFit="cover" source={resolveProductImageUrl(shop.shopLogo)} style={styles.shopLogo} />
                  <Text numberOfLines={1} style={styles.shopName}>{shop.shopName || 'Gian hàng'}</Text>
                  <Text style={styles.chevron}>›</Text>
                </View>
                {shop.items.map((item) => (
                  <View key={item.cartItemId} style={styles.itemRow}>
                    <CheckBox checked={selectedIds.has(item.cartItemId)} disabled={!item.available} onPress={() => toggleItem(item)} />
                    <Pressable onPress={() => router.push({ pathname: '/products/[productId]', params: { productId: item.productId } })}>
                      <Image contentFit="cover" source={resolveProductImageUrl(item.primaryImg || item.thumbnailUrl)} style={styles.productImage} />
                    </Pressable>
                    <View style={styles.itemContent}>
                      <View style={styles.itemTitleRow}>
                        <Pressable style={styles.itemTitlePressable} onPress={() => router.push({ pathname: '/products/[productId]', params: { productId: item.productId } })}><Text numberOfLines={2} style={styles.itemName}>{item.productName || 'Sản phẩm không còn tồn tại'}</Text></Pressable>
                        <Pressable accessibilityLabel="Xóa sản phẩm" disabled={busyIds.has(item.cartItemId)} hitSlop={8} onPress={() => confirmRemove(item)}><Text style={styles.deleteIcon}>×</Text></Pressable>
                      </View>
                      <Text numberOfLines={1} style={styles.variantText}>{item.attributeValues.length > 0 ? item.attributeValues.map((attribute) => attribute.attributeValueName).join(', ') : item.variantName || 'Phân loại mặc định'}</Text>
                      {item.available ? (
                        <View style={styles.itemBottomRow}>
                          <Text style={styles.itemPrice}>{formatPrice(item.price ?? 0)}</Text>
                          <View style={styles.stepper}>
                            <Pressable disabled={item.quantity <= 1 || busyIds.has(item.cartItemId)} onPress={() => void changeQuantity(item, item.quantity - 1)} style={styles.stepButton}><Text style={styles.stepText}>−</Text></Pressable>
                            <View style={styles.quantityBox}>{busyIds.has(item.cartItemId) ? <ActivityIndicator color={ORANGE} size="small" /> : <Text style={styles.quantityText}>{item.quantity}</Text>}</View>
                            <Pressable disabled={item.quantity >= item.availableStock || busyIds.has(item.cartItemId)} onPress={() => void changeQuantity(item, item.quantity + 1)} style={styles.stepButton}><Text style={styles.stepText}>+</Text></Pressable>
                          </View>
                        </View>
                      ) : <Text style={styles.unavailableText}>{item.unavailableReason ? unavailableMessages[item.unavailableReason] : 'Sản phẩm không khả dụng'}</Text>}
                      {item.available && item.availableStock <= 5 && <Text style={styles.lowStockText}>Chỉ còn {item.availableStock} sản phẩm</Text>}
                    </View>
                  </View>
                ))}
              </View>
            );
          })}
        </ScrollView>
      )}

      {!!cart?.shops.length && (
        <View style={[styles.bottomBar, { paddingBottom: Math.max(insets.bottom, 10) }]}>
          <Pressable onPress={toggleAll} style={styles.selectAll}><CheckBox checked={allSelected} disabled={availableItems.length === 0} onPress={toggleAll} /><Text style={styles.selectAllText}>Tất cả</Text></Pressable>
          <View style={styles.totalBox}><Text style={styles.totalLabel}>Tổng thanh toán</Text><Text style={styles.totalPrice}>{formatPrice(total)}</Text></View>
          <Pressable disabled={selectedItems.length === 0} onPress={handleCheckout} style={[styles.checkoutButton, selectedItems.length === 0 && styles.disabledButton]}><Text style={styles.checkoutText}>Mua hàng ({selectedItems.length})</Text></Pressable>
        </View>
      )}
      {error && cart && <Pressable onPress={() => void loadCart()} style={styles.errorToast}><Text numberOfLines={2} style={styles.errorToastText}>{error}</Text><Text style={styles.errorToastRetry}>Thử lại</Text></Pressable>}
    </View>
  );
}

function CheckBox({ checked, disabled, onPress }: { checked: boolean; disabled?: boolean; onPress: () => void }) {
  return <Pressable accessibilityRole="checkbox" accessibilityState={{ checked, disabled }} disabled={disabled} hitSlop={7} onPress={onPress} style={[styles.checkbox, checked && styles.checkboxChecked, disabled && styles.checkboxDisabled]}>{checked && <Text style={styles.checkmark}>✓</Text>}</Pressable>;
}

const styles = StyleSheet.create({
  screen: { flex: 1, backgroundColor: '#F4F4F4' },
  topSafeArea: { backgroundColor: '#FFF' },
  header: { height: 54, flexDirection: 'row', alignItems: 'center', borderBottomWidth: StyleSheet.hairlineWidth, borderBottomColor: '#DDD', paddingHorizontal: 12 },
  backButton: { width: 40, height: 40, alignItems: 'center', justifyContent: 'center' },
  backIcon: { color: '#222', fontSize: 35, lineHeight: 36, marginTop: -3 },
  headerTitle: { flex: 1, color: '#222', fontSize: 17, fontWeight: '800', textAlign: 'center' },
  headerSpacer: { width: 40 },
  cartContent: { width: '100%', maxWidth: 720, alignSelf: 'center', paddingTop: 9 },
  shopCard: { backgroundColor: '#FFF', marginBottom: 9 },
  shopHeader: { height: 52, flexDirection: 'row', alignItems: 'center', gap: 9, paddingHorizontal: 13, borderBottomWidth: StyleSheet.hairlineWidth, borderBottomColor: '#EEE' },
  checkbox: { width: 22, height: 22, borderRadius: 11, borderWidth: 1.5, borderColor: '#BBB', alignItems: 'center', justifyContent: 'center', backgroundColor: '#FFF' },
  checkboxChecked: { borderColor: ORANGE, backgroundColor: ORANGE },
  checkboxDisabled: { opacity: 0.35 },
  checkmark: { color: '#FFF', fontSize: 13, fontWeight: '900' },
  shopLogo: { width: 28, height: 28, borderRadius: 14, backgroundColor: '#EEE' },
  shopName: { flex: 1, color: '#222', fontSize: 13, fontWeight: '800' },
  chevron: { color: '#999', fontSize: 23 },
  itemRow: { flexDirection: 'row', alignItems: 'center', gap: 10, paddingHorizontal: 13, paddingVertical: 14, borderBottomWidth: StyleSheet.hairlineWidth, borderBottomColor: '#EEE' },
  productImage: { width: 88, height: 88, borderRadius: 5, backgroundColor: '#EEE' },
  itemContent: { flex: 1, minHeight: 88 },
  itemTitleRow: { flexDirection: 'row', alignItems: 'flex-start', gap: 5 },
  itemTitlePressable: { flex: 1 },
  itemName: { color: '#292929', fontSize: 13, lineHeight: 18 },
  deleteIcon: { color: '#999', fontSize: 23, lineHeight: 20 },
  variantText: { alignSelf: 'flex-start', maxWidth: '100%', color: '#777', fontSize: 10, backgroundColor: '#F4F4F4', borderRadius: 3, paddingHorizontal: 6, paddingVertical: 4, marginTop: 5 },
  itemBottomRow: { flexDirection: 'row', alignItems: 'flex-end', justifyContent: 'space-between', marginTop: 'auto', paddingTop: 7 },
  itemPrice: { color: ORANGE, fontSize: 15, fontWeight: '800' },
  stepper: { height: 29, flexDirection: 'row', borderWidth: 1, borderColor: '#DDD', borderRadius: 4, overflow: 'hidden' },
  stepButton: { width: 30, alignItems: 'center', justifyContent: 'center', backgroundColor: '#FAFAFA' },
  stepText: { color: '#444', fontSize: 17 },
  quantityBox: { width: 36, alignItems: 'center', justifyContent: 'center', borderLeftWidth: 1, borderRightWidth: 1, borderColor: '#DDD' },
  quantityText: { color: '#333', fontSize: 11 },
  unavailableText: { color: '#C53A2A', fontSize: 11, fontWeight: '700', marginTop: 10 },
  lowStockText: { color: '#D67A14', fontSize: 9, textAlign: 'right', marginTop: 4 },
  bottomBar: { position: 'absolute', left: 0, right: 0, bottom: 0, minHeight: 70, flexDirection: 'row', alignItems: 'center', gap: 9, paddingTop: 9, paddingHorizontal: 12, backgroundColor: '#FFF', borderTopWidth: StyleSheet.hairlineWidth, borderTopColor: '#DDD' },
  selectAll: { flexDirection: 'row', alignItems: 'center', gap: 6 },
  selectAllText: { color: '#444', fontSize: 11 },
  totalBox: { flex: 1, alignItems: 'flex-end' },
  totalLabel: { color: '#777', fontSize: 10 },
  totalPrice: { color: ORANGE, fontSize: 17, fontWeight: '900', marginTop: 2 },
  checkoutButton: { height: 46, minWidth: 120, paddingHorizontal: 13, borderRadius: 6, backgroundColor: ORANGE, alignItems: 'center', justifyContent: 'center' },
  checkoutText: { color: '#FFF', fontSize: 12, fontWeight: '800' },
  disabledButton: { opacity: 0.4 },
  centerState: { flex: 1, alignItems: 'center', justifyContent: 'center', padding: 30, backgroundColor: '#FFF' },
  stateMessage: { color: '#888', fontSize: 13, lineHeight: 19, textAlign: 'center', marginTop: 10 },
  emptyCartIcon: { width: 90, height: 90, borderRadius: 45, backgroundColor: '#FFF0EC', alignItems: 'center', justifyContent: 'center' },
  emptyCartEmoji: { fontSize: 42 },
  emptyTitle: { color: '#222', fontSize: 18, fontWeight: '800', marginTop: 17 },
  shoppingButton: { backgroundColor: ORANGE, borderRadius: 7, paddingHorizontal: 22, paddingVertical: 12, marginTop: 22 },
  shoppingButtonText: { color: '#FFF', fontWeight: '800' },
  errorIcon: { width: 50, height: 50, borderRadius: 25, color: ORANGE, backgroundColor: '#FFF0EC', fontSize: 34, fontWeight: '900', textAlign: 'center' },
  retryButton: { borderRadius: 6, backgroundColor: ORANGE, paddingHorizontal: 20, paddingVertical: 11, marginTop: 20 },
  retryText: { color: '#FFF', fontWeight: '800' },
  errorToast: { position: 'absolute', left: 16, right: 16, bottom: 88, minHeight: 50, borderRadius: 9, backgroundColor: '#333', flexDirection: 'row', alignItems: 'center', paddingHorizontal: 13, paddingVertical: 9, gap: 10 },
  errorToastText: { flex: 1, color: '#FFF', fontSize: 11 },
  errorToastRetry: { color: '#FF9D83', fontSize: 11, fontWeight: '800' },
});
