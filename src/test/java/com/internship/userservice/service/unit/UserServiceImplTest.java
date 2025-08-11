package com.internship.userservice.service.unit;

import com.internship.userservice.dto.user.UserRequest;
import com.internship.userservice.dto.user.UserResponse;
import com.internship.userservice.entity.User;
import com.internship.userservice.exception.AlreadyExistsException;
import com.internship.userservice.exception.NotFoundException;
import com.internship.userservice.mapper.UserMapper;
import com.internship.userservice.repository.UserRepository;
import com.internship.userservice.security.JwtUtils;
import com.internship.userservice.service.impl.UserServiceImpl;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.AssertionsForClassTypes.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

public class UserServiceImplTest {

    private UserRepository userRepository;
    private UserMapper userMapper;
    private UserServiceImpl userService;
    private MockedStatic<JwtUtils> jwtUtilsMock;

    @BeforeEach
    void setUp() {
        userRepository = mock(UserRepository.class);
        userMapper = mock(UserMapper.class);
        userService = new UserServiceImpl(userRepository, userMapper);
        jwtUtilsMock = mockStatic(JwtUtils.class);
    }

    @AfterEach
    void tearDown() {
        jwtUtilsMock.close();
    }

    @Test
    void create_ShouldCreateUser_WhenEmailIsUnique() {

        UserRequest request = new UserRequest();
        request.setEmail("maks@gmail.com");

        User userToSave = new User();
        userToSave.setEmail("maks@gmail.com");

        User savedUser = new User();
        savedUser.setId(1L);
        savedUser.setEmail("maks@gmail.com");
        savedUser.setUserCredentialsId(100L);

        UserResponse expectedResponse = new UserResponse();
        expectedResponse.setId(1L);
        expectedResponse.setEmail("maks@gmail.com");

        when(userRepository.findByEmail(request.getEmail())).thenReturn(Optional.empty());
        when(userMapper.toEntity(request)).thenReturn(userToSave);
        when(userRepository.save(userToSave)).thenReturn(savedUser);
        when(userMapper.toDto(savedUser)).thenReturn(expectedResponse);

        jwtUtilsMock.when(JwtUtils::getUserCredentialsIdFromToken).thenReturn(100L);

        UserResponse result = userService.create(request);

        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo(1L);
        assertThat(result.getEmail()).isEqualTo("maks@gmail.com");
        assertThat(userToSave.getUserCredentialsId()).isEqualTo(100L);

        verify(userRepository).findByEmail("maks@gmail.com");
        verify(userMapper).toEntity(request);
        verify(userRepository).save(userToSave);
        verify(userMapper).toDto(savedUser);
        jwtUtilsMock.verify(JwtUtils::getUserCredentialsIdFromToken);
    }

    @Test
    void create_ShouldThrowAlreadyExistsException_WhenEmailAlreadyExists() {

        UserRequest request = new UserRequest();
        request.setEmail("maks@gmail.com");

        User existingUser = new User();
        existingUser.setId(2L);
        existingUser.setEmail("maks@gmail.com");

        when(userRepository.findByEmail("maks@gmail.com"))
                .thenReturn(Optional.of(existingUser));

        assertThatThrownBy(() -> userService.create(request))
                .isInstanceOf(AlreadyExistsException.class)
                .hasMessageContaining("User with email 'maks@gmail.com' already exists");

        verify(userRepository).findByEmail("maks@gmail.com");
        verifyNoMoreInteractions(userMapper, userRepository);
        jwtUtilsMock.verifyNoInteractions();
    }

    @Test
    void getUserById_ShouldReturnUser_WhenUserExists() {

        Long userId = 1L;

        User user = new User();
        user.setId(userId);
        user.setEmail("test@example.com");

        UserResponse userResponse = new UserResponse();
        userResponse.setId(userId);
        userResponse.setEmail("test@example.com");

        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(userMapper.toDto(user)).thenReturn(userResponse);

        UserResponse result = userService.getUserById(userId);

        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo(userId);
        assertThat(result.getEmail()).isEqualTo("test@example.com");

        verify(userRepository).findById(userId);
        verify(userMapper).toDto(user);
    }

