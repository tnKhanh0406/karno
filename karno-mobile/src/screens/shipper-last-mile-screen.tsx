import { router } from 'expo-router';
import { StatusBar } from 'expo-status-bar';
import { useCallback, useEffect, useMemo, useRef, useState } from 'react';
import { ActivityIndicator, Alert, Pressable, ScrollView, StyleSheet, Text, View } from 'react-native';
import MapView, { Marker, Polyline } from 'react-native-maps';
import { SafeAreaView } from 'react-native-safe-area-context';

import { loadLastMileDemo, planLastMileDemo } from '@/services/fleet-api';
import type { LastMileLoad, LastMileOrder, LastMilePlan, LastMilePlannedCluster } from '@/types/fleet';
import { decodeOsrmPolyline } from '@/utils/polyline';

const ORANGE = '#EE4D2D';
const NAVY = '#173B57';
const DEFAULT_REGION = { latitude: 21.0285, longitude: 105.8542, latitudeDelta: 0.1, longitudeDelta: 0.1 };
type DeliveryState = 'PENDING' | 'DELIVERED' | 'FAILED';

export default function ShipperLastMileScreen() {
  const mapRef = useRef<MapView>(null);
  const [maxDiameter, setMaxDiameter] = useState(200);
  const [load, setLoad] = useState<LastMileLoad | null>(null);
  const [plan, setPlan] = useState<LastMilePlan | null>(null);
  const [deliveryStates, setDeliveryStates] = useState<Record<string, DeliveryState>>({});
  const [expandedCluster, setExpandedCluster] = useState<string>();
  const [loading, setLoading] = useState(true);
  const [planning, setPlanning] = useState(false);
  const [error, setError] = useState<string>();

  const refreshLoad = useCallback(async (diameter: number) => {
    setLoading(true);
    setError(undefined);
    try {
      setLoad(await loadLastMileDemo(diameter));
      setPlan(null);
      setDeliveryStates({});
    } catch (cause) {
      setError(toMessage(cause, 'Không thể tải danh sách đơn last-mile demo.'));
    } finally {
      setLoading(false);
    }
  }, []);

  useEffect(() => {
    const task = setTimeout(() => void refreshLoad(maxDiameter), 0);
    return () => clearTimeout(task);
  }, [maxDiameter, refreshLoad]);

  const routeCoordinates = useMemo(() => {
    if (!plan?.geometry.encodedPolyline || plan.geometry.polylineFormat !== 'OSRM') return [];
    return decodeOsrmPolyline(plan.geometry.encodedPolyline);
  }, [plan]);

  const clusters = useMemo(() => plan?.selectedClusters ?? load?.clusters ?? [], [load?.clusters, plan?.selectedClusters]);
  const allOrders = useMemo(() => clusters.flatMap((cluster) => cluster.orders), [clusters]);
  const delivered = allOrders.filter((order) => deliveryStates[order.orderId] === 'DELIVERED').length;
  const failed = allOrders.filter((order) => deliveryStates[order.orderId] === 'FAILED').length;

  useEffect(() => {
    if (routeCoordinates.length < 2) return;
    const task = setTimeout(() => {
      mapRef.current?.fitToCoordinates(routeCoordinates, {
        edgePadding: { top: 45, right: 35, bottom: 45, left: 35 },
        animated: true,
      });
    }, 250);
    return () => clearTimeout(task);
  }, [routeCoordinates]);

  const handlePlan = async () => {
    setPlanning(true);
    setError(undefined);
    try {
      const value = await planLastMileDemo(toLocalDateTime(new Date()), maxDiameter);
      setPlan(value);
      setExpandedCluster(value.selectedClusters[0]?.clusterId);
    } catch (cause) {
      setError(toMessage(cause, 'Không thể lập tuyến last-mile demo.'));
    } finally {
      setPlanning(false);
    }
  };

  const setOrderState = (order: LastMileOrder, state: DeliveryState) => {
    const action = state === 'DELIVERED' ? 'giao thành công' : 'giao thất bại';
    Alert.alert('Xác nhận trạng thái', `Đánh dấu ${order.orderId} là ${action}?`, [
      { text: 'Hủy', style: 'cancel' },
      { text: 'Xác nhận', onPress: () => setDeliveryStates((current) => ({ ...current, [order.orderId]: state })) },
    ]);
  };

  const hub = routeCoordinates[0];

  return (
    <View style={styles.screen}>
      <StatusBar style="light" />
      <SafeAreaView edges={['top']} style={styles.headerSafeArea}>
        <View style={styles.header}>
          <Pressable onPress={() => router.back()} style={styles.backButton}><Text style={styles.backText}>‹</Text></Pressable>
          <View style={styles.headerCopy}><Text style={styles.title}>Last Mile</Text><Text style={styles.subtitle}>Demo xếp tải · CFI · OSRM · giao hàng</Text></View>
          <View style={styles.demoBadge}><Text style={styles.demoBadgeText}>DEMO</Text></View>
        </View>
      </SafeAreaView>

      <ScrollView contentContainerStyle={styles.content} showsVerticalScrollIndicator={false}>
        <View style={styles.mapCard}>
          <MapView ref={mapRef} initialRegion={DEFAULT_REGION} showsCompass showsScale style={styles.map}>
            {hub && <Marker coordinate={hub} title="Hub xuất phát" pinColor="#16835E" />}
            {clusters.map((cluster, index) => (
              <Marker key={cluster.clusterId} coordinate={cluster.location} title={`${plan ? `Điểm ${index + 1} · ` : ''}${cluster.clusterId}`} description={`${cluster.orders.length} đơn`}>
                <View style={[styles.marker, clusterCompleted(cluster.orders, deliveryStates) && styles.markerDone]}>
                  <Text style={styles.markerText}>{plan ? index + 1 : cluster.orders.length}</Text>
                </View>
              </Marker>
            ))}
            {routeCoordinates.length > 1 && <Polyline coordinates={routeCoordinates} strokeColor="#208AEF" strokeWidth={5} />}
          </MapView>
          <View style={styles.legend}><LegendDot color="#16835E" label="Hub" /><LegendDot color={ORANGE} label="Chờ giao" /><LegendDot color="#16835E" label="Đã xử lý" /></View>
        </View>

        {error && <Pressable onPress={() => setError(undefined)} style={styles.errorBox}><Text style={styles.errorText}>{error}</Text><Text style={styles.errorClose}>×</Text></Pressable>}

        <View style={styles.section}>
          <View style={styles.sectionHeader}><View><Text style={styles.sectionTitle}>1. Gom cụm và xếp tải</Text><Text style={styles.sectionHint}>Dữ liệu đọc từ fixture JSON của Fleet Service</Text></View>{loading && <ActivityIndicator color={ORANGE} />}</View>
          <Text style={styles.optionLabel}>Đường kính cụm tối đa</Text>
          <View style={styles.chips}>{[100, 200, 300].map((diameter) => <Pressable key={diameter} disabled={loading || planning} onPress={() => setMaxDiameter(diameter)} style={[styles.chip, diameter === maxDiameter && styles.chipActive]}><Text style={[styles.chipText, diameter === maxDiameter && styles.chipTextActive]}>{diameter} m</Text></Pressable>)}</View>
          {load && <View style={styles.statsRow}><Metric value={`${load.clusters.length}`} label="Cụm được chọn" /><Metric value={`${load.totalWeight.toFixed(1)} kg`} label={`Tải / ${load.capacityWeight} kg`} /><Metric value={`${load.totalVolume.toFixed(3)} m³`} label={`Thể tích / ${load.capacityVolume} m³`} /></View>}
        </View>

        <View style={styles.section}>
          <View style={styles.sectionHeader}><View><Text style={styles.sectionTitle}>2. Tuyến giao đề xuất</Text><Text style={styles.sectionHint}>CFI chọn thứ tự · OSRM tạo đường đi</Text></View>{plan && <Text style={[styles.feasible, !plan.feasible && styles.infeasible]}>{plan.feasible ? 'KHẢ THI' : 'KHÔNG KHẢ THI'}</Text>}</View>
          {!plan ? <Pressable disabled={!load || loading || planning} onPress={() => void handlePlan()} style={[styles.primaryButton, (!load || loading) && styles.disabled]}>{planning ? <ActivityIndicator color="#FFF" /> : <Text style={styles.primaryButtonText}>🗺️ Lập tuyến giao hàng demo</Text>}</Pressable> : <>
            <View style={styles.statsRow}><Metric value={`${(plan.geometry.distanceMeters / 1000).toFixed(1)} km`} label="Quãng đường" /><Metric value={formatDuration(plan.geometry.travelDuration)} label="Di chuyển" /><Metric value={`${plan.selectedClusters.length}`} label="Điểm giao" /></View>
            <Text style={styles.returnText}>Dự kiến về hub: <Text style={styles.returnStrong}>{formatTime(plan.returnToHubAt)}</Text></Text>
            <Pressable disabled={planning} onPress={() => void handlePlan()} style={styles.secondaryButton}><Text style={styles.secondaryButtonText}>{planning ? 'Đang tính lại...' : 'Tính lại tuyến'}</Text></Pressable>
          </>}
        </View>

        {plan && <View style={styles.progressCard}><View><Text style={styles.progressTitle}>Tiến độ chuyến giao</Text><Text style={styles.progressMeta}>{delivered} thành công · {failed} thất bại · {allOrders.length - delivered - failed} chờ giao</Text></View><Text style={styles.progressValue}>{allOrders.length ? Math.round(((delivered + failed) / allOrders.length) * 100) : 0}%</Text></View>}

        <View style={styles.section}>
          <Text style={styles.sectionTitle}>{plan ? '3. Danh sách điểm giao' : 'Các cụm được xếp lên xe'}</Text>
          <Text style={styles.sectionHint}>Nhấn vào một điểm để xem và cập nhật các đơn trong cụm.</Text>
          {loading ? <ActivityIndicator color={ORANGE} style={styles.loader} /> : clusters.length === 0 ? <Text style={styles.emptyText}>Không có cụm giao hàng phù hợp.</Text> : clusters.map((cluster, index) => (
            <ClusterCard key={cluster.clusterId} cluster={cluster} index={index} expanded={expandedCluster === cluster.clusterId} states={deliveryStates} onToggle={() => setExpandedCluster((current) => current === cluster.clusterId ? undefined : cluster.clusterId)} onState={setOrderState} />
          ))}
        </View>

        {plan && plan.unassignedClusters.length > 0 && <View style={styles.unassignedSection}><Text style={styles.unassignedTitle}>Chưa thể đưa vào tuyến ({plan.unassignedClusters.length})</Text>{plan.unassignedClusters.map((cluster) => <Text key={cluster.clusterId} style={styles.unassignedText}>• {cluster.clusterId} · {cluster.orders.length} đơn: {reasonLabel(cluster.reason)}</Text>)}</View>}
      </ScrollView>
    </View>
  );
}

