import { useState } from 'react';
import {
  Alert,
  KeyboardAvoidingView,
  Modal,
  Platform,
  Pressable,
  ScrollView,
  StyleSheet,
  Text,
  TextInput,
  View,
} from 'react-native';
import { SafeAreaView } from 'react-native-safe-area-context';

import type { ProductCategory, ProductFilters } from '@/types/product';

type Props = {
  categories: ProductCategory[];
  initialFilters: ProductFilters;
  onApply: (filters: ProductFilters) => void;
  onClose: () => void;
};

const ratings = [5, 4, 3, 2, 1];

export function ProductFilterModal({ categories, initialFilters, onApply, onClose }: Props) {
  const [categoryId, setCategoryId] = useState(initialFilters.categoryId);
  const [minPrice, setMinPrice] = useState(initialFilters.minPrice?.toString() ?? '');
  const [maxPrice, setMaxPrice] = useState(initialFilters.maxPrice?.toString() ?? '');
  const [minRating, setMinRating] = useState(initialFilters.minRating);

  const parsePrice = (value: string) => {
    const normalized = value.replace(/[^0-9]/g, '');
    return normalized ? Number(normalized) : undefined;
  };

  const apply = () => {
    const parsedMinPrice = parsePrice(minPrice);
    const parsedMaxPrice = parsePrice(maxPrice);
    if (parsedMinPrice !== undefined && parsedMaxPrice !== undefined && parsedMinPrice > parsedMaxPrice) {
      Alert.alert('Khoảng giá không hợp lệ', 'Giá tối thiểu phải nhỏ hơn hoặc bằng giá tối đa.');
      return;
    }
    onApply({ categoryId, minPrice: parsedMinPrice, maxPrice: parsedMaxPrice, minRating });
  };

  const reset = () => {
    setCategoryId(undefined);
    setMinPrice('');
    setMaxPrice('');
    setMinRating(undefined);
  };

  return (
    <Modal animationType="slide" onRequestClose={onClose} transparent>
      <KeyboardAvoidingView behavior={Platform.OS === 'ios' ? 'padding' : undefined} style={styles.overlay}>
        <Pressable accessibilityLabel="Đóng bộ lọc" onPress={onClose} style={styles.backdrop} />
        <SafeAreaView edges={['bottom']} style={styles.sheet}>
          <View style={styles.handle} />
          <View style={styles.header}>
            <Text style={styles.title}>Bộ lọc sản phẩm</Text>
            <Pressable hitSlop={10} onPress={onClose}><Text style={styles.close}>×</Text></Pressable>
          </View>

          <ScrollView contentContainerStyle={styles.content} keyboardShouldPersistTaps="handled">
            <Text style={styles.sectionTitle}>Danh mục</Text>
            <View style={styles.chips}>
              <FilterChip label="Tất cả" selected={!categoryId} onPress={() => setCategoryId(undefined)} />
              {categories.map((category) => (
                <FilterChip
                  key={category.id}
                  label={category.name}
                  selected={category.id === categoryId}
                  onPress={() => setCategoryId(category.id)}
                />
              ))}
            </View>

            <Text style={styles.sectionTitle}>Khoảng giá</Text>
            <View style={styles.priceRow}>
              <TextInput
                accessibilityLabel="Giá tối thiểu"
                keyboardType="number-pad"
                maxLength={12}
                onChangeText={setMinPrice}
                placeholder="Từ"
                placeholderTextColor="#AAA"
                style={styles.priceInput}
                value={minPrice}
              />
              <View style={styles.priceDivider} />
              <TextInput
                accessibilityLabel="Giá tối đa"
                keyboardType="number-pad"
                maxLength={12}
                onChangeText={setMaxPrice}
                placeholder="Đến"
                placeholderTextColor="#AAA"
                style={styles.priceInput}
                value={maxPrice}
              />
            </View>

            <Text style={styles.sectionTitle}>Đánh giá tối thiểu</Text>
            <View style={styles.chips}>
              <FilterChip label="Tất cả" selected={minRating === undefined} onPress={() => setMinRating(undefined)} />
              {ratings.map((rating) => (
                <FilterChip
                  key={rating}
                  label={`${rating} ★ trở lên`}
                  selected={minRating === rating}
                  onPress={() => setMinRating(rating)}
                />
              ))}
            </View>
          </ScrollView>

          <View style={styles.actions}>
            <Pressable onPress={reset} style={styles.resetButton}><Text style={styles.resetText}>Đặt lại</Text></Pressable>
            <Pressable onPress={apply} style={styles.applyButton}><Text style={styles.applyText}>Áp dụng</Text></Pressable>
          </View>
        </SafeAreaView>
      </KeyboardAvoidingView>
    </Modal>
  );
}

