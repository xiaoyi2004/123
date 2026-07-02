package org.example.onlineexam.repository;

import org.example.onlineexam.entity.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {
    User findByUsernameAndPassword(String username, String password);
    User findByUsername(String username);
    List<User> findByRole(String role);
    List<User> findByRoleAndClassName(String role, String className);
    Page<User> findByRole(String role, Pageable pageable);
    Page<User> findByStatus(Integer status, Pageable pageable);
    Page<User> findByRoleAndStatus(String role, Integer status, Pageable pageable);
    Page<User> findByUsernameContainingOrRealNameContaining(String username, String realName, Pageable pageable);

    @Query("select distinct u.className from User u where u.role='student' and u.className is not null and u.className <> '' order by u.className")
    List<String> findDistinctClassNames();
}