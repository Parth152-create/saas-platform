package com.yourco.saas.hrm;

import com.yourco.saas.domain.hrm.Department;
import com.yourco.saas.domain.hrm.DepartmentRepository;
import com.yourco.saas.domain.hrm.Employee;
import com.yourco.saas.domain.hrm.EmployeeRepository;
import com.yourco.saas.domain.hrm.EmployeeStatus;
import com.yourco.saas.hrm.dto.*;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@Service
@Transactional
public class HrmService {

    private final EmployeeRepository employeeRepository;
    private final DepartmentRepository departmentRepository;

    public HrmService(EmployeeRepository employeeRepository, DepartmentRepository departmentRepository) {
        this.employeeRepository = employeeRepository;
        this.departmentRepository = departmentRepository;
    }

    @Transactional(readOnly = true)
    public List<EmployeeDto> getEmployees(String department, EmployeeStatus status, String search) {
        List<Employee> list;

        if (search != null && !search.isBlank()) {
            list = employeeRepository.searchEmployees(search.trim());
        } else if (department != null && !department.isBlank() && !"ALL".equalsIgnoreCase(department)) {
            list = employeeRepository.findByDepartmentIgnoreCase(department.trim());
        } else if (status != null) {
            list = employeeRepository.findByStatus(status);
        } else {
            list = employeeRepository.findAll();
        }

        // Apply secondary in-memory filters if multiple parameters were specified
        return list.stream()
                .filter(e -> department == null || department.isBlank() || "ALL".equalsIgnoreCase(department) || e.getDepartment().equalsIgnoreCase(department.trim()))
                .filter(e -> status == null || e.getStatus() == status)
                .map(EmployeeDto::fromEntity)
                .toList();
    }

    @Transactional(readOnly = true)
    public EmployeeDto getEmployeeById(UUID id) {
        Employee employee = employeeRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Employee not found"));
        return EmployeeDto.fromEntity(employee);
    }

