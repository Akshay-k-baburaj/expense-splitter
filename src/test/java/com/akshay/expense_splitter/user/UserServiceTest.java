package com.akshay.expense_splitter.user;

import com.akshay.expense_splitter.exception.DuplicateUserException;
import com.akshay.expense_splitter.exception.EmailVerificationRequiredException;
import com.akshay.expense_splitter.exception.LegacyAccountLinkRequiredException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.oauth2.jwt.Jwt;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {
    private static final String EMAIL_CLAIM = "https://expense-splitter.example/claims/email";
    private static final String VERIFIED_CLAIM = "https://expense-splitter.example/claims/email_verified";
    @Mock UserRepository userRepository;
    @Mock UserMapper userMapper;
    private UserService userService;
    private Jwt verifiedPrincipal;

    @BeforeEach
    void setUp() {
        userService = new UserService(userRepository, userMapper, EMAIL_CLAIM, VERIFIED_CLAIM);
        verifiedPrincipal = Jwt.withTokenValue("signed-token").header("alg", "none")
                .subject("auth0|person").claim(EMAIL_CLAIM, "person@example.com")
                .claim(VERIFIED_CLAIM, true).build();
    }

    @Test
    void createsProfileUsingVerifiedTokenIdentity() {
        CreateUserProfileRequest request = new CreateUserProfileRequest(" person ");
        User saved = user("person", "person@example.com", "auth0|person");
        when(userRepository.findByAuth0Subject("auth0|person")).thenReturn(Optional.empty());
        when(userRepository.existsByEmail("person@example.com")).thenReturn(false);
        when(userRepository.existsByUsername("person")).thenReturn(false);
        when(userRepository.saveAndFlush(any(User.class))).thenReturn(saved);
        when(userMapper.toResponse(saved)).thenReturn(new UserResponse(1L, "person", "person@example.com"));

        UserResponse result = userService.createCurrentUser(verifiedPrincipal, request);

        assertEquals(1L, result.getId());
        verify(userRepository).saveAndFlush(argThat(user -> user.getUsername().equals("person")
                && user.getAuth0Subject().equals("auth0|person")));
    }

    @Test
    void rejectsUnverifiedEmail() {
        Jwt unverified = Jwt.withTokenValue("signed-token").header("alg", "none")
                .subject("auth0|person").claim(EMAIL_CLAIM, "person@example.com")
                .claim(VERIFIED_CLAIM, false).build();
        when(userRepository.findByAuth0Subject("auth0|person")).thenReturn(Optional.empty());

        assertThrows(EmailVerificationRequiredException.class,
                () -> userService.createCurrentUser(unverified, new CreateUserProfileRequest("person")));
        verify(userRepository, never()).saveAndFlush(any());
    }

    @Test
    void doesNotAutomaticallyLinkLegacyAccountByEmail() {
        when(userRepository.findByAuth0Subject("auth0|person")).thenReturn(Optional.empty());
        when(userRepository.existsByEmail("person@example.com")).thenReturn(true);

        assertThrows(LegacyAccountLinkRequiredException.class,
                () -> userService.createCurrentUser(verifiedPrincipal, new CreateUserProfileRequest("person")));
        verify(userRepository, never()).saveAndFlush(any());
    }

    @Test
    void rejectsDuplicateUsername() {
        when(userRepository.findByAuth0Subject("auth0|person")).thenReturn(Optional.empty());
        when(userRepository.existsByEmail("person@example.com")).thenReturn(false);
        when(userRepository.existsByUsername("person")).thenReturn(true);

        assertThrows(DuplicateUserException.class,
                () -> userService.createCurrentUser(verifiedPrincipal, new CreateUserProfileRequest("person")));
        verify(userRepository, never()).saveAndFlush(any());
    }

    private static User user(String username, String email, String subject) {
        User user = new User();
        user.setId(1L);
        user.setUsername(username);
        user.setEmail(email);
        user.setAuth0Subject(subject);
        return user;
    }
}
