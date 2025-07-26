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

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.AssertionsForClassTypes.assertThatThrownBy;
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
    void create_ShouldCreateUser_WhenEmailIsUnique() {

        UserRequest request = new UserRequest();
        request.setEmail("maks@gmail.com");

        User userToSave = new User();
        userToSave.setEmail("maks@gmail.com");

        User savedUser = new User();
        savedUser.setId(1L);
        savedUser.setEmail("maks@gmail.com");

        UserResponse expectedResponse = new UserResponse();
        expectedResponse.setId(1L);
        expectedResponse.setEmail("maks@gmail.com");

        when(userRepository.findByEmail(request.getEmail())).thenReturn(Optional.empty());
        when(userMapper.toEntity(request)).thenReturn(userToSave);
        when(userRepository.save(userToSave)).thenReturn(savedUser);
        when(userMapper.toDto(savedUser)).thenReturn(expectedResponse);

        UserResponse result = userService.create(request);

        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo(1L);
        assertThat(result.getEmail()).isEqualTo("maks@gmail.com");

        verify(userRepository).findByEmail("maks@gmail.com");
        verify(userMapper).toEntity(request);
        verify(userRepository).save(userToSave);
        verify(userMapper).toDto(savedUser);
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
    void updateUserById_ShouldUpdateUser_WhenUserExistsAndEmailIsUnchanged() {

        Long userId = 1L;

        UserRequest request = new UserRequest();
        request.setEmail("maks@gmail.com");

        User existingUser = new User();
        existingUser.setId(userId);
        existingUser.setEmail("maks@gmail.com");

        UserResponse expectedResponse = new UserResponse();
        expectedResponse.setId(userId);
        expectedResponse.setEmail("maks@gmail.com");

        when(userRepository.findById(userId)).thenReturn(Optional.of(existingUser));
        when(userMapper.toDto(existingUser)).thenReturn(expectedResponse);

        UserResponse result = userService.updateUserById(userId, request);

        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo(userId);
        assertThat(result.getEmail()).isEqualTo("maks@gmail.com");

        verify(userRepository).findById(userId);
        verify(userMapper).updateEntity(existingUser, request);
        verify(userMapper).toDto(existingUser);
    }

    @Test
    void updateUserById_ShouldThrowAlreadyExistsException_WhenEmailAlreadyInUse() {

        Long userId = 1L;

        UserRequest request = new UserRequest();
        request.setEmail("other@example.com");

        User existingUser = new User();
        existingUser.setId(userId);
        existingUser.setEmail("maks@gmail.com");

        User anotherUser = new User();
        anotherUser.setId(2L);
        anotherUser.setEmail("other@example.com");

        when(userRepository.findById(userId)).thenReturn(Optional.of(existingUser));
        when(userRepository.findByEmail("other@example.com")).thenReturn(Optional.of(anotherUser));

        assertThatThrownBy(() -> userService.updateUserById(userId, request))
                .isInstanceOf(AlreadyExistsException.class)
                .hasMessageContaining("Email 'other@example.com' already in use");

        verify(userRepository).findById(userId);
        verify(userRepository).findByEmail("other@example.com");
        verifyNoMoreInteractions(userMapper);
    }

    @Test
    void updateUserById_ShouldThrowNotFoundException_WhenUserDoesNotExist() {
        Long userId = 999L;
        UserRequest request = new UserRequest();
        request.setEmail("maks@gmail.com");

        when(userRepository.findById(userId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> userService.updateUserById(userId, request))
                .isInstanceOf(NotFoundException.class)
                .hasMessageContaining("User id=999 not found");

        verify(userRepository).findById(userId);
        verifyNoMoreInteractions(userRepository, userMapper);
    }

    @Test
    void deleteUserById_ShouldDeleteUser_WhenUserExists() {
        Long userId = 1L;

        when(userRepository.existsById(userId)).thenReturn(true);

        userService.deleteUserById(userId);

        verify(userRepository).existsById(userId);
        verify(userRepository).deleteById(userId);
    }

    @Test
    void deleteUserById_ShouldThrowNotFoundException_WhenUserDoesNotExist() {

        Long userId = 1488L;

        when(userRepository.existsById(userId)).thenReturn(false);

        assertThatThrownBy(() -> userService.deleteUserById(userId))
                .isInstanceOf(NotFoundException.class)
                .hasMessageContaining("User id=1488 not found");

        verify(userRepository).existsById(userId);
        verifyNoMoreInteractions(userRepository);
    }
}
