package com.karnofleet;


import com.karnofleet.component.OsrmRouteGeometryProvider;
import com.karnofleet.config.OsrmProperties;
import com.karnofleet.dto.request.GeoPoint;
import com.karnofleet.dto.response.RouteGeometryResponse;
import com.karnofleet.exception.RoutingProviderException;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import okhttp3.mockwebserver.RecordedRequest;

import org.junit.jupiter.api.*;

import org.springframework.web.reactive.function.client.WebClient;

import java.io.IOException;
import java.math.BigDecimal;
import java.time.Duration;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;


class OsrmRouteGeometryProviderTest {


    private MockWebServer server;


    private OsrmRouteGeometryProvider provider;


    private GeoPoint start;


    private GeoPoint shopA;


    private GeoPoint shopB;


    private GeoPoint hub;



    @BeforeEach
    void setup() throws IOException {


        server = new MockWebServer();

        server.start();



        WebClient client =
                WebClient.builder()
                        .baseUrl(
                                server.url("").toString()
                        )
                        .build();



        OsrmProperties properties =
                new OsrmProperties(
                        server.url("").toString(),
                        "driving"
                );



        provider =
                new OsrmRouteGeometryProvider(
                        client,
                        properties
                );



        start =
                new GeoPoint(
                        new BigDecimal("21.0285"),
                        new BigDecimal("105.8542")
                );


        shopA =
                new GeoPoint(
                        new BigDecimal("21.0185"),
                        new BigDecimal("105.8290")
                );


        shopB =
                new GeoPoint(
                        new BigDecimal("21.0335"),
                        new BigDecimal("105.8140")
                );


        hub = start;
    }



    @AfterEach
    void tearDown() throws IOException {

        server.shutdown();

    }



    /**
     * Test 1:
     * OSRM trả route hợp lệ
     */
    @Test
    void shouldReturnRouteGeometrySuccessfully()
            throws Exception {


        server.enqueue(
                jsonResponse(
                        """
                        {
                          "code":"Ok",
                          "routes":[
                            {
                              "distance":15200,
                              "duration":920,
                              "geometry":"fake_polyline_xyz"
                            }
                          ]
                        }
                        """
                )
        );



        RouteGeometryResponse result =
                provider.compute(
                        start,
                        List.of(shopA, shopB),
                        hub
                );



        assertThat(
                result.encodedPolyline()
        )
                .isEqualTo(
                        "fake_polyline_xyz"
                );



        assertThat(
                result.distanceMeters()
        )
                .isEqualTo(15200);



        assertThat(
                result.travelDuration()
        )
                .isEqualTo(
                        Duration.ofSeconds(920)
                );
    }



    /**
     * Test 2:
     * Kiểm tra OSRM nhận longitude trước latitude
     */
    @Test
    void shouldSendLongitudeBeforeLatitude()
            throws Exception {


        server.enqueue(
                jsonResponse(
                        """
                        {
                          "code":"Ok",
                          "routes":[
                            {
                              "distance":1000,
                              "duration":100,
                              "geometry":"abc"
                            }
                          ]
                        }
                        """
                )
        );



        provider.compute(
                start,
                List.of(shopA),
                hub
        );



        RecordedRequest request =
                server.takeRequest();



        assertThat(
                request.getPath()
        )
                .contains(
                        "105.8542,21.0285"
                );
    }




    /**
     * Test 3:
     * OSRM không tìm thấy route
     */
    @Test
    void shouldThrowWhenOsrmReturnNoRoute()
    {


        server.enqueue(
                jsonResponse(
                        """
                        {
                          "code":"NoRoute",
                          "routes":[]
                        }
                        """
                )
        );



        assertThatThrownBy(
                () ->
                        provider.compute(
                                start,
                                List.of(shopA),
                                hub
                        )
        )
                .isInstanceOf(
                        RoutingProviderException.class
                );
    }




    /**
     * Test 4:
     * OSRM trả HTTP 500
     */
    @Test
    void shouldThrowWhenOsrmServerError()
    {


        server.enqueue(
                new MockResponse()
                        .setResponseCode(500)
        );



        assertThatThrownBy(
                () ->
                        provider.compute(
                                start,
                                List.of(shopA),
                                hub
                        )
        )
                .isInstanceOf(
                        RoutingProviderException.class
                );
    }




    /**
     * Test 5:
     * OSRM trả routes null hoặc empty
     */
    @Test
    void shouldThrowWhenRouteListEmpty()
    {


        server.enqueue(
                jsonResponse(
                        """
                        {
                          "code":"Ok",
                          "routes":[]
                        }
                        """
                )
        );



        assertThatThrownBy(
                () ->
                        provider.compute(
                                start,
                                List.of(shopA),
                                hub
                        )
        )
                .isInstanceOf(
                        RoutingProviderException.class
                );
    }





    private MockResponse jsonResponse(
            String body
    ){

        return new MockResponse()
                .setResponseCode(200)
                .setHeader(
                        "Content-Type",
                        "application/json"
                )
                .setBody(body);
    }

}