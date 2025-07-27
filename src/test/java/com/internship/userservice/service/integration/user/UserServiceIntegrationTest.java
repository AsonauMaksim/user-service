package com.internship.userservice.service.integration.user;

import com.internship.userservice.dto.user.UserRequest;
import com.internship.userservice.dto.user.UserResponse;
import com.internship.userservice.entity.User;
import com.internship.userservice.exception.AlreadyExistsException;
import com.internship.userservice.exception.NotFoundException;
import com.internship.userservice.repository.UserRepository;
import com.internship.userservice.service.UserService;
import com.internship.userservice.service.integration.BaseIntegrationTest;
import org.springframework.beans.factory.annotation.Autowired;
import org.junit.jupiter.api.Test;
import org.springframework.test.context.jdbc.Sql;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@Sql(scripts = "classpath:/sql/cleanup.sql", executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD)
public class UserServiceIntegrationTest extends BaseIntegrationTest {

    @Autowired
    private UserService userService;

    @Autowired
    private UserRepository userRepository;

    @Test
    void createUser_ShouldPersistUserInDatabase() {

        UserRequest request = createUserRequest();

        UserResponse response = userService.create(request);

        assertThat(response).isNotNull();
        assertThat(response.getEmail()).isEqualTo(request.getEmail());
        assertThat(response.getName()).isEqualTo(request.getName());

        User savedUser = userRepository.findById(response.getId())
                .orElseThrow(() -> new AssertionError("User not saved in DB"));

        assertThat(savedUser.getEmail()).isEqualTo("max@gmail.com");
        assertThat(savedUser.getName()).isEqualTo("Max");
        assertThat(savedUser.getSurname()).isEqualTo("Ivanov");
        assertThat(savedUser.getBirthDate()).isEqualTo(LocalDate.of(1995, 10, 17));
    }

    @Test
    void createUser_ShouldThrowAlreadyExistsException_WhenEmailAlreadyExists() {

        UserRequest request = createUserRequest();

        userService.create(request);

        assertThatThrownBy(() -> userService.create(request))
                .isInstanceOf(AlreadyExistsException.class)
                .hasMessageContaining("User with email");
    }

    @Test
    void getUserById_ShouldReturnUser_WhenUserExists() {

        UserRequest request = createUserRequest();

        UserResponse createdUser = userService.create(request);

        UserResponse fetchedUser = userService.getUserById(createdUser.getId());

        assertThat(fetchedUser).isNotNull();
        assertThat(fetchedUser.getId()).isEqualTo(createdUser.getId());
        assertThat(fetchedUser.getName()).isEqualTo(request.getName());
        assertThat(fetchedUser.getSurname()).isEqualTo(request.getSurname());
        assertThat(fetchedUser.getEmail()).isEqualTo(request.getEmail());
        assertThat(fetchedUser.getBirthDate()).isEqualTo(request.getBirthDate());

        User userFromDb = userRepository.findById(fetchedUser.getId())
                .orElseThrow(() -> new AssertionError("User not found in DB"));

        assertThat(userFromDb.getEmail()).isEqualTo("max@gmail.com");
    }

    @Test
    void getUserById_ShouldThrowNotFoundException_WhenUserNotFound() {

        long notExistInDB = 1488L;

        assertThatThrownBy(() -> userService.getUserById(notExistInDB))
                .isInstanceOf(NotFoundException.class)
                .hasMessageContaining("User id=" + notExistInDB + " not found");
    }

    @Test
    void getUserByEmail_ShouldReturnUser_WhenEmailExists() {

        UserRequest request = createUserRequest();

        UserResponse created = userService.create(request);

        UserResponse found = userService.getUserByEmail("max@gmail.com");

        assertThat(found).isNotNull();
        assertThat(found.getId()).isEqualTo(created.getId());
        assertThat(found.getEmail()).isEqualTo("max@gmail.com");
        assertThat(found.getName()).isEqualTo("Max");
        assertThat(found.getSurname()).isEqualTo("Ivanov");
        assertThat(found.getBirthDate()).isEqualTo(LocalDate.of(1995, 10, 17));
    }

    @Test
    void getUserByEmail_ShouldThrowNotFoundException_WhenEmailDoesNotExist() {

        String notExistEmail = "1488@gmail.com";

        assertThatThrownBy(() -> userService.getUserByEmail(notExistEmail))
                .isInstanceOf(NotFoundException.class)
                .hasMessageContaining("User email=" + notExistEmail + " not found");
    }

