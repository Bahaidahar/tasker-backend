package com.tasktracker.repository;

import com.tasktracker.entity.Task;
import com.tasktracker.entity.TaskPriority;
import com.tasktracker.entity.TaskStatus;
import com.tasktracker.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface TaskRepository extends JpaRepository<Task, Long> {

    @Query("SELECT t FROM Task t WHERE t.user = :user AND " +
           "(:search IS NULL OR :search = '' OR " +
           "LOWER(t.title) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
           "LOWER(t.description) LIKE LOWER(CONCAT('%', :search, '%'))) " +
           "AND (:status IS NULL OR t.status = :status) " +
           "AND (:priority IS NULL OR t.priority = :priority) " +
           "ORDER BY t.createdAt DESC")
    List<Task> searchTasks(
            @Param("user") User user,
            @Param("search") String search,
            @Param("status") TaskStatus status,
            @Param("priority") TaskPriority priority
    );

    List<Task> findByUserOrderByCreatedAtDesc(User user);

    Optional<Task> findByIdAndUser(Long id, User user);

    List<Task> findByUserAndStatus(User user, TaskStatus status);

    List<Task> findByUserAndPriority(User user, TaskPriority priority);
}
