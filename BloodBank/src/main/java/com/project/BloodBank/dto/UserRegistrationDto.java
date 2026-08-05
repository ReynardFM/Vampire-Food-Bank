package com.project.BloodBank.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

// What the registration form is allowed to submit.
//
// A DTO - Data Transfer Object - is a plain class that carries form values and nothing else. Using
// one instead of binding the User entity directly is the point: this class has no role and no
// active field, so no amount of tampering with the posted data can set them.
//
// The annotations are Jakarta Bean Validation. They are checked when a controller parameter is
// marked @Valid, and each message is what the user sees beside the field.
public class UserRegistrationDto {

    // @NotBlank rejects null, empty, and whitespace only - unlike @NotNull, which would accept " ".
    @NotBlank(message = "Full name is required")
    private String fullName;

    // @Email checks the shape of the address, not that it exists.
    @NotBlank(message = "Email is required")
    @Email(message = "Invalid email format")
    private String email;

    // Held in plain text only for the moment it takes to reach UserService, which hashes it before
    // anything is stored. It is never written to the database in this form.
    @NotBlank(message = "Password is required")
    @Size(min = 8, message = "Password must be at least 8 characters")
    private String password;

    public UserRegistrationDto() {
    }

    public String getFullName() {
        return fullName;
    }

    public void setFullName(String fullName) {
        this.fullName = fullName;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }
}
