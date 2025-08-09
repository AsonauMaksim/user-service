package com.internship.userservice.controller.integration;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.internship.userservice.dto.user.UserRequest;
import com.internship.userservice.dto.user.UserResponse;
import com.internship.userservice.entity.User;
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
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.hamcrest.Matchers.hasItem;

@Sql(scripts = "classpath:/sql/cleanup.sql", executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD)
public class UserControllerIntegrationTest extends BaseIntegrationTest {

    private static final long AUTH_SUBJECT_ID = 777L;
    private static final String AUTH_HEADER = "Authorization";
    private static final String BEARER = "Bearer test-token";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private UserRepository userRepository;

    @MockBean
    private JwtService jwtService;

    @BeforeEach
    void mockJwt() {

        when(jwtService.isTokenValid(anyString())).thenReturn(true);
        when(jwtService.extractUserId(anyString())).thenReturn(AUTH_SUBJECT_ID);
    }

    private UserRequest createUserRequest() {

        return UserRequest.builder()
                .name("Max")
                .surname("Ivanov")
                .birthDate(LocalDate.of(1995, 10, 17))
                .email("max@gmail.com")
                .build();
    }

    @Test
    void createUser_ShouldReturn201AndSaveUser() throws Exception {

        UserRequest request = createUserRequest();

        mockMvc.perform(post("/api/users")
                        .header(AUTH_HEADER, BEARER)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.id").isNumber())
                .andExpect(jsonPath("$.name").value(request.getName()))
                .andExpect(jsonPath("$.surname").value(request.getSurname()))
                .andExpect(jsonPath("$.email").value(request.getEmail()))
                .andExpect(jsonPath("$.birthDate").value(request.getBirthDate().toString()));

        assertThat(userRepository.findAll()).hasSize(1);
        assertThat(userRepository.findAll().getFirst().getEmail()).isEqualTo(request.getEmail());
    }

    @Test
    void createUser_ShouldReturn400_WhenInvalidInput() throws Exception {

        UserRequest request = UserRequest.builder()
                .name("")
                .surname("Ivanov")
                .email("invalid-email")
                .birthDate(LocalDate.now().plusDays(1))
                .build();

        mockMvc.perform(post("/api/users")
                        .header(AUTH_HEADER, BEARER)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.errors").isArray())
                .andExpect(jsonPath("$.errors", hasItem("name: Name is required")))
                .andExpect(jsonPath("$.errors", hasItem("email: Email is not valid")))
                .andExpect(jsonPath("$.errors", hasItem("birthDate: Birth date can't be in the future")));

        assertThat(userRepository.findAll()).isEmpty();
    }

    @Test
    void getUserById_ShouldReturn200AndUser_WhenExists() throws Exception {

        String responseJson = mockMvc.perform(post("/api/users")
                        .header(AUTH_HEADER, BEARER)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createUserRequest())))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();

        UserResponse created = objectMapper.readValue(responseJson, UserResponse.class);

        mockMvc.perform(get("/api/users/{id}", created.getId())
                        .header(AUTH_HEADER, BEARER))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.id").value(created.getId()))
                .andExpect(jsonPath("$.name").value("Max"))
                .andExpect(jsonPath("$.surname").value("Ivanov"))
                .andExpect(jsonPath("$.email").value("max@gmail.com"))
                .andExpect(jsonPath("$.birthDate").value("1995-10-17"));
    }

    @Test
    void getUserById_ShouldReturn404_WhenUserNotFound() throws Exception {

        long nonExistingId = 1488L;

        mockMvc.perform(get("/api/users/{id}", nonExistingId)
                        .header(AUTH_HEADER, BEARER))
                .andExpect(status().isNotFound())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.error").value("Not Found"))
                .andExpect(jsonPath("$.message").value("User id=1488 not found"))
                .andExpect(jsonPath("$.path").value("/api/users/1488"));
    }

