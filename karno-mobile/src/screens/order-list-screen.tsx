import { Image } from 'expo-image';
import { router } from 'expo-router';
import { StatusBar } from 'expo-status-bar';
import { useCallback, useEffect, useRef, useState } from 'react';
import { ActivityIndicator, FlatList, Pressable, RefreshControl, ScrollView, StyleSheet, Text, TextInput, View } from 'react-native';
import { SafeAreaView } from 'react-native-safe-area-context';

import { getCustomerOrders } from '@/services/order-api';
import { resolveProductImageUrl } from '@/services/product-api';
import type { CustomerOrder, OrderStatus } from '@/types/order';

const ORANGE = '#EE4D2D';
const PAGE_SIZE = 10;
const STATUS_OPTIONS: { value?: OrderStatus; label: string }[] = [
  { label: 'Tất cả' },
  { value: 'PENDING', label: 'Chờ xác nhận' },
  { value: 'CONFIRMED', label: 'Đã xác nhận' },
  { value: 'PREPARING', label: 'Đang chuẩn bị' },
  { value: 'PACKED', label: 'Đã đóng gói' },
  { value: 'READY_FOR_PICKUP', label: 'Chờ lấy hàng' },
  { value: 'PICKED_UP', label: 'Đã lấy hàng' },
  { value: 'IN_TRANSIT', label: 'Đang vận chuyển' },
  { value: 'OUT_FOR_DELIVERY', label: 'Đang giao' },
  { value: 'DELIVERED', label: 'Đã giao' },
  { value: 'CANCELLED', label: 'Đã hủy' },
  { value: 'RETURN_REQUESTED', label: 'Yêu cầu trả hàng' },
  { value: 'RETURNING', label: 'Đang hoàn hàng' },
  { value: 'RETURNED', label: 'Đã hoàn hàng' },
];

const statusLabels = Object.fromEntries(STATUS_OPTIONS.filter((item) => item.value).map((item) => [item.value, item.label])) as Record<OrderStatus, string>;
const formatPrice = (value: number) => `${Math.round(value).toLocaleString('vi-VN')}đ`;

