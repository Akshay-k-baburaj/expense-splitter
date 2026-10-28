package com.akshay.expense_splitter.group;

import com.akshay.expense_splitter.exception.AccountNotFoundException;
import com.akshay.expense_splitter.exception.DuplicateMembershipException;
import com.akshay.expense_splitter.exception.GroupNotFoundException;
import com.akshay.expense_splitter.exception.MemberNotFoundException;
import com.akshay.expense_splitter.user.User;
import com.akshay.expense_splitter.user.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class GroupServiceTest {

    @Mock
    private GroupRepository groupRepository;

    @Mock
    private GroupMemberRepository groupMemberRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private GroupMapper groupMapper;

    @InjectMocks
    private GroupService groupService;

    private Group group;
    private User user;
    private GroupRequest groupRequest;
    private GroupResponse groupResponse;

    @BeforeEach
    void setUp() {
        group = new Group();
        group.setId(1L);
        group.setGroupName("Trip to Goa");
        group.setDescription("Vacation expenses");

        user = new User();
        user.setId(10L);
        user.setUsername("akshay");
        user.setEmail("akshay@example.com");

        groupRequest = new GroupRequest("Trip to Goa", "Vacation expenses");
        groupResponse = new GroupResponse(1L, "Trip to Goa", "Vacation expenses");
    }

    @Test
    void createGroup_Success() {
        when(groupMapper.toEntity(groupRequest)).thenReturn(group);
        when(groupRepository.save(group)).thenReturn(group);
        when(groupMapper.toResponse(group)).thenReturn(groupResponse);

        GroupResponse result = groupService.createGroup(groupRequest);

        assertNotNull(result);
        assertEquals(1L, result.getId());
        assertEquals("Trip to Goa", result.getGroupName());
        verify(groupRepository).save(group);
    }

    @Test
    void getGroupById_Success() {
        when(groupRepository.findById(1L)).thenReturn(Optional.of(group));
        when(groupMapper.toResponse(group)).thenReturn(groupResponse);

        GroupResponse result = groupService.getGroupById(1L);

        assertNotNull(result);
        assertEquals(1L, result.getId());
        verify(groupRepository).findById(1L);
    }

    @Test
    void getGroupById_NotFound_ThrowsGroupNotFoundException() {
        when(groupRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(GroupNotFoundException.class, () -> groupService.getGroupById(99L));
    }

    @Test
    void updateGroupDetails_Success() {
        GroupRequest updateRequest = new GroupRequest("Updated Goa Trip", "Updated Description");
        Group updatedGroup = new Group();
        updatedGroup.setId(1L);
        updatedGroup.setGroupName("Updated Goa Trip");
        updatedGroup.setDescription("Updated Description");
        GroupResponse updatedResponse = new GroupResponse(1L, "Updated Goa Trip", "Updated Description");

        when(groupRepository.findById(1L)).thenReturn(Optional.of(group));
        when(groupRepository.save(any(Group.class))).thenReturn(updatedGroup);
        when(groupMapper.toResponse(updatedGroup)).thenReturn(updatedResponse);

        GroupResponse result = groupService.updateGroupDetails(1L, updateRequest);

        assertNotNull(result);
        assertEquals("Updated Goa Trip", result.getGroupName());
        assertEquals("Updated Description", result.getDescription());
        verify(groupRepository).save(group);
    }

    @Test
    void updateGroupDetails_NotFound_ThrowsGroupNotFoundException() {
        GroupRequest updateRequest = new GroupRequest("Updated", "Desc");
        when(groupRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(GroupNotFoundException.class, () -> groupService.updateGroupDetails(99L, updateRequest));
        verify(groupRepository, never()).save(any());
    }

    @Test
    void deleteGroup_Success() {
        when(groupRepository.existsById(1L)).thenReturn(true);

        groupService.deleteGroup(1L);

        verify(groupRepository).deleteById(1L);
    }

    @Test
    void deleteGroup_NotFound_ThrowsGroupNotFoundException() {
        when(groupRepository.existsById(99L)).thenReturn(false);

        assertThrows(GroupNotFoundException.class, () -> groupService.deleteGroup(99L));
        verify(groupRepository, never()).deleteById(anyLong());
    }

    @Test
    void getAllGroupForUser_Success() {
        GroupMembers member = new GroupMembers();
        member.setId(new GroupMemberId(1L, 10L));
        member.setGroup(group);
        member.setUser(user);

        when(userRepository.findById(10L)).thenReturn(Optional.of(user));
        when(groupMemberRepository.findByUser_Id(10L)).thenReturn(List.of(member));
        when(groupMapper.toResponse(group)).thenReturn(groupResponse);

        List<GroupResponse> result = groupService.getAllGroupForUser(10L);

        assertEquals(1, result.size());
        assertEquals(1L, result.getFirst().getId());
    }

    @Test
    void getAllGroupForUser_UserNotFound_ThrowsAccountNotFoundException() {
        when(userRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(AccountNotFoundException.class, () -> groupService.getAllGroupForUser(99L));
    }

    @Test
    void getGroupMembers_Success() {
        GroupMembers member = new GroupMembers();
        member.setId(new GroupMemberId(1L, 10L));
        member.setGroup(group);
        member.setUser(user);
        member.setJoinedAt(OffsetDateTime.now());

        when(groupRepository.existsById(1L)).thenReturn(true);
        when(groupMemberRepository.findByGroup_Id(1L)).thenReturn(List.of(member));

        List<GroupMemberResponse> members = groupService.getGroupMembers(1L);

        assertEquals(1, members.size());
        assertEquals(1L, members.getFirst().getGroupId());
        assertEquals(10L, members.getFirst().getUserId());
    }

    @Test
    void getGroupMembers_GroupNotFound_ThrowsGroupNotFoundException() {
        when(groupRepository.existsById(99L)).thenReturn(false);

        assertThrows(GroupNotFoundException.class, () -> groupService.getGroupMembers(99L));
    }

    @Test
    void addMemberToGroup_Success() {
        AddMemberRequest request = new AddMemberRequest(10L);
        GroupMembers member = new GroupMembers();
        member.setId(new GroupMemberId(1L, 10L));
        member.setGroup(group);
        member.setUser(user);
        OffsetDateTime now = OffsetDateTime.now();
        member.setJoinedAt(now);

        when(groupMemberRepository.existsById(new GroupMemberId(1L, 10L))).thenReturn(false);
        when(groupRepository.findById(1L)).thenReturn(Optional.of(group));
        when(userRepository.findById(10L)).thenReturn(Optional.of(user));
        when(groupMemberRepository.save(any(GroupMembers.class))).thenReturn(member);

        GroupMemberResponse response = groupService.addMemberToGroup(1L, request);

        assertNotNull(response);
        assertEquals(1L, response.getGroupId());
        assertEquals(10L, response.getUserId());
        assertEquals(now, response.getJoinedAt());
    }

    @Test
    void addMemberToGroup_AlreadyMember_ThrowsDuplicateMembershipException() {
        AddMemberRequest request = new AddMemberRequest(10L);
        when(groupMemberRepository.existsById(new GroupMemberId(1L, 10L))).thenReturn(true);

        assertThrows(DuplicateMembershipException.class, () -> groupService.addMemberToGroup(1L, request));
        verify(groupMemberRepository, never()).save(any());
    }

    @Test
    void addMemberToGroup_GroupNotFound_ThrowsGroupNotFoundException() {
        AddMemberRequest request = new AddMemberRequest(10L);
        when(groupMemberRepository.existsById(new GroupMemberId(99L, 10L))).thenReturn(false);
        when(groupRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(GroupNotFoundException.class, () -> groupService.addMemberToGroup(99L, request));
    }

    @Test
    void addMemberToGroup_UserNotFound_ThrowsAccountNotFoundException() {
        AddMemberRequest request = new AddMemberRequest(99L);
        when(groupMemberRepository.existsById(new GroupMemberId(1L, 99L))).thenReturn(false);
        when(groupRepository.findById(1L)).thenReturn(Optional.of(group));
        when(userRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(AccountNotFoundException.class, () -> groupService.addMemberToGroup(1L, request));
    }

    @Test
    void removeMemberFromGroup_Success() {
        when(groupRepository.existsById(1L)).thenReturn(true);
        when(userRepository.existsById(10L)).thenReturn(true);
        when(groupMemberRepository.existsById(new GroupMemberId(1L, 10L))).thenReturn(true);

        groupService.removeMemberFromGroup(1L, 10L);

        verify(groupMemberRepository).deleteById(new GroupMemberId(1L, 10L));
    }

    @Test
    void removeMemberFromGroup_GroupNotFound_ThrowsGroupNotFoundException() {
        when(groupRepository.existsById(99L)).thenReturn(false);

        assertThrows(GroupNotFoundException.class, () -> groupService.removeMemberFromGroup(99L, 10L));
        verify(groupMemberRepository, never()).deleteById(any());
    }

    @Test
    void removeMemberFromGroup_UserNotFound_ThrowsAccountNotFoundException() {
        when(groupRepository.existsById(1L)).thenReturn(true);
        when(userRepository.existsById(99L)).thenReturn(false);

        assertThrows(AccountNotFoundException.class, () -> groupService.removeMemberFromGroup(1L, 99L));
        verify(groupMemberRepository, never()).deleteById(any());
    }

    @Test
    void removeMemberFromGroup_MemberNotFound_ThrowsMemberNotFoundException() {
        when(groupRepository.existsById(1L)).thenReturn(true);
        when(userRepository.existsById(10L)).thenReturn(true);
        when(groupMemberRepository.existsById(new GroupMemberId(1L, 10L))).thenReturn(false);

        assertThrows(MemberNotFoundException.class, () -> groupService.removeMemberFromGroup(1L, 10L));
        verify(groupMemberRepository, never()).deleteById(any());
    }
}
