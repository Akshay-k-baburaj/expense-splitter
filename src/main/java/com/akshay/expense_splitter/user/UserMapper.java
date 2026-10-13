package com.akshay.expense_splitter.user;

import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface UserMapper {
        User toEntity(UserRequest userRequest);
        UserResponse toResponse(User user);
}
