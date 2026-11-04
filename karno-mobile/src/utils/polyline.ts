import type { GeoPoint } from '@/types/fleet';

export function decodeOsrmPolyline(encoded: string): GeoPoint[] {
  const points: GeoPoint[] = [];
  let index = 0;
  let latitude = 0;
  let longitude = 0;
  while (index < encoded.length) {
    const latResult = decodeValue(encoded, index);
    index = latResult.nextIndex;
    latitude += latResult.value;
    const lonResult = decodeValue(encoded, index);
    index = lonResult.nextIndex;
    longitude += lonResult.value;
    points.push({ latitude: latitude / 1e5, longitude: longitude / 1e5 });
  }
  return points;
}

function decodeValue(encoded: string, startIndex: number) {
  let result = 0;
  let shift = 0;
  let index = startIndex;
  let byte: number;
  do {
    byte = encoded.charCodeAt(index++) - 63;
    result |= (byte & 0x1f) << shift;
    shift += 5;
  } while (byte >= 0x20 && index < encoded.length);
  return { value: result & 1 ? ~(result >> 1) : result >> 1, nextIndex: index };
}
