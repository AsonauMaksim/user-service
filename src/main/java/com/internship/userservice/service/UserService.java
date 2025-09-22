package com.internship.userservice.service;

import com.internship.userservice.dto.user.UserRequest;
import com.internship.userservice.dto.user.UserResponse;

import java.util.List;

public interface UserService {

    UserResponse create(UserRequest dto, Long userCredentialsId);
    UserResponse getUserById(Long id);
    UserResponse getUserByEmail(String email);
    List<UserResponse> getUsersByIds(List<Long> ids);
    UserResponse updateUserById(Long id, UserRequest dto, Long userCredentialsId);
    void deleteUserById(Long id, Long userCredentialsId);
    List<UserResponse> getAllUsers();
    UserResponse getByUserCredentialsId(Long userCredentialsId);
}

