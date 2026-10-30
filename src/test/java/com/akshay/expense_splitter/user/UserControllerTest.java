package com.akshay.expense_splitter.user;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.oauth2.jwt.Jwt;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UserControllerTest {
    @Mock UserService userService;
    @InjectMocks UserController userController;
    private final Jwt principal = Jwt.withTokenValue("signed-token").header("alg", "none").subject("auth0|user").build();

    @Test
    void getMeReturnsOnlyAuthenticatedProfile() {
        UserResponse response = new UserResponse(4L, "akshay", "akshay@example.com");
        when(userService.getCurrentUser(principal)).thenReturn(response);

        var result = userController.getCurrentUser(principal);

        assertEquals(200, result.getStatusCode().value());
        assertEquals(4L, result.getBody().getId());
        verify(userService).getCurrentUser(principal);
    }

    @Test
    void createProfilePassesAuthenticatedPrincipal() {
        CreateUserProfileRequest request = new CreateUserProfileRequest("akshay");
        UserResponse response = new UserResponse(4L, "akshay", "akshay@example.com");
        when(userService.createCurrentUser(principal, request)).thenReturn(response);

        var result = userController.createCurrentUser(principal, request);

        assertEquals(201, result.getStatusCode().value());
        verify(userService).createCurrentUser(principal, request);
    }

    @Test
    void deactivationReturnsNoContent() {
        var result = userController.deactivateCurrentUser(principal);

        assertEquals(204, result.getStatusCode().value());
        verify(userService).deactivateCurrentUser(principal);
    }
}
