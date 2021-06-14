package be.kuritsu.gt.integration;

import static be.kuritsu.gt.integration.TestDataFactory.getDefaultPurchaseRequest;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.math.BigDecimal;
import java.time.LocalDate;

import be.kuritsu.gt.Application;
import be.kuritsu.gt.model.PurchaseRequest;
import be.kuritsu.gt.model.PurchaseResponse;
import be.kuritsu.gt.model.UnitMeasurement;

import com.amazonaws.services.dynamodbv2.AmazonDynamoDB;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.AuthenticationCredentialsNotFoundException;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

@RunWith(SpringRunner.class)
@SpringBootTest(classes = Application.class, webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT, properties = {
        "oauth.client.id=clientId",
        "oauth.client.secret=clientSecret"
})
public class PurchaseIntegrationTest {

    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private WebApplicationContext webApplicationContext;

    @Autowired
    private AmazonDynamoDB amazonDynamoDB;

    @Before
    public void setup() {
        mockMvc = MockMvcBuilders.webAppContextSetup(webApplicationContext)
                .build();
    }

    @Test
    public void test_register_purchase_unauthenticated_user() throws JsonProcessingException {
        String requestJsonString = objectMapper.writeValueAsString(getDefaultPurchaseRequest());
        Exception thrownException = Assert.assertThrows(Exception.class, () ->
                mockMvc.perform(post("/purchases")
                        .contentType("application/json")
                        .content(requestJsonString)));

        assertThat(thrownException).hasCauseInstanceOf(AuthenticationCredentialsNotFoundException.class);
    }

    @Test
    @WithMockUser(roles = "GUEST")
    public void test_register_purchase_unauthorized_user() throws JsonProcessingException {
        String requestJsonString = objectMapper.writeValueAsString(getDefaultPurchaseRequest());
        Exception thrownException = Assert.assertThrows(Exception.class, () ->
                mockMvc.perform(post("/purchases")
                        .contentType("application/json")
                        .content(requestJsonString)));

        assertThat(thrownException).hasCauseInstanceOf(AccessDeniedException.class);
    }

    @Test
    @WithMockUser(roles = "USERS", username = "ron_swanson")
    public void test_register_purchase() throws Exception {
        MvcResult mvcResult = mockMvc.perform(post("/purchases")
                .contentType("application/json")
                .content(objectMapper.writeValueAsString(getDefaultPurchaseRequest())))
                .andExpect(status().isCreated())
                .andReturn();

        String jsonResponse = mvcResult.getResponse().getContentAsString();
        PurchaseResponse purchaseResponse = objectMapper.readValue(jsonResponse, PurchaseResponse.class);
        assertThat(purchaseResponse.getId()).isNotNull();
        assertThat(purchaseResponse.getDate()).isEqualTo(LocalDate.of(2020, 12, 14));
        assertThat(purchaseResponse.getShop().getName()).isEqualTo("Provigo");
        assertThat(purchaseResponse.getShop().getLocation()).isEqualTo("Montreal");
        assertThat(purchaseResponse.getAmount()).isEqualTo(BigDecimal.valueOf(27.15));
        assertThat(purchaseResponse.getItems()).hasSize(1);
        assertThat(purchaseResponse.getItems()).anySatisfy(purchaseItemResponse -> {
            assertThat(purchaseItemResponse.getId()).isNotNull();
            assertThat(purchaseItemResponse.getBrand()).isEqualTo("Ben & Jerry's");
            assertThat(purchaseItemResponse.getProductTags()).hasSize(1);
            assertThat(purchaseItemResponse.getProductTags()).contains("ice cream");
            assertThat(purchaseItemResponse.getUnitPrice()).isEqualByComparingTo(BigDecimal.valueOf(9.05));
            assertThat(purchaseItemResponse.getNbUnit()).isEqualTo(3);
            assertThat(purchaseItemResponse.getPackaging().getNbUnitPerPackage()).isEqualTo(1);
            assertThat(purchaseItemResponse.getPackaging().getUnitMeasurements().getQuantity()).isEqualTo(465);
            assertThat(purchaseItemResponse.getPackaging().getUnitMeasurements().getType()).isEqualTo(UnitMeasurement.TypeEnum.ML);
        });
    }

    @Test
    @WithMockUser(roles = "USERS", username = "ron_swanson")
    public void test_register_purchase_invalid_request() throws Exception {
        PurchaseRequest purchaseRequest = getDefaultPurchaseRequest();
        purchaseRequest.setAmount(null);

        mockMvc.perform(post("/purchases")
                .contentType("application/json")
                .content(objectMapper.writeValueAsString(purchaseRequest)))
                .andExpect(status().isBadRequest());
    }

}