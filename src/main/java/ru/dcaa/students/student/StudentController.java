package ru.dcaa.students.student;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.util.UriComponentsBuilder;

@RestController
@RequestMapping("/api/students")
@Tag(name = "Студенты")
class StudentController {

    private final StudentService service;

    StudentController(StudentService service) {
        this.service = service;
    }

    @PostMapping
    @Operation(summary = "Принять студента")
    ResponseEntity<StudentResponse> admit(@Valid @RequestBody AdmitStudentRequest request,
                                          UriComponentsBuilder uri) {
        Student student = service.admit(request);
        return ResponseEntity
                .created(uri.path("/api/students/{id}").buildAndExpand(student.getId()).toUri())
                .body(StudentResponse.from(student));
    }

    @GetMapping
    @Operation(summary = "Список всех студентов в порядке приёма")
    List<StudentResponse> findAll() {
        return service.findAll().stream().map(StudentResponse::from).toList();
    }

    @GetMapping("/{id}")
    @Operation(summary = "Студент по идентификатору")
    StudentResponse get(@PathVariable long id) {
        return StudentResponse.from(service.get(id));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Изменить номер зачётки, ФИО и группу")
    StudentResponse update(@PathVariable long id, @Valid @RequestBody UpdateStudentRequest request) {
        return StudentResponse.from(service.update(id, request));
    }

    @PostMapping("/{id}/transfer")
    @Operation(summary = "Перевести на курс")
    StudentResponse transfer(@PathVariable long id, @Valid @RequestBody TransferStudentRequest request) {
        return StudentResponse.from(service.transfer(id, request));
    }

    @PostMapping("/{id}/expel")
    @Operation(summary = "Отчислить (запись сохраняется)")
    StudentResponse expel(@PathVariable long id) {
        return StudentResponse.from(service.expel(id));
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Удалить запись")
    void delete(@PathVariable long id) {
        service.delete(id);
    }
}
