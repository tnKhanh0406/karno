import { Image } from 'expo-image';
import { router, useLocalSearchParams } from 'expo-router';
import { StatusBar } from 'expo-status-bar';
import { useCallback, useEffect, useMemo, useRef, useState } from 'react';
import { ActivityIndicator, Alert, Pressable, ScrollView, StyleSheet, Text, TextInput, View } from 'react-native';
import { SafeAreaView, useSafeAreaInsets } from 'react-native-safe-area-context';

import { createIdempotencyKey, createOrder, getCheckoutStatus, previewCheckout } from '@/services/checkout-api';
import { resolveProductImageUrl } from '@/services/product-api';
import type { CheckoutPreview, CheckoutStatus, PaymentMethod } from '@/types/checkout';

const ORANGE = '#EE4D2D';
const formatPrice = (price: number) => `${Math.round(price).toLocaleString('vi-VN')}đ`;
const wait = (milliseconds: number) => new Promise((resolve) => setTimeout(resolve, milliseconds));

const statusMessages: Record<CheckoutStatus, string> = {
  PROCESSING: 'Đang giữ tồn kho...',
  INVENTORY_RESERVED: 'Đã giữ hàng, đang tạo đơn...',
  ORDER_COMMITTED: 'Đơn hàng đã được ghi nhận...',
  COMPENSATING: 'Đang hoàn tác giao dịch...',
  COMPLETED: 'Đặt hàng thành công',
  FAILED: 'Đặt hàng thất bại',
};

