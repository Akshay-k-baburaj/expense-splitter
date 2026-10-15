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

    public GroupController(GroupService groupService){
        this.groupService = groupService;
    }

    @PostMapping
    public ResponseEntity<GroupResponse> createGroup(@RequestBody GroupRequest groupRequest){
        GroupResponse createdGroup = groupService.createGroup(groupRequest);
        return ResponseEntity.status(HttpStatus.CREATED).body(createdGroup);
    }

    @GetMapping("/{id}")
    public ResponseEntity<GroupResponse> getGroupById(@PathVariable Long id){
        GroupResponse group = groupService.getGroupById(id);
        return ResponseEntity.status(HttpStatus.OK).body(group);
    }

    @GetMapping("/users/{userId}")
    public ResponseEntity<List<GroupResponse>> getAllGroupsForUser(@PathVariable Long userId){
        List<GroupResponse> groupResponses = groupService.getAllGroupForUser(userId);
        return ResponseEntity.ok(groupResponses);
    }

    @PostMapping("/{groupId}/members")
    public ResponseEntity<GroupMemberResponse> addMemberToGroup(@PathVariable Long groupId, @Valid @RequestBody AddMemberRequest request){
        GroupMemberResponse response = groupService.addMemberToGroup(groupId,request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

}