    @Test
    void getAllUsers_ShouldReturn200AndListOfUsers() throws Exception {

        userRepository.save(User.builder()
                .name("Alice")
                .surname("Wonderland")
                .email("alice@example.com")
                .birthDate(LocalDate.of(1990, 1, 1))
                .userCredentialsId(AUTH_SUBJECT_ID)
                .build());

        userRepository.save(User.builder()
                .name("Bob")
                .surname("Builder")
                .email("bob@example.com")
                .birthDate(LocalDate.of(1985, 5, 20))
                .userCredentialsId(AUTH_SUBJECT_ID)
                .build());

        mockMvc.perform(get("/api/users/all")
                        .header(AUTH_HEADER, BEARER))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].name").value("Alice"))
                .andExpect(jsonPath("$[1].name").value("Bob"));
    }

    @Test
    void updateUser_ShouldReturn200AndUpdatedUser_WhenValid() throws Exception {

        String createdJson = mockMvc.perform(post("/api/users")
                        .header(AUTH_HEADER, BEARER)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                UserRequest.builder()
                                        .name("OldName")
                                        .surname("OldSurname")
                                        .email("old@gmail.com")
                                        .birthDate(LocalDate.of(1990, 1, 1))
                                        .build())))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();

        UserResponse created = objectMapper.readValue(createdJson, UserResponse.class);

        UserRequest updateRequest = UserRequest.builder()
                .name("NewName")
                .surname("NewSurname")
                .email("new@gmail.com")
                .birthDate(LocalDate.of(1995, 5, 5))
                .build();

        mockMvc.perform(put("/api/users/{id}", created.getId())
                        .header(AUTH_HEADER, BEARER)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(created.getId()))
                .andExpect(jsonPath("$.name").value("NewName"))
                .andExpect(jsonPath("$.email").value("new@gmail.com"));
    }

    @Test
    void updateUser_ShouldReturn400_WhenInvalidInput() throws Exception {

        String createdJson = mockMvc.perform(post("/api/users")
                        .header(AUTH_HEADER, BEARER)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                UserRequest.builder()
                                        .name("Valid")
                                        .surname("User")
                                        .email("valid@example.com")
                                        .birthDate(LocalDate.of(1990, 1, 1))
                                        .build())))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();

        UserResponse created = objectMapper.readValue(createdJson, UserResponse.class);

        UserRequest invalidUpdate = UserRequest.builder()
                .name("")
                .surname("NewSurname")
                .email("invalid-email")
                .birthDate(LocalDate.now().plusDays(10))
                .build();

        mockMvc.perform(put("/api/users/{id}", created.getId())
                        .header(AUTH_HEADER, BEARER)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidUpdate)))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.errors").isArray())
                .andExpect(jsonPath("$.errors", hasItem("name: Name is required")))
                .andExpect(jsonPath("$.errors", hasItem("email: Email is not valid")))
                .andExpect(jsonPath("$.errors", hasItem("birthDate: Birth date can't be in the future")));
    }

    @Test
    void updateUser_ShouldReturn404_WhenUserNotFound() throws Exception {

        long nonExistentId = 1488L;

        UserRequest request = UserRequest.builder()
                .name("NewName")
                .surname("NewSurname")
                .email("new@example.com")
                .birthDate(LocalDate.of(1990, 1, 1))
                .build();

        mockMvc.perform(put("/api/users/{id}", nonExistentId)
                        .header(AUTH_HEADER, BEARER)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isNotFound())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.error").value("Not Found"))
                .andExpect(jsonPath("$.message").value("User id=1488 not found"))
                .andExpect(jsonPath("$.path").value("/api/users/1488"));
    }

    @Test
    void deleteUser_ShouldReturn204_WhenUserDeleted() throws Exception {

        String createdJson = mockMvc.perform(post("/api/users")
                        .header(AUTH_HEADER, BEARER)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createUserRequest())))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();

        UserResponse created = objectMapper.readValue(createdJson, UserResponse.class);

        mockMvc.perform(delete("/api/users/{id}", created.getId())
                        .header(AUTH_HEADER, BEARER))
                .andExpect(status().isNoContent());

        assertThat(userRepository.findById(created.getId())).isEmpty();
    }

    @Test
    void deleteUser_ShouldReturn404_WhenUserNotFound() throws Exception {

        long nonExistingId = 1488L;

        mockMvc.perform(delete("/api/users/{id}", nonExistingId)
                        .header(AUTH_HEADER, BEARER))
                .andExpect(status().isNotFound())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.error").value("Not Found"))
                .andExpect(jsonPath("$.message").value("User id=1488 not found"))
                .andExpect(jsonPath("$.path").value("/api/users/1488"));
    }
}
