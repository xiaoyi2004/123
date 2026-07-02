package org.example.onlineexam.repository;

import org.example.onlineexam.entity.OperationLog;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface OperationLogRepository extends JpaRepository<OperationLog, Long> {
    Page<OperationLog> findByUsernameContaining(String username, Pageable pageable);
    Page<OperationLog> findByModule(String module, Pageable pageable);
}