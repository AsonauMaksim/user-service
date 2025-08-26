package com.internship.userservice.service.unit;

import com.internship.userservice.dto.user.UserRequest;
import com.internship.userservice.dto.user.UserResponse;
import com.internship.userservice.entity.User;
import com.internship.userservice.exception.AlreadyExistsException;
import com.internship.userservice.exception.NotFoundException;
import com.internship.userservice.mapper.UserMapper;
import com.internship.userservice.repository.UserRepository;
import com.internship.userservice.service.impl.UserServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.AssertionsForClassTypes.assertThatThrownBy;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

public class UserServiceImplTest {

    private UserRepository userRepository;
    private UserMapper userMapper;
    private UserServiceImpl userService;

    @BeforeEach
    void setUp() {
        userRepository = mock(UserRepository.class);
        userMapper = mock(UserMapper.class);
        userService = new UserServiceImpl(userRepository, userMapper);
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

        UserResponse result = userService.create(request, 100L);

        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo(1L);
        assertThat(result.getEmail()).isEqualTo("maks@gmail.com");
        assertThat(userToSave.getUserCredentialsId()).isEqualTo(100L);

        verify(userRepository).findByEmail("maks@gmail.com");
        verify(userMapper).toEntity(request);
        verify(userRepository).save(userToSave);
        verify(userMapper).toDto(savedUser);
    }

    @Test
    void create_ShouldThrowAlreadyExistsException_WhenEmailExists() {

        UserRequest request = new UserRequest();
        request.setEmail("maks@gmail.com");

        when(userRepository.findByEmail(request.getEmail())).thenReturn(Optional.of(new User()));

        assertThatThrownBy(() -> userService.create(request, 100L))
                .isInstanceOf(AlreadyExistsException.class)
                .hasMessageContaining("User with email 'maks@gmail.com' already exists");

        verify(userRepository).findByEmail("maks@gmail.com");
        verifyNoMoreInteractions(userMapper, userRepository);
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
        UserRequest updateRequest = new UserRequest();
        updateRequest.setEmail("maks@gmail.com");

        User existingUser = new User();
        existingUser.setId(userId);
        existingUser.setEmail("maks@gmail.com");
        existingUser.setUserCredentialsId(100L);

        UserResponse expectedResponse = new UserResponse();
        expectedResponse.setId(userId);
        expectedResponse.setEmail("maks@gmail.com");

        when(userRepository.findById(userId)).thenReturn(Optional.of(existingUser));
        doNothing().when(userMapper).updateEntity(existingUser, updateRequest);
        when(userRepository.save(existingUser)).thenReturn(existingUser);
        when(userMapper.toDto(existingUser)).thenReturn(expectedResponse);

        UserResponse result = userService.updateUserById(userId, updateRequest, 100L);

        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo(userId);
        assertThat(result.getEmail()).isEqualTo("maks@gmail.com");

        verify(userRepository).findById(userId);
        verify(userMapper).updateEntity(existingUser, updateRequest);
        verify(userMapper).toDto(existingUser);
    }

    @Test
    void updateUserById_ShouldUpdate_WhenOwnerAndEmailChangedToUnique() {

        Long userId = 1L;
        UserRequest updateRequest = new UserRequest();
        updateRequest.setEmail("new@gmail.com");

        User existingUser = new User();
        existingUser.setId(userId);
        existingUser.setEmail("maks@gmail.com");
        existingUser.setUserCredentialsId(100L);

        UserResponse expectedResponse = new UserResponse();
        expectedResponse.setId(userId);
        expectedResponse.setEmail("new@gmail.com");

        when(userRepository.findById(userId)).thenReturn(Optional.of(existingUser));
        when(userRepository.findByEmail("new@gmail.com")).thenReturn(Optional.empty());
        doNothing().when(userMapper).updateEntity(existingUser, updateRequest);
        when(userRepository.save(existingUser)).thenReturn(existingUser);
        when(userMapper.toDto(existingUser)).thenReturn(expectedResponse);

        UserResponse result = userService.updateUserById(userId, updateRequest, 100L);

        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo(userId);
        assertThat(result.getEmail()).isEqualTo("new@gmail.com");

        verify(userRepository).findById(userId);
        verify(userRepository).findByEmail("new@gmail.com");
        verify(userMapper).updateEntity(existingUser, updateRequest);
        verify(userMapper).toDto(existingUser);
    }

