package com.karnofleet.dto.response;
import com.karnofleet.client.dto.PickupManifestItem;
import com.karnofleet.enums.PickupRequestStatus;
import java.util.List;
import java.util.UUID;
public record PickupManifestResponse(UUID pickupRequestId, String requestCode,
                                     PickupRequestStatus status,
                                     List<PickupManifestItem> shipments) {}
