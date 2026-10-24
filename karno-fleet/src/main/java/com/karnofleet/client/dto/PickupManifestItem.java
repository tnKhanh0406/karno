package com.karnofleet.client.dto;
import java.util.UUID;
public record PickupManifestItem(UUID shipmentId, UUID orderId, String trackingCode,
                                 String status, Integer weightGram) {}
