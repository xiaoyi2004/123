package org.example.onlineexam.repository;

import org.example.onlineexam.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import java.util.List;

public interface UserRepository extends JpaRepository<User, Long> {
    User findByUsernameAndPassword(String username, String password);
    User findByUsername(String username);
    List<User> findByRole(String role);
    List<User> findByRoleAndClassName(String role, String className);
    @Query("select distinct u.className from User u where u.role='student' and u.className is not null and u.className <> '' order by u.className")
    List<String> findDistinctClassNames();
}
