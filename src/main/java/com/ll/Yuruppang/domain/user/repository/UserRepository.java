package com.ll.Yuruppang.domain.user.repository;

import com.ll.Yuruppang.domain.user.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserRepository extends JpaRepository<User, Long> {
}
