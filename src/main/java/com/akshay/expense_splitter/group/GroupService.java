package com.akshay.expense_splitter.group;

import com.akshay.expense_splitter.exception.AccountNotFoundException;
import com.akshay.expense_splitter.exception.DuplicateMembershipException;
import com.akshay.expense_splitter.exception.GroupNotFoundException;
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

    public GroupService(GroupRepository groupRepository,GroupMemberRepository groupMemberRepository,UserRepository userRepository,  GroupMapper groupMapper){
        this.groupRepository = groupRepository;
        this.groupMapper = groupMapper;
        this.groupMemberRepository = groupMemberRepository;
        this.userRepository = userRepository;
    }

    public GroupResponse createGroup(GroupRequest request){
        Group group = groupMapper.toEntity(request);
        groupRepository.save(group);
        return groupMapper.toResponse(group);
    }

    public GroupResponse getGroupById(Long id) {
        Group group = groupRepository.findById(id).orElseThrow(() -> new GroupNotFoundException("Group Not Found with id : " + id));
        return groupMapper.toResponse(group);
    }

    @Transactional(readOnly = true)
    public List<GroupResponse> getAllGroupForUser(Long userId){
        User user = userRepository.findById(userId).orElseThrow(() -> new AccountNotFoundException("User doesn't exist with id :" + userId));
        List<GroupMembers> membersList = groupMemberRepository.findByUser_Id(userId);
        return membersList.stream()
                .map(GroupMembers::getGroup)
                .map(groupMapper::toResponse)
                .toList();
    }

    @Transactional
    public GroupMemberResponse addMemberToGroup(Long groupId, AddMemberRequest request){
        boolean alreadyMember = groupMemberRepository.existsById(new GroupMemberId(groupId, request.getUserId()));
        if (alreadyMember) {
            throw new DuplicateMembershipException(
                    "User is already a member of this group"
            );
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


}
