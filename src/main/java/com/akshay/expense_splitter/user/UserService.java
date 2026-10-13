package com.akshay.expense_splitter.user;

import com.akshay.expense_splitter.exception.AccountNotFoundException;
import jakarta.validation.Valid;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Service
public class UserService {

    private final UserRepository userRepository;
    private final UserMapper userMapper;

    public UserService(UserRepository userRepository,UserMapper userMapper){
        this.userRepository = userRepository;
        this.userMapper = userMapper;
    }

    @Transactional
    public UserResponse createUserAccount(UserRequest userRequest) {
       User user = userMapper.toEntity(userRequest);
       userRepository.save(user);
       return userMapper.toResponse(user);
    }

    public UserResponse getUserById(Long id) {
        User user = userRepository.findById(id).orElseThrow(() -> new AccountNotFoundException("User not found with id : " + id));
        return userMapper.toResponse(user);
    }

    @Transactional
    public UserResponse updateAccountById(Long id, @Valid UserRequest userRequest) {
        User existingUser = userRepository.findById(id).orElseThrow(() -> new AccountNotFoundException("User not found with id : " + id));
        existingUser.setUsername(userRequest.getUsername());
        existingUser.setEmail(userRequest.getEmail());
        User savedUser = userRepository.save(existingUser);
        return userMapper.toResponse(savedUser);
    }
}