    @Test
    void getUsersByIds_ShouldReturnUsers_WhenIdsExist() {

        UserRequest user1 = createUserRequest();

        UserRequest user2 = UserRequest.builder()
                .name("Sveta")
                .surname("Svetikova")
                .birthDate(LocalDate.of(1998, 2, 5))
                .email("sveta@gmail.com")
                .build();

        UserResponse savedUser1 = userService.create(user1);
        UserResponse savedUser2 = userService.create(user2);

        List<UserResponse> foundUsers = userService.getUsersByIds(List.of(savedUser1.getId(), savedUser2.getId()));

        assertThat(foundUsers).hasSize(2);
        assertThat(foundUsers).extracting(UserResponse::getEmail)
                .containsExactlyInAnyOrder("max@gmail.com", "sveta@gmail.com");
    }

    @Test
    void getUsersByIds_ShouldReturnEmptyList_WhenNoIdsMatch() {

        List<Long> unknownIds = List.of(1488L, 5588L);

        List<UserResponse> result = userService.getUsersByIds(unknownIds);

        assertThat(result).isEmpty();
    }

    @Test
    void updateUserById_ShouldUpdateUser_WhenDataIsValid() {

        UserRequest originalRequest = createUserRequest();

        UserResponse savedUser = userService.create(originalRequest);

        UserRequest updateRequest = UserRequest.builder()
                .name("Maxim")
                .surname("Petrov")
                .birthDate(LocalDate.of(1995, 10, 17))
                .email("max@gmail.com")
                .build();

        UserResponse updated = userService.updateUserById(savedUser.getId(), updateRequest);

        assertThat(updated).isNotNull();
        assertThat(updated.getName()).isEqualTo("Maxim");
        assertThat(updated.getSurname()).isEqualTo("Petrov");
    }

    @Test
    void updateUserById_ShouldThrowException_WhenUserNotFound() {

        UserRequest updateRequest = createUserRequest();

        assertThatThrownBy(() -> userService.updateUserById(1488L, updateRequest))
                .isInstanceOf(NotFoundException.class)
                .hasMessageContaining("User id=1488 not found");
    }

    @Test
    void updateUserById_ShouldThrowException_WhenEmailAlreadyUsedByAnotherUser() {

        UserRequest user1 = UserRequest.builder()
                .name("User1")
                .surname("One")
                .birthDate(LocalDate.of(1995, 10, 17))
                .email("first@gmail.com")
                .build();

        UserRequest user2 = UserRequest.builder()
                .name("User2")
                .surname("Two")
                .birthDate(LocalDate.of(1992, 2, 2))
                .email("second@gmail.com")
                .build();

        UserResponse savedUser1 = userService.create(user1);
        UserResponse savedUser2 = userService.create(user2);

        UserRequest updateRequest = UserRequest.builder()
                .name("User2")
                .surname("Two")
                .birthDate(LocalDate.of(1992, 2, 2))
                .email("first@gmail.com")
                .build();

        assertThatThrownBy(() -> userService.updateUserById(savedUser2.getId(), updateRequest))
                .isInstanceOf(AlreadyExistsException.class)
                .hasMessageContaining("Email 'first@gmail.com' already in use");
    }

    @Test
    void deleteUserById_ShouldDeleteUser_WhenUserExists() {

        UserRequest request = createUserRequest();

        UserResponse savedUser = userService.create(request);

        userService.deleteUserById(savedUser.getId());

        boolean exists = userRepository.existsById(savedUser.getId());
        assertThat(exists).isFalse();
    }

    @Test
    void deleteUserById_ShouldThrowException_WhenUserDoesNotExist() {

        assertThatThrownBy(() -> userService.deleteUserById(1488L))
                .isInstanceOf(NotFoundException.class)
                .hasMessageContaining("User id=1488 not found");
    }

    @Test
    void getAllUsers_ShouldReturnAllSavedUsers() {

        UserRequest user1 = createUserRequest();
        UserRequest user2 = UserRequest.builder()
                .name("Sveta")
                .surname("Svetikova")
                .birthDate(LocalDate.of(1998, 2, 5))
                .email("sveta@gmail.com")
                .build();

        userService.create(user1);
        userService.create(user2);

        List<UserResponse> allUsers = userService.getAllUsers();

        assertThat(allUsers)
                .extracting(UserResponse::getEmail)
                .contains("max@gmail.com", "sveta@gmail.com");
    }

    private UserRequest createUserRequest() {
//        String uniqueSuffix = UUID.randomUUID().toString().substring(0, 8);
//        return UserRequest.builder()
//                .name("Max_" + uniqueSuffix)
//                .surname("Ivanov")
//                .birthDate(LocalDate.of(1995, 10, 17))
//                .email("max_" + uniqueSuffix + "@gmail.com")
//                .build();
        return UserRequest.builder()
                .name("Max")
                .surname("Ivanov")
                .birthDate(LocalDate.of(1995, 10, 17))
                .email("max@gmail.com")
                .build();
    }
}