    public EmployeeDto createEmployee(CreateEmployeeRequest req) {
        if (employeeRepository.findByEmployeeId(req.employeeId()).isPresent()) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Employee ID already exists: " + req.employeeId());
        }
        if (employeeRepository.findByEmailIgnoreCase(req.email()).isPresent()) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Employee email already exists: " + req.email());
        }

        Employee employee = new Employee();
        employee.setEmployeeId(req.employeeId().trim());
        employee.setName(req.name().trim());
        employee.setEmail(req.email().trim().toLowerCase());
        employee.setDepartment(req.department().trim());
        employee.setPosition(req.position().trim());
        employee.setStatus(req.status() != null ? req.status() : EmployeeStatus.ACTIVE);
        employee.setHireDate(req.hireDate() != null ? req.hireDate() : LocalDate.now());
        employee.setPhone(req.phone());
        employee.setWorkModel(req.workModel() != null ? req.workModel() : "Hybrid");
        employee.setLocation(req.location());
        employee.setManager(req.manager());
        employee.setAvatarColor(req.avatarColor() != null ? req.avatarColor() : "bg-zinc-800 text-zinc-100");
        employee.setAttendanceRate(req.attendanceRate() != null ? req.attendanceRate() : new BigDecimal("100.00"));
        employee.setBillableHours(req.billableHours() != null ? req.billableHours() : 0);

        Employee saved = employeeRepository.save(employee);
        return EmployeeDto.fromEntity(saved);
    }

    public EmployeeDto updateEmployee(UUID id, UpdateEmployeeRequest req) {
        Employee employee = employeeRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Employee not found"));

        if (req.email() != null && !req.email().isBlank() && !req.email().equalsIgnoreCase(employee.getEmail())) {
            if (employeeRepository.findByEmailIgnoreCase(req.email().trim()).isPresent()) {
                throw new ResponseStatusException(HttpStatus.CONFLICT, "Email is already taken: " + req.email());
            }
            employee.setEmail(req.email().trim().toLowerCase());
        }

        if (req.name() != null && !req.name().isBlank()) {
            employee.setName(req.name().trim());
        }
        if (req.department() != null && !req.department().isBlank()) {
            employee.setDepartment(req.department().trim());
        }
        if (req.position() != null && !req.position().isBlank()) {
            employee.setPosition(req.position().trim());
        }
        if (req.status() != null) {
            employee.setStatus(req.status());
        }
        if (req.hireDate() != null) {
            employee.setHireDate(req.hireDate());
        }
        if (req.phone() != null) {
            employee.setPhone(req.phone());
        }
        if (req.workModel() != null) {
            employee.setWorkModel(req.workModel());
        }
        if (req.location() != null) {
            employee.setLocation(req.location());
        }
        if (req.manager() != null) {
            employee.setManager(req.manager());
        }
        if (req.avatarColor() != null) {
            employee.setAvatarColor(req.avatarColor());
        }
        if (req.attendanceRate() != null) {
            employee.setAttendanceRate(req.attendanceRate());
        }
        if (req.billableHours() != null) {
            employee.setBillableHours(req.billableHours());
        }

        Employee updated = employeeRepository.save(employee);
        return EmployeeDto.fromEntity(updated);
    }

    public void deleteEmployee(UUID id) {
        Employee employee = employeeRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Employee not found"));
        employeeRepository.delete(employee);
    }

    @Transactional(readOnly = true)
    public List<DepartmentDto> getDepartments() {
        List<Department> departments = departmentRepository.findAll();
        List<Employee> allEmployees = employeeRepository.findAll();

        return departments.stream()
                .map(d -> {
                    long headCount = allEmployees.stream()
                            .filter(e -> d.getName().equalsIgnoreCase(e.getDepartment()))
                            .count();
                    return DepartmentDto.fromEntity(d, headCount);
                })
                .toList();
    }

    public DepartmentDto createDepartment(CreateDepartmentRequest req) {
        if (departmentRepository.findByNameIgnoreCase(req.name().trim()).isPresent()) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Department already exists: " + req.name());
        }

        Department dept = new Department(
                req.name().trim(),
                req.lead() != null ? req.lead().trim() : null,
                req.budgetUtilization() != null ? req.budgetUtilization() : BigDecimal.ZERO
        );

        Department saved = departmentRepository.save(dept);
        return DepartmentDto.fromEntity(saved, 0);
    }

    @Transactional(readOnly = true)
    public HrmStatsDto getStats() {
        List<Employee> employees = employeeRepository.findAll();
        long total = employees.size();
        long active = employees.stream().filter(e -> e.getStatus() == EmployeeStatus.ACTIVE).count();
        long onLeave = employees.stream().filter(e -> e.getStatus() == EmployeeStatus.ON_LEAVE).count();
        long probation = employees.stream().filter(e -> e.getStatus() == EmployeeStatus.PROBATION).count();
        long inactive = employees.stream().filter(e -> e.getStatus() == EmployeeStatus.INACTIVE || e.getStatus() == EmployeeStatus.TERMINATED).count();
        long totalDepts = departmentRepository.count();

        BigDecimal avgAttendance = BigDecimal.ZERO;
        if (total > 0) {
            BigDecimal sum = employees.stream()
                    .map(e -> e.getAttendanceRate() != null ? e.getAttendanceRate() : BigDecimal.ZERO)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
            avgAttendance = sum.divide(BigDecimal.valueOf(total), 1, RoundingMode.HALF_UP);
        }

        int totalHours = employees.stream()
                .mapToInt(e -> e.getBillableHours() != null ? e.getBillableHours() : 0)
                .sum();

        return new HrmStatsDto(
                total,
                active,
                onLeave,
                probation,
                inactive,
                totalDepts,
                avgAttendance,
                totalHours
        );
    }
}
