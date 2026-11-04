package com.karnofleet.dto.response;
import java.util.List;
import java.util.UUID;
public record PickupSessionResponse(UUID sessionId, String status, UUID vehicleId,
                                    UUID hubId, UUID zoneId,
                                    List<UUID> claimedPickupRequestIds,
                                    FirstMilePreviewResponse routePreview,
                                    List<SessionPickupRequestResponse> pickupRequests) {}
