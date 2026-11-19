package ru.dcaa.students.student;

import java.util.List;
import org.hibernate.exception.ConstraintViolationException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.dcaa.students.error.ConflictException;
import ru.dcaa.students.error.NotFoundException;

@Service
@Transactional
class StudentService {

    /** Имя ограничения уникальности из 001-create-students.sql. */
    private static final String RECORD_BOOK_CONSTRAINT = "uq_students_record_book";

    private final StudentRepository repository;

    StudentService(StudentRepository repository) {
        this.repository = repository;
    }

    Student admit(AdmitStudentRequest request) {
        return saveChecked(new Student(request.personalData(), request.course()));
    }

    @Transactional(readOnly = true)
    List<Student> findAll() {
        return repository.findAll(Sort.by("id"));
    }

    @Transactional(readOnly = true)
    Student get(long id) {
        return repository.findById(id).orElseThrow(() -> notFound(id));
    }

    Student update(long id, PersonalData data) {
        Student student = getForUpdate(id);
        student.updatePersonalData(data);
        return saveChecked(student);
    }

    Student transfer(long id, TransferStudentRequest request) {
        Student student = getForUpdate(id);
        student.transfer(request.course(), request.groupName());
        return student;
    }

    Student expel(long id) {
        Student student = getForUpdate(id);
        student.expel();
        return student;
    }

    void delete(long id) {
        repository.delete(getForUpdate(id));
    }

    private Student getForUpdate(long id) {
        return repository.findForUpdateById(id).orElseThrow(() -> notFound(id));
    }

    private static NotFoundException notFound(long id) {
        return new NotFoundException("Студент " + id + " не найден");
    }

    /** flush выполняет SQL сразу, чтобы нарушение уникальности пришло сюда, а не при коммите за пределами сервиса. */
    private Student saveChecked(Student student) {
        try {
            return repository.saveAndFlush(student);
        } catch (DataIntegrityViolationException exception) {
            if (exception.getCause() instanceof ConstraintViolationException violation
                    && RECORD_BOOK_CONSTRAINT.equals(violation.getConstraintName())) {
                throw new ConflictException("Номер зачётки " + student.getRecordBookNumber() + " уже занят");
            }
            throw exception;
        }
    }
}
