package com.example.todo.repository;

import com.example.todo.model.Todo;
import com.example.todo.model.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface TodoRepository extends JpaRepository<Todo, Long> {
    List<Todo> findByUser(User user);
    List<Todo> findByUserAndCompleted(User user, boolean completed);
    void deleteByUser(User user);
    long countByUser(User user);
    long countByUserAndCompleted(User user, boolean completed);
    long countByUserAndPriority(User user, String priority);

    /**
     * Paginated query with optional filters: completed, priority, and full-text search
     * (title or description). Any null parameter is ignored.
     */
    @Query("SELECT t FROM Todo t WHERE t.user = :user " +
           "AND (:completed IS NULL OR t.completed = :completed) " +
           "AND (:priority IS NULL OR t.priority = :priority) " +
           "AND (:search IS NULL OR LOWER(t.title) LIKE LOWER(CONCAT('%', :search, '%')) " +
           "     OR LOWER(t.description) LIKE LOWER(CONCAT('%', :search, '%')))")
    Page<Todo> findWithFilters(@Param("user") User user,
                               @Param("completed") Boolean completed,
                               @Param("priority") String priority,
                               @Param("search") String search,
                               Pageable pageable);
}
