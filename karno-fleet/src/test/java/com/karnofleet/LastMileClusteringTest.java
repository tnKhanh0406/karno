package com.karnofleet;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.karnofleet.component.LastMileLocationClusterer;
import com.karnofleet.controller.LastMileClusteringController;
import com.karnofleet.dto.request.GeoPoint;
import com.karnofleet.dto.request.LastMileOrder;
import com.karnofleet.dto.request.LastMileOrder.DeliveryMethod;
import com.karnofleet.dto.request.LastMileOrder.DeliveryWindow;
import com.karnofleet.service.LastMileClusteringService;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class LastMileClusteringTest {
    private final LastMileLocationClusterer clusterer = new LastMileLocationClusterer();

    private LastMileClusteringService service() throws Exception {
        return new LastMileClusteringService(clusterer, new ObjectMapper().findAndRegisterModules());
    }

    @Test
    void fixtureConservesOrdersAndSeparatesMethodsAndWindows() throws Exception {
        var response = service().demo(200);
        List<LastMileOrder> all = new ArrayList<>();
        for (var cluster : response.express()) {
            assertDiameter(cluster.orders());
            assertTrue(cluster.orders().stream().anyMatch(order -> order.location().equals(cluster.location())));
            cluster.orders().forEach(order -> {
                assertEquals(DeliveryMethod.EXPRESS, order.deliveryMethod());
                assertNull(order.deliveryWindow());
            });
            all.addAll(cluster.orders());
        }
        assertEquals(24, all.size());
        for (var cluster : response.standard()) {
            List<LastMileOrder> members = new ArrayList<>();
            for (var group : cluster.groups()) {
                group.orders().forEach(order -> {
                    assertEquals(DeliveryMethod.STANDARD, order.deliveryMethod());
                    assertEquals(group.deliveryWindow(), order.deliveryWindow());
                });
                members.addAll(group.orders());
            }
            assertDiameter(members);
            assertTrue(members.stream().anyMatch(order -> order.location().equals(cluster.location())));
            all.addAll(members);
        }
        assertEquals(200, response.totalOrders());
        assertEquals(200, all.size());
        assertEquals(200, all.stream().map(LastMileOrder::orderId).distinct().count());
        Collections.reverse(all);
        assertEquals(response, service().cluster(all, 200));
    }

    @Test
    void preventsLongChainsAndGroupsCoincidentLocations() {
        var orders = List.of(order("A", "0"), order("B", "0.00135"), order("C", "0.0027"));
        var clusters = clusterer.cluster(orders, 200);
        assertEquals(2, clusters.size());
        clusters.forEach(this::assertDiameter);
        assertEquals(1, clusterer.cluster(List.of(order("A", "0"), order("B", "0")), 200).size());
    }

    @Test
    void windowGroupingDoesNotSplitGeographicCluster() throws Exception {
        var admin = order("A", "0");
        var outside = new LastMileOrder("B", DeliveryMethod.STANDARD, DeliveryWindow.OUTSIDE,
                admin.location(), admin.weightKg(), admin.volumeM3(), admin.createdAt());
        var response = service().cluster(List.of(admin, outside), 200);
        assertEquals(1, response.standard().size());
        assertEquals(2, response.standard().getFirst().groups().size());
    }

    @Test
    void representativeIsCentralDeliveryPointAndStableForTiesAndSingletons() throws Exception {
        var a = order("A", "0");
        var b = order("B", "0.0005");
        var c = order("C", "0.0015");
        var service = service();
        assertEquals(b.location(), service.cluster(List.of(c, a, b), 200)
                .standard().getFirst().location());
        assertEquals(a.location(), service.cluster(List.of(b, a), 200)
                .standard().getFirst().location());
        assertEquals(c.location(), service.cluster(List.of(c), 200)
                .standard().getFirst().location());
    }

    @Test
    void validatesInputsAndSupportsEmptyPool() throws Exception {
        var service = service();
        assertEquals(0, service.cluster(List.of(), 200).totalOrders());
        assertThrows(IllegalArgumentException.class, () -> service.cluster(List.of(), Double.NaN));
        assertThrows(IllegalArgumentException.class, () -> service.cluster(List.of(order("A", "0"), order("A", "1")), 200));
        var invalid = new LastMileOrder("A", DeliveryMethod.EXPRESS, DeliveryWindow.ADMIN,
                order("A", "0").location(), BigDecimal.ONE, BigDecimal.ONE, OffsetDateTime.now());
        assertThrows(IllegalArgumentException.class, () -> service.cluster(List.of(invalid), 200));
        assertThrows(IllegalArgumentException.class, () -> service.cluster(List.of(order("A", "181")), 200));
    }

    @Test
    void demoEndpointReturnsGroupedJsonAndRejectsInvalidThreshold() throws Exception {
        var mvc = MockMvcBuilders.standaloneSetup(new LastMileClusteringController(service())).build();
        mvc.perform(get("/api/last-mile/clusters/demo"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.totalOrders").value(200))
                .andExpect(jsonPath("$.express[0].orders").isArray())
                .andExpect(jsonPath("$.express[0].location.latitude").isNumber())
                .andExpect(jsonPath("$.express[0].location.longitude").isNumber())
                .andExpect(jsonPath("$.standard[0].location.latitude").isNumber())
                .andExpect(jsonPath("$.standard[0].location.longitude").isNumber())
                .andExpect(jsonPath("$.standard[0].groups[0].orders").isArray());
        mvc.perform(get("/api/last-mile/clusters/demo").param("maxDiameterMeters", "-1"))
                .andExpect(status().isBadRequest());
    }

    private void assertDiameter(List<LastMileOrder> members) {
        for (var a : members) {
            for (var b : members) {
                assertTrue(LastMileLocationClusterer.distanceMeters(a.location(), b.location()) <= 200);
            }
        }
    }

    private LastMileOrder order(String id, String longitude) {
        return new LastMileOrder(id, DeliveryMethod.STANDARD, DeliveryWindow.ADMIN,
                new GeoPoint(BigDecimal.ZERO, new BigDecimal(longitude)), BigDecimal.ONE,
                new BigDecimal("0.01"), OffsetDateTime.parse("2026-10-14T08:00:00+07:00"));
    }
}