export default function CheckoutScreen() {
  const { cartItemIds: encodedCartItemIds } = useLocalSearchParams<{ cartItemIds?: string }>();
  const insets = useSafeAreaInsets();
  const cartItemIds = useMemo(
    () => [...new Set((encodedCartItemIds ?? '').split(',').map((id) => id.trim()).filter(Boolean))],
    [encodedCartItemIds],
  );
  const idempotencyKey = useRef(createIdempotencyKey());
  const [preview, setPreview] = useState<CheckoutPreview | null>(null);
  const [paymentMethod, setPaymentMethod] = useState<PaymentMethod>('COD');
  const [note, setNote] = useState('');
  const [loading, setLoading] = useState(true);
  const [submitting, setSubmitting] = useState(false);
  const [processingMessage, setProcessingMessage] = useState('Đang gửi yêu cầu đặt hàng...');
  const [error, setError] = useState<string | null>(null);

  const loadPreview = useCallback(async (signal?: AbortSignal) => {
    if (cartItemIds.length === 0) {
      setError('Bạn chưa chọn sản phẩm để thanh toán.');
      setLoading(false);
      return;
    }
    setLoading(true);
    setError(null);
    try {
      setPreview(await previewCheckout(cartItemIds, signal));
      idempotencyKey.current = createIdempotencyKey();
    } catch (cause) {
      if (cause instanceof Error && cause.name === 'AbortError') return;
      setError(cause instanceof Error ? cause.message : 'Không thể chuẩn bị checkout.');
    } finally {
      if (!signal?.aborted) setLoading(false);
    }
  }, [cartItemIds]);

  useEffect(() => {
    const controller = new AbortController();
    const task = setTimeout(() => void loadPreview(controller.signal), 0);
    return () => {
      clearTimeout(task);
      controller.abort();
    };
  }, [loadPreview]);

  const pollCheckout = async (checkoutId: string, initialStatus: CheckoutStatus) => {
    let status = initialStatus;
    for (let attempt = 0; attempt < 20; attempt += 1) {
      setProcessingMessage(statusMessages[status]);
      if (status === 'COMPLETED') return { completed: true, failureCode: null };
      if (status === 'FAILED') return { completed: false, failureCode: 'ORDER_CREATION_FAILED' };
      await wait(1500);
      const response = await getCheckoutStatus(checkoutId);
      status = response.status;
      if (status === 'FAILED') return { completed: false, failureCode: response.failureCode };
    }
    return null;
  };

  const handlePlaceOrder = async () => {
    if (!preview || submitting) return;
    setSubmitting(true);
    setProcessingMessage('Đang xác nhận giá và phí vận chuyển...');
    try {
      const started = await createOrder(preview, cartItemIds, paymentMethod, note, idempotencyKey.current);
      const result = await pollCheckout(started.checkoutId, started.status);
      if (result?.completed) {
        Alert.alert('Đặt hàng thành công', 'Đơn hàng của bạn đã được tạo và người bán sẽ sớm chuẩn bị hàng.', [
          { text: 'Tiếp tục mua sắm', onPress: () => router.replace('/') },
        ]);
      } else if (result) {
        Alert.alert('Đặt hàng thất bại', result.failureCode ? `Mã lỗi: ${result.failureCode}` : 'Hệ thống không thể tạo đơn hàng.');
        idempotencyKey.current = createIdempotencyKey();
      } else {
        Alert.alert('Đơn hàng đang được xử lý', 'Yêu cầu đã được ghi nhận. Hệ thống vẫn đang hoàn tất đơn hàng, vui lòng kiểm tra lại sau.');
      }
    } catch (cause) {
      const message = cause instanceof Error ? cause.message : 'Vui lòng thử lại.';
      Alert.alert('Không thể đặt hàng', message, [
        { text: 'Đóng', style: 'cancel' },
        { text: 'Cập nhật checkout', onPress: () => void loadPreview() },
      ]);
    } finally {
      setSubmitting(false);
    }
  };

  if (loading) {
    return <View style={styles.centerState}><StatusBar style="dark" /><ActivityIndicator color={ORANGE} size="large" /><Text style={styles.stateMessage}>Đang kiểm tra giá, tồn kho và phí vận chuyển...</Text></View>;
  }

  if (!preview || error) {
    return (
      <SafeAreaView style={styles.centerState}>
        <StatusBar style="dark" />
        <Text style={styles.errorIcon}>!</Text><Text style={styles.errorTitle}>Không thể mở thanh toán</Text><Text style={styles.stateMessage}>{error}</Text>
        <View style={styles.errorActions}><Pressable onPress={() => router.back()} style={styles.secondaryButton}><Text style={styles.secondaryButtonText}>Quay lại</Text></Pressable><Pressable onPress={() => void loadPreview()} style={styles.primaryButton}><Text style={styles.primaryButtonText}>Thử lại</Text></Pressable></View>
      </SafeAreaView>
    );
  }

  return (
    <View style={styles.screen}>
      <StatusBar style="dark" />
      <SafeAreaView edges={['top']} style={styles.topSafeArea}>
        <View style={styles.header}><Pressable accessibilityLabel="Quay lại" hitSlop={10} onPress={() => router.back()} style={styles.backButton}><Text style={styles.backIcon}>‹</Text></Pressable><Text style={styles.headerTitle}>Thanh toán</Text><View style={styles.headerSpacer} /></View>
      </SafeAreaView>

      <ScrollView contentContainerStyle={[styles.content, { paddingBottom: 94 + insets.bottom }]} showsVerticalScrollIndicator={false}>
        <Pressable onPress={() => Alert.alert('Địa chỉ nhận hàng', 'Backend hiện chưa có API danh sách địa chỉ. Address ID đang được lấy từ cấu hình phát triển.')} style={styles.addressCard}>
          <View style={styles.addressAccent} />
          <View style={styles.addressIcon}><Text>⌖</Text></View>
          <View style={styles.addressContent}>
            <Text style={styles.addressLabel}>Địa chỉ nhận hàng</Text>
            <Text style={styles.receiver}>{preview.receiver.name}  |  {preview.receiver.phone}</Text>
            <Text style={styles.address}>{preview.receiver.address}</Text>
          </View>
          <Text style={styles.chevron}>›</Text>
        </Pressable>

        {preview.shops.map((shop) => (
          <View key={shop.shopId} style={styles.shopCard}>
            <View style={styles.shopHeader}><Text style={styles.shopIcon}>▣</Text><Text style={styles.shopName}>{shop.shopName}</Text></View>
            {shop.items.map((item) => (
              <View key={item.cartItemId} style={styles.itemRow}>
                <Image contentFit="cover" source={resolveProductImageUrl(item.productImageUrl)} style={styles.itemImage} />
                <View style={styles.itemContent}><Text numberOfLines={2} style={styles.itemName}>{item.productName}</Text><Text numberOfLines={1} style={styles.variantName}>{item.variantName || item.sku}</Text><View style={styles.itemMeta}><Text style={styles.itemPrice}>{formatPrice(item.unitPrice)}</Text><Text style={styles.itemQuantity}>×{item.quantity}</Text></View></View>
              </View>
            ))}
            <View style={styles.shippingBlock}>
              <View style={styles.shippingTitleRow}><View><Text style={styles.shippingLabel}>Phương thức vận chuyển</Text><Text style={styles.shippingMethod}>{shop.shipping.shippingMethod === 'STANDARD' ? 'Giao hàng tiêu chuẩn' : shop.shipping.shippingMethod}</Text></View><Text style={styles.shippingFee}>{formatPrice(shop.shipping.shippingFee)}</Text></View>
              <View style={styles.deliveryRow}><Text style={styles.deliveryIcon}>🚚</Text><Text style={styles.deliveryText}>Dự kiến giao vào {new Date(shop.shipping.estimatedDeliveryAt).toLocaleDateString('vi-VN', { day: '2-digit', month: '2-digit', year: 'numeric' })}</Text></View>
            </View>
            <View style={styles.shopTotal}><Text style={styles.shopTotalLabel}>Tổng số tiền ({shop.items.length} sản phẩm)</Text><Text style={styles.shopTotalValue}>{formatPrice(shop.totalAmount)}</Text></View>
          </View>
        ))}

        <View style={styles.section}>
          <Text style={styles.sectionTitle}>Phương thức thanh toán</Text>
          <PaymentOption selected={paymentMethod === 'COD'} icon="💵" title="Thanh toán khi nhận hàng" subtitle="Thanh toán cho shipper khi đơn hàng được giao" onPress={() => setPaymentMethod('COD')} />
          <PaymentOption selected={paymentMethod === 'ONLINE'} icon="💳" title="Thanh toán trực tuyến" subtitle="Ghi nhận phương thức ONLINE cho đơn hàng" onPress={() => setPaymentMethod('ONLINE')} />
          {paymentMethod === 'ONLINE' && <Text style={styles.paymentWarning}>Backend chưa trả payment URL; lựa chọn này mới ghi nhận phương thức thanh toán trên order.</Text>}
        </View>

        <View style={styles.section}>
          <Text style={styles.sectionTitle}>Lời nhắn cho người bán</Text>
          <TextInput value={note} onChangeText={setNote} placeholder="Ví dụ: đóng gói cẩn thận giúp mình" placeholderTextColor="#AAA" maxLength={300} multiline style={styles.noteInput} />
          <Text style={styles.characterCount}>{note.length}/300</Text>
        </View>

        <View style={styles.section}>
          <Text style={styles.sectionTitle}>Chi tiết thanh toán</Text>
          <PriceRow label="Tổng tiền hàng" value={preview.subtotal} />
          <PriceRow label="Tổng phí vận chuyển" value={preview.shippingFee} />
          <View style={styles.grandTotalRow}><Text style={styles.grandTotalLabel}>Tổng thanh toán</Text><Text style={styles.grandTotalValue}>{formatPrice(preview.totalAmount)}</Text></View>
        </View>
      </ScrollView>

      <View style={[styles.bottomBar, { paddingBottom: Math.max(insets.bottom, 10) }]}>
        <View style={styles.bottomTotal}><Text style={styles.bottomTotalLabel}>Tổng thanh toán</Text><Text style={styles.bottomTotalValue}>{formatPrice(preview.totalAmount)}</Text></View>
        <Pressable disabled={submitting} onPress={() => void handlePlaceOrder()} style={[styles.placeOrderButton, submitting && styles.disabledButton]}>{submitting ? <View style={styles.submittingContent}><ActivityIndicator color="#FFF" size="small" /><Text numberOfLines={1} style={styles.submittingText}>{processingMessage}</Text></View> : <Text style={styles.placeOrderText}>Đặt hàng</Text>}</Pressable>
      </View>
    </View>
  );
}

