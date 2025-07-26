package com.internship.userservice.service.impl;

import com.internship.userservice.dto.user.UserRequest;
import com.internship.userservice.dto.user.UserResponse;
import com.internship.userservice.entity.User;
import com.internship.userservice.exception.AlreadyExistsException;
import com.internship.userservice.exception.NotFoundException;
import com.internship.userservice.mapper.UserMapper;
import com.internship.userservice.repository.UserRepository;
import com.internship.userservice.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.CachePut;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class UserServiceImpl implements UserService {

    private final UserRepository userRepository;
    private final UserMapper userMapper;

    @Override
    @Transactional
    public UserResponse create(UserRequest dto) {
        if (userRepository.findByEmail(dto.getEmail()).isPresent()) {
            throw new AlreadyExistsException("User with email '" + dto.getEmail() + "' already exists");
        }
        User user = userMapper.toEntity(dto);
        return userMapper.toDto(userRepository.save(user));
    }

    @Override
    @Cacheable(value = "users", key = "#id")
    public UserResponse getUserById(Long id) {
        return userMapper.toDto(userRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("User id=" + id + " not found")));
    }

    @Override
    @Cacheable(value = "usersByEmail", key = "#email")
    public UserResponse getUserByEmail(String email) {
        return userMapper.toDto(userRepository.findByEmail(email)
                .orElseThrow(() -> new NotFoundException("User email=" + email + " not found"))
        );
    }

    @Override
    public List<UserResponse> getUsersByIds(List<Long> ids) {
        return userMapper.toDtoList(userRepository.findAllById(ids));
    }

    @Transactional
    @Override
    @CachePut(value = "users", key = "#id")
    @CacheEvict(value = "usersByEmail", key = "#dto.email")
    public UserResponse updateUserById(Long id, UserRequest dto) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("User id=" + id + " not found"));

        if (!user.getEmail().equals(dto.getEmail()) && userRepository.findByEmail(dto.getEmail()).isPresent()) {
            throw new AlreadyExistsException("Email '" + dto.getEmail() + "' already in use");
        }

        userMapper.updateEntity(user, dto);
        return userMapper.toDto(user);
    }

    @Transactional
    @Override
    @CacheEvict(value = "users", key = "#id")
    public void deleteUserById(Long id) {
        if (!userRepository.existsById(id)) {
            throw new NotFoundException("User id=" + id + " not found");
        }
        userRepository.deleteById(id);
    }

    @Override
    public List<UserResponse> getAllUsers() {
        return userMapper.toDtoList(userRepository.findAll());
    }
}
