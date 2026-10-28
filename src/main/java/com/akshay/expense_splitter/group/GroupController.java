package com.akshay.expense_splitter.group;

import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
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
    public ResponseEntity<GroupResponse> createGroup(@Valid @RequestBody GroupRequest groupRequest) {
        GroupResponse createdGroup = groupService.createGroup(groupRequest);
        return ResponseEntity.status(HttpStatus.CREATED).body(createdGroup);
    }

    @GetMapping("/{id}")
    public ResponseEntity<GroupResponse> getGroupById(@PathVariable Long id) {
        GroupResponse group = groupService.getGroupById(id);
        return ResponseEntity.status(HttpStatus.OK).body(group);
    }

    @PutMapping("/{id}")
    public ResponseEntity<GroupResponse> updateGroupDetails(@PathVariable Long id, @Valid @RequestBody GroupRequest groupRequest) {
        GroupResponse updatedGroup = groupService.updateGroupDetails(id, groupRequest);
        return ResponseEntity.ok(updatedGroup);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteGroup(@PathVariable Long id) {
        groupService.deleteGroup(id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/users/{userId}")
    public ResponseEntity<List<GroupResponse>> getAllGroupsForUser(@PathVariable Long userId) {
        List<GroupResponse> groupResponses = groupService.getAllGroupForUser(userId);
        return ResponseEntity.ok(groupResponses);
    }

    @GetMapping("/{groupId}/members")
    public ResponseEntity<List<GroupMemberResponse>> getGroupMembers(@PathVariable Long groupId) {
        List<GroupMemberResponse> members = groupService.getGroupMembers(groupId);
        return ResponseEntity.ok(members);
    }

    @PostMapping("/{groupId}/members")
    public ResponseEntity<GroupMemberResponse> addMemberToGroup(@PathVariable Long groupId, @Valid @RequestBody AddMemberRequest request) {
        GroupMemberResponse response = groupService.addMemberToGroup(groupId, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @DeleteMapping("/{groupId}/members/{userId}")
    public ResponseEntity<Void> removeMemberFromGroup(@PathVariable Long groupId, @PathVariable Long userId) {
        groupService.removeMemberFromGroup(groupId, userId);
        return ResponseEntity.noContent().build();
    }
}
