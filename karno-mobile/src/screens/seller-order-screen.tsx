import { Image } from 'expo-image';
import { StatusBar } from 'expo-status-bar';
import { useCallback, useEffect, useMemo, useState } from 'react';
import { ActivityIndicator, Alert, Pressable, RefreshControl, ScrollView, StyleSheet, Text, TextInput, View } from 'react-native';
import { SafeAreaView } from 'react-native-safe-area-context';

import { getShopOrders, packOrder } from '@/services/order-api';
import { resolveProductImageUrl } from '@/services/product-api';
import { createPickupRequest, getReadyShipments, requireSellerIdentity } from '@/services/seller-api';
import type { CustomerOrder, ReadyShipment } from '@/types/order';

const ORANGE = '#EE4D2D';
const formatPrice = (value: number) => `${Math.round(value).toLocaleString('vi-VN')}đ`;

export default function SellerOrderScreen() {
  const [mode, setMode] = useState<'PACK' | 'PICKUP'>('PACK');
  const [pendingOrders, setPendingOrders] = useState<CustomerOrder[]>([]);
  const [packedOrders, setPackedOrders] = useState<CustomerOrder[]>([]);
  const [shipments, setShipments] = useState<ReadyShipment[]>([]);
  const [selectedIds, setSelectedIds] = useState<Set<string>>(new Set());
  const [windowStart, setWindowStart] = useState('08:00');
  const [windowEnd, setWindowEnd] = useState('18:00');
  const [loading, setLoading] = useState(true);
  const [refreshing, setRefreshing] = useState(false);
  const [busyId, setBusyId] = useState<string>();
  const [creating, setCreating] = useState(false);
  const [error, setError] = useState<string | null>(null);

  const loadData = useCallback(async (silent = false) => {
    if (!silent) setLoading(true);
    setError(null);
    try {
      const { shopId } = requireSellerIdentity();
      if (mode === 'PACK') {
        const response = await getShopOrders(shopId, { status: 'PENDING', page: 0, size: 50 });
        setPendingOrders(response.content);
      } else {
        const [ready, packed] = await Promise.all([
          getReadyShipments(shopId),
          getShopOrders(shopId, { status: 'PACKED', page: 0, size: 50 }),
        ]);
        setShipments(ready);
        setPackedOrders(packed.content);
        setSelectedIds((current) => new Set([...current].filter((id) => ready.some((shipment) => shipment.shipmentId === id))));
      }
    } catch (cause) {
      setError(cause instanceof Error ? cause.message : 'Không thể tải dữ liệu seller.');
    } finally {
      setLoading(false);
      setRefreshing(false);
    }
  }, [mode]);

  useEffect(() => {
    const task = setTimeout(() => void loadData(), 0);
    return () => clearTimeout(task);
  }, [loadData]);

  const packedByOrder = useMemo(() => new Map(packedOrders.map((order) => [order.id, order])), [packedOrders]);

  const handlePack = async (order: CustomerOrder) => {
    try {
      const { shopId, sellerId } = requireSellerIdentity();
      setBusyId(order.id);
      await packOrder(order.id, shopId, sellerId);
      setPendingOrders((current) => current.filter((item) => item.id !== order.id));
      Alert.alert('Đã đóng gói', 'Order đã chuyển sang PACKED. Shipment sẽ sẵn sàng tạo pickup request sau khi event được xử lý.');
    } catch (cause) {
      Alert.alert('Không thể đóng gói', cause instanceof Error ? cause.message : 'Vui lòng thử lại.');
    } finally {
      setBusyId(undefined);
    }
  };

  const toggleShipment = (shipmentId: string) => {
    setSelectedIds((current) => {
      const next = new Set(current);
      if (next.has(shipmentId)) next.delete(shipmentId);
      else next.add(shipmentId);
      return next;
    });
  };

  const handleCreatePickup = async () => {
    if (selectedIds.size === 0) return;
    if (!/^([01]\d|2[0-3]):[0-5]\d$/.test(windowStart) || !/^([01]\d|2[0-3]):[0-5]\d$/.test(windowEnd)) {
      Alert.alert('Khung giờ không hợp lệ', 'Vui lòng nhập theo định dạng HH:mm, ví dụ 08:00.');
      return;
    }
    try {
      setCreating(true);
      const identity = requireSellerIdentity();
      const response = await createPickupRequest({ ...identity, shipmentIds: [...selectedIds], pickupWindowStart: windowStart, pickupWindowEnd: windowEnd });
      Alert.alert('Tạo pickup request thành công', `Mã yêu cầu: ${response.requestCode}`);
      setSelectedIds(new Set());
      await loadData(true);
    } catch (cause) {
      Alert.alert('Không thể tạo pickup request', cause instanceof Error ? cause.message : 'Vui lòng thử lại.');
    } finally {
      setCreating(false);
    }
  };

  const refresh = () => {
    setRefreshing(true);
    void loadData(true);
  };

  return (
    <View style={styles.screen}>
      <StatusBar style="light" />
      <SafeAreaView edges={['top']} style={styles.headerSafeArea}>
        <View style={styles.header}><Text style={styles.title}>Kênh người bán</Text><Text style={styles.subtitle}>Quản lý đóng gói và lấy hàng</Text></View>
      </SafeAreaView>
      <View style={styles.tabs}>
        <Pressable onPress={() => setMode('PACK')} style={[styles.tab, mode === 'PACK' && styles.tabActive]}><Text style={[styles.tabText, mode === 'PACK' && styles.tabTextActive]}>Cần đóng gói</Text></Pressable>
        <Pressable onPress={() => setMode('PICKUP')} style={[styles.tab, mode === 'PICKUP' && styles.tabActive]}><Text style={[styles.tabText, mode === 'PICKUP' && styles.tabTextActive]}>Tạo pickup request</Text></Pressable>
      </View>
      {loading ? <View style={styles.center}><ActivityIndicator color={ORANGE} size="large" /><Text style={styles.hint}>Đang tải dữ liệu...</Text></View> : (
        <ScrollView contentContainerStyle={styles.content} refreshControl={<RefreshControl colors={[ORANGE]} onRefresh={refresh} refreshing={refreshing} tintColor={ORANGE} />}>
          {error && <Pressable onPress={() => void loadData()} style={styles.errorBox}><Text style={styles.errorText}>{error}</Text><Text style={styles.retry}>Thử lại</Text></Pressable>}
          {mode === 'PACK' ? (
            pendingOrders.length === 0 ? <EmptyState text="Không có order PENDING cần đóng gói." /> : pendingOrders.map((order) => (
              <View key={order.id} style={styles.card}>
                <View style={styles.cardHeader}><Text style={styles.orderCode}>Order #{order.id.slice(0, 8).toUpperCase()}</Text><Text style={styles.pending}>PENDING</Text></View>
                {order.items.map((item) => <View key={item.id} style={styles.itemRow}><Image contentFit="cover" source={resolveProductImageUrl(item.productImageUrl)} style={styles.image} /><View style={styles.itemInfo}><Text numberOfLines={2} style={styles.itemName}>{item.productName}</Text><Text style={styles.variant}>{item.variantName} · x{item.quantity}</Text><Text style={styles.price}>{formatPrice(item.subtotal)}</Text></View></View>)}
                <View style={styles.cardActions}><Text style={styles.total}>{order.totalItems} sản phẩm · {formatPrice(order.totalAmount)}</Text><Pressable disabled={busyId === order.id} onPress={() => void handlePack(order)} style={styles.primaryButton}>{busyId === order.id ? <ActivityIndicator color="#FFF" /> : <Text style={styles.primaryText}>Đã đóng gói</Text>}</Pressable></View>
              </View>
            ))
          ) : (
            <>
              <View style={styles.notice}><Text style={styles.noticeText}>Chỉ order PACKED có shipment đã nhận event và ở trạng thái READY_FOR_PICKUP mới xuất hiện tại đây.</Text></View>
              {shipments.length === 0 ? <EmptyState text="Chưa có shipment sẵn sàng. Hãy kéo xuống để kiểm tra lại." /> : shipments.map((shipment) => {
                const order = packedByOrder.get(shipment.orderId);
                const selected = selectedIds.has(shipment.shipmentId);
                return <Pressable key={shipment.shipmentId} onPress={() => toggleShipment(shipment.shipmentId)} style={[styles.shipmentCard, selected && styles.shipmentCardSelected]}><View style={[styles.checkbox, selected && styles.checkboxSelected]}>{selected && <Text style={styles.check}>✓</Text>}</View><View style={styles.shipmentInfo}><Text style={styles.tracking}>{shipment.trackingCode}</Text><Text style={styles.orderReference}>Order #{shipment.orderId.slice(0, 8).toUpperCase()}</Text><Text style={styles.shipmentMeta}>{order ? `${order.totalItems} sản phẩm · ${formatPrice(order.totalAmount)}` : `${shipment.totalWeightGram} g`} · {shipment.shippingMethod}</Text>{order?.items.slice(0, 2).map((item) => <Text key={item.id} numberOfLines={1} style={styles.productLine}>• {item.productName} × {item.quantity}</Text>)}</View></Pressable>;
              })}
              {shipments.length > 0 && <View style={styles.pickupForm}><Text style={styles.formTitle}>Khung giờ lấy hàng</Text><View style={styles.timeRow}><TextInput value={windowStart} onChangeText={setWindowStart} placeholder="08:00" style={styles.timeInput} /><Text style={styles.to}>đến</Text><TextInput value={windowEnd} onChangeText={setWindowEnd} placeholder="18:00" style={styles.timeInput} /></View><Pressable disabled={selectedIds.size === 0 || creating} onPress={() => void handleCreatePickup()} style={[styles.createButton, selectedIds.size === 0 && styles.disabled]}>{creating ? <ActivityIndicator color="#FFF" /> : <Text style={styles.createText}>Tạo pickup request ({selectedIds.size})</Text>}</Pressable></View>}
            </>
          )}
        </ScrollView>
      )}
    </View>
  );
}

