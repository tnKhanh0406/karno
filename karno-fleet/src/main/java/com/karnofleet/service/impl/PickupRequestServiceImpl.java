package com.karnofleet.service.impl;

import com.karnofleet.entity.PickupRequest;
import com.karnofleet.client.ShippingClient;
import com.karnofleet.client.UserClient;
import com.karnofleet.client.dto.PickupShipmentAssignmentRequest;
import com.karnofleet.client.dto.PickupShipmentSnapshot;
import com.karnofleet.client.dto.ShopPickupContext;
import com.karnofleet.dto.request.CreatePickupRequest;
import com.karnofleet.dto.request.UpdatePickupRequest;
import com.karnofleet.dto.response.PickupRequestResponse;
import com.karnofleet.enums.PickupRequestStatus;
import com.karnofleet.repository.PickupRequestRepo;
import com.karnofleet.service.PickupRequestService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.http.HttpStatus;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class PickupRequestServiceImpl implements PickupRequestService {
    private final PickupRequestRepo pickupRequestRepo;
    private final UserClient userClient;
    private final ShippingClient shippingClient;

    @Override
    public List<PickupRequest> findPlanningCandidates(UUID hubId) {
        return pickupRequestRepo.findPlanningCandidates(hubId, PickupRequestStatus.OPEN);
    }

    @Override
    @Transactional
    public PickupRequestResponse create(CreatePickupRequest request) {
        validateWindow(request.pickupWindowStart(), request.pickupWindowEnd());
        ShopPickupContext shop = loadAndAuthorizeShop(request.shopId(), request.sellerId());
        UUID id = UUID.randomUUID();
        List<PickupShipmentSnapshot> shipments = shippingClient.claim(id,
                new PickupShipmentAssignmentRequest(request.shopId(), request.shipmentIds()));
        try {
            PickupRequest pickupRequest = buildPickupRequest(id, request, shop, shipments);
            if (pickupRequestRepo.insert(pickupRequest) != 1) {
                throw new IllegalStateException("Cannot insert pickup request");
            }
            insertMemberships(id, shipments);
            return PickupRequestResponse.from(pickupRequest,
                    shipments.stream().map(PickupShipmentSnapshot::shipmentId).toList());
        } catch (RuntimeException ex) {
            compensateRelease(id, request.shopId(), ex);
            throw ex;
        }
    }

    @Override
    @Transactional
    public PickupRequestResponse update(UUID id, UpdatePickupRequest request) {
        validateWindow(request.pickupWindowStart(), request.pickupWindowEnd());
        PickupRequest current = requireEditable(id, request.sellerId());
        List<UUID> previousIds = pickupRequestRepo.findShipmentIds(id);
        List<PickupShipmentSnapshot> shipments = shippingClient.replace(id,
                new PickupShipmentAssignmentRequest(current.getShopId(), request.shipmentIds()));
        try {
            current.setPickupWindowStart(request.pickupWindowStart());
            current.setPickupWindowEnd(request.pickupWindowEnd());
            applyTotals(current, shipments);
            if (pickupRequestRepo.updateOpen(current) != 1) {
                throw conflict("Pickup request is no longer editable");
            }
            pickupRequestRepo.deleteShipments(id);
            insertMemberships(id, shipments);
            return PickupRequestResponse.from(current,
                    shipments.stream().map(PickupShipmentSnapshot::shipmentId).toList());
        } catch (RuntimeException ex) {
            compensateReplace(id, current.getShopId(), previousIds, ex);
            throw ex;
        }
    }

    @Override
    @Transactional
    public void cancel(UUID id, UUID sellerId) {
        PickupRequest current = requireEditable(id, sellerId);
        List<UUID> shipmentIds = pickupRequestRepo.findShipmentIds(id);
        shippingClient.release(id, current.getShopId());
        try {
            if (pickupRequestRepo.cancelOpen(id, sellerId) != 1) {
                throw conflict("Pickup request is no longer editable");
            }
            pickupRequestRepo.deleteShipments(id);
        } catch (RuntimeException ex) {
            try {
                shippingClient.claim(id,
                        new PickupShipmentAssignmentRequest(current.getShopId(), shipmentIds));
            } catch (RuntimeException compensationError) {
                ex.addSuppressed(compensationError);
            }
            throw ex;
        }
    }

    @Override
    public List<PickupRequestResponse> findByShop(UUID shopId, UUID sellerId) {
        loadAndAuthorizeShop(shopId, sellerId);
        return pickupRequestRepo.findByShopId(shopId).stream()
                .map(request -> PickupRequestResponse.from(
                        request, pickupRequestRepo.findShipmentIds(request.getId())))
                .toList();
    }

    private PickupRequest buildPickupRequest(UUID id, CreatePickupRequest request,
                                             ShopPickupContext shop,
                                             List<PickupShipmentSnapshot> shipments) {
        PickupRequest result = PickupRequest.builder()
                .id(id).requestCode("PU-" + id.toString().substring(0, 8).toUpperCase())
                .shopId(shop.shopId()).sellerId(request.sellerId()).shopName(shop.shopName())
                .pickupAddress(shop.pickupAddress()).pickupLatitude(shop.pickupLatitude())
                .pickupLongitude(shop.pickupLongitude()).targetHubId(shop.originHubId())
                .pickupZoneId(shop.pickupZoneId())
                .serviceDurationSeconds(request.serviceDurationSeconds() == null
                        ? 300 : request.serviceDurationSeconds())
                .pickupWindowStart(request.pickupWindowStart())
                .pickupWindowEnd(request.pickupWindowEnd()).status(PickupRequestStatus.OPEN)
                .build();
        applyTotals(result, shipments);
        return result;
    }

    private void applyTotals(PickupRequest request, List<PickupShipmentSnapshot> shipments) {
        int weightGram = shipments.stream().map(PickupShipmentSnapshot::weightGram)
                .filter(Objects::nonNull).reduce(0, Integer::sum);
        BigDecimal volume = shipments.stream().map(PickupShipmentSnapshot::volumeM3)
                .filter(Objects::nonNull).reduce(BigDecimal.ZERO, BigDecimal::add);
        request.setTotalWeight(BigDecimal.valueOf(weightGram)
                .divide(BigDecimal.valueOf(1000), 3, RoundingMode.HALF_UP));
        request.setTotalVolume(volume.max(new BigDecimal("0.000001")));
    }

    private ShopPickupContext loadAndAuthorizeShop(UUID shopId, UUID sellerId) {
        ShopPickupContext shop = userClient.getPickupContext(shopId);
        if (!sellerId.equals(shop.ownerId())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                    "Seller does not own this shop");
        }
        if (shop.originHubId() == null || shop.pickupZoneId() == null || shop.pickupLatitude() == null
                || shop.pickupLongitude() == null) {
            throw conflict("Shop has not been assigned to an origin hub or valid location");
        }
        return shop;
    }

    private PickupRequest requireEditable(UUID id, UUID sellerId) {
        PickupRequest request = pickupRequestRepo.findById(id);
        if (request == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Pickup request not found");
        }
        if (!sellerId.equals(request.getSellerId())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                    "Seller does not own this pickup request");
        }
        if (request.getStatus() != PickupRequestStatus.OPEN) {
            throw conflict("Only OPEN pickup request can be changed");
        }
        return request;
    }

    private void insertMemberships(UUID id, List<PickupShipmentSnapshot> shipments) {
        for (PickupShipmentSnapshot shipment : shipments) {
            if (pickupRequestRepo.insertShipment(id, shipment.shipmentId()) != 1) {
                throw new IllegalStateException("Cannot insert pickup request shipment");
            }
        }
    }

    private void validateWindow(java.time.LocalTime start, java.time.LocalTime end) {
        if (!start.isBefore(end)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "pickupWindowStart must be before pickupWindowEnd");
        }
    }

    private void compensateRelease(UUID id, UUID shopId, RuntimeException original) {
        try {
            shippingClient.release(id, shopId);
        } catch (RuntimeException compensationError) {
            original.addSuppressed(compensationError);
        }
    }

    private void compensateReplace(UUID id, UUID shopId, List<UUID> previous,
                                   RuntimeException original) {
        try {
            shippingClient.replace(id, new PickupShipmentAssignmentRequest(shopId, previous));
        } catch (RuntimeException compensationError) {
            original.addSuppressed(compensationError);
        }
    }

    private ResponseStatusException conflict(String message) {
        return new ResponseStatusException(HttpStatus.CONFLICT, message);
    }
}