function FilterChip({ label, selected, onPress }: { label: string; selected: boolean; onPress: () => void }) {
  return (
    <Pressable accessibilityRole="button" accessibilityState={{ selected }} onPress={onPress} style={[styles.chip, selected && styles.chipSelected]}>
      <Text style={[styles.chipText, selected && styles.chipTextSelected]}>{label}</Text>
    </Pressable>
  );
}

const styles = StyleSheet.create({
  overlay: { flex: 1, justifyContent: 'flex-end' },
  backdrop: { ...StyleSheet.absoluteFill, backgroundColor: 'rgba(0,0,0,0.42)' },
  sheet: { maxHeight: '84%', borderTopLeftRadius: 20, borderTopRightRadius: 20, backgroundColor: '#FFF', overflow: 'hidden' },
  handle: { width: 42, height: 4, borderRadius: 2, backgroundColor: '#D5D5D5', alignSelf: 'center', marginTop: 9 },
  header: { height: 55, flexDirection: 'row', alignItems: 'center', justifyContent: 'space-between', paddingHorizontal: 16, borderBottomWidth: StyleSheet.hairlineWidth, borderBottomColor: '#E6E6E6' },
  title: { color: '#222', fontSize: 17, fontWeight: '800' },
  close: { color: '#777', fontSize: 27 },
  content: { padding: 16, paddingBottom: 22 },
  sectionTitle: { color: '#333', fontSize: 13, fontWeight: '800', marginTop: 6, marginBottom: 11 },
  chips: { flexDirection: 'row', flexWrap: 'wrap', gap: 8, marginBottom: 18 },
  chip: { minHeight: 36, borderRadius: 7, borderWidth: 1, borderColor: '#E0E0E0', backgroundColor: '#F8F8F8', paddingHorizontal: 12, alignItems: 'center', justifyContent: 'center' },
  chipSelected: { borderColor: '#EE4D2D', backgroundColor: '#FFF1ED' },
  chipText: { color: '#555', fontSize: 11, fontWeight: '600' },
  chipTextSelected: { color: '#EE4D2D', fontWeight: '800' },
  priceRow: { flexDirection: 'row', alignItems: 'center', gap: 10, marginBottom: 20 },
  priceInput: { flex: 1, height: 44, borderRadius: 7, borderWidth: 1, borderColor: '#DDD', color: '#333', fontSize: 13, paddingHorizontal: 12 },
  priceDivider: { width: 14, height: 1, backgroundColor: '#AAA' },
  actions: { flexDirection: 'row', gap: 10, borderTopWidth: StyleSheet.hairlineWidth, borderTopColor: '#DDD', paddingHorizontal: 14, paddingTop: 11 },
  resetButton: { flex: 1, height: 47, borderRadius: 7, borderWidth: 1, borderColor: '#EE4D2D', alignItems: 'center', justifyContent: 'center' },
  resetText: { color: '#EE4D2D', fontSize: 13, fontWeight: '800' },
  applyButton: { flex: 1.5, height: 47, borderRadius: 7, backgroundColor: '#EE4D2D', alignItems: 'center', justifyContent: 'center' },
  applyText: { color: '#FFF', fontSize: 13, fontWeight: '800' },
});
