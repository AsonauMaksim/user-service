package com.internship.userservice.service;

import com.internship.userservice.dto.user.UserRequest;
import com.internship.userservice.dto.user.UserResponse;

import java.util.List;

public interface UserService {

    UserResponse create(UserRequest dto);

    UserResponse getUserById(Long id);

    UserResponse getUserByEmail(String email);

    List<UserResponse> getUsersByIds(List<Long> ids);

    UserResponse updateUserById(Long id, UserRequest dto);

    void deleteUserById(Long id);

    List<UserResponse> getAllUsers();
}

