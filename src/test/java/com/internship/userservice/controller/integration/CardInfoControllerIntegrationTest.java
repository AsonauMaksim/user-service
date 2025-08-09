package com.internship.userservice.controller.integration;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.internship.userservice.dto.card.CardInfoRequest;
import com.internship.userservice.entity.CardInfo;
import com.internship.userservice.entity.User;
import com.internship.userservice.repository.CardInfoRepository;
import com.internship.userservice.repository.UserRepository;
import com.internship.userservice.service.JwtService;
import com.internship.userservice.service.integration.BaseIntegrationTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.hasItem;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@Sql(scripts = "classpath:/sql/cleanup.sql", executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD)
public class CardInfoControllerIntegrationTest extends BaseIntegrationTest {

    private static final long OWNER_AUTH_ID = 100L;
    private static final String AUTH_HEADER = "Authorization";
    private static final String BEARER = "Bearer test-token";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private CardInfoRepository cardRepository;

    @Autowired
    private UserRepository userRepository;

    @MockBean
    private JwtService jwtService;

    @BeforeEach
    void mockJwt() {
        when(jwtService.isTokenValid(anyString())).thenReturn(true);
        when(jwtService.extractUserId(anyString())).thenReturn(OWNER_AUTH_ID);
    }

    @Test
    void createCard_ShouldReturn201AndSaveCard() throws Exception {

        User user = createDefaultUserWithAuthId(OWNER_AUTH_ID);

        CardInfoRequest request = createCardRequest();

        mockMvc.perform(post("/api/cards")
                        .header(AUTH_HEADER, BEARER)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.id").isNumber())
                .andExpect(jsonPath("$.number").value("1234567812345678"))
                .andExpect(jsonPath("$.holder").value("Test Holder"))
                .andExpect(jsonPath("$.expirationDate").value("12/30"))
                .andExpect(jsonPath("$.userId").value(user.getId()));

        assertThat(cardRepository.findAll()).hasSize(1);
        assertThat(cardRepository.findAll().getFirst().getNumber()).isEqualTo("1234567812345678");
    }

    @Test
    void createCard_ShouldReturn400_WhenInvalidInput() throws Exception {

        createDefaultUserWithAuthId(OWNER_AUTH_ID);

        CardInfoRequest invalidRequest = createInvalidCardRequest();

        mockMvc.perform(post("/api/cards")
                        .header(AUTH_HEADER, BEARER)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidRequest)))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.errors").isArray())
                .andExpect(jsonPath("$.errors", hasItem("number: Card number must be 16 digits")))
                .andExpect(jsonPath("$.errors", hasItem("holder: must not be blank")))
                .andExpect(jsonPath("$.errors", hasItem("expirationDate: must not be blank")));