function EmptyState({ text }: { text: string }) { return <View style={styles.empty}><Text style={styles.emptyIcon}>📦</Text><Text style={styles.hint}>{text}</Text></View>; }

const styles = StyleSheet.create({
  screen: { flex: 1, backgroundColor: '#F4F4F4' }, headerSafeArea: { backgroundColor: ORANGE }, header: { paddingHorizontal: 15, paddingBottom: 15 },
  title: { color: '#FFF', fontSize: 22, fontWeight: '900' }, subtitle: { color: '#FFE2DC', fontSize: 11, marginTop: 3 },
  tabs: { height: 50, flexDirection: 'row', backgroundColor: '#FFF' }, tab: { flex: 1, alignItems: 'center', justifyContent: 'center', borderBottomWidth: 2, borderBottomColor: 'transparent' }, tabActive: { borderBottomColor: ORANGE }, tabText: { color: '#777', fontSize: 12, fontWeight: '700' }, tabTextActive: { color: ORANGE },
  content: { padding: 10, paddingBottom: 120 }, center: { flex: 1, alignItems: 'center', justifyContent: 'center' }, hint: { color: '#888', fontSize: 12, lineHeight: 18, textAlign: 'center', marginTop: 9 },
  errorBox: { flexDirection: 'row', alignItems: 'center', gap: 10, backgroundColor: '#3B3B3B', borderRadius: 8, padding: 12, marginBottom: 10 }, errorText: { flex: 1, color: '#FFF', fontSize: 11 }, retry: { color: '#FF9D83', fontWeight: '800', fontSize: 11 },
  card: { backgroundColor: '#FFF', borderRadius: 9, marginBottom: 10, overflow: 'hidden' }, cardHeader: { height: 45, flexDirection: 'row', alignItems: 'center', justifyContent: 'space-between', paddingHorizontal: 13, borderBottomWidth: StyleSheet.hairlineWidth, borderBottomColor: '#EEE' }, orderCode: { color: '#333', fontSize: 12, fontWeight: '800' }, pending: { color: '#D67A14', fontSize: 10, fontWeight: '900' },
  itemRow: { flexDirection: 'row', gap: 10, padding: 12, borderBottomWidth: StyleSheet.hairlineWidth, borderBottomColor: '#EEE' }, image: { width: 65, height: 65, borderRadius: 5, backgroundColor: '#EEE' }, itemInfo: { flex: 1 }, itemName: { color: '#333', fontSize: 12, lineHeight: 17 }, variant: { color: '#888', fontSize: 10, marginTop: 4 }, price: { color: ORANGE, fontSize: 12, fontWeight: '800', marginTop: 'auto' },
  cardActions: { minHeight: 62, flexDirection: 'row', alignItems: 'center', justifyContent: 'space-between', gap: 8, padding: 11 }, total: { flex: 1, color: '#555', fontSize: 10 }, primaryButton: { minWidth: 112, height: 40, borderRadius: 6, backgroundColor: ORANGE, alignItems: 'center', justifyContent: 'center', paddingHorizontal: 12 }, primaryText: { color: '#FFF', fontSize: 11, fontWeight: '800' },
  notice: { borderRadius: 8, backgroundColor: '#FFF7E8', padding: 12, marginBottom: 10 }, noticeText: { color: '#8A6425', fontSize: 10, lineHeight: 16 }, empty: { alignItems: 'center', padding: 45 }, emptyIcon: { fontSize: 45 },
  shipmentCard: { flexDirection: 'row', alignItems: 'flex-start', gap: 11, backgroundColor: '#FFF', borderRadius: 9, borderWidth: 1, borderColor: 'transparent', padding: 13, marginBottom: 9 }, shipmentCardSelected: { borderColor: ORANGE, backgroundColor: '#FFF8F6' }, checkbox: { width: 23, height: 23, borderRadius: 12, borderWidth: 1.5, borderColor: '#BBB', alignItems: 'center', justifyContent: 'center' }, checkboxSelected: { borderColor: ORANGE, backgroundColor: ORANGE }, check: { color: '#FFF', fontSize: 13, fontWeight: '900' }, shipmentInfo: { flex: 1 }, tracking: { color: '#222', fontSize: 12, fontWeight: '900' }, orderReference: { color: ORANGE, fontSize: 10, fontWeight: '700', marginTop: 4 }, shipmentMeta: { color: '#777', fontSize: 10, marginTop: 5 }, productLine: { color: '#555', fontSize: 10, marginTop: 5 },
  pickupForm: { backgroundColor: '#FFF', borderRadius: 9, padding: 14, marginTop: 4 }, formTitle: { color: '#333', fontSize: 13, fontWeight: '800' }, timeRow: { flexDirection: 'row', alignItems: 'center', gap: 9, marginTop: 12 }, timeInput: { flex: 1, height: 42, borderWidth: 1, borderColor: '#DDD', borderRadius: 6, color: '#333', textAlign: 'center' }, to: { color: '#888', fontSize: 11 }, createButton: { height: 47, borderRadius: 7, backgroundColor: ORANGE, alignItems: 'center', justifyContent: 'center', marginTop: 14 }, createText: { color: '#FFF', fontSize: 12, fontWeight: '900' }, disabled: { opacity: 0.4 },
});
