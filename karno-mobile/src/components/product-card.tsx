import { Image } from 'expo-image';
import { router } from 'expo-router';
import { Pressable, StyleSheet, Text, View } from 'react-native';

import { resolveProductImageUrl } from '@/services/product-api';
import type { ProductListItem } from '@/types/product';

type Props = { product: ProductListItem };

const formatPrice = (price: number) => `${Math.round(price).toLocaleString('vi-VN')}đ`;
const formatSoldCount = (count: number) => {
  if (count >= 1_000_000) return `${(count / 1_000_000).toFixed(1).replace('.0', '')}tr`;
  if (count >= 1_000) return `${(count / 1_000).toFixed(1).replace('.0', '')}k`;
  return count.toString();
};

export function ProductCard({ product }: Props) {
  return (
    <Pressable
      accessibilityLabel={`${product.name}, giá ${formatPrice(product.minPrice)}`}
      onPress={() => router.push({ pathname: '/products/[productId]', params: { productId: product.id } })}
      style={({ pressed }) => [styles.card, pressed && styles.cardPressed]}>
      <View style={styles.imageContainer}>
        <Image
          accessibilityLabel={`Ảnh ${product.name}`}
          contentFit="cover"
          source={resolveProductImageUrl(product.thumbnailImg)}
          style={styles.image}
          transition={180}
        />
        <View style={styles.mallBadge}><Text style={styles.mallBadgeText}>Mall</Text></View>
      </View>
      <View style={styles.content}>
        <Text numberOfLines={2} style={styles.name}>{product.name}</Text>
        <Text style={styles.price}>{formatPrice(product.minPrice)}</Text>
        <View style={styles.metaRow}>
          <Text style={styles.rating}>★ {product.rating.toFixed(1)}</Text>
          <Text style={styles.sold}>Đã bán {formatSoldCount(product.soldCount)}</Text>
        </View>
      </View>
    </Pressable>
  );
}

const styles = StyleSheet.create({
  card: { flex: 1, minWidth: 0, borderRadius: 8, backgroundColor: '#FFF', overflow: 'hidden', shadowColor: '#000', shadowOffset: { width: 0, height: 2 }, shadowOpacity: 0.06, shadowRadius: 5, elevation: 2 },
  cardPressed: { opacity: 0.82, transform: [{ scale: 0.985 }] },
  imageContainer: { width: '100%', aspectRatio: 1, backgroundColor: '#F1F1F1' },
  image: { width: '100%', height: '100%' },
  mallBadge: { position: 'absolute', left: 7, top: 7, backgroundColor: '#D9381E', borderRadius: 3, paddingHorizontal: 6, paddingVertical: 3 },
  mallBadgeText: { color: '#FFF', fontSize: 9, fontWeight: '900', textTransform: 'uppercase' },
  content: { minHeight: 108, padding: 9 },
  name: { color: '#292929', fontSize: 13, lineHeight: 18, minHeight: 36 },
  price: { color: '#EE4D2D', fontSize: 17, fontWeight: '800', marginTop: 7 },
  metaRow: { flexDirection: 'row', alignItems: 'center', justifyContent: 'space-between', marginTop: 6 },
  rating: { color: '#E5A300', fontSize: 10, fontWeight: '700' },
  sold: { color: '#777', fontSize: 10 },
});
