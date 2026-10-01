package com.karnofleet;

import com.karnofleet.component.OsrmMatrixLoader;
import com.karnofleet.component.TravelTimeProvider;
import com.karnofleet.config.OsrmProperties;
import com.karnofleet.dto.request.GeoPoint;
import com.karnofleet.exception.RoutingProviderException;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import okhttp3.mockwebserver.RecordedRequest;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.reactive.function.client.WebClient;

import java.io.IOException;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;

import static com.karnofleet.CheapestFeasibleInsertionPlannerTest.point;
import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.assertj.core.api.AssertionsForClassTypes.assertThatThrownBy;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.jsonPath;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class OsrmMatrixLoaderTest {


    private MockWebServer server;

    private OsrmMatrixLoader loader;


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


        loader =
                new OsrmMatrixLoader(
                        client,
                        properties
                );
    }


    @AfterEach
    void cleanup() throws IOException {

        server.shutdown();
    }

    @Test
    void shouldBuildTravelTimeMatrix() {


        server.enqueue(
                new MockResponse()
                        .setResponseCode(200)
                        .setHeader(
                                "Content-Type",
                                "application/json"
                        )
                        .setBody("""
            {
              "code":"Ok",
              "durations":[
                [0,600,900],
                [700,0,300],
                [1000,400,0]
              ]
            }
            """)
        );


        GeoPoint start =
                point(
                        "21.0285",
                        "105.8542"
                );


        GeoPoint shop =
                point(
                        "21.0185",
                        "105.829"
                );


        GeoPoint hub =
                point(
                        "21.0335",
                        "105.814"
                );


        TravelTimeProvider provider =
                loader.load(
                        List.of(
                                start,
                                shop,
                                hub
                        ),
                        LocalDateTime.now()
                );


        Duration duration =
                provider.getTravelTime(
                        start,
                        shop,
                        LocalDateTime.now()
                );


        assertThat(duration)
                .isEqualTo(
                        Duration.ofSeconds(600)
                );
    }

    @Test
    void shouldSendLongitudeBeforeLatitude()
            throws Exception {


        server.enqueue(
                new MockResponse()
                        .setResponseCode(200)
                        .setHeader(
                                "Content-Type",
                                "application/json"
                        )
                        .setBody("""
        {
          "code":"Ok",
          "durations":[[0]]
        }
        """)
        );


        GeoPoint point =
                point(
                        "21.0285",
                        "105.8542"
                );


        loader.load(
                List.of(point),
                LocalDateTime.now()
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

    @Test
    void shouldThrowWhenOsrmFailed(){


        server.enqueue(
                new MockResponse()
                        .setResponseCode(200)
                        .setHeader(
                                "Content-Type",
                                "application/json"
                        )
                        .setBody("""
        {
          "code":"NoRoute"
        }
        """)
        );


        assertThatThrownBy(
                () ->
                        loader.load(
                                List.of(
                                        point("21","105")
                                ),
                                LocalDateTime.now()
                        )
        )
                .isInstanceOf(
                        RoutingProviderException.class
                );
    }

}