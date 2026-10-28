package com.akshay.expense_splitter.group;

import com.akshay.expense_splitter.exception.AccountNotFoundException;
import com.akshay.expense_splitter.exception.DuplicateMembershipException;
import com.akshay.expense_splitter.exception.GroupNotFoundException;
import com.akshay.expense_splitter.exception.MemberNotFoundException;
import com.akshay.expense_splitter.user.User;
import com.akshay.expense_splitter.user.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class GroupService {
    private final GroupRepository groupRepository;
    private final GroupMemberRepository groupMemberRepository;
    private final UserRepository userRepository;
    private final GroupMapper groupMapper;

    public GroupService(GroupRepository groupRepository, GroupMemberRepository groupMemberRepository, UserRepository userRepository, GroupMapper groupMapper) {
        this.groupRepository = groupRepository;
        this.groupMapper = groupMapper;
        this.groupMemberRepository = groupMemberRepository;
        this.userRepository = userRepository;
    }

    @Transactional
    public GroupResponse createGroup(GroupRequest request) {
        Group group = groupMapper.toEntity(request);
        groupRepository.save(group);
        return groupMapper.toResponse(group);
    }

    @Transactional(readOnly = true)
    public GroupResponse getGroupById(Long id) {
        Group group = groupRepository.findById(id).orElseThrow(() -> new GroupNotFoundException("Group Not Found with id : " + id));
        return groupMapper.toResponse(group);
    }

    @Transactional
    public GroupResponse updateGroupDetails(Long id, GroupRequest request) {
        Group group = groupRepository.findById(id).orElseThrow(() -> new GroupNotFoundException("Group Not Found with id : " + id));
        group.setGroupName(request.getGroupName());
        group.setDescription(request.getDescription());
        Group updatedGroup = groupRepository.save(group);
        return groupMapper.toResponse(updatedGroup);
    }

    @Transactional
    public void deleteGroup(Long id) {
        if (!groupRepository.existsById(id)) {
            throw new GroupNotFoundException("Group Not Found with id : " + id);
        }
        groupRepository.deleteById(id);
    }

    @Transactional(readOnly = true)
    public List<GroupResponse> getAllGroupForUser(Long userId) {
        userRepository.findById(userId).orElseThrow(() -> new AccountNotFoundException("User doesn't exist with id :" + userId));
        List<GroupMembers> membersList = groupMemberRepository.findByUser_Id(userId);
        return membersList.stream()
                .map(GroupMembers::getGroup)
                .map(groupMapper::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<GroupMemberResponse> getGroupMembers(Long groupId) {
        if (!groupRepository.existsById(groupId)) {
            throw new GroupNotFoundException("Group Not Found with id : " + groupId);
        }
        List<GroupMembers> membersList = groupMemberRepository.findByGroup_Id(groupId);
        return membersList.stream()
                .map(member -> {
                    GroupMemberResponse response = new GroupMemberResponse();
                    response.setGroupId(member.getGroup().getId());
                    response.setUserId(member.getUser().getId());
                    response.setJoinedAt(member.getJoinedAt());
                    return response;
                })
                .toList();
    }

    @Transactional
    public GroupMemberResponse addMemberToGroup(Long groupId, AddMemberRequest request) {
        boolean alreadyMember = groupMemberRepository.existsById(new GroupMemberId(groupId, request.getUserId()));
        if (alreadyMember) {
            throw new DuplicateMembershipException("User is already a member of this group");
        }
        Group group = groupRepository.findById(groupId).orElseThrow(() -> new GroupNotFoundException("Group Not Found with id : " + groupId));
        User user = userRepository.findById(request.getUserId()).orElseThrow(() -> new AccountNotFoundException("User doesn't exist with id :" + request.getUserId()));

        GroupMembers groupMembers = new GroupMembers();
        groupMembers.setId(new GroupMemberId(groupId, request.getUserId()));
        groupMembers.setGroup(group);
        groupMembers.setUser(user);

        GroupMembers savedMembers = groupMemberRepository.save(groupMembers);
        GroupMemberResponse response = new GroupMemberResponse();
        response.setGroupId(savedMembers.getGroup().getId());
        response.setUserId(savedMembers.getUser().getId());
        response.setJoinedAt(savedMembers.getJoinedAt());

        return response;
    }

    @Transactional
    public void removeMemberFromGroup(Long groupId, Long userId) {
        if (!groupRepository.existsById(groupId)) {
            throw new GroupNotFoundException("Group Not Found with id : " + groupId);
        }
        if (!userRepository.existsById(userId)) {
            throw new AccountNotFoundException("User doesn't exist with id :" + userId);
        }
        GroupMemberId memberId = new GroupMemberId(groupId, userId);
        if (!groupMemberRepository.existsById(memberId)) {
            throw new MemberNotFoundException("User with id " + userId + " is not a member of group " + groupId);
        }
        groupMemberRepository.deleteById(memberId);
    }
}
