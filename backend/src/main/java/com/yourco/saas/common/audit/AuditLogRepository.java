package com.yourco.saas.common.audit;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.UUID;

public interface AuditLogRepository extends JpaRepository<AuditLog, UUID> {

    @Query("SELECT a FROM AuditLog a WHERE a.action LIKE 'PROJECT_%' OR a.action LIKE 'TASK_%' ORDER BY a.createdAt DESC")
    List<AuditLog> findRecentProjectAndTaskActivities(Pageable pageable);

    @Query("SELECT a FROM AuditLog a WHERE (a.action LIKE 'PROJECT_%' OR a.action LIKE 'TASK_%') AND a.details LIKE CONCAT('%project_id=', :projectId, '%') ORDER BY a.createdAt DESC")
    List<AuditLog> findProjectActivities(@Param("projectId") String projectId, Pageable pageable);
}
