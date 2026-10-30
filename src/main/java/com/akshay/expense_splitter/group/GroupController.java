package com.akshay.expense_splitter.group;

import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/groups")
public class GroupController {
    private final GroupService groupService;

    public GroupController(GroupService groupService) {
        this.groupService = groupService;
    }

    @PostMapping
    public ResponseEntity<GroupResponse> createGroup(
            @AuthenticationPrincipal Jwt principal,
            @Valid @RequestBody GroupRequest groupRequest) {
        return ResponseEntity.status(HttpStatus.CREATED).body(groupService.createGroup(groupRequest, principal));
    }

    @GetMapping("/me")
    public ResponseEntity<List<GroupResponse>> getGroupsForCurrentUser(@AuthenticationPrincipal Jwt principal) {
        return ResponseEntity.ok(groupService.getGroupsForCurrentUser(principal));
    }

    @GetMapping("/{id}")
    public ResponseEntity<GroupResponse> getGroupById(@PathVariable Long id, @AuthenticationPrincipal Jwt principal) {
        return ResponseEntity.ok(groupService.getGroupById(id, principal));
    }

    @PutMapping("/{id}")
    public ResponseEntity<GroupResponse> updateGroupDetails(
            @PathVariable Long id,
            @AuthenticationPrincipal Jwt principal,
            @Valid @RequestBody GroupRequest groupRequest) {
        return ResponseEntity.ok(groupService.updateGroupDetails(id, groupRequest, principal));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteGroup(@PathVariable Long id, @AuthenticationPrincipal Jwt principal) {
        groupService.deleteGroup(id, principal);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/{groupId}/members")
    public ResponseEntity<List<GroupMemberResponse>> getGroupMembers(
            @PathVariable Long groupId,
            @AuthenticationPrincipal Jwt principal) {
        return ResponseEntity.ok(groupService.getGroupMembers(groupId, principal));
    }

    @PostMapping("/{groupId}/members")
    public ResponseEntity<GroupMemberResponse> addMemberToGroup(
            @PathVariable Long groupId,
            @AuthenticationPrincipal Jwt principal,
            @Valid @RequestBody AddMemberRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(groupService.addMemberToGroup(groupId, request, principal));
    }

    @DeleteMapping("/{groupId}/members/{userId}")
    public ResponseEntity<Void> removeMemberFromGroup(
            @PathVariable Long groupId,
            @PathVariable Long userId,
            @AuthenticationPrincipal Jwt principal) {
        groupService.removeMemberFromGroup(groupId, userId, principal);
        return ResponseEntity.noContent().build();
    }

    @PutMapping("/{groupId}/owner")
    public ResponseEntity<GroupResponse> transferOwnership(
            @PathVariable Long groupId,
            @AuthenticationPrincipal Jwt principal,
            @Valid @RequestBody TransferGroupOwnershipRequest request) {
        return ResponseEntity.ok(groupService.transferOwnership(groupId, request, principal));
    }

    @GetMapping("/{groupId}/invite-code")
    public ResponseEntity<GroupInviteStatusResponse> getInviteCodeStatus(
            @PathVariable Long groupId,
            @AuthenticationPrincipal Jwt principal) {
        return ResponseEntity.ok(groupService.getInviteStatus(groupId, principal));
    }

    @PostMapping("/{groupId}/invite-code")
    public ResponseEntity<GroupInviteCodeResponse> createInviteCode(
            @PathVariable Long groupId,
            @AuthenticationPrincipal Jwt principal) {
        return ResponseEntity.ok(groupService.createInviteCode(groupId, principal));
    }

    @PostMapping("/join-by-code")
    public ResponseEntity<GroupResponse> joinByCode(
            @AuthenticationPrincipal Jwt principal,
            @Valid @RequestBody JoinGroupByCodeRequest request) {
        return ResponseEntity.ok(groupService.joinGroupByCode(request, principal));
    }
}
