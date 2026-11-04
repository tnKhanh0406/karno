import * as Location from 'expo-location';
import { StatusBar } from 'expo-status-bar';
import { useCallback, useEffect, useMemo, useRef, useState } from 'react';
import { ActivityIndicator, Alert, Pressable, ScrollView, StyleSheet, Text, View } from 'react-native';
import MapView, { Marker, Polyline } from 'react-native-maps';
import { SafeAreaView } from 'react-native-safe-area-context';

import { addPickupRequestToSession, cancelPickupSession, completePickupSession, confirmPickup, deferPickupRequest, getActivePickupSession, getAvailableVehicles, getMapPickupRequests, getNewPickupRequests, getPickupManifest, previewFirstMile, replanFirstMileSession, startFirstMileSession } from '@/services/fleet-api';
import type { FirstMilePreview, FirstMileRequest, GeoPoint, NewPickupRequest, PickupManifest, PickupSession, Vehicle } from '@/types/fleet';
import { decodeOsrmPolyline } from '@/utils/polyline';

const ORANGE = '#EE4D2D';
const DEFAULT_REGION = { latitude: 21.0285, longitude: 105.8542, latitudeDelta: 0.12, longitudeDelta: 0.12 };

type MapPickupRequest = {
  id: string;
  requestCode: string;
  shopName: string;
  address: string;
  location: GeoPoint;
  status: string;
  inRoute: boolean;
};

type GroupedMapMarker = {
  key: string;
  location: GeoPoint;
  requests: MapPickupRequest[];
};

