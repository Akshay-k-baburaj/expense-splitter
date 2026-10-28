package com.akshay.expense_splitter.group;

import com.akshay.expense_splitter.exception.AccountNotFoundException;
import com.akshay.expense_splitter.exception.DuplicateMembershipException;
import com.akshay.expense_splitter.exception.GlobalExceptionHandler;
import com.akshay.expense_splitter.exception.GroupNotFoundException;
import com.akshay.expense_splitter.exception.MemberNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.time.OffsetDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(MockitoExtension.class)
class GroupControllerTest {

    private MockMvc mockMvc;

    @Mock
    private GroupService groupService;

    @InjectMocks
    private GroupController groupController;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(groupController)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    void createGroup_Success() throws Exception {
        GroupResponse response = new GroupResponse(1L, "Road Trip", "Trip details");
        when(groupService.createGroup(any(GroupRequest.class))).thenReturn(response);

        String json = """
                {
                    "groupName": "Road Trip",
                    "description": "Trip details"
                }
                """;

        mockMvc.perform(post("/api/v1/groups")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(1L))
                .andExpect(jsonPath("$.groupName").value("Road Trip"))
                .andExpect(jsonPath("$.description").value("Trip details"));
    }

    @Test
    void createGroup_ValidationFailure_BlankName() throws Exception {
        String json = """
                {
                    "groupName": "",
                    "description": "Trip details"
                }
                """;

        mockMvc.perform(post("/api/v1/groups")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.groupName").exists());

        verify(groupService, never()).createGroup(any());
    }

    @Test
    void getGroupById_Success() throws Exception {
        GroupResponse response = new GroupResponse(1L, "Road Trip", "Trip details");
        when(groupService.getGroupById(1L)).thenReturn(response);

        mockMvc.perform(get("/api/v1/groups/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1L))
                .andExpect(jsonPath("$.groupName").value("Road Trip"));
    }

    @Test
    void getGroupById_NotFound() throws Exception {
        when(groupService.getGroupById(99L)).thenThrow(new GroupNotFoundException("Group Not Found with id : 99"));

        mockMvc.perform(get("/api/v1/groups/99"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("Group Not Found with id : 99"));
    }

    @Test
    void updateGroupDetails_Success() throws Exception {
        GroupResponse response = new GroupResponse(1L, "Updated Trip", "New details");
        when(groupService.updateGroupDetails(eq(1L), any(GroupRequest.class))).thenReturn(response);

        String json = """
                {
                    "groupName": "Updated Trip",
                    "description": "New details"
                }
                """;

        mockMvc.perform(put("/api/v1/groups/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1L))
                .andExpect(jsonPath("$.groupName").value("Updated Trip"))
                .andExpect(jsonPath("$.description").value("New details"));
    }

    @Test
    void updateGroupDetails_ValidationFailure() throws Exception {
        String json = """
                {
                    "groupName": "   ",
                    "description": "New details"
                }
                """;

        mockMvc.perform(put("/api/v1/groups/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.groupName").exists());

        verify(groupService, never()).updateGroupDetails(anyLong(), any());
    }

    @Test
    void updateGroupDetails_NotFound() throws Exception {
        when(groupService.updateGroupDetails(eq(99L), any(GroupRequest.class)))
                .thenThrow(new GroupNotFoundException("Group Not Found with id : 99"));

        String json = """
                {
                    "groupName": "Updated Trip",
                    "description": "New details"
                }
                """;

        mockMvc.perform(put("/api/v1/groups/99")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("Group Not Found with id : 99"));
    }

    @Test
    void deleteGroup_Success() throws Exception {
        doNothing().when(groupService).deleteGroup(1L);

        mockMvc.perform(delete("/api/v1/groups/1"))
                .andExpect(status().isNoContent());

        verify(groupService).deleteGroup(1L);
    }

    @Test
    void deleteGroup_NotFound() throws Exception {
        doThrow(new GroupNotFoundException("Group Not Found with id : 99")).when(groupService).deleteGroup(99L);

        mockMvc.perform(delete("/api/v1/groups/99"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("Group Not Found with id : 99"));
    }

    @Test
    void getAllGroupsForUser_Success() throws Exception {
        GroupResponse response = new GroupResponse(1L, "Road Trip", "Trip details");
        when(groupService.getAllGroupForUser(5L)).thenReturn(List.of(response));

        mockMvc.perform(get("/api/v1/groups/users/5"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(1L))
                .andExpect(jsonPath("$[0].groupName").value("Road Trip"));
    }

    @Test
    void getAllGroupsForUser_UserNotFound() throws Exception {
        when(groupService.getAllGroupForUser(99L)).thenThrow(new AccountNotFoundException("User doesn't exist with id :99"));

        mockMvc.perform(get("/api/v1/groups/users/99"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("User doesn't exist with id :99"));
    }

    @Test
    void getGroupMembers_Success() throws Exception {
        GroupMemberResponse member = new GroupMemberResponse(1L, 10L, OffsetDateTime.now());
        when(groupService.getGroupMembers(1L)).thenReturn(List.of(member));

        mockMvc.perform(get("/api/v1/groups/1/members"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].groupId").value(1L))
                .andExpect(jsonPath("$[0].userId").value(10L));
    }

    @Test
    void getGroupMembers_GroupNotFound() throws Exception {
        when(groupService.getGroupMembers(99L)).thenThrow(new GroupNotFoundException("Group Not Found with id : 99"));

        mockMvc.perform(get("/api/v1/groups/99/members"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("Group Not Found with id : 99"));
    }

    @Test
    void addMemberToGroup_Success() throws Exception {
        GroupMemberResponse response = new GroupMemberResponse(1L, 10L, OffsetDateTime.now());
        when(groupService.addMemberToGroup(eq(1L), any(AddMemberRequest.class))).thenReturn(response);

        String json = """
                {
                    "userId": 10
                }
                """;

        mockMvc.perform(post("/api/v1/groups/1/members")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.groupId").value(1L))
                .andExpect(jsonPath("$.userId").value(10L));
    }

    @Test
    void addMemberToGroup_ValidationFailure_NullUserId() throws Exception {
        String json = """
                {
                    "userId": null
                }
                """;

        mockMvc.perform(post("/api/v1/groups/1/members")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.userId").exists());

        verify(groupService, never()).addMemberToGroup(anyLong(), any());
    }

    @Test
    void addMemberToGroup_DuplicateMember_Conflict() throws Exception {
        when(groupService.addMemberToGroup(eq(1L), any(AddMemberRequest.class)))
                .thenThrow(new DuplicateMembershipException("User is already a member of this group"));

        String json = """
                {
                    "userId": 10
                }
                """;

        mockMvc.perform(post("/api/v1/groups/1/members")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value("User is already a member of this group"));
    }

    @Test
    void removeMemberFromGroup_Success() throws Exception {
        doNothing().when(groupService).removeMemberFromGroup(1L, 10L);

        mockMvc.perform(delete("/api/v1/groups/1/members/10"))
                .andExpect(status().isNoContent());

        verify(groupService).removeMemberFromGroup(1L, 10L);
    }

    @Test
    void removeMemberFromGroup_MemberNotFound() throws Exception {
        doThrow(new MemberNotFoundException("User with id 10 is not a member of group 1"))
                .when(groupService).removeMemberFromGroup(1L, 10L);

        mockMvc.perform(delete("/api/v1/groups/1/members/10"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("User with id 10 is not a member of group 1"));
    }
}
