package com.akshay.expense_splitter.group;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface GroupMapper {
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "owner", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "inviteCodeHash", ignore = true)
    @Mapping(target = "inviteExpiresAt", ignore = true)
    Group toEntity(GroupRequest request);

    @Mapping(target = "ownerId", source = "owner.id")
    @Mapping(target = "viewerIsOwner", ignore = true)
    GroupResponse toResponse(Group group);
}
