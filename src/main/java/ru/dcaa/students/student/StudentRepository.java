package ru.dcaa.students.student;

import org.springframework.data.jpa.repository.JpaRepository;

interface StudentRepository extends JpaRepository<Student, Long> {

    boolean existsByRecordBookNumber(String recordBookNumber);

    boolean existsByRecordBookNumberAndIdNot(String recordBookNumber, Long id);
}