export default function OrderListScreen() {
  const [orders, setOrders] = useState<CustomerOrder[]>([]);
  const [searchText, setSearchText] = useState('');
  const [keyword, setKeyword] = useState('');
  const [status, setStatus] = useState<OrderStatus>();
  const [page, setPage] = useState(0);
  const [totalPages, setTotalPages] = useState(0);
  const [loading, setLoading] = useState(true);
  const [refreshing, setRefreshing] = useState(false);
  const [loadingMore, setLoadingMore] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const requestId = useRef(0);

  useEffect(() => {
    const task = setTimeout(() => setKeyword(searchText.trim()), 450);
    return () => clearTimeout(task);
  }, [searchText]);

  const loadFirstPage = useCallback(async () => {
    const id = ++requestId.current;
    setLoading(true);
    setError(null);
    try {
      const response = await getCustomerOrders({ status, keyword, page: 0, size: PAGE_SIZE });
      if (id !== requestId.current) return;
      setOrders(response.content);
      setPage(response.page);
      setTotalPages(response.totalPages);
    } catch (cause) {
      if (id === requestId.current) setError(cause instanceof Error ? cause.message : 'Không thể tải đơn hàng.');
    } finally {
      if (id === requestId.current) setLoading(false);
    }
  }, [keyword, status]);

  useEffect(() => {
    const task = setTimeout(() => void loadFirstPage(), 0);
    return () => clearTimeout(task);
  }, [loadFirstPage]);

  const refresh = async () => {
    setRefreshing(true);
    try {
      const response = await getCustomerOrders({ status, keyword, page: 0, size: PAGE_SIZE });
      setOrders(response.content);
      setPage(response.page);
      setTotalPages(response.totalPages);
      setError(null);
    } catch (cause) {
      setError(cause instanceof Error ? cause.message : 'Không thể làm mới đơn hàng.');
    } finally {
      setRefreshing(false);
    }
  };

  const loadMore = async () => {
    if (loading || refreshing || loadingMore || page + 1 >= totalPages) return;
    setLoadingMore(true);
    try {
      const response = await getCustomerOrders({ status, keyword, page: page + 1, size: PAGE_SIZE });
      setOrders((current) => [...current, ...response.content.filter((order) => !current.some((item) => item.id === order.id))]);
      setPage(response.page);
      setTotalPages(response.totalPages);
    } catch (cause) {
      setError(cause instanceof Error ? cause.message : 'Không thể tải thêm đơn hàng.');
    } finally {
      setLoadingMore(false);
    }
  };

  return (
    <View style={styles.screen}>
      <StatusBar style="light" />
      <SafeAreaView edges={['top']} style={styles.headerSafeArea}>
        <View style={styles.header}>
          <Text style={styles.title}>Đơn mua</Text>
          <View style={styles.searchBar}>
            <Text style={styles.searchIcon}>⌕</Text>
            <TextInput value={searchText} onChangeText={setSearchText} placeholder="Tìm theo tên sản phẩm" placeholderTextColor="#999" returnKeyType="search" style={styles.searchInput} />
            {!!searchText && <Pressable hitSlop={10} onPress={() => setSearchText('')}><Text style={styles.clear}>×</Text></Pressable>}
          </View>
        </View>
      </SafeAreaView>
      <View style={styles.filtersBox}>
        <ScrollView horizontal showsHorizontalScrollIndicator={false} contentContainerStyle={styles.filters}>
          {STATUS_OPTIONS.map((option) => {
            const selected = option.value === status;
            return <Pressable key={option.value ?? 'all'} onPress={() => setStatus(option.value)} style={[styles.filterChip, selected && styles.filterChipSelected]}><Text style={[styles.filterText, selected && styles.filterTextSelected]}>{option.label}</Text></Pressable>;
          })}
        </ScrollView>
      </View>
      {loading ? (
        <View style={styles.center}><ActivityIndicator color={ORANGE} size="large" /><Text style={styles.message}>Đang tải đơn hàng...</Text></View>
      ) : (
        <FlatList
          data={orders}
          keyExtractor={(item) => item.id}
          renderItem={({ item }) => <OrderCard order={item} />}
          contentContainerStyle={[styles.list, orders.length === 0 && styles.emptyList]}
          ListEmptyComponent={<View style={styles.center}><Text style={styles.emptyIcon}>📦</Text><Text style={styles.emptyTitle}>Chưa có đơn hàng</Text><Text style={styles.message}>{keyword || status ? 'Không có đơn phù hợp với bộ lọc.' : 'Các đơn bạn đặt sẽ xuất hiện tại đây.'}</Text></View>}
          ListFooterComponent={loadingMore ? <ActivityIndicator color={ORANGE} style={styles.footerLoader} /> : undefined}
          onEndReached={() => void loadMore()}
          onEndReachedThreshold={0.35}
          refreshControl={<RefreshControl colors={[ORANGE]} onRefresh={() => void refresh()} refreshing={refreshing} tintColor={ORANGE} />}
          showsVerticalScrollIndicator={false}
        />
      )}
      {error && !loading && <Pressable onPress={() => void loadFirstPage()} style={styles.errorToast}><Text numberOfLines={2} style={styles.errorText}>{error}</Text><Text style={styles.retry}>Thử lại</Text></Pressable>}
    </View>
  );
}

function OrderCard({ order }: { order: CustomerOrder }) {
  return (
    <View style={styles.card}>
      <View style={styles.cardHeader}><Text numberOfLines={1} style={styles.shopName}>🏬 {order.shopName}</Text><Text style={styles.status}>{statusLabels[order.status]}</Text></View>
      {order.items.map((item) => (
        <Pressable key={item.id} onPress={() => router.push({ pathname: '/products/[productId]', params: { productId: item.productId } })} style={styles.itemRow}>
          <Image contentFit="cover" source={resolveProductImageUrl(item.productImageUrl)} style={styles.itemImage} />
          <View style={styles.itemInfo}><Text numberOfLines={2} style={styles.itemName}>{item.productName}</Text><Text numberOfLines={1} style={styles.variant}>{item.variantName}</Text><View style={styles.itemPriceRow}><Text style={styles.quantity}>x{item.quantity}</Text><Text style={styles.itemPrice}>{formatPrice(item.unitPrice)}</Text></View></View>
        </Pressable>
      ))}
      <View style={styles.cardFooter}>
        <Text style={styles.date}>{new Date(order.createdAt).toLocaleString('vi-VN')}</Text>
        <View style={styles.totalRow}><Text style={styles.totalLabel}>{order.totalItems} sản phẩm · Thành tiền</Text><Text style={styles.total}>{formatPrice(order.totalAmount)}</Text></View>
      </View>
    </View>
  );
}