function PaymentOption({ selected, icon, title, subtitle, onPress }: { selected: boolean; icon: string; title: string; subtitle: string; onPress: () => void }) {
  return <Pressable onPress={onPress} style={styles.paymentOption}><Text style={styles.paymentIcon}>{icon}</Text><View style={styles.paymentContent}><Text style={styles.paymentTitle}>{title}</Text><Text style={styles.paymentSubtitle}>{subtitle}</Text></View><View style={[styles.radio, selected && styles.radioSelected]}>{selected && <View style={styles.radioDot} />}</View></Pressable>;
}

function PriceRow({ label, value }: { label: string; value: number }) {
  return <View style={styles.priceRow}><Text style={styles.priceLabel}>{label}</Text><Text style={styles.priceValue}>{formatPrice(value)}</Text></View>;
}

const styles = StyleSheet.create({
  screen: { flex: 1, backgroundColor: '#F4F4F4' },
  topSafeArea: { backgroundColor: '#FFF' },
  header: { height: 54, flexDirection: 'row', alignItems: 'center', borderBottomWidth: StyleSheet.hairlineWidth, borderBottomColor: '#DDD', paddingHorizontal: 12 },
  backButton: { width: 40, height: 40, alignItems: 'center', justifyContent: 'center' },
  backIcon: { color: '#222', fontSize: 35, lineHeight: 36, marginTop: -3 },
  headerTitle: { flex: 1, color: '#222', fontSize: 17, fontWeight: '800', textAlign: 'center' },
  headerSpacer: { width: 40 },
  content: { width: '100%', maxWidth: 720, alignSelf: 'center' },
  addressCard: { minHeight: 108, flexDirection: 'row', alignItems: 'center', backgroundColor: '#FFF', paddingHorizontal: 15, paddingVertical: 15, marginBottom: 9, overflow: 'hidden' },
  addressAccent: { position: 'absolute', top: 0, left: 0, right: 0, height: 3, backgroundColor: ORANGE },
  addressIcon: { width: 35, height: 35, borderRadius: 18, backgroundColor: '#FFF0EC', alignItems: 'center', justifyContent: 'center', marginRight: 11 },
  addressContent: { flex: 1 },
  addressLabel: { color: ORANGE, fontSize: 11, fontWeight: '800', marginBottom: 5 },
  receiver: { color: '#222', fontSize: 13, fontWeight: '800' },
  address: { color: '#666', fontSize: 12, lineHeight: 18, marginTop: 4 },
  chevron: { color: '#AAA', fontSize: 24, marginLeft: 7 },
  shopCard: { backgroundColor: '#FFF', marginBottom: 9 },
  shopHeader: { height: 50, flexDirection: 'row', alignItems: 'center', gap: 8, paddingHorizontal: 15, borderBottomWidth: StyleSheet.hairlineWidth, borderBottomColor: '#EEE' },
  shopIcon: { color: ORANGE, fontSize: 17 },
  shopName: { color: '#222', fontSize: 13, fontWeight: '800' },
  itemRow: { flexDirection: 'row', gap: 11, paddingHorizontal: 15, paddingVertical: 13, borderBottomWidth: StyleSheet.hairlineWidth, borderBottomColor: '#EEE' },
  itemImage: { width: 78, height: 78, borderRadius: 5, backgroundColor: '#EEE' },
  itemContent: { flex: 1, minHeight: 78 },
  itemName: { color: '#292929', fontSize: 12, lineHeight: 17 },
  variantName: { color: '#888', fontSize: 10, marginTop: 5 },
  itemMeta: { flexDirection: 'row', justifyContent: 'space-between', marginTop: 'auto' },
  itemPrice: { color: ORANGE, fontSize: 13, fontWeight: '800' },
  itemQuantity: { color: '#666', fontSize: 11 },
  shippingBlock: { padding: 14, backgroundColor: '#F6FFFB', borderBottomWidth: StyleSheet.hairlineWidth, borderBottomColor: '#E0EEE8' },
  shippingTitleRow: { flexDirection: 'row', justifyContent: 'space-between', alignItems: 'center' },
  shippingLabel: { color: '#555', fontSize: 11 },
  shippingMethod: { color: '#16835E', fontSize: 12, fontWeight: '800', marginTop: 4 },
  shippingFee: { color: '#333', fontSize: 12, fontWeight: '700' },
  deliveryRow: { flexDirection: 'row', alignItems: 'center', gap: 7, marginTop: 10 },
  deliveryIcon: { fontSize: 14 },
  deliveryText: { color: '#5E8074', fontSize: 10 },
  shopTotal: { flexDirection: 'row', justifyContent: 'flex-end', alignItems: 'center', gap: 10, paddingHorizontal: 15, paddingVertical: 14 },
  shopTotalLabel: { color: '#666', fontSize: 11 },
  shopTotalValue: { color: ORANGE, fontSize: 15, fontWeight: '900' },
  section: { backgroundColor: '#FFF', marginBottom: 9, padding: 15 },
  sectionTitle: { color: '#222', fontSize: 15, fontWeight: '800', marginBottom: 10 },
  paymentOption: { minHeight: 65, flexDirection: 'row', alignItems: 'center', borderTopWidth: StyleSheet.hairlineWidth, borderTopColor: '#EEE', paddingVertical: 10 },
  paymentIcon: { width: 38, fontSize: 22 },
  paymentContent: { flex: 1 },
  paymentTitle: { color: '#333', fontSize: 12, fontWeight: '700' },
  paymentSubtitle: { color: '#888', fontSize: 10, marginTop: 4 },
  radio: { width: 21, height: 21, borderRadius: 11, borderWidth: 1.5, borderColor: '#BBB', alignItems: 'center', justifyContent: 'center' },
  radioSelected: { borderColor: ORANGE },
  radioDot: { width: 11, height: 11, borderRadius: 6, backgroundColor: ORANGE },
  paymentWarning: { color: '#B46D18', fontSize: 10, lineHeight: 15, backgroundColor: '#FFF8EB', borderRadius: 5, padding: 9 },
  noteInput: { minHeight: 76, borderRadius: 7, backgroundColor: '#F7F7F7', color: '#333', fontSize: 12, lineHeight: 18, padding: 11, textAlignVertical: 'top' },
  characterCount: { color: '#AAA', fontSize: 9, textAlign: 'right', marginTop: 5 },
  priceRow: { flexDirection: 'row', justifyContent: 'space-between', paddingVertical: 7 },
  priceLabel: { color: '#666', fontSize: 12 },
  priceValue: { color: '#333', fontSize: 12 },
  grandTotalRow: { flexDirection: 'row', justifyContent: 'space-between', borderTopWidth: StyleSheet.hairlineWidth, borderTopColor: '#DDD', marginTop: 7, paddingTop: 15 },
  grandTotalLabel: { color: '#222', fontSize: 14, fontWeight: '800' },
  grandTotalValue: { color: ORANGE, fontSize: 19, fontWeight: '900' },
  bottomBar: { position: 'absolute', left: 0, right: 0, bottom: 0, minHeight: 72, flexDirection: 'row', alignItems: 'center', gap: 12, paddingTop: 9, paddingHorizontal: 13, backgroundColor: '#FFF', borderTopWidth: StyleSheet.hairlineWidth, borderTopColor: '#DDD' },
  bottomTotal: { flex: 1, alignItems: 'flex-end' },
  bottomTotalLabel: { color: '#777', fontSize: 10 },
  bottomTotalValue: { color: ORANGE, fontSize: 18, fontWeight: '900', marginTop: 2 },
  placeOrderButton: { width: 145, height: 48, borderRadius: 6, backgroundColor: ORANGE, alignItems: 'center', justifyContent: 'center', paddingHorizontal: 9 },
  placeOrderText: { color: '#FFF', fontSize: 14, fontWeight: '900' },
  submittingContent: { alignItems: 'center', gap: 3 },
  submittingText: { color: '#FFF', fontSize: 8, maxWidth: 125 },
  disabledButton: { opacity: 0.7 },
  centerState: { flex: 1, alignItems: 'center', justifyContent: 'center', padding: 30, backgroundColor: '#FFF' },
  stateMessage: { color: '#777', fontSize: 13, lineHeight: 19, textAlign: 'center', marginTop: 11 },
  errorIcon: { width: 52, height: 52, borderRadius: 26, color: ORANGE, backgroundColor: '#FFF0EC', fontSize: 35, fontWeight: '900', textAlign: 'center' },
  errorTitle: { color: '#222', fontSize: 18, fontWeight: '800', marginTop: 16 },
  errorActions: { flexDirection: 'row', gap: 10, marginTop: 22 },
  secondaryButton: { borderWidth: 1, borderColor: '#CCC', borderRadius: 7, paddingHorizontal: 18, paddingVertical: 11 },
  secondaryButtonText: { color: '#555', fontWeight: '700' },
  primaryButton: { borderRadius: 7, backgroundColor: ORANGE, paddingHorizontal: 18, paddingVertical: 11 },
  primaryButtonText: { color: '#FFF', fontWeight: '700' },
});