    @Test
    void updateUserById_ShouldThrowAlreadyExistsException_WhenEmailIsUsedByOther() {
        Long userId = 1L;
        UserRequest updateRequest = new UserRequest();
        updateRequest.setEmail("other@gmail.com");

        User existingUser = new User();
        existingUser.setId(userId);
        existingUser.setEmail("maks@gmail.com");
        existingUser.setUserCredentialsId(100L);

        User otherUser = new User();
        otherUser.setId(2L);
        otherUser.setEmail("other@gmail.com");

        when(userRepository.findById(userId)).thenReturn(Optional.of(existingUser));
        when(userRepository.findByEmail("other@gmail.com")).thenReturn(Optional.of(otherUser));

        assertThatThrownBy(() -> userService.updateUserById(userId, updateRequest, 100L))
                .isInstanceOf(AlreadyExistsException.class)
                .hasMessageContaining("Email 'other@gmail.com' already in use");

        verify(userRepository).findById(userId);
        verify(userRepository).findByEmail("other@gmail.com");
        verifyNoMoreInteractions(userRepository, userMapper);
    }

    @Test
    void updateUserById_ShouldThrowAccessDeniedException_WhenOwnerMismatch() {
        Long userId = 1L;
        UserRequest updateRequest = new UserRequest();
        updateRequest.setEmail("maks@gmail.com");

        User existingUser = new User();
        existingUser.setId(userId);
        existingUser.setEmail("maks@gmail.com");
        existingUser.setUserCredentialsId(999L);

        when(userRepository.findById(userId)).thenReturn(Optional.of(existingUser));

        assertThatThrownBy(() -> userService.updateUserById(userId, updateRequest, 100L))
                .isInstanceOf(org.springframework.security.access.AccessDeniedException.class)
                .hasMessageContaining("Access denied");

        verify(userRepository).findById(userId);
        verifyNoMoreInteractions(userRepository, userMapper);
    }

    @Test
    void updateUserById_ShouldThrowNotFoundException_WhenUserNotFound() {
        Long userId = 999L;
        UserRequest updateRequest = new UserRequest();
        updateRequest.setEmail("maks@gmail.com");

        when(userRepository.findById(userId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> userService.updateUserById(userId, updateRequest, 100L))
                .isInstanceOf(NotFoundException.class)
                .hasMessageContaining("User id=999 not found");

        verify(userRepository).findById(userId);
        verifyNoMoreInteractions(userRepository, userMapper);
    }

    @Test
    void deleteUserById_ShouldDeleteUser_WhenOwnerMatches() {
        Long userId = 1L;
        User existingUser = new User();
        existingUser.setId(userId);
        existingUser.setEmail("maks@gmail.com");
        existingUser.setUserCredentialsId(100L);

        when(userRepository.findById(userId)).thenReturn(Optional.of(existingUser));
        doNothing().when(userRepository).delete(existingUser);

        userService.deleteUserById(userId, 100L);

        verify(userRepository).findById(userId);
        verify(userRepository).delete(existingUser);
    }

    @Test
    void deleteUserById_ShouldThrowNotFoundException_WhenUserNotFound() {
        Long userId = 999L;

        when(userRepository.findById(userId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> userService.deleteUserById(userId, 100L))
                .isInstanceOf(NotFoundException.class)
                .hasMessageContaining("User id=999 not found");

        verify(userRepository).findById(userId);
        verifyNoMoreInteractions(userRepository);
    }

    @Test
    void deleteUserById_ShouldThrowAccessDeniedException_WhenOwnerMismatch() {
        Long userId = 1L;
        User existingUser = new User();
        existingUser.setId(userId);
        existingUser.setEmail("maks@gmail.com");
        existingUser.setUserCredentialsId(999L);

        when(userRepository.findById(userId)).thenReturn(Optional.of(existingUser));

        assertThatThrownBy(() -> userService.deleteUserById(userId, 100L))
                .isInstanceOf(org.springframework.security.access.AccessDeniedException.class)
                .hasMessageContaining("Access denied");

        verify(userRepository).findById(userId);
        verifyNoMoreInteractions(userRepository);
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