function ClusterCard({ cluster, index, expanded, states, onToggle, onState }: { cluster: LastMilePlannedCluster | LastMileLoad['clusters'][number]; index: number; expanded: boolean; states: Record<string, DeliveryState>; onToggle: () => void; onState: (order: LastMileOrder, state: DeliveryState) => void }) {
  const planned = 'sequence' in cluster;
  return <View style={styles.clusterCard}>
    <Pressable onPress={onToggle} style={styles.clusterHeader}>
      <View style={[styles.sequence, clusterCompleted(cluster.orders, states) && styles.sequenceDone]}><Text style={styles.sequenceText}>{planned ? cluster.sequence : index + 1}</Text></View>
      <View style={styles.clusterInfo}><Text style={styles.clusterCode}>{cluster.clusterId}</Text><Text style={styles.clusterMeta}>{methodLabel(cluster.deliveryMethod, cluster.deliveryWindow)} · {cluster.orders.length} đơn · {cluster.totalWeight.toFixed(1)} kg</Text>{planned && <Text style={styles.clusterTime}>Đến {formatTime(cluster.arrivalAt)} · Rời {formatTime(cluster.departureAt)}</Text>}</View>
      <Text style={styles.chevron}>{expanded ? '⌃' : '⌄'}</Text>
    </Pressable>
    {expanded && <View style={styles.orders}>{cluster.orders.map((order) => <OrderRow key={order.orderId} order={order} state={states[order.orderId] ?? 'PENDING'} onState={onState} />)}</View>}
  </View>;
}

