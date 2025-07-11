package com.internship.userservice.repository;

import com.internship.userservice.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {

    Optional<User> findByEmail(String email);

    @Query("SELECT u FROM User u WHERE u.name = :name AND u.surname = :surname")
    List<User> findByFullName(@Param("name") String name, @Param("surname") String surname);

    @Query(value = "SELECT * FROM users WHERE email ILIKE %:domain", nativeQuery = true)
    List<User> findAllByEmailDomain(@Param("domain") String domain);
}
