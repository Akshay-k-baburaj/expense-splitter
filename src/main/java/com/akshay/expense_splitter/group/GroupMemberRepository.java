package com.akshay.expense_splitter.group;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface GroupMemberRepository extends JpaRepository<GroupMembers, GroupMemberId> {
    List<GroupMembers> findByUser_Id(Long userId);
    List<GroupMembers> findByGroup_Id(Long groupId);

}
