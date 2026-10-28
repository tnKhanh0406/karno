import { StatusBar } from 'expo-status-bar';
import { router } from 'expo-router';
import { useCallback, useEffect, useRef, useState } from 'react';
import { ActivityIndicator, FlatList, Keyboard, Pressable, RefreshControl, StyleSheet, Text, TextInput, View } from 'react-native';
import { SafeAreaView } from 'react-native-safe-area-context';

import { ProductCard } from '@/components/product-card';
import { ProductFilterModal } from '@/components/product-filter-modal';
import { getCategories, getProducts } from '@/services/product-api';
import type { ProductCategory, ProductFilters, ProductListItem, ProductSort } from '@/types/product';

const PAGE_SIZE = 20;
const SORT_OPTIONS: { label: string; value: ProductSort }[] = [
  { label: 'Mới nhất', value: 'NEWEST' },
  { label: 'Bán chạy', value: 'SOLD_DESC' },
  { label: 'Đánh giá', value: 'RATING_DESC' },
  { label: 'Giá thấp', value: 'PRICE_ASC' },
  { label: 'Giá cao', value: 'PRICE_DESC' },
];

export default function ProductListScreen() {
  const [products, setProducts] = useState<ProductListItem[]>([]);
  const [searchText, setSearchText] = useState('');
  const [keyword, setKeyword] = useState('');
  const [sort, setSort] = useState<ProductSort>('NEWEST');
  const [filters, setFilters] = useState<ProductFilters>({});
  const [categories, setCategories] = useState<ProductCategory[]>([]);
  const [filterVisible, setFilterVisible] = useState(false);
  const [page, setPage] = useState(0);
  const [totalPages, setTotalPages] = useState(0);
  const [loading, setLoading] = useState(true);
  const [refreshing, setRefreshing] = useState(false);
  const [loadingMore, setLoadingMore] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const requestId = useRef(0);

  const loadFirstPage = useCallback(async () => {
    const id = ++requestId.current;
    setLoading(true);
    setError(null);
    try {
      const response = await getProducts({ keyword, sort, ...filters, page: 0, size: PAGE_SIZE });
      if (id !== requestId.current) return;
      setProducts(response.content);
      setPage(response.page);
      setTotalPages(response.totalPages);
    } catch (cause) {
      if (id === requestId.current) setError(cause instanceof Error ? cause.message : 'Không thể tải sản phẩm.');
    } finally {
      if (id === requestId.current) setLoading(false);
    }
  }, [filters, keyword, sort]);

  useEffect(() => {
    const task = setTimeout(() => void loadFirstPage(), 0);
    return () => clearTimeout(task);
  }, [loadFirstPage]);

  useEffect(() => {
    const controller = new AbortController();
    const task = setTimeout(() => {
      void getCategories(controller.signal)
        .then(setCategories)
        .catch((cause) => {
          if (!(cause instanceof Error && cause.name === 'AbortError')) setCategories([]);
        });
    }, 0);
    return () => {
      clearTimeout(task);
      controller.abort();
    };
  }, []);

  const handleSearch = () => {
    Keyboard.dismiss();
    setKeyword(searchText.trim());
  };

  const handleRefresh = async () => {
    setRefreshing(true);
    setError(null);
    try {
      const response = await getProducts({ keyword, sort, ...filters, page: 0, size: PAGE_SIZE });
      setProducts(response.content);
      setPage(response.page);
      setTotalPages(response.totalPages);
    } catch (cause) {
      setError(cause instanceof Error ? cause.message : 'Không thể làm mới sản phẩm.');
    } finally {
      setRefreshing(false);
    }
  };

  const handleLoadMore = async () => {
    if (loading || refreshing || loadingMore || page + 1 >= totalPages) return;
    setLoadingMore(true);
    try {
      const response = await getProducts({ keyword, sort, ...filters, page: page + 1, size: PAGE_SIZE });
      setProducts((current) => {
        const ids = new Set(current.map((product) => product.id));
        return [...current, ...response.content.filter((product) => !ids.has(product.id))];
      });
      setPage(response.page);
      setTotalPages(response.totalPages);
    } catch (cause) {
      setError(cause instanceof Error ? cause.message : 'Không thể tải thêm sản phẩm.');
    } finally {
      setLoadingMore(false);
    }
  };

  const activeFilterCount = Object.values(filters).filter((value) => value !== undefined).length;

  const listHeader = (
    <>
      <View style={styles.promoBanner}>
        <View>
          <Text style={styles.promoEyebrow}>KARNO MALL</Text>
          <Text style={styles.promoTitle}>Mua sắm dễ dàng,{`\n`}giao hàng tận nơi</Text>
        </View>
      </View>
      <View style={styles.sectionHeading}>
        <View>
          <Text style={styles.sectionTitle}>{keyword ? `Kết quả cho “${keyword}”` : 'Gợi ý hôm nay'}</Text>
          {!loading && <Text style={styles.resultCount}>{products.length} sản phẩm đang hiển thị</Text>}
        </View>
        <Pressable accessibilityRole="button" onPress={() => setFilterVisible(true)} style={[styles.filterButton, activeFilterCount > 0 && styles.filterButtonActive]}>
          <Text style={[styles.filterButtonText, activeFilterCount > 0 && styles.filterButtonTextActive]}>☷ Lọc{activeFilterCount > 0 ? ` (${activeFilterCount})` : ''}</Text>
        </Pressable>
      </View>
      <FlatList
        data={SORT_OPTIONS}
        horizontal
        keyExtractor={(item) => item.value}
        showsHorizontalScrollIndicator={false}
        contentContainerStyle={styles.sortList}
        renderItem={({ item }) => {
          const selected = item.value === sort;
          return (
            <Pressable accessibilityRole="button" accessibilityState={{ selected }} onPress={() => setSort(item.value)} style={[styles.sortChip, selected && styles.sortChipSelected]}>
              <Text style={[styles.sortChipText, selected && styles.sortChipTextSelected]}>{item.label}</Text>
            </Pressable>
          );
        }}
      />
    </>
  );

  return (
    <View style={styles.screen}>
      <StatusBar style="light" />
      <SafeAreaView edges={['top']} style={styles.headerSafeArea}>
        <View style={styles.header}>
          <View style={styles.brandRow}>
            <View><Text style={styles.brand}>karno</Text><Text style={styles.brandCaption}>Mua gì cũng có</Text></View>
            <View style={styles.headerActions}>
              <Pressable accessibilityLabel="Thông báo" style={styles.headerAction}><Text style={styles.headerActionIcon}>♡</Text></Pressable>
              <Pressable accessibilityLabel="Giỏ hàng" onPress={() => router.push('/cart')} style={styles.headerAction}><Text style={styles.headerActionIcon}>🛒</Text></Pressable>
            </View>
          </View>
          <View style={styles.searchBar}>
            <Text style={styles.searchIcon}>⌕</Text>
            <TextInput accessibilityLabel="Tìm kiếm sản phẩm" value={searchText} onChangeText={setSearchText} onSubmitEditing={handleSearch} placeholder="Tìm sản phẩm bạn muốn..." placeholderTextColor="#9A9A9A" returnKeyType="search" style={styles.searchInput} />
            {searchText.length > 0 && <Pressable accessibilityLabel="Xóa từ khóa" hitSlop={10} onPress={() => setSearchText('')}><Text style={styles.clearSearch}>×</Text></Pressable>}
          </View>
        </View>
      </SafeAreaView>

      {loading ? (
        <View style={styles.centerState}><ActivityIndicator color="#EE4D2D" size="large" /><Text style={styles.stateMessage}>Đang tìm sản phẩm phù hợp...</Text></View>
      ) : (
        <FlatList
          data={products}
          keyExtractor={(item) => item.id}
          numColumns={2}
          renderItem={({ item }) => <ProductCard product={item} />}
          ListHeaderComponent={listHeader}
          ListEmptyComponent={<View style={styles.emptyState}><Text style={styles.emptyIcon}>⌕</Text><Text style={styles.emptyTitle}>Chưa tìm thấy sản phẩm</Text><Text style={styles.stateMessage}>Thử đổi từ khóa hoặc cách sắp xếp nhé.</Text></View>}
          ListFooterComponent={loadingMore ? <ActivityIndicator color="#EE4D2D" style={styles.footerLoader} /> : undefined}
          columnWrapperStyle={styles.productRow}
          contentContainerStyle={styles.productList}
          onEndReached={() => void handleLoadMore()}
          onEndReachedThreshold={0.4}
          refreshControl={<RefreshControl colors={['#EE4D2D']} onRefresh={() => void handleRefresh()} refreshing={refreshing} tintColor="#EE4D2D" />}
          showsVerticalScrollIndicator={false}
        />
      )}
      {error && !loading && <Pressable accessibilityRole="button" onPress={() => void loadFirstPage()} style={styles.errorToast}><Text numberOfLines={2} style={styles.errorText}>{error}</Text><Text style={styles.retryText}>Thử lại</Text></Pressable>}
      {filterVisible && (
        <ProductFilterModal
          categories={categories}
          initialFilters={filters}
          onApply={(nextFilters) => {
            setFilters(nextFilters);
            setFilterVisible(false);
          }}
          onClose={() => setFilterVisible(false)}
        />
      )}
    </View>
  );
}

