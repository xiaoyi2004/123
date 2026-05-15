package org.example.onlineexam.repository;

import org.example.onlineexam.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserRepository extends JpaRepository<User, Long> {
    User findByUsernameAndPassword(String username, String password);
    User findByUsername(String username);   // 新增：用于注册时检查重名
}