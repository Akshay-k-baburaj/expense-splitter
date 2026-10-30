package com.akshay.expense_splitter.group;

import com.akshay.expense_splitter.exception.AccountNotFoundException;
import com.akshay.expense_splitter.exception.DuplicateMembershipException;
import com.akshay.expense_splitter.exception.GroupNotFoundException;
import com.akshay.expense_splitter.exception.GroupJoinRateLimitException;
import com.akshay.expense_splitter.exception.InvalidGroupInviteCodeException;
import com.akshay.expense_splitter.exception.MemberNotFoundException;
import com.akshay.expense_splitter.exception.FinancialOperationException;
import com.akshay.expense_splitter.expense.GroupFinancialService;
import com.akshay.expense_splitter.expense.GroupBalanceResponse;
import com.akshay.expense_splitter.expense.CurrencySupport;
import com.akshay.expense_splitter.expense.MemberBalanceResponse;
import com.akshay.expense_splitter.user.User;
import com.akshay.expense_splitter.user.UserRepository;
import com.akshay.expense_splitter.user.UserService;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Duration;
import java.time.OffsetDateTime;
import java.util.HexFormat;

@Service
public class GroupService {
    private static final SecureRandom SECURE_RANDOM = new SecureRandom();
    private static final Duration INVITE_CODE_LIFETIME = Duration.ofDays(7);
    private static final Duration JOIN_ATTEMPT_WINDOW = Duration.ofMinutes(15);
    private static final int MAX_JOIN_ATTEMPTS = 5;
    private final GroupRepository groupRepository;
    private final GroupMemberRepository groupMemberRepository;
    private final UserRepository userRepository;
    private final UserService userService;
    private final GroupMapper groupMapper;
    private final GroupJoinAttemptRepository groupJoinAttemptRepository;
    private final GroupFinancialService groupFinancialService;

    public GroupService(
            GroupRepository groupRepository,
            GroupMemberRepository groupMemberRepository,
            UserRepository userRepository,
            UserService userService,
            GroupMapper groupMapper,
            GroupJoinAttemptRepository groupJoinAttemptRepository,
            GroupFinancialService groupFinancialService) {
        this.groupRepository = groupRepository;
        this.groupMapper = groupMapper;
        this.groupMemberRepository = groupMemberRepository;
        this.userRepository = userRepository;
        this.userService = userService;
        this.groupJoinAttemptRepository = groupJoinAttemptRepository;
        this.groupFinancialService = groupFinancialService;
    }

    @Transactional
    public GroupResponse createGroup(GroupRequest request, Jwt principal) {
        User owner = userService.requireCurrentUser(principal);
        Group group = groupMapper.toEntity(request);
        group.setCurrencyCode(CurrencySupport.normalizeOrDefault(request.getCurrencyCode(), "INR"));
        group.setOwner(owner);
        Group savedGroup = groupRepository.saveAndFlush(group);

        GroupMembers ownerMembership = new GroupMembers();
        ownerMembership.setId(new GroupMemberId(savedGroup.getId(), owner.getId()));
        ownerMembership.setGroup(savedGroup);
        ownerMembership.setUser(owner);
        groupMemberRepository.save(ownerMembership);
        return toResponse(savedGroup, owner);
    }

    @Transactional(readOnly = true)
    public GroupResponse getGroupById(Long id, Jwt principal) {
        User viewer = userService.requireCurrentUser(principal);
        Group group = requireGroup(id);
        requireMembership(group, viewer);
        return toResponse(group, viewer);
    }

    @Transactional
    public GroupResponse updateGroupDetails(Long id, GroupRequest request, Jwt principal) {
        User actor = userService.requireCurrentUser(principal);
        Group group = requireGroup(id);
        requireOwner(group, actor);
        if (request.getCurrencyCode() != null
                && !CurrencySupport.normalizeOrDefault(request.getCurrencyCode(), group.getCurrencyCode()).equals(group.getCurrencyCode())) {
            throw new FinancialOperationException("A group's currency is fixed when the group is created.");
        }
        group.setGroupName(request.getGroupName().trim());
        group.setDescription(request.getDescription());
        return toResponse(groupRepository.save(group), actor);
    }