const styles = StyleSheet.create({
  screen: { flex: 1, backgroundColor: '#F5F5F5' },
  headerSafeArea: { backgroundColor: '#EE4D2D' },
  header: { paddingHorizontal: 14, paddingBottom: 14 },
  brandRow: { flexDirection: 'row', alignItems: 'center', justifyContent: 'space-between', marginBottom: 12 },
  brand: { color: '#FFF', fontSize: 28, fontWeight: '900', letterSpacing: -1.5 },
  brandCaption: { color: '#FFE2DC', fontSize: 11, fontWeight: '600', marginTop: -2 },
  headerActions: { flexDirection: 'row', gap: 8 },
  headerAction: { width: 38, height: 38, borderRadius: 19, backgroundColor: 'rgba(255,255,255,0.16)', alignItems: 'center', justifyContent: 'center' },
  headerActionIcon: { color: '#FFF', fontSize: 20 },
  searchBar: { height: 46, flexDirection: 'row', alignItems: 'center', backgroundColor: '#FFF', borderRadius: 8, paddingHorizontal: 12 },
  searchIcon: { color: '#EE4D2D', fontSize: 27, lineHeight: 27, marginRight: 7 },
  searchInput: { flex: 1, color: '#242424', fontSize: 14, paddingVertical: 0 },
  clearSearch: { color: '#999', fontSize: 24, lineHeight: 24 },
  promoBanner: { minHeight: 126, marginHorizontal: 12, marginTop: 12, borderRadius: 14, padding: 18, backgroundColor: '#FF7A45', justifyContent: 'center', overflow: 'hidden' },
  promoEyebrow: { color: '#FFF1C7', fontSize: 11, fontWeight: '900', letterSpacing: 1.5 },
  promoTitle: { color: '#FFF', fontSize: 20, fontWeight: '800', lineHeight: 27, marginTop: 6 },
  sectionHeading: { paddingHorizontal: 12, paddingTop: 22, paddingBottom: 12, flexDirection: 'row', justifyContent: 'space-between', alignItems: 'flex-end' },
  sectionTitle: { color: '#222', fontSize: 18, fontWeight: '800' },
  resultCount: { color: '#888', fontSize: 11, marginTop: 3 },
  filterButton: { minHeight: 36, borderRadius: 18, borderWidth: 1, borderColor: '#DDD', backgroundColor: '#FFF', paddingHorizontal: 12, alignItems: 'center', justifyContent: 'center' },
  filterButtonActive: { borderColor: '#EE4D2D', backgroundColor: '#FFF0EC' },
  filterButtonText: { color: '#555', fontSize: 12, fontWeight: '700' },
  filterButtonTextActive: { color: '#EE4D2D' },
  sortList: { paddingHorizontal: 12, paddingBottom: 14, gap: 8 },
  sortChip: { borderRadius: 18, borderWidth: 1, borderColor: '#E2E2E2', backgroundColor: '#FFF', paddingHorizontal: 14, paddingVertical: 8 },
  sortChipSelected: { backgroundColor: '#FFF0EC', borderColor: '#EE4D2D' },
  sortChipText: { color: '#666', fontSize: 12, fontWeight: '600' },
  sortChipTextSelected: { color: '#EE4D2D', fontWeight: '800' },
  productList: { paddingBottom: 110 },
  productRow: { gap: 8, paddingHorizontal: 8, marginBottom: 8 },
  centerState: { flex: 1, alignItems: 'center', justifyContent: 'center', padding: 30 },
  stateMessage: { color: '#858585', fontSize: 13, textAlign: 'center', marginTop: 12 },
  emptyState: { alignItems: 'center', justifyContent: 'center', paddingHorizontal: 30, paddingVertical: 70 },
  emptyIcon: { color: '#C9C9C9', fontSize: 56 },
  emptyTitle: { color: '#333', fontSize: 17, fontWeight: '700', marginTop: 12 },
  footerLoader: { paddingVertical: 22 },
  errorToast: { position: 'absolute', left: 16, right: 16, bottom: 90, minHeight: 52, borderRadius: 10, backgroundColor: '#333', paddingHorizontal: 14, paddingVertical: 10, flexDirection: 'row', alignItems: 'center', gap: 12 },
  errorText: { color: '#FFF', flex: 1, fontSize: 12, lineHeight: 17 },
  retryText: { color: '#FF9D83', fontSize: 12, fontWeight: '800' },
});
