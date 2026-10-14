package com.akshay.expense_splitter.group;

import org.springframework.stereotype.Service;

@Service
public class GroupService {
    private final GroupRepository groupRepository;
    private final GroupMapper groupMapper;

    public GroupService(GroupRepository groupRepository,GroupMapper groupMapper){
        this.groupRepository = groupRepository;
        this.groupMapper = groupMapper;
    }

    public GroupResponse createGroup(GroupRequest request){
        Group group = groupMapper.toEntity(request);
        groupRepository.save(group);
        return groupMapper.toResponse(group);
    }
}