    @Transactional
    public void deleteGroup(Long id, Jwt principal) {
        User actor = userService.requireCurrentUser(principal);
        Group group = requireGroup(id);
        requireOwner(group, actor);
        groupRepository.delete(group);
    }

    @Transactional(readOnly = true)
    public List<GroupResponse> getGroupsForCurrentUser(Jwt principal) {
        User viewer = userService.requireCurrentUser(principal);
        return groupMemberRepository.findByUser_Id(viewer.getId()).stream()
                .map(GroupMembers::getGroup)
                .map(group -> toResponse(group, viewer))
                .toList();
    }

    @Transactional(readOnly = true)
    public List<GroupMemberResponse> getGroupMembers(Long groupId, Jwt principal) {
        User viewer = userService.requireCurrentUser(principal);
        Group group = requireGroup(groupId);
        requireMembership(group, viewer);
        return groupMemberRepository.findByGroup_Id(groupId).stream()
                .map(member -> new GroupMemberResponse(
                        groupId,
                        member.getUser().getId(),
                        displayUsername(member.getUser()),
                        member.getJoinedAt(),
                        group.getOwner() != null && group.getOwner().getId().equals(member.getUser().getId())))
                .toList();
    }

    @Transactional
    public GroupMemberResponse addMemberToGroup(Long groupId, AddMemberRequest request, Jwt principal) {
        User actor = userService.requireCurrentUser(principal);
        Group group = requireGroup(groupId);
        requireOwner(group, actor);
        User user = userRepository.findById(request.getUserId())
                .filter(User::isActive)
                .orElseThrow(() -> new AccountNotFoundException("User account was not found"));
        GroupMemberId memberId = new GroupMemberId(groupId, user.getId());
        if (groupMemberRepository.existsById(memberId)) {
            throw new DuplicateMembershipException("User is already a member of this group");
        }

        GroupMembers membership = new GroupMembers();
        membership.setId(memberId);
        membership.setGroup(group);
        membership.setUser(user);
        GroupMembers saved = groupMemberRepository.saveAndFlush(membership);
        return new GroupMemberResponse(groupId, user.getId(), user.getUsername(), saved.getJoinedAt(), false);
    }

    @Transactional
    public void removeMemberFromGroup(Long groupId, Long userId, Jwt principal) {
        User actor = userService.requireCurrentUser(principal);
        Group group = requireGroup(groupId);
        GroupMemberId memberId = new GroupMemberId(groupId, userId);
        if (!groupMemberRepository.existsById(memberId)) {
            throw new MemberNotFoundException("That user is not a member of this group");
        }

        boolean actorIsOwner = isOwner(group, actor);
        if (!actorIsOwner && !actor.getId().equals(userId)) {
            throw new AccessDeniedException("Only the group owner can remove another member.");
        }
        if (group.getOwner() != null && group.getOwner().getId().equals(userId)) {
            throw new AccessDeniedException("Transfer group ownership before leaving the group.");
        }
        GroupBalanceResponse balances = groupFinancialService.getBalances(groupId, principal);
        boolean hasOutstandingBalance = balances.members().stream()
                .filter(balance -> balance.userId().equals(userId))
                .map(MemberBalanceResponse::netBalance)
                .anyMatch(balance -> balance.signum() != 0);
        if (hasOutstandingBalance) {
            throw new FinancialOperationException("Settle this member's balance before removing them from the group.");
        }
        groupMemberRepository.deleteById(memberId);
    }

    @Transactional
    public GroupResponse transferOwnership(Long groupId, TransferGroupOwnershipRequest request, Jwt principal) {
        User actor = userService.requireCurrentUser(principal);
        Group group = requireGroup(groupId);
        requireOwner(group, actor);
        User nextOwner = userRepository.findById(request.userId())
                .filter(User::isActive)
                .orElseThrow(() -> new AccountNotFoundException("User account was not found"));
        if (!groupMemberRepository.existsById(new GroupMemberId(groupId, nextOwner.getId()))) {
            throw new MemberNotFoundException("The next owner must already be a member of this group");
        }
        group.setOwner(nextOwner);
        return toResponse(groupRepository.save(group), actor);
    }

