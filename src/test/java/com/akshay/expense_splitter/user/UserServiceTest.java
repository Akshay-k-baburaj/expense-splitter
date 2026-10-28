package com.akshay.expense_splitter.user;

import com.akshay.expense_splitter.exception.AccountNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private UserMapper userMapper;

    @InjectMocks
    private UserService userService;

    private User user;
    private UserRequest userRequest;
    private UserResponse userResponse;

    @BeforeEach
    void setUp() {
        user = new User();
        user.setId(1L);
        user.setUsername("akshay");
        user.setEmail("akshay@example.com");
        user.setActive(true);

        userRequest = new UserRequest("akshay", "akshay@example.com");
        userResponse = new UserResponse("akshay", "akshay@example.com");
    }

    @Test
    void createUserAccount_Success() {
        when(userMapper.toEntity(userRequest)).thenReturn(user);
        when(userRepository.save(user)).thenReturn(user);
        when(userMapper.toResponse(user)).thenReturn(userResponse);

        UserResponse result = userService.createUserAccount(userRequest);

        assertNotNull(result);
        assertEquals("akshay", result.getUsername());
        assertEquals("akshay@example.com", result.getEmail());
        verify(userRepository).save(user);
    }

    @Test
    void getUserById_Success() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(userMapper.toResponse(user)).thenReturn(userResponse);

        UserResponse result = userService.getUserById(1L);

        assertNotNull(result);
        assertEquals("akshay", result.getUsername());
        verify(userRepository).findById(1L);
    }

    @Test
    void getUserById_NotFound_ThrowsAccountNotFoundException() {
        when(userRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(AccountNotFoundException.class, () -> userService.getUserById(99L));
    }

    @Test
    void updateAccountById_Success() {
        UserRequest updateRequest = new UserRequest("newname", "newname@example.com");
        User updatedUser = new User();
        updatedUser.setId(1L);
        updatedUser.setUsername("newname");
        updatedUser.setEmail("newname@example.com");
        UserResponse updatedResponse = new UserResponse("newname", "newname@example.com");

        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(userRepository.save(any(User.class))).thenReturn(updatedUser);
        when(userMapper.toResponse(updatedUser)).thenReturn(updatedResponse);

        UserResponse result = userService.updateAccountById(1L, updateRequest);

        assertNotNull(result);
        assertEquals("newname", result.getUsername());
        assertEquals("newname@example.com", result.getEmail());
        verify(userRepository).save(user);
    }

    @Test
    void updateAccountById_NotFound_ThrowsAccountNotFoundException() {
        UserRequest updateRequest = new UserRequest("newname", "newname@example.com");
        when(userRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(AccountNotFoundException.class, () -> userService.updateAccountById(99L, updateRequest));
        verify(userRepository, never()).save(any());
    }

    @Test
    void deactivateAccount_Success() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));

        userService.deactivateAccount(1L);

        assertFalse(user.isActive());
        assertEquals("archived_1_akshay", user.getUsername());
        assertEquals("archived_1_akshay@example.com", user.getEmail());
        verify(userRepository).save(user);
    }

    @Test
    void deactivateAccount_NotFound_ThrowsAccountNotFoundException() {
        when(userRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(AccountNotFoundException.class, () -> userService.deactivateAccount(99L));
        verify(userRepository, never()).save(any());
    }
}