export default function ShipperFirstMileScreen() {
  const mapRef = useRef<MapView>(null);
  const watchRef = useRef<Location.LocationSubscription | null>(null);
  const [location, setLocation] = useState<Location.LocationObject | null>(null);
  const [locationError, setLocationError] = useState<string | null>(null);
  const [locating, setLocating] = useState(true);
  const [vehicles, setVehicles] = useState<Vehicle[]>([]);
  const [vehicleId, setVehicleId] = useState<string>();
  const [returnHours, setReturnHours] = useState(4);
  const [preview, setPreview] = useState<FirstMilePreview | null>(null);
  const [session, setSession] = useState<PickupSession | null>(null);
  const [manifests, setManifests] = useState<PickupManifest[]>([]);
  const [newRequests, setNewRequests] = useState<NewPickupRequest[]>([]);
  const [mapRequests, setMapRequests] = useState<NewPickupRequest[]>([]);
  const [loading, setLoading] = useState(true);
  const [previewing, setPreviewing] = useState(false);
  const [starting, setStarting] = useState(false);
  const [manifestLoading, setManifestLoading] = useState(false);
  const [confirmingId, setConfirmingId] = useState<string>();
  const [sessionAction, setSessionAction] = useState<string>();
  const [error, setError] = useState<string | null>(null);

  const loadManifests = useCallback(async (ids: string[]) => {
    if (ids.length === 0) {
      setManifests([]);
      return;
    }
    setManifestLoading(true);
    try {
      setManifests(await Promise.all(ids.map(getPickupManifest)));
    } catch (cause) {
      setError(cause instanceof Error ? cause.message : 'Không thể tải manifest lấy hàng.');
    } finally {
      setManifestLoading(false);
    }
  }, []);

  const requestLocation = useCallback(async () => {
    setLocating(true);
    setLocationError(null);
    try {
      const permission = await Location.requestForegroundPermissionsAsync();
      if (!permission.granted) throw new Error('Bạn cần cấp quyền vị trí để lập tuyến first-mile.');
      const servicesEnabled = await Location.hasServicesEnabledAsync();
      if (!servicesEnabled) throw new Error('GPS đang tắt. Hãy bật dịch vụ vị trí trên điện thoại.');
      const current = await Location.getCurrentPositionAsync({ accuracy: Location.Accuracy.High });
      setLocation(current);
      watchRef.current?.remove();
      watchRef.current = await Location.watchPositionAsync(
        { accuracy: Location.Accuracy.High, timeInterval: 10_000, distanceInterval: 20 },
        setLocation,
      );
    } catch (cause) {
      setLocationError(cause instanceof Error ? cause.message : 'Không thể lấy vị trí hiện tại.');
    } finally {
      setLocating(false);
    }
  }, []);

  useEffect(() => {
    const controller = new AbortController();
    const task = setTimeout(() => {
      void Promise.all([getAvailableVehicles(controller.signal), getActivePickupSession(controller.signal), getMapPickupRequests(controller.signal), requestLocation()])
        .then(([available, active, visibleRequests]) => {
          setVehicles(available);
          setMapRequests(visibleRequests);
          setVehicleId(active?.vehicleId ?? available[0]?.id);
          if (active) {
            setSession(active);
            setPreview(active.routePreview);
            void loadManifests(active.claimedPickupRequestIds);
          }
        })
        .catch((cause) => setError(cause instanceof Error ? cause.message : 'Không thể tải dữ liệu first-mile.'))
        .finally(() => setLoading(false));
    }, 0);
    return () => {
      clearTimeout(task);
      controller.abort();
      watchRef.current?.remove();
    };
  }, [loadManifests, requestLocation]);

  useEffect(() => {
    if (!session || session.status !== 'ACTIVE') return;
    let mounted = true;
    const refresh = async () => {
      try {
        const [values, visibleRequests] = await Promise.all([
          getNewPickupRequests(session.sessionId),
          getMapPickupRequests(),
        ]);
        if (mounted) {
          setNewRequests(values);
          setMapRequests(visibleRequests);
        }
      } catch {
        // Polling failures must not interrupt an active pickup flow.
      }
    };
    void refresh();
    const timer = setInterval(() => void refresh(), 15_000);
    return () => { mounted = false; clearInterval(timer); };
  }, [session]);

  const activePreview = session?.routePreview ?? preview;
  const mapMarkers = useMemo(() => {
    const requests = new Map<string, MapPickupRequest>();
    const routeIds = new Set(activePreview?.stops.map((stop) => stop.requestId) ?? []);

    mapRequests.forEach((item) => requests.set(item.id, {
      id: item.id,
      requestCode: item.requestCode,
      shopName: item.shopName,
      address: item.pickupAddress,
      location: { latitude: item.pickupLatitude, longitude: item.pickupLongitude },
      status: item.status,
      inRoute: routeIds.has(item.id),
    }));
    session?.pickupRequests.forEach((item) => requests.set(item.id, {
      id: item.id,
      requestCode: item.requestCode,
      shopName: item.shopName,
      address: item.pickupAddress,
      location: { latitude: item.pickupLatitude, longitude: item.pickupLongitude },
      status: item.status,
      inRoute: routeIds.has(item.id),
    }));
    newRequests.forEach((item) => requests.set(item.id, {
      id: item.id,
      requestCode: item.requestCode,
      shopName: item.shopName,
      address: item.pickupAddress,
      location: { latitude: item.pickupLatitude, longitude: item.pickupLongitude },
      status: item.status,
      inRoute: routeIds.has(item.id),
    }));
    activePreview?.stops.forEach((stop) => {
      const current = requests.get(stop.requestId);
      requests.set(stop.requestId, current ? { ...current, inRoute: true } : {
        id: stop.requestId, requestCode: stop.requestCode, shopName: 'Điểm lấy hàng',
        address: '', location: stop.location, status: session ? 'PLANNING' : 'OPEN', inRoute: true,
      });
    });
    activePreview?.unassigned.forEach((item) => {
      if (!requests.has(item.requestId)) requests.set(item.requestId, {
        id: item.requestId, requestCode: item.requestCode, shopName: 'Điểm lấy hàng',
        address: '', location: item.location, status: 'OPEN', inRoute: false,
      });
    });

    const groups = new Map<string, GroupedMapMarker>();
    requests.forEach((item) => {
      const key = `${item.location.latitude.toFixed(6)}:${item.location.longitude.toFixed(6)}`;
      const group = groups.get(key);
      if (group) group.requests.push(item);
      else groups.set(key, { key, location: item.location, requests: [item] });
    });
    return [...groups.values()];
  }, [activePreview, mapRequests, newRequests, session]);

  const routeCoordinates = useMemo(() => {
    if (!activePreview?.geometry.encodedPolyline || activePreview.geometry.polylineFormat !== 'OSRM') return [];
    return decodeOsrmPolyline(activePreview.geometry.encodedPolyline);
  }, [activePreview]);

  useEffect(() => {
    if (!mapRef.current || (mapMarkers.length === 0 && !activePreview)) return;
    const coordinates = [
      ...(location ? [{ latitude: location.coords.latitude, longitude: location.coords.longitude }] : []),
      ...mapMarkers.map((marker) => marker.location),
      ...(activePreview?.hub ? [activePreview.hub] : []),
    ];
    if (coordinates.length > 1) {
      const task = setTimeout(() => mapRef.current?.fitToCoordinates(coordinates, { edgePadding: { top: 55, right: 45, bottom: 55, left: 45 }, animated: true }), 250);
      return () => clearTimeout(task);
    }
  }, [activePreview, location, mapMarkers]);

  const showMarkerRequests = (marker: GroupedMapMarker) => {
    const details = marker.requests.map((item) =>
      `${item.requestCode} · ${statusLabel(item.status)}${item.inRoute ? ' · trong route' : ' · ngoài route'}`,
    ).join('\n');
    Alert.alert(marker.requests[0]?.shopName ?? 'Điểm lấy hàng', details);
  };

  const freshLocation = async () => {
    const permission = await Location.getForegroundPermissionsAsync();
    if (!permission.granted) throw new Error('Chưa có quyền truy cập vị trí.');
    const current = await Location.getCurrentPositionAsync({ accuracy: Location.Accuracy.High });
    setLocation(current);
    return current;
  };

  const buildRequest = (current: Location.LocationObject): FirstMileRequest => {
    const selectedVehicleId = session?.vehicleId ?? vehicleId;
    if (!selectedVehicleId) throw new Error('Hãy chọn phương tiện.');
    const start = new Date();
    const end = new Date(start.getTime() + returnHours * 60 * 60 * 1000);
    return {
      vehicleId: selectedVehicleId,
      latitude: current.coords.latitude,
      longitude: current.coords.longitude,
      plannedStartAt: toLocalDateTime(start),
      desiredReturnAt: toLocalDateTime(end),
    };
  };

  const handlePreview = async () => {
    try {
      setPreviewing(true);
      setError(null);
      const current = await freshLocation();
      setPreview(await previewFirstMile(buildRequest(current)));
    } catch (cause) {
      setError(cause instanceof Error ? cause.message : 'Không thể tạo tuyến đề xuất.');
    } finally {
      setPreviewing(false);
    }
  };

  const handleStart = async () => {
    try {
      setStarting(true);
      setError(null);
      const current = await freshLocation();
      const created = await startFirstMileSession(buildRequest(current));
      setSession(created);
      setPreview(created.routePreview);
      await loadManifests(created.claimedPickupRequestIds);
      Alert.alert('Đã bắt đầu lấy hàng', `Session ${created.sessionId.slice(0, 8).toUpperCase()} đang hoạt động.`);
    } catch (cause) {
      setError(cause instanceof Error ? cause.message : 'Không thể bắt đầu phiên lấy hàng.');
    } finally {
      setStarting(false);
    }
  };

  const askConfirmPickup = (manifest: PickupManifest) => {
    Alert.alert(
      'Xác nhận đã nhận đủ',
      `Bạn đã kiểm đủ ${manifest.shipments.length} kiện hàng của ${manifest.requestCode}?`,
      [{ text: 'Quay lại', style: 'cancel' }, { text: 'Đã nhận đủ', onPress: () => void handleConfirmPickup(manifest.pickupRequestId) }],
    );
  };

  const handleConfirmPickup = async (pickupRequestId: string) => {
    try {
      setConfirmingId(pickupRequestId);
      const result = await confirmPickup(pickupRequestId);
      setManifests((current) => current.map((item) => item.pickupRequestId === pickupRequestId ? { ...item, status: result.status, shipments: item.shipments.map((shipment) => ({ ...shipment, status: 'PICKED_UP' })) } : item));
      setSession((current) => current ? { ...current, pickupRequests: current.pickupRequests.map((item) => item.id === pickupRequestId ? { ...item, status: 'PICKED_UP' } : item) } : current);
    } catch (cause) {
      Alert.alert('Không thể xác nhận', cause instanceof Error ? cause.message : 'Vui lòng thử lại.');
    } finally {
      setConfirmingId(undefined);
    }
  };

  const handleReplan = async () => {
    if (!session) return;
    try {
      setSessionAction('replan');
      setError(null);
      const current = await freshLocation();
      const updated = await replanFirstMileSession(session.sessionId, buildRequest(current));
      setSession(updated);
      setPreview(updated.routePreview);
    } catch (cause) {
      setError(cause instanceof Error ? cause.message : 'Không thể cập nhật tuyến đề xuất.');
    } finally {
      setSessionAction(undefined);
    }
  };

  const handleAddRequest = async (pickupRequestId: string) => {
    if (!session) return;
    try {
      setSessionAction(pickupRequestId);
      const updated = await addPickupRequestToSession(session.sessionId, pickupRequestId);
      setSession(updated);
      setNewRequests((current) => current.filter((item) => item.id !== pickupRequestId));
      await loadManifests(updated.claimedPickupRequestIds);
      Alert.alert('Đã thêm vào ca hiện tại', 'Tuyến cũ được giữ nguyên. Bấm “Xem lại tuyến” khi bạn muốn tính lại.');
    } catch (cause) {
      Alert.alert('Không thể thêm yêu cầu', cause instanceof Error ? cause.message : 'Vui lòng thử lại.');
    } finally {
      setSessionAction(undefined);
    }
  };

  const handleDeferRequest = async (pickupRequestId: string) => {
    if (!session) return;
    try {
      setSessionAction(`defer-${pickupRequestId}`);
      await deferPickupRequest(session.sessionId, pickupRequestId);
      setNewRequests((current) => current.filter((item) => item.id !== pickupRequestId));
    } catch (cause) {
      Alert.alert('Không thể để ca sau', cause instanceof Error ? cause.message : 'Vui lòng thử lại.');
    } finally {
      setSessionAction(undefined);
    }
  };

  const resetCompletedSession = async () => {
    setSession(null);
    setPreview(null);
    setManifests([]);
    setNewRequests([]);
    setMapRequests([]);
    const [available, visibleRequests] = await Promise.all([
      getAvailableVehicles(),
      getMapPickupRequests(),
    ]);
    setVehicles(available);
    setMapRequests(visibleRequests);
    setVehicleId(available[0]?.id);
  };

  const handleCancel = async () => {
    if (!session) return;
    try {
      setSessionAction('cancel');
      await cancelPickupSession(session.sessionId);
      await resetCompletedSession();
    } catch (cause) {
      Alert.alert('Không thể hủy session', cause instanceof Error ? cause.message : 'Vui lòng thử lại.');
    } finally {
      setSessionAction(undefined);
    }
  };

  const askCancel = () => Alert.alert('Hủy ca lấy hàng?', 'Các pickup request chưa lấy sẽ trở lại OPEN và xe được giải phóng.', [
    { text: 'Quay lại', style: 'cancel' },
    { text: 'Hủy session', style: 'destructive', onPress: () => void handleCancel() },
  ]);

  const handleComplete = async () => {
    if (!session) return;
    try {
      setSessionAction('complete');
      await completePickupSession(session.sessionId);
      await resetCompletedSession();
      Alert.alert('Đã kết thúc chuyến', 'Hàng đã lấy được ghi nhận tại hub; các yêu cầu chưa lấy đã được mở lại.');
    } catch (cause) {
      Alert.alert('Chưa thể kết thúc chuyến', cause instanceof Error ? cause.message : 'Hãy thử lại để tiếp tục workflow.');
    } finally {
      setSessionAction(undefined);
    }
  };

  const askComplete = () => Alert.alert('Xác nhận đã về hub', 'Hệ thống sẽ bàn giao các kiện đã lấy cho hub và giải phóng xe.', [
    { text: 'Quay lại', style: 'cancel' },
    { text: 'Kết thúc chuyến', onPress: () => void handleComplete() },
  ]);

  const coordinate: GeoPoint | undefined = location ? { latitude: location.coords.latitude, longitude: location.coords.longitude } : undefined;

  return (
    <View style={styles.screen}>
      <StatusBar style="light" />
      <SafeAreaView edges={['top']} style={styles.headerSafeArea}>
        <View style={styles.header}><View><Text style={styles.title}>First Mile</Text><Text style={styles.subtitle}>{session ? 'Đang trong phiên lấy hàng' : 'Lập tuyến lấy hàng đề xuất'}</Text></View><View style={[styles.liveBadge, !!coordinate && styles.liveBadgeActive]}><View style={styles.liveDot} /><Text style={styles.liveText}>{coordinate ? 'GPS' : 'Mất GPS'}</Text></View></View>
      </SafeAreaView>

      <ScrollView contentContainerStyle={styles.content} showsVerticalScrollIndicator={false}>
        <View style={styles.mapCard}>
          <MapView ref={mapRef} initialRegion={coordinate ? { ...coordinate, latitudeDelta: 0.04, longitudeDelta: 0.04 } : DEFAULT_REGION} showsCompass showsScale style={styles.map}>
            {coordinate && <Marker coordinate={coordinate} title="Vị trí của bạn" pinColor="#208AEF" />}
            {mapMarkers.map((marker) => <Marker key={marker.key} coordinate={marker.location} onPress={() => showMarkerRequests(marker)}><View style={[styles.pickupMarker, { backgroundColor: markerColor(marker.requests) }, marker.requests.some((item) => item.inRoute) && styles.routeMarker]}><Text style={styles.pickupMarkerText}>{marker.requests.length}</Text></View></Marker>)}
            {activePreview?.hub && <Marker coordinate={activePreview.hub} title="Hub đích" pinColor="#16835E" />}
            {routeCoordinates.length > 1 && <Polyline coordinates={routeCoordinates} strokeColor="#208AEF" strokeWidth={5} />}
          </MapView>
          <View style={styles.locationPanel}>
            {locating ? <ActivityIndicator color={ORANGE} size="small" /> : coordinate ? <Text style={styles.locationText}>📍 {coordinate.latitude.toFixed(6)}, {coordinate.longitude.toFixed(6)}{location?.coords.accuracy ? ` · ±${Math.round(location.coords.accuracy)}m` : ''}</Text> : <Text style={styles.locationError}>{locationError}</Text>}
            {!coordinate && !locating && <Pressable onPress={() => void requestLocation()}><Text style={styles.retryText}>Lấy lại GPS</Text></Pressable>}
          </View>
        </View>

        {error && <Pressable onPress={() => setError(null)} style={styles.errorBox}><Text style={styles.errorText}>{error}</Text><Text style={styles.closeError}>×</Text></Pressable>}

        {!session && <>
          <View style={styles.section}>
            <Text style={styles.sectionTitle}>Chọn phương tiện</Text>
            {loading ? <ActivityIndicator color={ORANGE} style={styles.loader} /> : vehicles.length === 0 ? <Text style={styles.emptyText}>Không có phương tiện khả dụng.</Text> : <ScrollView horizontal showsHorizontalScrollIndicator={false} contentContainerStyle={styles.vehicleList}>{vehicles.map((vehicle) => { const selected = vehicle.id === vehicleId; return <Pressable key={vehicle.id} onPress={() => { setVehicleId(vehicle.id); setPreview(null); }} style={[styles.vehicleCard, selected && styles.vehicleCardSelected]}><Text style={styles.vehicleIcon}>🏍️</Text><Text style={styles.vehicleCode}>{vehicle.vehicleCode}</Text><Text style={styles.plate}>{vehicle.licensePlate}</Text><Text style={styles.capacity}>{vehicle.capacityWeight} kg · {vehicle.capacityVolume} m³</Text><Text style={styles.ownership}>{vehicle.ownershipType === 'SHIPPER' ? 'Xe cá nhân' : 'Xe của hub'}</Text></Pressable>; })}</ScrollView>}
          </View>

          <View style={styles.section}>
            <Text style={styles.sectionTitle}>Thời gian muốn quay về hub</Text>
            <View style={styles.hourOptions}>{[2, 4, 6, 8].map((hours) => <Pressable key={hours} onPress={() => { setReturnHours(hours); setPreview(null); }} style={[styles.hourChip, hours === returnHours && styles.hourChipSelected]}><Text style={[styles.hourText, hours === returnHours && styles.hourTextSelected]}>Sau {hours} giờ</Text></Pressable>)}</View>
          </View>

          <Pressable disabled={!vehicleId || !coordinate || previewing} onPress={() => void handlePreview()} style={[styles.previewButton, (!vehicleId || !coordinate) && styles.disabled]}>{previewing ? <ActivityIndicator color={ORANGE} /> : <Text style={styles.previewButtonText}>🗺️  Xem tuyến đường đề xuất</Text>}</Pressable>
        </>}

        {activePreview && <RouteSummary preview={activePreview} />}

        {!session && preview && <Pressable disabled={starting || !preview.route.feasible} onPress={() => void handleStart()} style={[styles.startButton, !preview.route.feasible && styles.disabled]}>{starting ? <ActivityIndicator color="#FFF" /> : <Text style={styles.startButtonText}>Bắt đầu lấy hàng</Text>}</Pressable>}

        {session && <View style={styles.sessionBanner}><View><Text style={styles.sessionLabel}>SESSION {session.status}</Text><Text style={styles.sessionCode}>#{session.sessionId.slice(0, 8).toUpperCase()}</Text></View><Text style={styles.claimedCount}>{session.claimedPickupRequestIds.length} yêu cầu lấy hàng</Text></View>}

        {session && <View style={styles.sessionActions}>
          <Pressable disabled={!!sessionAction} onPress={() => void handleReplan()} style={styles.secondaryAction}><Text style={styles.secondaryActionText}>{sessionAction === 'replan' ? 'Đang tính lại...' : '🗺️ Xem lại tuyến'}</Text></Pressable>
          <Pressable disabled={!!sessionAction} onPress={askComplete} style={styles.completeAction}><Text style={styles.completeActionText}>{sessionAction === 'complete' ? 'Đang kết thúc...' : 'Về hub · Kết thúc chuyến'}</Text></Pressable>
          {!manifests.some((item) => item.status === 'PICKED_UP') && <Pressable disabled={!!sessionAction} onPress={askCancel} style={styles.cancelAction}><Text style={styles.cancelActionText}>Hủy session</Text></Pressable>}
        </View>}

        {session && newRequests.length > 0 && <View style={styles.newRequestSection}>
          <Text style={styles.newRequestTitle}>🔔 {newRequests.length} yêu cầu lấy hàng mới</Text>
          <Text style={styles.newRequestHint}>Để nguyên nếu muốn xử lý ca sau, hoặc thêm vào session hiện tại. Tuyến chỉ đổi khi bạn bấm xem lại.</Text>
          {newRequests.map((item) => <View key={item.id} style={styles.newRequestRow}><View style={styles.newRequestInfo}><Text style={styles.manifestCode}>{item.requestCode}</Text><Text style={styles.manifestMeta}>{item.shopName} · {item.shipmentIds.length} kiện · {item.totalWeight} kg</Text></View><View style={styles.newRequestButtons}><Pressable disabled={!!sessionAction} onPress={() => void handleDeferRequest(item.id)} style={styles.deferRequestButton}><Text style={styles.deferRequestText}>{sessionAction === `defer-${item.id}` ? '...' : 'Ca sau'}</Text></Pressable><Pressable disabled={!!sessionAction} onPress={() => void handleAddRequest(item.id)} style={styles.addRequestButton}>{sessionAction === item.id ? <ActivityIndicator color="#FFF" size="small" /> : <Text style={styles.addRequestText}>Thêm vào ca</Text>}</Pressable></View></View>)}
        </View>}

        {session && <View style={styles.section}>
          <View style={styles.sectionHeader}><Text style={styles.sectionTitle}>Danh sách lấy hàng</Text><Pressable disabled={manifestLoading} onPress={() => void loadManifests(session.claimedPickupRequestIds)}><Text style={styles.refreshText}>Làm mới</Text></Pressable></View>
          {manifestLoading ? <ActivityIndicator color={ORANGE} style={styles.loader} /> : manifests.length === 0 ? <Text style={styles.emptyText}>Session không có pickup request.</Text> : manifests.map((manifest) => <ManifestCard key={manifest.pickupRequestId} manifest={manifest} confirming={confirmingId === manifest.pickupRequestId} onConfirm={() => askConfirmPickup(manifest)} />)}
        </View>}
      </ScrollView>
    </View>
  );
}

function RouteSummary({ preview }: { preview: FirstMilePreview }) {
  return <View style={styles.section}><View style={styles.sectionHeader}><Text style={styles.sectionTitle}>Tuyến đường đề xuất</Text><Text style={[styles.feasible, !preview.route.feasible && styles.infeasible]}>{preview.route.feasible ? 'KHẢ THI' : 'KHÔNG KHẢ THI'}</Text></View><View style={styles.summaryRow}><SummaryValue value={`${(preview.geometry.distanceMeters / 1000).toFixed(1)} km`} label="Quãng đường" /><SummaryValue value={`${Math.ceil(preview.geometry.durationSeconds / 60)} phút`} label="Di chuyển" /><SummaryValue value={`${preview.route.totalStops}`} label="Điểm lấy" /></View>{preview.stops.map((stop) => <View key={stop.requestId} style={styles.stopRow}><View style={styles.sequence}><Text style={styles.sequenceText}>{stop.sequence}</Text></View><View style={styles.stopInfo}><Text style={styles.stopCode}>{stop.requestCode}</Text><Text style={styles.stopMeta}>Đến {formatTime(stop.arrivalAt)} · Rời {formatTime(stop.departureAt)}</Text></View><Text style={styles.stopLoad}>{stop.loadWeightAfter} kg</Text></View>)}<View style={styles.hubRow}><Text style={styles.hubIcon}>🏢</Text><View><Text style={styles.stopCode}>Quay về hub</Text><Text style={styles.stopMeta}>Dự kiến {formatTime(preview.route.returnToHubAt)}</Text></View></View>{preview.unassigned.length > 0 && <View style={styles.unassignedBox}><Text style={styles.unassignedTitle}>{preview.unassigned.length} yêu cầu chưa được đưa vào tuyến</Text>{preview.unassigned.map((item) => <Text key={item.requestId} style={styles.unassignedText}>• {item.requestCode}: {reasonLabel(item.reason)}</Text>)}</View>}</View>;
}

function SummaryValue({ value, label }: { value: string; label: string }) { return <View style={styles.summaryValue}><Text style={styles.summaryNumber}>{value}</Text><Text style={styles.summaryLabel}>{label}</Text></View>; }

function ManifestCard({ manifest, confirming, onConfirm }: { manifest: PickupManifest; confirming: boolean; onConfirm: () => void }) {
  const pickedUp = manifest.status === 'PICKED_UP';
  const totalWeight = manifest.shipments.reduce((sum, shipment) => sum + (shipment.weightGram ?? 0), 0);
  return <View style={styles.manifestCard}><View style={styles.manifestHeader}><View><Text style={styles.manifestCode}>{manifest.requestCode}</Text><Text style={styles.manifestMeta}>{manifest.shipments.length} kiện · {(totalWeight / 1000).toFixed(1)} kg</Text></View><Text style={[styles.manifestStatus, pickedUp && styles.pickedUpStatus]}>{pickedUp ? 'ĐÃ NHẬN' : 'CHỜ LẤY'}</Text></View>{manifest.shipments.map((shipment) => <View key={shipment.shipmentId} style={styles.shipmentRow}><Text style={styles.packageIcon}>📦</Text><View style={styles.shipmentInfo}><Text style={styles.tracking}>{shipment.trackingCode}</Text><Text style={styles.shipmentMeta}>Order #{shipment.orderId.slice(0, 8).toUpperCase()} · {shipment.weightGram} g</Text></View><Text style={styles.checkIcon}>{pickedUp ? '✓' : '○'}</Text></View>)}<Pressable disabled={pickedUp || confirming} onPress={onConfirm} style={[styles.confirmButton, pickedUp && styles.confirmedButton]}>{confirming ? <ActivityIndicator color="#FFF" /> : <Text style={[styles.confirmText, pickedUp && styles.confirmedText]}>{pickedUp ? '✓ Đã nhận đủ hàng' : 'Xác nhận đã nhận đủ'}</Text>}</Pressable></View>;
}

function toLocalDateTime(date: Date) { const offset = date.getTimezoneOffset() * 60_000; return new Date(date.getTime() - offset).toISOString().slice(0, 19); }
function formatTime(value: string) { return new Date(value).toLocaleTimeString('vi-VN', { hour: '2-digit', minute: '2-digit' }); }
function reasonLabel(reason: string) { return ({ CAPACITY: 'vượt tải', TIME_WINDOW: 'không kịp khung giờ', SHIFT_END: 'không kịp giờ về' } as Record<string, string>)[reason] ?? reason; }
function statusLabel(status: string) { return ({ OPEN: 'Chờ lấy', PLANNING: 'Đã đưa vào session', PICKED_UP: 'Đã lấy', AT_ORIGIN_HUB: 'Đã đến hub' } as Record<string, string>)[status] ?? status; }
function markerColor(requests: MapPickupRequest[]) {
  const statuses = new Set(requests.map((item) => item.status));
  if (statuses.size > 1) return '#6B4BBC';
  if (statuses.has('PICKED_UP')) return '#16835E';
  if (statuses.has('AT_ORIGIN_HUB')) return '#677784';
  if (statuses.has('PLANNING')) return ORANGE;
  return '#208AEF';
}

const styles = StyleSheet.create({
  screen: { flex: 1, backgroundColor: '#F3F5F7' }, headerSafeArea: { backgroundColor: '#173B57' }, header: { minHeight: 64, flexDirection: 'row', alignItems: 'center', justifyContent: 'space-between', paddingHorizontal: 15, paddingBottom: 12 }, title: { color: '#FFF', fontSize: 22, fontWeight: '900' }, subtitle: { color: '#BFD2E1', fontSize: 10, marginTop: 2 }, liveBadge: { flexDirection: 'row', alignItems: 'center', gap: 5, borderRadius: 14, backgroundColor: '#8C3B3B', paddingHorizontal: 9, paddingVertical: 6 }, liveBadgeActive: { backgroundColor: '#176B52' }, liveDot: { width: 6, height: 6, borderRadius: 3, backgroundColor: '#FFF' }, liveText: { color: '#FFF', fontSize: 9, fontWeight: '800' },
  content: { width: '100%', maxWidth: 760, alignSelf: 'center', paddingBottom: 120 }, mapCard: { backgroundColor: '#FFF', overflow: 'hidden' }, map: { width: '100%', height: 310 }, locationPanel: { minHeight: 42, flexDirection: 'row', alignItems: 'center', justifyContent: 'space-between', paddingHorizontal: 13 }, locationText: { color: '#556', fontSize: 10 }, locationError: { flex: 1, color: '#B33', fontSize: 10 }, retryText: { color: ORANGE, fontSize: 10, fontWeight: '800' },
  pickupMarker: { width: 34, height: 34, borderRadius: 17, borderWidth: 2, borderColor: '#FFF', alignItems: 'center', justifyContent: 'center', shadowColor: '#000', shadowOpacity: 0.25, shadowRadius: 3, shadowOffset: { width: 0, height: 2 }, elevation: 5 }, routeMarker: { borderColor: '#FFE45C', borderWidth: 3 }, pickupMarkerText: { color: '#FFF', fontSize: 12, fontWeight: '900' },
  errorBox: { flexDirection: 'row', alignItems: 'center', backgroundColor: '#3D4148', margin: 10, marginBottom: 0, borderRadius: 8, padding: 11 }, errorText: { flex: 1, color: '#FFF', fontSize: 11, lineHeight: 16 }, closeError: { color: '#FFF', fontSize: 21 }, section: { backgroundColor: '#FFF', marginTop: 9, padding: 14 }, sectionHeader: { flexDirection: 'row', alignItems: 'center', justifyContent: 'space-between' }, sectionTitle: { color: '#26333D', fontSize: 15, fontWeight: '900' }, loader: { marginVertical: 24 }, emptyText: { color: '#888', fontSize: 11, textAlign: 'center', paddingVertical: 25 },
  vehicleList: { gap: 9, paddingTop: 12, paddingRight: 15 }, vehicleCard: { width: 150, borderWidth: 1, borderColor: '#DDD', borderRadius: 9, padding: 12, backgroundColor: '#FAFAFA' }, vehicleCardSelected: { borderColor: ORANGE, backgroundColor: '#FFF3EF' }, vehicleIcon: { fontSize: 25 }, vehicleCode: { color: '#26333D', fontSize: 12, fontWeight: '900', marginTop: 7 }, plate: { color: '#555', fontSize: 11, marginTop: 3 }, capacity: { color: '#777', fontSize: 9, marginTop: 7 }, ownership: { color: ORANGE, fontSize: 9, fontWeight: '700', marginTop: 4 }, hourOptions: { flexDirection: 'row', flexWrap: 'wrap', gap: 8, marginTop: 12 }, hourChip: { flexGrow: 1, minWidth: '22%', borderWidth: 1, borderColor: '#DDD', borderRadius: 7, alignItems: 'center', paddingVertical: 9 }, hourChipSelected: { borderColor: ORANGE, backgroundColor: '#FFF3EF' }, hourText: { color: '#666', fontSize: 10, fontWeight: '600' }, hourTextSelected: { color: ORANGE, fontWeight: '900' },
  previewButton: { height: 49, margin: 10, borderWidth: 1, borderColor: ORANGE, borderRadius: 8, backgroundColor: '#FFF', alignItems: 'center', justifyContent: 'center' }, previewButtonText: { color: ORANGE, fontSize: 12, fontWeight: '900' }, disabled: { opacity: 0.4 }, feasible: { color: '#16835E', backgroundColor: '#EAF8F2', borderRadius: 11, paddingHorizontal: 8, paddingVertical: 4, fontSize: 8, fontWeight: '900' }, infeasible: { color: '#B33', backgroundColor: '#FFF0F0' }, summaryRow: { flexDirection: 'row', marginTop: 15, backgroundColor: '#F5F7F9', borderRadius: 8, paddingVertical: 12 }, summaryValue: { flex: 1, alignItems: 'center' }, summaryNumber: { color: '#243B4A', fontSize: 14, fontWeight: '900' }, summaryLabel: { color: '#888', fontSize: 8, marginTop: 3 }, stopRow: { minHeight: 61, flexDirection: 'row', alignItems: 'center', borderBottomWidth: StyleSheet.hairlineWidth, borderBottomColor: '#E7E7E7' }, sequence: { width: 29, height: 29, borderRadius: 15, backgroundColor: ORANGE, alignItems: 'center', justifyContent: 'center', marginRight: 10 }, sequenceText: { color: '#FFF', fontSize: 11, fontWeight: '900' }, stopInfo: { flex: 1 }, stopCode: { color: '#333', fontSize: 11, fontWeight: '800' }, stopMeta: { color: '#888', fontSize: 9, marginTop: 3 }, stopLoad: { color: '#657', fontSize: 9 }, hubRow: { minHeight: 59, flexDirection: 'row', alignItems: 'center', gap: 10 }, hubIcon: { fontSize: 24 }, unassignedBox: { borderRadius: 7, backgroundColor: '#FFF7E8', padding: 10 }, unassignedTitle: { color: '#8A6425', fontSize: 10, fontWeight: '800' }, unassignedText: { color: '#8A6425', fontSize: 9, marginTop: 5 },
  startButton: { height: 52, margin: 10, borderRadius: 8, backgroundColor: ORANGE, alignItems: 'center', justifyContent: 'center' }, startButtonText: { color: '#FFF', fontSize: 13, fontWeight: '900' }, sessionBanner: { flexDirection: 'row', alignItems: 'center', justifyContent: 'space-between', backgroundColor: '#173B57', marginTop: 9, padding: 14 }, sessionLabel: { color: '#6FE1B6', fontSize: 9, fontWeight: '900' }, sessionCode: { color: '#FFF', fontSize: 13, fontWeight: '900', marginTop: 3 }, claimedCount: { color: '#D8E6F0', fontSize: 10 }, refreshText: { color: ORANGE, fontSize: 10, fontWeight: '800' },
  sessionActions: { backgroundColor: '#FFF', padding: 10, gap: 8 }, secondaryAction: { height: 43, borderRadius: 7, borderWidth: 1, borderColor: '#208AEF', alignItems: 'center', justifyContent: 'center' }, secondaryActionText: { color: '#1674C5', fontSize: 11, fontWeight: '900' }, completeAction: { height: 46, borderRadius: 7, backgroundColor: '#16835E', alignItems: 'center', justifyContent: 'center' }, completeActionText: { color: '#FFF', fontSize: 11, fontWeight: '900' }, cancelAction: { height: 38, alignItems: 'center', justifyContent: 'center' }, cancelActionText: { color: '#B33', fontSize: 10, fontWeight: '800' },
  newRequestSection: { backgroundColor: '#FFF8E8', marginTop: 9, padding: 14 }, newRequestTitle: { color: '#7A5215', fontSize: 13, fontWeight: '900' }, newRequestHint: { color: '#806B4C', fontSize: 9, lineHeight: 14, marginTop: 5 }, newRequestRow: { flexDirection: 'row', alignItems: 'center', borderTopWidth: StyleSheet.hairlineWidth, borderTopColor: '#E7D5B8', paddingVertical: 11, marginTop: 7 }, newRequestInfo: { flex: 1, paddingRight: 8 }, newRequestButtons: { flexDirection: 'row', alignItems: 'center', gap: 6 }, deferRequestButton: { minHeight: 34, justifyContent: 'center', paddingHorizontal: 8 }, deferRequestText: { color: '#806B4C', fontSize: 9, fontWeight: '800' }, addRequestButton: { minWidth: 82, minHeight: 34, borderRadius: 6, backgroundColor: ORANGE, alignItems: 'center', justifyContent: 'center', paddingHorizontal: 9 }, addRequestText: { color: '#FFF', fontSize: 9, fontWeight: '900' },
  manifestCard: { borderWidth: 1, borderColor: '#E2E2E2', borderRadius: 9, marginTop: 12, overflow: 'hidden' }, manifestHeader: { minHeight: 54, flexDirection: 'row', alignItems: 'center', justifyContent: 'space-between', paddingHorizontal: 12, backgroundColor: '#F8F9FA' }, manifestCode: { color: '#26333D', fontSize: 12, fontWeight: '900' }, manifestMeta: { color: '#888', fontSize: 9, marginTop: 3 }, manifestStatus: { color: '#D67A14', fontSize: 9, fontWeight: '900' }, pickedUpStatus: { color: '#16835E' }, shipmentRow: { minHeight: 54, flexDirection: 'row', alignItems: 'center', gap: 9, paddingHorizontal: 12, borderTopWidth: StyleSheet.hairlineWidth, borderTopColor: '#E8E8E8' }, packageIcon: { fontSize: 19 }, shipmentInfo: { flex: 1 }, tracking: { color: '#333', fontSize: 10, fontWeight: '800' }, shipmentMeta: { color: '#888', fontSize: 8, marginTop: 3 }, checkIcon: { color: '#16835E', fontSize: 17 }, confirmButton: { height: 44, backgroundColor: ORANGE, alignItems: 'center', justifyContent: 'center' }, confirmedButton: { backgroundColor: '#EDF8F3' }, confirmText: { color: '#FFF', fontSize: 11, fontWeight: '900' }, confirmedText: { color: '#16835E' },
});