const styles = StyleSheet.create({
  screen: { flex: 1, backgroundColor: '#F4F4F4' },
  headerSafeArea: { backgroundColor: ORANGE },
  header: { paddingHorizontal: 14, paddingBottom: 14 },
  title: { color: '#FFF', fontSize: 23, fontWeight: '900', marginBottom: 12 },
  searchBar: { height: 44, flexDirection: 'row', alignItems: 'center', backgroundColor: '#FFF', borderRadius: 8, paddingHorizontal: 12 },
  searchIcon: { color: ORANGE, fontSize: 25, marginRight: 7 },
  searchInput: { flex: 1, color: '#222', fontSize: 13, paddingVertical: 0 },
  clear: { color: '#999', fontSize: 23 },
  filtersBox: { backgroundColor: '#FFF', borderBottomWidth: StyleSheet.hairlineWidth, borderBottomColor: '#DDD' },
  filters: { gap: 8, paddingHorizontal: 12, paddingVertical: 11 },
  filterChip: { borderRadius: 17, borderWidth: 1, borderColor: '#DDD', paddingHorizontal: 13, paddingVertical: 7, backgroundColor: '#FFF' },
  filterChipSelected: { borderColor: ORANGE, backgroundColor: '#FFF0EC' },
  filterText: { color: '#666', fontSize: 11, fontWeight: '600' },
  filterTextSelected: { color: ORANGE, fontWeight: '800' },
  list: { padding: 10, paddingBottom: 110 },
  emptyList: { flexGrow: 1 },
  card: { backgroundColor: '#FFF', borderRadius: 9, marginBottom: 10, overflow: 'hidden' },
  cardHeader: { minHeight: 48, flexDirection: 'row', alignItems: 'center', justifyContent: 'space-between', gap: 10, paddingHorizontal: 13, borderBottomWidth: StyleSheet.hairlineWidth, borderBottomColor: '#EEE' },
  shopName: { flex: 1, color: '#222', fontSize: 13, fontWeight: '800' },
  status: { color: ORANGE, fontSize: 10, fontWeight: '800', textTransform: 'uppercase' },
  itemRow: { flexDirection: 'row', gap: 10, padding: 13, borderBottomWidth: StyleSheet.hairlineWidth, borderBottomColor: '#EEE' },
  itemImage: { width: 76, height: 76, borderRadius: 5, backgroundColor: '#EEE' },
  itemInfo: { flex: 1, minHeight: 76 },
  itemName: { color: '#292929', fontSize: 13, lineHeight: 18 },
  variant: { color: '#888', fontSize: 10, marginTop: 5 },
  itemPriceRow: { flexDirection: 'row', justifyContent: 'space-between', marginTop: 'auto' },
  quantity: { color: '#777', fontSize: 11 },
  itemPrice: { color: '#333', fontSize: 12 },
  cardFooter: { alignItems: 'flex-end', padding: 13 },
  date: { alignSelf: 'flex-start', color: '#999', fontSize: 9 },
  totalRow: { flexDirection: 'row', alignItems: 'center', gap: 7, marginTop: 9 },
  totalLabel: { color: '#666', fontSize: 11 },
  total: { color: ORANGE, fontSize: 17, fontWeight: '900' },
  center: { flex: 1, alignItems: 'center', justifyContent: 'center', padding: 30 },
  message: { color: '#888', fontSize: 12, lineHeight: 18, textAlign: 'center', marginTop: 10 },
  emptyIcon: { fontSize: 50 },
  emptyTitle: { color: '#333', fontSize: 17, fontWeight: '800', marginTop: 12 },
  footerLoader: { marginVertical: 20 },
  errorToast: { position: 'absolute', left: 15, right: 15, bottom: 90, minHeight: 50, borderRadius: 9, backgroundColor: '#333', flexDirection: 'row', alignItems: 'center', gap: 12, padding: 12 },
  errorText: { flex: 1, color: '#FFF', fontSize: 11 },
  retry: { color: '#FF9D83', fontSize: 11, fontWeight: '800' },
});
