package com.akshay.expense_splitter.group;

import com.akshay.expense_splitter.user.UserResponse;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface GroupMapper {
    Group toEntity(GroupRequest request);
    GroupResponse toResponse(Group group);
}