        assertThat(cardRepository.findAll()).isEmpty();
    }


    @Test
    void getCardById_ShouldReturn200AndCard_WhenCardExists() throws Exception {

        User user = createDefaultUserWithAuthId(OWNER_AUTH_ID);
        CardInfo card = cardRepository.save(CardInfo.builder()
                .user(user)
                .number("1234567812345678")
                .holder("Test Holder")
                .expirationDate("12/30")
                .build());

        mockMvc.perform(get("/api/cards/{id}", card.getId())
                        .header(AUTH_HEADER, BEARER))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.id").value(card.getId()))
                .andExpect(jsonPath("$.number").value("1234567812345678"))
                .andExpect(jsonPath("$.holder").value("Test Holder"))
                .andExpect(jsonPath("$.expirationDate").value("12/30"))
                .andExpect(jsonPath("$.userId").value(user.getId()));
    }

    @Test
    void getCardById_ShouldReturn404_WhenCardNotFound() throws Exception {

        Long nonExistingId = 1488L;

        mockMvc.perform(get("/api/cards/{id}", nonExistingId)
                        .header(AUTH_HEADER, BEARER))
                .andExpect(status().isNotFound())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.error").value("Not Found"))
                .andExpect(jsonPath("$.message").value("Card id=1488 not found"))
                .andExpect(jsonPath("$.path").value("/api/cards/1488"));
    }


    @Test
    void getCardsByIds_ShouldReturn200AndListOfCards() throws Exception {

        User user = createDefaultUserWithAuthId(OWNER_AUTH_ID);

        CardInfo card1 = cardRepository.save(CardInfo.builder()
                .user(user)
                .number("1234567812345678")
                .holder("Card One")
                .expirationDate("11/28")
                .build());

        CardInfo card2 = cardRepository.save(CardInfo.builder()
                .user(user)
                .number("8765432187654321")
                .holder("Card Two")
                .expirationDate("09/29")
                .build());

        mockMvc.perform(get("/api/cards")
                        .header(AUTH_HEADER, BEARER)
                        .param("ids", card1.getId().toString(), card2.getId().toString()))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].id").value(card1.getId()))
                .andExpect(jsonPath("$[1].id").value(card2.getId()));
    }


    @Test
    void getByUserId_ShouldReturn200AndCardsOfUser() throws Exception {

        User user = createDefaultUserWithAuthId(OWNER_AUTH_ID);

        cardRepository.save(CardInfo.builder()
                .user(user)
                .number("1111222233334444")
                .holder("User Card One")
                .expirationDate("08/27")
                .build());

        cardRepository.save(CardInfo.builder()
                .user(user)
                .number("5555666677778888")
                .holder("User Card Two")
                .expirationDate("07/29")
                .build());

        mockMvc.perform(get("/api/cards/by-user/{userId}", user.getId())
                        .header(AUTH_HEADER, BEARER))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].userId").value(user.getId()))
                .andExpect(jsonPath("$[1].userId").value(user.getId()));
    }


    @Test
    void updateCard_ShouldReturn200AndUpdatedCard_WhenCardExists() throws Exception {

        User user = createDefaultUserWithAuthId(OWNER_AUTH_ID);

        CardInfo savedCard = cardRepository.save(CardInfo.builder()
                .user(user)
                .number("1111222233334444")
                .holder("Old Holder")
                .expirationDate("10/25")
                .build());

        CardInfoRequest updateRequest = CardInfoRequest.builder()
                .number("9999888877776666")
                .holder("New Holder")
                .expirationDate("09/30")
                .build();

        mockMvc.perform(put("/api/cards/{id}", savedCard.getId())
                        .header(AUTH_HEADER, BEARER)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateRequest)))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.id").value(savedCard.getId()))
                .andExpect(jsonPath("$.number").value("9999888877776666"))
                .andExpect(jsonPath("$.holder").value("New Holder"))
                .andExpect(jsonPath("$.expirationDate").value("09/30"))
                .andExpect(jsonPath("$.userId").value(user.getId()));

        CardInfo updated = cardRepository.findById(savedCard.getId()).orElseThrow();
        assertThat(updated.getNumber()).isEqualTo("9999888877776666");
        assertThat(updated.getHolder()).isEqualTo("New Holder");
        assertThat(updated.getExpirationDate()).isEqualTo("09/30");
    }

    @Test
    void updateCard_ShouldReturn404_WhenCardNotFound() throws Exception {

        createDefaultUserWithAuthId(OWNER_AUTH_ID);
        Long nonExistingId = 1488L;

        CardInfoRequest updateRequest = createCardRequest();

        mockMvc.perform(put("/api/cards/{id}", nonExistingId)
                        .header(AUTH_HEADER, BEARER)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateRequest)))
                .andExpect(status().isNotFound())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.error").value("Not Found"))
                .andExpect(jsonPath("$.message").value("Card id=1488 not found"))
                .andExpect(jsonPath("$.path").value("/api/cards/1488"));
    }

    @Test
    void updateCard_ShouldReturn400_WhenInvalidInput() throws Exception {

        User user = createDefaultUserWithAuthId(OWNER_AUTH_ID);

        CardInfo savedCard = cardRepository.save(CardInfo.builder()
                .user(user)
                .number("1111222233334444")
                .holder("Original Holder")
                .expirationDate("11/30")
                .build());

        CardInfoRequest invalidRequest = createInvalidCardRequest();

        mockMvc.perform(put("/api/cards/{id}", savedCard.getId())
                        .header(AUTH_HEADER, BEARER)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidRequest)))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.errors").isArray())
                .andExpect(jsonPath("$.errors", hasItem("number: Card number must be 16 digits")))
                .andExpect(jsonPath("$.errors", hasItem("holder: must not be blank")))
                .andExpect(jsonPath("$.errors", hasItem("expirationDate: must not be blank")));

        CardInfo cardAfter = cardRepository.findById(savedCard.getId()).orElseThrow();
        assertThat(cardAfter.getHolder()).isEqualTo("Original Holder");
        assertThat(cardAfter.getNumber()).isEqualTo("1111222233334444");
        assertThat(cardAfter.getExpirationDate()).isEqualTo("11/30");
    }

    @Test
    void deleteCard_ShouldReturn204AndRemoveCard() throws Exception {

        User user = createDefaultUserWithAuthId(OWNER_AUTH_ID);

        CardInfo savedCard = cardRepository.save(CardInfo.builder()
                .user(user)
                .number("1111222233334444")
                .holder("To Be Deleted")
                .expirationDate("10/29")
                .build());

        mockMvc.perform(delete("/api/cards/{id}", savedCard.getId())
                        .header(AUTH_HEADER, BEARER))
                .andExpect(status().isNoContent());

        assertThat(cardRepository.findById(savedCard.getId())).isEmpty();
    }

    @Test
    void deleteCard_ShouldReturn404_WhenCardNotFound() throws Exception {

        createDefaultUserWithAuthId(OWNER_AUTH_ID);
        long nonExistingId = 1488;

        mockMvc.perform(delete("/api/cards/{id}", nonExistingId)
                        .header(AUTH_HEADER, BEARER))
                .andExpect(status().isNotFound())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.error").value("Not Found"))
                .andExpect(jsonPath("$.message").value("Card id=1488 not found"))
                .andExpect(jsonPath("$.path").value("/api/cards/1488"));
    }

    private User createDefaultUserWithAuthId(long userCredentialsId) {

        return userRepository.save(User.builder()
                .name("Max")
                .surname("Ivanov")
                .email("max@example.com")
                .birthDate(LocalDate.of(1995, 10, 17))
                .userCredentialsId(userCredentialsId)
                .build());
    }

    private CardInfoRequest createCardRequest() {

        return CardInfoRequest.builder()
                .number("1234567812345678")
                .holder("Test Holder")
                .expirationDate("12/30")
                .build();
    }

    private CardInfoRequest createInvalidCardRequest() {

        return CardInfoRequest.builder()
                .number("123")
                .holder("")
                .expirationDate("")
                .build();
    }
}
