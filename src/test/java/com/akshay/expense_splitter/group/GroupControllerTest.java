package com.akshay.expense_splitter.group;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.oauth2.jwt.Jwt;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class GroupControllerTest {
    @Mock GroupService groupService;
    @InjectMocks GroupController groupController;
    private final Jwt principal = Jwt.withTokenValue("signed-token").header("alg", "none").subject("auth0|user").build();

    @Test
    void listGroupsUsesAuthenticatedPrincipal() {
        when(groupService.getGroupsForCurrentUser(principal)).thenReturn(List.of());

        assertEquals(200, groupController.getGroupsForCurrentUser(principal).getStatusCode().value());
        verify(groupService).getGroupsForCurrentUser(principal);
    }

    @Test
    void createGroupPassesPrincipalAndReturnsCreated() {
        GroupRequest request = new GroupRequest("Trip", "Description");
        GroupResponse response = new GroupResponse(3L, "Trip", "Description", 7L, true);
        when(groupService.createGroup(request, principal)).thenReturn(response);

        var result = groupController.createGroup(principal, request);

        assertEquals(201, result.getStatusCode().value());
        assertEquals(7L, result.getBody().getOwnerId());
        verify(groupService).createGroup(request, principal);
    }

    @Test
    void ownershipTransferUsesAuthenticatedPrincipal() {
        TransferGroupOwnershipRequest request = new TransferGroupOwnershipRequest(9L);
        GroupResponse response = new GroupResponse(3L, "Trip", "Description", 9L, false);
        when(groupService.transferOwnership(3L, request, principal)).thenReturn(response);

        var result = groupController.transferOwnership(3L, principal, request);

        assertEquals(200, result.getStatusCode().value());
        assertEquals(9L, result.getBody().getOwnerId());
        verify(groupService).transferOwnership(3L, request, principal);
    }
}
