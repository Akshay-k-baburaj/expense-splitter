package com.akshay.expense_splitter.user;

import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/users/me")
public class UserController {
    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    @GetMapping
    public ResponseEntity<UserResponse> getCurrentUser(@AuthenticationPrincipal Jwt principal) {
        return ResponseEntity.ok(userService.getCurrentUser(principal));
    }

    @PostMapping
    public ResponseEntity<UserResponse> createCurrentUser(
            @AuthenticationPrincipal Jwt principal,
            @Valid @RequestBody CreateUserProfileRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(userService.createCurrentUser(principal, request));
    }

    @PutMapping
    public ResponseEntity<UserResponse> updateCurrentUser(
            @AuthenticationPrincipal Jwt principal,
            @Valid @RequestBody UpdateUserProfileRequest request) {
        return ResponseEntity.ok(userService.updateCurrentUser(principal, request));
    }

    @DeleteMapping
    public ResponseEntity<Void> deactivateCurrentUser(@AuthenticationPrincipal Jwt principal) {
        userService.deactivateCurrentUser(principal);
        return ResponseEntity.noContent().build();
    }
}
