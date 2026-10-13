package com.akshay.expense_splitter.user;

import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/users")
public class UserController {
    private final UserService userService;

    public UserController(UserService userService){
        this.userService = userService;
    }

    @PostMapping
    public ResponseEntity<UserResponse> createUserAccount(@Valid @RequestBody UserRequest userRequest){
        UserResponse createdUser = userService.createUserAccount(userRequest);
        return ResponseEntity.status(HttpStatus.CREATED).body(createdUser);
    }

    @GetMapping("/{id}")
    public ResponseEntity<UserResponse> getAccountById(@PathVariable Long id){
        UserResponse userById = userService.getUserById(id);
        return ResponseEntity.status(HttpStatus.OK).body(userById);
    }

    @PutMapping("/{id}")
    public ResponseEntity<UserResponse> updateAccountDetails(@PathVariable Long id, @Valid @RequestBody UserRequest userRequest){
        UserResponse updatedUser = userService.updateAccountById(id,userRequest);
        return ResponseEntity.status(HttpStatus.OK).body(updatedUser);
    }
}
