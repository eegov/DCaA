package ru.dcaa.students.student;

import jakarta.persistence.LockModeType;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;

interface StudentRepository extends JpaRepository<Student, Long> {

    /** SELECT ... FOR UPDATE: параллельные изменения одной записи выполняются по очереди. */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    Optional<Student> findForUpdateById(long id);
}