function OrderRow({ order, state, onState }: { order: LastMileOrder; state: DeliveryState; onState: (order: LastMileOrder, state: DeliveryState) => void }) {
  return <View style={styles.orderRow}><View style={styles.orderTop}><View><Text style={styles.orderCode}>📦 {order.orderId}</Text><Text style={styles.orderMeta}>{order.weightKg} kg · {order.volumeM3} m³</Text></View><Text style={[styles.orderStatus, state === 'DELIVERED' && styles.deliveredStatus, state === 'FAILED' && styles.failedStatus]}>{stateLabel(state)}</Text></View>{state === 'PENDING' && <View style={styles.orderActions}><Pressable onPress={() => onState(order, 'FAILED')} style={styles.failButton}><Text style={styles.failText}>Không giao được</Text></Pressable><Pressable onPress={() => onState(order, 'DELIVERED')} style={styles.deliverButton}><Text style={styles.deliverText}>✓ Đã giao</Text></Pressable></View>}</View>;
}

function Metric({ value, label }: { value: string; label: string }) { return <View style={styles.metric}><Text style={styles.metricValue}>{value}</Text><Text style={styles.metricLabel}>{label}</Text></View>; }
function LegendDot({ color, label }: { color: string; label: string }) { return <View style={styles.legendItem}><View style={[styles.legendDot, { backgroundColor: color }]} /><Text style={styles.legendText}>{label}</Text></View>; }
function clusterCompleted(orders: LastMileOrder[], states: Record<string, DeliveryState>) { return orders.length > 0 && orders.every((order) => states[order.orderId] && states[order.orderId] !== 'PENDING'); }
function methodLabel(method: string, window: string | null) { if (method === 'EXPRESS') return 'Hỏa tốc'; return window === 'ADMIN' ? 'Tiêu chuẩn · Giờ hành chính' : 'Tiêu chuẩn · Ngoài giờ'; }
function stateLabel(state: DeliveryState) { return ({ PENDING: 'CHỜ GIAO', DELIVERED: 'THÀNH CÔNG', FAILED: 'THẤT BẠI' } as const)[state]; }
function reasonLabel(reason: string) { return ({ CAPACITY: 'vượt tải xe', TIME_WINDOW: 'không kịp khung giờ', SHIFT_END: 'không kịp giờ về hub' } as Record<string, string>)[reason] ?? reason; }
function formatTime(value: string) { return new Date(value).toLocaleTimeString('vi-VN', { hour: '2-digit', minute: '2-digit' }); }
function formatDuration(value: string) { const hours = Number(value.match(/(\d+)H/)?.[1] ?? 0); const minutes = Number(value.match(/(\d+)M/)?.[1] ?? 0); return hours ? `${hours}g ${minutes}p` : `${minutes} phút`; }
function toLocalDateTime(date: Date) { const offset = date.getTimezoneOffset() * 60_000; return new Date(date.getTime() - offset).toISOString().slice(0, 19); }
function toMessage(cause: unknown, fallback: string) { return cause instanceof Error ? cause.message : fallback; }

