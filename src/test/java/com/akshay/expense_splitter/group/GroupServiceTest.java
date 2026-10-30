package com.akshay.expense_splitter.group;

import com.akshay.expense_splitter.exception.DuplicateMembershipException;
import com.akshay.expense_splitter.exception.InvalidGroupInviteCodeException;
import com.akshay.expense_splitter.expense.GroupFinancialService;
import com.akshay.expense_splitter.expense.GroupBalanceResponse;
import com.akshay.expense_splitter.expense.MemberBalanceResponse;
import com.akshay.expense_splitter.user.User;
import com.akshay.expense_splitter.user.UserRepository;
import com.akshay.expense_splitter.user.UserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.oauth2.jwt.Jwt;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.HexFormat;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class GroupServiceTest {
    @Mock GroupRepository groupRepository;
    @Mock GroupMemberRepository groupMemberRepository;
    @Mock UserRepository userRepository;
    @Mock UserService userService;
    @Mock GroupMapper groupMapper;
    @Mock GroupJoinAttemptRepository groupJoinAttemptRepository;
    @Mock GroupFinancialService groupFinancialService;
    @InjectMocks GroupService groupService;

    private final Jwt ownerToken = Jwt.withTokenValue("owner-token").header("alg", "none").subject("auth0|owner").build();
    private final User owner = user(1L);
    private final User member = user(2L);
    private Group group;

    @BeforeEach
    void setUp() {
        group = new Group();
        group.setId(10L);
        group.setGroupName("Weekend trip");
        group.setOwner(owner);
        lenient().when(userService.requireCurrentUser(ownerToken)).thenReturn(owner);
    }

    @Test
    void createGroupAssignsOwnerAndCreatesMembership() {
        GroupRequest request = new GroupRequest("Weekend trip", "", "USD");
        when(groupMapper.toEntity(request)).thenReturn(group);
        when(groupRepository.saveAndFlush(group)).thenReturn(group);
        when(groupMapper.toResponse(group)).thenReturn(new GroupResponse(10L, "Weekend trip", "", 1L, false));

        GroupResponse result = groupService.createGroup(request, ownerToken);

        assertEquals(1L, result.getOwnerId());
        assertEquals("USD", group.getCurrencyCode());
        assertTrue(result.isViewerIsOwner());
        verify(groupMemberRepository).save(argThat(m -> m.getId().equals(new GroupMemberId(10L, 1L))));
    }

    @Test
    void memberCannotReadGroupWithoutMembership() {
        when(userService.requireCurrentUser(ownerToken)).thenReturn(member);
        when(groupRepository.findById(10L)).thenReturn(Optional.of(group));
        when(groupMemberRepository.existsById(new GroupMemberId(10L, 2L))).thenReturn(false);

        assertThrows(AccessDeniedException.class, () -> groupService.getGroupById(10L, ownerToken));
        verifyNoInteractions(groupMapper);
    }

    @Test
    void nonOwnerCannotUpdateGroup() {
        when(userService.requireCurrentUser(ownerToken)).thenReturn(member);
        when(groupRepository.findById(10L)).thenReturn(Optional.of(group));

        assertThrows(AccessDeniedException.class, () -> groupService.updateGroupDetails(10L, new GroupRequest("Changed", ""), ownerToken));
        verify(groupRepository, never()).save(any());
    }

    @Test
    void ownerCanAddMemberAndDuplicateIsRejected() {
        AddMemberRequest request = new AddMemberRequest(2L);
        GroupMembers saved = new GroupMembers();
        saved.setJoinedAt(OffsetDateTime.now());
        when(groupRepository.findById(10L)).thenReturn(Optional.of(group));
        when(userRepository.findById(2L)).thenReturn(Optional.of(member));
        when(groupMemberRepository.existsById(new GroupMemberId(10L, 2L))).thenReturn(false, true);
        when(groupMemberRepository.saveAndFlush(any())).thenReturn(saved);

        assertEquals(2L, groupService.addMemberToGroup(10L, request, ownerToken).getUserId());
        assertThrows(DuplicateMembershipException.class, () -> groupService.addMemberToGroup(10L, request, ownerToken));
        verify(groupMemberRepository, times(1)).saveAndFlush(any());
    }

    @Test
    void groupMembersIncludeUsernamesAndHideAnonymizedInactiveUsernames() {
        User inactive = user(3L);
        inactive.setUsername("archived_3_oldname");
        inactive.setActive(false);
        GroupMembers ownerMembership = membership(owner);
        GroupMembers memberMembership = membership(member);
        GroupMembers inactiveMembership = membership(inactive);
        when(groupRepository.findById(10L)).thenReturn(Optional.of(group));
        when(groupMemberRepository.existsById(new GroupMemberId(10L, 1L))).thenReturn(true);
        when(groupMemberRepository.findByGroup_Id(10L)).thenReturn(List.of(ownerMembership, memberMembership, inactiveMembership));

        List<GroupMemberResponse> result = groupService.getGroupMembers(10L, ownerToken);

        assertEquals("user1", result.get(0).getUsername());
        assertTrue(result.get(0).isOwner());
        assertEquals("user2", result.get(1).getUsername());
        assertEquals("Former member", result.get(2).getUsername());
    }

    @Test
    void ownerCanCreateSixDigitInviteCodeAndOnlyHashIsStored() {
        when(groupRepository.findById(10L)).thenReturn(Optional.of(group));
        when(groupRepository.existsByInviteCodeHash(any())).thenReturn(false);
        when(groupRepository.save(group)).thenReturn(group);

        GroupInviteCodeResponse result = groupService.createInviteCode(10L, ownerToken);

        assertTrue(result.code().matches("[0-9]{6}"));
        assertTrue(result.expiresAt().isAfter(OffsetDateTime.now().plusDays(6)));
        assertNotEquals(result.code(), group.getInviteCodeHash());
        assertEquals(64, group.getInviteCodeHash().length());
    }

    @Test
    void memberCanJoinWithValidCode() throws Exception {
        String code = "123456";
        Jwt memberToken = Jwt.withTokenValue("member-token").header("alg", "none").subject("auth0|member").build();
        when(userService.requireCurrentUser(memberToken)).thenReturn(member);
        group.setInviteCodeHash(hash(code));
        group.setInviteExpiresAt(OffsetDateTime.now().plusDays(1));
        when(groupRepository.findByInviteCodeHashAndInviteExpiresAtAfter(eq(hash(code)), any())).thenReturn(Optional.of(group));
        when(groupMemberRepository.existsById(new GroupMemberId(10L, 2L))).thenReturn(false);
        when(groupMapper.toResponse(group)).thenReturn(new GroupResponse(10L, "Weekend trip", "", 1L, false));

        GroupResponse result = groupService.joinGroupByCode(new JoinGroupByCodeRequest(code), memberToken);

        assertEquals(10L, result.getId());
        verify(groupMemberRepository).saveAndFlush(any(GroupMembers.class));
    }

    @Test
    void invalidCodeRecordsAnAttempt() {
        when(groupRepository.findByInviteCodeHashAndInviteExpiresAtAfter(any(), any())).thenReturn(Optional.empty());
        when(groupJoinAttemptRepository.countByUser_IdAndAttemptedAtAfter(eq(1L), any())).thenReturn(0L);

        assertThrows(InvalidGroupInviteCodeException.class,
                () -> groupService.joinGroupByCode(new JoinGroupByCodeRequest("000000"), ownerToken));
        verify(groupJoinAttemptRepository).save(any(GroupJoinAttempt.class));
    }

    @Test
    void ownerCannotRemoveMemberWithAnOutstandingBalance() {
        when(groupRepository.findById(10L)).thenReturn(Optional.of(group));
        when(groupMemberRepository.existsById(new GroupMemberId(10L, 2L))).thenReturn(true);
        when(groupFinancialService.getBalances(10L, ownerToken)).thenReturn(new GroupBalanceResponse(
                10L, 1L, "INR", List.of(new MemberBalanceResponse(2L, "user2", new java.math.BigDecimal("-2.00"))), List.of()));

        assertThrows(com.akshay.expense_splitter.exception.FinancialOperationException.class,
                () -> groupService.removeMemberFromGroup(10L, 2L, ownerToken));
        verify(groupMemberRepository, never()).deleteById(any());
    }

    private GroupMembers membership(User user) {
        GroupMembers membership = new GroupMembers();
        membership.setUser(user);
        membership.setGroup(group);
        membership.setJoinedAt(OffsetDateTime.now());
        return membership;
    }

    private static String hash(String value) throws Exception {
        return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(value.getBytes(StandardCharsets.UTF_8)));
    }

    private static User user(Long id) {
        User user = new User();
        user.setId(id);
        user.setActive(true);
        user.setUsername("user" + id);
        user.setEmail("user" + id + "@example.com");
        return user;
    }
}