    @Transactional(readOnly = true)
    public GroupInviteStatusResponse getInviteStatus(Long groupId, Jwt principal) {
        User actor = userService.requireCurrentUser(principal);
        Group group = requireGroup(groupId);
        requireOwner(group, actor);
        boolean active = group.getInviteCodeHash() != null
                && group.getInviteExpiresAt() != null
                && group.getInviteExpiresAt().isAfter(OffsetDateTime.now());
        return new GroupInviteStatusResponse(active, active ? group.getInviteExpiresAt() : null);
    }

    @Transactional
    public GroupInviteCodeResponse createInviteCode(Long groupId, Jwt principal) {
        User actor = userService.requireCurrentUser(principal);
        Group group = requireGroup(groupId);
        requireOwner(group, actor);

        String code;
        String codeHash;
        do {
            code = "%06d".formatted(SECURE_RANDOM.nextInt(1_000_000));
            codeHash = hashInviteCode(code);
        } while (groupRepository.existsByInviteCodeHash(codeHash));

        OffsetDateTime expiresAt = OffsetDateTime.now().plus(INVITE_CODE_LIFETIME);
        group.setInviteCodeHash(codeHash);
        group.setInviteExpiresAt(expiresAt);
        groupRepository.save(group);
        return new GroupInviteCodeResponse(code, expiresAt);
    }

    @Transactional(noRollbackFor = InvalidGroupInviteCodeException.class)
    public GroupResponse joinGroupByCode(JoinGroupByCodeRequest request, Jwt principal) {
        User actor = userService.requireCurrentUser(principal);
        OffsetDateTime now = OffsetDateTime.now();
        groupJoinAttemptRepository.deleteByAttemptedAtBefore(now.minusDays(30));
        if (groupJoinAttemptRepository.countByUser_IdAndAttemptedAtAfter(actor.getId(), now.minus(JOIN_ATTEMPT_WINDOW)) >= MAX_JOIN_ATTEMPTS) {
            throw new GroupJoinRateLimitException();
        }

        Group group = groupRepository.findByInviteCodeHashAndInviteExpiresAtAfter(hashInviteCode(request.code()), now)
                .orElseThrow(() -> {
                    GroupJoinAttempt attempt = new GroupJoinAttempt();
                    attempt.setUser(actor);
                    groupJoinAttemptRepository.save(attempt);
                    return new InvalidGroupInviteCodeException();
                });
        GroupMemberId memberId = new GroupMemberId(group.getId(), actor.getId());
        if (groupMemberRepository.existsById(memberId)) {
            throw new DuplicateMembershipException("You are already a member of this group.");
        }

        GroupMembers membership = new GroupMembers();
        membership.setId(memberId);
        membership.setGroup(group);
        membership.setUser(actor);
        groupMemberRepository.saveAndFlush(membership);
        return toResponse(group, actor);
    }

    private Group requireGroup(Long groupId) {
        return groupRepository.findById(groupId)
                .orElseThrow(() -> new GroupNotFoundException("Group not found"));
    }

    private void requireMembership(Group group, User user) {
        if (!groupMemberRepository.existsById(new GroupMemberId(group.getId(), user.getId()))) {
            throw new AccessDeniedException("Group membership is required to view this group.");
        }
    }

    private void requireOwner(Group group, User user) {
        if (!isOwner(group, user)) {
            throw new AccessDeniedException("Only the group owner can perform this action.");
        }
    }

    private boolean isOwner(Group group, User user) {
        return group.getOwner() != null && group.getOwner().getId().equals(user.getId());
    }

    private String displayUsername(User user) {
        return user.isActive() ? user.getUsername() : "Former member";
    }

    private String hashInviteCode(String code) {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(code.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 is unavailable", exception);
        }
    }

    private GroupResponse toResponse(Group group, User viewer) {
        GroupResponse response = groupMapper.toResponse(group);
        response.setViewerIsOwner(isOwner(group, viewer));
        return response;
    }
}