const styles = StyleSheet.create({
  screen: { flex: 1, backgroundColor: '#F3F5F7' }, headerSafeArea: { backgroundColor: NAVY }, header: { minHeight: 67, flexDirection: 'row', alignItems: 'center', paddingHorizontal: 14, paddingBottom: 12 }, backButton: { width: 34, height: 34, borderRadius: 17, backgroundColor: '#294F6A', alignItems: 'center', justifyContent: 'center' }, backText: { color: '#FFF', fontSize: 27, lineHeight: 29 }, headerCopy: { flex: 1, marginLeft: 11 }, title: { color: '#FFF', fontSize: 21, fontWeight: '900' }, subtitle: { color: '#BFD2E1', fontSize: 9, marginTop: 2 }, demoBadge: { borderRadius: 12, backgroundColor: '#D98521', paddingHorizontal: 9, paddingVertical: 5 }, demoBadgeText: { color: '#FFF', fontSize: 8, fontWeight: '900' },
  content: { paddingBottom: 32 }, mapCard: { backgroundColor: '#FFF', borderBottomWidth: StyleSheet.hairlineWidth, borderColor: '#DDD' }, map: { width: '100%', height: 285 }, marker: { minWidth: 29, height: 29, borderRadius: 15, backgroundColor: ORANGE, borderWidth: 3, borderColor: '#FFF', alignItems: 'center', justifyContent: 'center' }, markerDone: { backgroundColor: '#16835E' }, markerText: { color: '#FFF', fontSize: 10, fontWeight: '900' }, legend: { minHeight: 37, flexDirection: 'row', alignItems: 'center', justifyContent: 'center', gap: 17 }, legendItem: { flexDirection: 'row', alignItems: 'center', gap: 5 }, legendDot: { width: 8, height: 8, borderRadius: 4 }, legendText: { color: '#687782', fontSize: 8 },
  errorBox: { marginHorizontal: 12, marginTop: 12, borderRadius: 8, backgroundColor: '#FFF0F0', padding: 11, flexDirection: 'row', alignItems: 'center' }, errorText: { flex: 1, color: '#A43A3A', fontSize: 10 }, errorClose: { color: '#A43A3A', fontSize: 18, marginLeft: 8 }, section: { marginHorizontal: 12, marginTop: 12, borderRadius: 10, backgroundColor: '#FFF', padding: 13 }, sectionHeader: { flexDirection: 'row', alignItems: 'center', justifyContent: 'space-between' }, sectionTitle: { color: '#253B49', fontSize: 13, fontWeight: '900' }, sectionHint: { color: '#8B969E', fontSize: 8, marginTop: 3 }, optionLabel: { color: '#596A75', fontSize: 9, fontWeight: '700', marginTop: 14 }, chips: { flexDirection: 'row', gap: 8, marginTop: 8 }, chip: { flex: 1, borderWidth: 1, borderColor: '#D8DEE2', borderRadius: 7, paddingVertical: 8, alignItems: 'center' }, chipActive: { borderColor: ORANGE, backgroundColor: '#FFF4F0' }, chipText: { color: '#78858E', fontSize: 9, fontWeight: '700' }, chipTextActive: { color: ORANGE }, statsRow: { flexDirection: 'row', backgroundColor: '#F5F7F9', borderRadius: 8, marginTop: 12, paddingVertical: 11 }, metric: { flex: 1, alignItems: 'center', paddingHorizontal: 3 }, metricValue: { color: '#243B4A', fontSize: 12, fontWeight: '900' }, metricLabel: { color: '#87939B', fontSize: 7, marginTop: 3, textAlign: 'center' }, feasible: { color: '#16835E', backgroundColor: '#EAF8F2', borderRadius: 10, paddingHorizontal: 8, paddingVertical: 4, fontSize: 8, fontWeight: '900' }, infeasible: { color: '#B33', backgroundColor: '#FFF0F0' }, primaryButton: { height: 47, borderRadius: 8, marginTop: 14, backgroundColor: ORANGE, alignItems: 'center', justifyContent: 'center' }, primaryButtonText: { color: '#FFF', fontSize: 11, fontWeight: '900' }, secondaryButton: { height: 36, borderRadius: 7, borderWidth: 1, borderColor: ORANGE, marginTop: 10, alignItems: 'center', justifyContent: 'center' }, secondaryButtonText: { color: ORANGE, fontSize: 9, fontWeight: '800' }, returnText: { color: '#697781', fontSize: 9, textAlign: 'center', marginTop: 10 }, returnStrong: { color: '#243B4A', fontWeight: '900' }, disabled: { opacity: 0.45 },
  progressCard: { marginHorizontal: 12, marginTop: 12, borderRadius: 10, backgroundColor: NAVY, padding: 14, flexDirection: 'row', alignItems: 'center', justifyContent: 'space-between' }, progressTitle: { color: '#FFF', fontSize: 12, fontWeight: '900' }, progressMeta: { color: '#BFD2E1', fontSize: 8, marginTop: 4 }, progressValue: { color: '#FFF', fontSize: 20, fontWeight: '900' }, loader: { marginVertical: 20 }, emptyText: { color: '#89949B', fontSize: 10, textAlign: 'center', marginVertical: 18 }, clusterCard: { borderWidth: StyleSheet.hairlineWidth, borderColor: '#E0E4E7', borderRadius: 8, marginTop: 10, overflow: 'hidden' }, clusterHeader: { minHeight: 66, flexDirection: 'row', alignItems: 'center', padding: 10 }, sequence: { width: 31, height: 31, borderRadius: 16, backgroundColor: ORANGE, alignItems: 'center', justifyContent: 'center', marginRight: 10 }, sequenceDone: { backgroundColor: '#16835E' }, sequenceText: { color: '#FFF', fontSize: 10, fontWeight: '900' }, clusterInfo: { flex: 1 }, clusterCode: { color: '#334A58', fontSize: 10, fontWeight: '900' }, clusterMeta: { color: '#7B8992', fontSize: 8, marginTop: 3 }, clusterTime: { color: '#3377A4', fontSize: 8, marginTop: 3, fontWeight: '700' }, chevron: { color: '#778690', fontSize: 17 }, orders: { borderTopWidth: StyleSheet.hairlineWidth, borderTopColor: '#E4E7E9', backgroundColor: '#FAFBFC', paddingHorizontal: 9 }, orderRow: { paddingVertical: 11, borderBottomWidth: StyleSheet.hairlineWidth, borderBottomColor: '#E3E6E8' }, orderTop: { flexDirection: 'row', alignItems: 'center', justifyContent: 'space-between' }, orderCode: { color: '#344A58', fontSize: 9, fontWeight: '900' }, orderMeta: { color: '#859199', fontSize: 8, marginTop: 3 }, orderStatus: { color: '#A06B18', backgroundColor: '#FFF5DF', borderRadius: 9, paddingHorizontal: 7, paddingVertical: 4, fontSize: 7, fontWeight: '900' }, deliveredStatus: { color: '#16835E', backgroundColor: '#EAF8F2' }, failedStatus: { color: '#B33', backgroundColor: '#FFF0F0' }, orderActions: { flexDirection: 'row', justifyContent: 'flex-end', gap: 7, marginTop: 9 }, failButton: { borderWidth: 1, borderColor: '#D8A1A1', borderRadius: 6, paddingHorizontal: 9, paddingVertical: 6 }, failText: { color: '#A94747', fontSize: 8, fontWeight: '800' }, deliverButton: { borderRadius: 6, backgroundColor: '#16835E', paddingHorizontal: 11, paddingVertical: 6 }, deliverText: { color: '#FFF', fontSize: 8, fontWeight: '800' }, unassignedSection: { marginHorizontal: 12, marginTop: 12, borderRadius: 9, backgroundColor: '#FFF7E8', padding: 13 }, unassignedTitle: { color: '#8A6425', fontSize: 10, fontWeight: '900' }, unassignedText: { color: '#8A6425', fontSize: 8, marginTop: 6 },
});