    @Test
    void getUserById_ShouldThrowNotFoundException_WhenUserDoesNotExist() {

        Long userId = 228L;
        when(userRepository.findById(userId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> userService.getUserById(userId))
                .isInstanceOf(NotFoundException.class)
                .hasMessageContaining("User id=228 not found");

        verify(userRepository).findById(userId);
        verifyNoMoreInteractions(userMapper);
    }

    @Test
    void getUserByEmail_ShouldReturnUser_WhenEmailExists() {

        String email = "maks@gmail.com";

        User user = new User();
        user.setId(1L);
        user.setEmail(email);

        UserResponse expectedResponse = new UserResponse();
        expectedResponse.setId(1L);
        expectedResponse.setEmail(email);

        when(userRepository.findByEmail(email)).thenReturn(Optional.of(user));
        when(userMapper.toDto(user)).thenReturn(expectedResponse);

        UserResponse result = userService.getUserByEmail(email);

        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo(1L);
        assertThat(result.getEmail()).isEqualTo(email);

        verify(userRepository).findByEmail(email);
        verify(userMapper).toDto(user);
    }

    @Test
    void getUserByEmail_ShouldThrowNotFoundException_WhenUserDoesNotExist() {

        String email = "maks@gmail.com";

        when(userRepository.findByEmail(email)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> userService.getUserByEmail(email))
                .isInstanceOf(NotFoundException.class)
                .hasMessageContaining("User email=" + email + " not found");

        verify(userRepository).findByEmail(email);
        verifyNoMoreInteractions(userMapper);
    }

    @Test
    void getUsersByIds_ShouldReturnUserList_WhenIdsAreValid() {

        List<Long> ids = List.of(1L, 2L);

        User user1 = new User();
        user1.setId(1L);
        user1.setEmail("user1@gmail.com");

        User user2 = new User();
        user2.setId(2L);
        user2.setEmail("user2@gmail.com");

        List<User> userList = List.of(user1, user2);

        UserResponse response1 = new UserResponse();
        response1.setId(1L);
        response1.setEmail("user1@gmail.com");

        UserResponse response2 = new UserResponse();
        response2.setId(2L);
        response2.setEmail("user2@gmail.com");

        List<UserResponse> responseList = List.of(response1, response2);

        when(userRepository.findAllById(ids)).thenReturn(userList);
        when(userMapper.toDtoList(userList)).thenReturn(responseList);

        List<UserResponse> result = userService.getUsersByIds(ids);

        assertThat(result).isNotNull();
        assertThat(result.size()).isEqualTo(2);
        assertThat(result.get(0).getId()).isEqualTo(1L);
        assertThat(result.get(1).getId()).isEqualTo(2L);

        verify(userRepository).findAllById(ids);
        verify(userMapper).toDtoList(userList);
    }

    @Test
    void getAllUsers_ShouldReturnUserList() {

        User user1 = new User();
        user1.setId(1L);
        user1.setEmail("user1@gmail.com");

        User user2 = new User();
        user2.setId(2L);
        user2.setEmail("user2@gmail.com");

        List<User> users = List.of(user1, user2);

        UserResponse response1 = new UserResponse();
        response1.setId(1L);
        response1.setEmail("user1@gmail.com");

        UserResponse response2 = new UserResponse();
        response2.setId(2L);
        response2.setEmail("user2@gmail.com");

        List<UserResponse> responses = List.of(response1, response2);

        when(userRepository.findAll()).thenReturn(users);
        when(userMapper.toDtoList(users)).thenReturn(responses);

        List<UserResponse> result = userService.getAllUsers();

        assertThat(result).isNotNull();
        assertThat(result).hasSize(2);
        assertThat(result.get(0).getEmail()).isEqualTo("user1@gmail.com");
        assertThat(result.get(1).getEmail()).isEqualTo("user2@gmail.com");

        verify(userRepository).findAll();
        verify(userMapper).toDtoList(users);
    }

    @Test
    void updateUserById_ShouldUpdate_WhenOwnerAndEmailUnchanged() {

        Long userId = 1L;
        Long authId = 100L;

        UserRequest request = new UserRequest();
        request.setEmail("maks@gmail.com");

        User existing = new User();
        existing.setId(userId);
        existing.setEmail("maks@gmail.com");
        existing.setUserCredentialsId(authId);

        UserResponse expected = new UserResponse();
        expected.setId(userId);
        expected.setEmail("maks@gmail.com");

        when(userRepository.findById(userId)).thenReturn(Optional.of(existing));
        when(userMapper.toDto(existing)).thenReturn(expected);
        jwtUtilsMock.when(JwtUtils::getUserCredentialsIdFromToken).thenReturn(authId);

        UserResponse result = userService.updateUserById(userId, request);

        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo(userId);
        assertThat(result.getEmail()).isEqualTo("maks@gmail.com");

        verify(userRepository).findById(userId);
        verify(userMapper).updateEntity(existing, request);
        verify(userMapper).toDto(existing);
        jwtUtilsMock.verify(JwtUtils::getUserCredentialsIdFromToken);
    }

    @Test
    void updateUserById_ShouldUpdate_WhenOwnerAndEmailChangedToUnique() {

        Long userId = 1L;
        Long authId = 100L;

        UserRequest request = new UserRequest();
        request.setEmail("new@mail.com");

        User existing = new User();
        existing.setId(userId);
        existing.setEmail("old@mail.com");
        existing.setUserCredentialsId(authId);

        UserResponse expected = new UserResponse();
        expected.setId(userId);
        expected.setEmail("new@mail.com");

        when(userRepository.findById(userId)).thenReturn(Optional.of(existing));
        when(userRepository.findByEmail("new@mail.com")).thenReturn(Optional.empty());
        when(userMapper.toDto(existing)).thenReturn(expected);
        jwtUtilsMock.when(JwtUtils::getUserCredentialsIdFromToken).thenReturn(authId);

        UserResponse result = userService.updateUserById(userId, request);

        assertThat(result).isNotNull();
        assertThat(result.getEmail()).isEqualTo("new@mail.com");

        verify(userRepository).findById(userId);
        verify(userRepository).findByEmail("new@mail.com");
        verify(userMapper).updateEntity(existing, request);
        verify(userMapper).toDto(existing);
        jwtUtilsMock.verify(JwtUtils::getUserCredentialsIdFromToken);
    }

    @Test
    void updateUserById_ShouldThrowAlreadyExists_WhenEmailAlreadyInUseByAnother() {

        Long userId = 1L;
        Long authId = 100L;

        UserRequest request = new UserRequest();
        request.setEmail("busy@mail.com");

        User existing = new User();
        existing.setId(userId);
        existing.setEmail("old@mail.com");
        existing.setUserCredentialsId(authId);

        User another = new User();
        another.setId(2L);
        another.setEmail("busy@mail.com");

        when(userRepository.findById(userId)).thenReturn(Optional.of(existing));
        when(userRepository.findByEmail("busy@mail.com")).thenReturn(Optional.of(another));
        jwtUtilsMock.when(JwtUtils::getUserCredentialsIdFromToken).thenReturn(authId);

        assertThatThrownBy(() -> userService.updateUserById(userId, request))
                .isInstanceOf(AlreadyExistsException.class)
                .hasMessageContaining("Email 'busy@mail.com' already in use");

        verify(userRepository).findById(userId);
        verify(userRepository).findByEmail("busy@mail.com");
        verifyNoMoreInteractions(userMapper);
        jwtUtilsMock.verify(JwtUtils::getUserCredentialsIdFromToken);
    }

    @Test
    void updateUserById_ShouldThrowAccessDenied_WhenOwnerMismatch() {

        Long userId = 1L;
        Long authId = 999L; // не владелец

        UserRequest request = new UserRequest();
        request.setEmail("whatever@mail.com");

        User existing = new User();
        existing.setId(userId);
        existing.setEmail("old@mail.com");
        existing.setUserCredentialsId(100L);

        when(userRepository.findById(userId)).thenReturn(Optional.of(existing));
        jwtUtilsMock.when(JwtUtils::getUserCredentialsIdFromToken).thenReturn(authId);

        assertThatThrownBy(() -> userService.updateUserById(userId, request))
                .isInstanceOf(org.springframework.security.access.AccessDeniedException.class)
                .hasMessageContaining("Access denied");

        verify(userRepository).findById(userId);
        verifyNoMoreInteractions(userRepository, userMapper);
        jwtUtilsMock.verify(JwtUtils::getUserCredentialsIdFromToken);
    }

    @Test
    void updateUserById_ShouldThrowNotFound_WhenUserMissing() {

        Long userId = 999L;
        UserRequest request = new UserRequest();
        request.setEmail("maks@gmail.com");

        when(userRepository.findById(userId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> userService.updateUserById(userId, request))
                .isInstanceOf(NotFoundException.class)
                .hasMessageContaining("User id=999 not found");

        verify(userRepository).findById(userId);
        verifyNoMoreInteractions(userRepository, userMapper);
        jwtUtilsMock.verifyNoInteractions();
    }

    @Test
    void deleteUserById_ShouldDelete_WhenOwnerMatches() {

        Long userId = 1L;
        Long authId = 100L;

        User existing = new User();
        existing.setId(userId);
        existing.setUserCredentialsId(authId);

        when(userRepository.findById(userId)).thenReturn(Optional.of(existing));
        jwtUtilsMock.when(JwtUtils::getUserCredentialsIdFromToken).thenReturn(authId);

        userService.deleteUserById(userId);

        verify(userRepository).findById(userId);
        verify(userRepository).delete(existing);
        jwtUtilsMock.verify(JwtUtils::getUserCredentialsIdFromToken);
    }

    @Test
    void deleteUserById_ShouldThrowNotFound_WhenUserMissing() {

        Long userId = 999L;

        when(userRepository.findById(userId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> userService.deleteUserById(userId))
                .isInstanceOf(NotFoundException.class)
                .hasMessageContaining("User id=999 not found");

        verify(userRepository).findById(userId);
        verifyNoMoreInteractions(userRepository, userMapper);
        jwtUtilsMock.verifyNoInteractions();
    }

    @Test
    void deleteUserById_ShouldThrowAccessDenied_WhenOwnerMismatch() {

        Long userId = 1L;

        User existing = new User();
        existing.setId(userId);
        existing.setUserCredentialsId(100L);

        when(userRepository.findById(userId)).thenReturn(Optional.of(existing));
        jwtUtilsMock.when(JwtUtils::getUserCredentialsIdFromToken).thenReturn(999L);

        assertThatThrownBy(() -> userService.deleteUserById(userId))
                .isInstanceOf(org.springframework.security.access.AccessDeniedException.class)
                .hasMessageContaining("Access denied");

        verify(userRepository).findById(userId);
        verify(userRepository, never()).delete(any());
        jwtUtilsMock.verify(JwtUtils::getUserCredentialsIdFromToken);
    }

    @Test
    void getByUserCredentialsId_ShouldReturnUser_WhenUserExists() {

        Long credentialsId = 123L;
        User user = User.builder()
                .id(1L)
                .userCredentialsId(credentialsId)
                .name("Test")
                .surname("User")
                .birthDate(LocalDate.of(2000, 1, 1))
                .email("test@example.com")
                .build();

        when(userRepository.findByUserCredentialsId(credentialsId)).thenReturn(Optional.of(user));
        when(userMapper.toDto(user)).thenReturn(UserResponse.builder()
                .id(user.getId())
                .name(user.getName())
                .surname(user.getSurname())
                .birthDate(user.getBirthDate())
                .email(user.getEmail())
                .build());

        UserResponse response = userService.getByUserCredentialsId(credentialsId);

        assertThat(response).isNotNull();
        assertThat(response.getId()).isEqualTo(user.getId());
        assertThat(response.getEmail()).isEqualTo(user.getEmail());
    }

    @Test
    void getByUserCredentialsId_ShouldThrowNotFoundException_WhenUserNotFound() {

        Long credentialsId = 123L;
        when(userRepository.findByUserCredentialsId(credentialsId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> userService.getByUserCredentialsId(credentialsId))
                .isInstanceOf(NotFoundException.class)
                .hasMessageContaining("User with credentials id=" + credentialsId + " not found");
    }
}
