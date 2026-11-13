package ru.dcaa.students.student;

import java.util.List;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.dcaa.students.error.ConflictException;
import ru.dcaa.students.error.NotFoundException;

@Service
@Transactional
class StudentService {

    private final StudentRepository repository;

    StudentService(StudentRepository repository) {
        this.repository = repository;
    }

    Student admit(AdmitStudentRequest request) {
        if (repository.existsByRecordBookNumber(request.recordBookNumber())) {
            throw duplicateRecordBook(request.recordBookNumber());
        }
        return repository.save(new Student(request.recordBookNumber(), request.lastName(), request.firstName(),
                request.middleName(), request.groupName(), request.course()));
    }

    @Transactional(readOnly = true)
    List<Student> findAll() {
        return repository.findAll(Sort.by("id"));
    }

    @Transactional(readOnly = true)
    Student get(long id) {
        return repository.findById(id)
                .orElseThrow(() -> new NotFoundException("Студент " + id + " не найден"));
    }

    Student update(long id, UpdateStudentRequest request) {
        Student student = getActive(id);
        if (repository.existsByRecordBookNumberAndIdNot(request.recordBookNumber(), id)) {
            throw duplicateRecordBook(request.recordBookNumber());
        }
        student.updatePersonalData(request.recordBookNumber(), request.lastName(), request.firstName(),
                request.middleName(), request.groupName());
        return student;
    }

    Student transfer(long id, TransferStudentRequest request) {
        Student student = getActive(id);
        student.transfer(request.course(), request.groupName());
        return student;
    }

    Student expel(long id) {
        Student student = get(id);
        student.expel();
        return student;
    }

    void delete(long id) {
        repository.delete(get(id));
    }

    private Student getActive(long id) {
        Student student = get(id);
        if (student.getStatus() == StudentStatus.EXPELLED) {
            throw new ConflictException("Студент " + id + " отчислен, изменение невозможно");
        }
        return student;
    }

    private static ConflictException duplicateRecordBook(String recordBookNumber) {
        return new ConflictException("Номер зачётки " + recordBookNumber + " уже занят");
    }
}
