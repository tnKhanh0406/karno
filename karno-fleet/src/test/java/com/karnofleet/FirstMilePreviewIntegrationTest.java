package com.karnofleet;


import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;


import java.util.UUID;


import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;


@SpringBootTest
@AutoConfigureMockMvc
class FirstMilePreviewIntegrationTest {


    @Autowired
    MockMvc mvc;


    private static final UUID SHIPPER_ID =
            UUID.fromString(
                    "80000000-0000-0000-0000-000000000007"
            );


    @Test
    void shouldGeneratePlanUsingOsrm()
            throws Exception {


        mvc.perform(
                        post(
                                "/api/first-mile/preview/{shipperUserId}",
                                SHIPPER_ID
                        )
                                .content("""
                        {
                          "latitude": 21.0285,
                          "longitude": 105.8542
                        }
                        """)
                                .contentType(
                                        MediaType.APPLICATION_JSON
                                )
                )
                .andExpect(
                        status().isOk()
                )
                .andExpect(
                        jsonPath(
                                "$.selectedStops"
                        )
                                .isArray()
                )
                .andExpect(
                        jsonPath(
                                "$.unassigned"
                        )
                                .isArray()
                );
    }
}