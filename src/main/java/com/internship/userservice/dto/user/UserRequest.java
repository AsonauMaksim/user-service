package com.internship.userservice.dto.user;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PastOrPresent;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserRequest {

    @NotBlank(message = "Name is required")
    @Size(max = 50)
    private String name;

    @Size(max = 50)
    private String surname;

    @NotNull(message = "Birth date is required")
    @PastOrPresent(message = "Birth date can't be in the future")
    private LocalDate birthDate;

    @NotBlank(message = "Email is required")
    @Email(message = "Email is not valid")
    private String email;
}
