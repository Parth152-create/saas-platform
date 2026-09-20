package com.yourco.saas.domain.projects;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@Repository
public interface TaskRepository extends JpaRepository<Task, UUID> {

    List<Task> findByProjectIdOrderByCreatedAtDesc(UUID projectId);

    List<Task> findByAssigneeIdOrderByDueDateAsc(UUID assigneeId);

    List<Task> findAllByOrderByCreatedAtDesc();

    @Query("SELECT t FROM Task t WHERE " +
           "(:projectId IS NULL OR t.project.id = :projectId) AND " +
           "(:status IS NULL OR t.status = :status) AND " +
           "(:priority IS NULL OR t.priority = :priority) AND " +
           "(:assigneeId IS NULL OR t.assigneeId = :assigneeId) AND " +
           "(:isOverdue IS NULL OR (:isOverdue = true AND t.dueDate < :currentDate AND t.status != 'DONE') OR (:isOverdue = false AND (t.dueDate >= :currentDate OR t.status = 'DONE'))) AND " +
           "(:query IS NULL OR :query = '' OR " +
           "LOWER(t.title) LIKE LOWER(CONCAT('%', :query, '%')) OR " +
           "LOWER(t.description) LIKE LOWER(CONCAT('%', :query, '%')) OR " +
           "LOWER(t.assigneeName) LIKE LOWER(CONCAT('%', :query, '%'))) " +
           "ORDER BY t.createdAt DESC")
    List<Task> searchTasks(@Param("projectId") UUID projectId,
                           @Param("status") TaskStatus status,
                           @Param("priority") TaskPriority priority,
                           @Param("assigneeId") UUID assigneeId,
                           @Param("isOverdue") Boolean isOverdue,
                           @Param("currentDate") LocalDate currentDate,
                           @Param("query") String query);

    long countByProjectId(UUID projectId);

    long countByProjectIdAndStatus(UUID projectId, TaskStatus status);

    long countByStatus(TaskStatus status);

    long countByStatusNot(TaskStatus status);

    long countByAssigneeIdAndStatusNot(UUID assigneeId, TaskStatus status);

    long countByDueDateBeforeAndStatusNot(LocalDate date, TaskStatus status);

    long countByProjectIdAndDueDateBeforeAndStatusNot(UUID projectId, LocalDate date, TaskStatus status);
}
