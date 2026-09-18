package com.yourco.saas.domain.hrm;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface EmployeeRepository extends JpaRepository<Employee, UUID> {

    Optional<Employee> findByEmployeeId(String employeeId);

    Optional<Employee> findByEmailIgnoreCase(String email);

    List<Employee> findByDepartmentIgnoreCase(String department);

    List<Employee> findByStatus(EmployeeStatus status);

    @Query("SELECT e FROM Employee e WHERE " +
           "LOWER(e.name) LIKE LOWER(CONCAT('%', :query, '%')) OR " +
           "LOWER(e.email) LIKE LOWER(CONCAT('%', :query, '%')) OR " +
           "LOWER(e.employeeId) LIKE LOWER(CONCAT('%', :query, '%')) OR " +
           "LOWER(e.position) LIKE LOWER(CONCAT('%', :query, '%'))")
    List<Employee> searchEmployees(@Param("query") String query);

    long countByStatus(EmployeeStatus status);
}
