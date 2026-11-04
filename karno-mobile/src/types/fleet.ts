export type GeoPoint = { latitude: number; longitude: number };

export type Vehicle = {
  id: string;
  vehicleCode: string;
  licensePlate: string;
  capacityWeight: number;
  capacityVolume: number;
  status: string;
  ownershipType: string;
  ownerShipperId: string | null;
  hubId: string | null;
};

export type FirstMileRequest = {
  vehicleId: string;
  latitude: number;
  longitude: number;
  plannedStartAt: string;
  desiredReturnAt: string;
};

export type PickupStop = {
  sequence: number;
  requestId: string;
  requestCode: string;
  location: GeoPoint;
  arrivalAt: string;
  serviceStartAt: string;
  departureAt: string;
  waitingTime: number | string;
  loadWeightAfter: number;
  loadVolumeAfter: number;
};

export type FirstMilePreview = {
  route: {
    feasible: boolean;
    totalStops: number;
    finalWeight: number;
    finalVolume: number;
    returnToHubAt: string;
    violations: string[];
  };
  stops: PickupStop[];
  unassigned: { requestId: string; requestCode: string; reason: string; location: GeoPoint }[];
  geometry: {
    encodedPolyline: string;
    polylineFormat: 'OSRM' | 'HERE_FLEXIBLE';
    distanceMeters: number;
    durationSeconds: number;
  };
  hub: GeoPoint;
};

export type PickupSession = {
  sessionId: string;
  status: string;
  vehicleId: string;
  hubId: string;
  zoneId: string;
  claimedPickupRequestIds: string[];
  routePreview: FirstMilePreview | null;
  pickupRequests: SessionPickupRequest[];
};

export type SessionPickupRequest = {
  id: string;
  requestCode: string;
  shopName: string;
  pickupAddress: string;
  pickupLatitude: number;
  pickupLongitude: number;
  status: 'PLANNING' | 'PICKED_UP' | 'AT_ORIGIN_HUB';
};

export type NewPickupRequest = {
  id: string;
  requestCode: string;
  shopName: string;
  pickupAddress: string;
  pickupLatitude: number;
  pickupLongitude: number;
  status: 'OPEN' | 'PLANNING' | 'PICKED_UP' | 'AT_ORIGIN_HUB';
  totalWeight: number;
  totalVolume: number;
  shipmentIds: string[];
};

export type PickupManifest = {
  pickupRequestId: string;
  requestCode: string;
  status: 'OPEN' | 'PLANNING' | 'PICKED_UP' | 'AT_ORIGIN_HUB' | 'CANCELLED';
  shipments: {
    shipmentId: string;
    orderId: string;
    trackingCode: string;
    status: string;
    weightGram: number;
  }[];
};

export type PickupConfirmation = {
  pickupRequestId: string;
  status: 'PICKED_UP';
  shipmentIds: string[];
  alreadyPickedUp: boolean;
};
