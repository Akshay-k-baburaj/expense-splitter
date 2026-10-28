package com.akshay.expense_splitter.user;

import com.akshay.expense_splitter.exception.AccountNotFoundException;
import com.akshay.expense_splitter.exception.GlobalExceptionHandler;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(MockitoExtension.class)
class UserControllerTest {

    private MockMvc mockMvc;

    @Mock
    private UserService userService;

    @InjectMocks
    private UserController userController;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(userController)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    void createUserAccount_Success() throws Exception {
        UserResponse response = new UserResponse("akshay", "akshay@example.com");
        when(userService.createUserAccount(any(UserRequest.class))).thenReturn(response);

        String json = """
                {
                    "username": "akshay",
                    "email": "akshay@example.com"
                }
                """;

        mockMvc.perform(post("/api/v1/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.username").value("akshay"))
                .andExpect(jsonPath("$.email").value("akshay@example.com"));
    }

    @Test
    void createUserAccount_ValidationFailure_BlankUsername() throws Exception {
        String json = """
                {
                    "username": "",
                    "email": "akshay@example.com"
                }
                """;

        mockMvc.perform(post("/api/v1/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.username").exists());

        verify(userService, never()).createUserAccount(any());
    }

    @Test
    void createUserAccount_ValidationFailure_InvalidEmail() throws Exception {
        String json = """
                {
                    "username": "akshay",
                    "email": "invalid-email"
                }
                """;

        mockMvc.perform(post("/api/v1/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.email").exists());

        verify(userService, never()).createUserAccount(any());
    }

    @Test
    void getAccountById_Success() throws Exception {
        UserResponse response = new UserResponse("akshay", "akshay@example.com");
        when(userService.getUserById(1L)).thenReturn(response);

        mockMvc.perform(get("/api/v1/users/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.username").value("akshay"))
                .andExpect(jsonPath("$.email").value("akshay@example.com"));
    }

    @Test
    void getAccountById_NotFound() throws Exception {
        when(userService.getUserById(99L)).thenThrow(new AccountNotFoundException("User not found with id : 99"));

        mockMvc.perform(get("/api/v1/users/99"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("User not found with id : 99"));
    }

    @Test
    void updateAccountDetails_Success() throws Exception {
        UserResponse response = new UserResponse("updatedUser", "updated@example.com");
        when(userService.updateAccountById(eq(1L), any(UserRequest.class))).thenReturn(response);

        String json = """
                {
                    "username": "updatedUser",
                    "email": "updated@example.com"
                }
                """;

        mockMvc.perform(put("/api/v1/users/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.username").value("updatedUser"))
                .andExpect(jsonPath("$.email").value("updated@example.com"));
    }

    @Test
    void updateAccountDetails_ValidationFailure() throws Exception {
        String json = """
                {
                    "username": "updatedUser",
                    "email": "not-an-email"
                }
                """;

        mockMvc.perform(put("/api/v1/users/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.email").exists());

        verify(userService, never()).updateAccountById(anyLong(), any());
    }

    @Test
    void updateAccountDetails_NotFound() throws Exception {
        when(userService.updateAccountById(eq(99L), any(UserRequest.class)))
                .thenThrow(new AccountNotFoundException("User not found with id : 99"));

        String json = """
                {
                    "username": "updatedUser",
                    "email": "updated@example.com"
                }
                """;

        mockMvc.perform(put("/api/v1/users/99")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("User not found with id : 99"));
    }

    @Test
    void deactivateAccount_Success() throws Exception {
        doNothing().when(userService).deactivateAccount(1L);

        mockMvc.perform(delete("/api/v1/users/1"))
                .andExpect(status().isNoContent());

        verify(userService).deactivateAccount(1L);
    }

    @Test
    void deactivateAccount_NotFound() throws Exception {
        doThrow(new AccountNotFoundException("User not found with id : 99")).when(userService).deactivateAccount(99L);

        mockMvc.perform(delete("/api/v1/users/99"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("User not found with id : 99"));
    }
}
