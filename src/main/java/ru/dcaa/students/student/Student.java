package ru.dcaa.students.student;

import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import ru.dcaa.students.error.ConflictException;

@Entity
@Table(name = "students")
public class Student {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String recordBookNumber;
    private String lastName;
    private String firstName;
    private String middleName;
    private String groupName;
    private int course;

    @Enumerated(EnumType.STRING)
    private StudentStatus status;

    protected Student() {
        // для JPA
    }

    Student(PersonalData data, int course) {
        setPersonalData(data);
        this.course = course;
        this.status = StudentStatus.ACTIVE;
    }

    void updatePersonalData(PersonalData data) {
        requireActive();
        setPersonalData(data);
    }

    void transfer(int course, String groupName) {
        requireActive();
        if (course == this.course) {
            throw new ConflictException("Студент уже учится на " + course + " курсе");
        }
        this.course = course;
        this.groupName = groupName;
    }

    /** Повторное отчисление ничего не меняет и ошибкой не считается. */
    void expel() {
        this.status = StudentStatus.EXPELLED;
    }

    private void requireActive() {
        if (status == StudentStatus.EXPELLED) {
            throw new ConflictException("Студент " + id + " отчислен, изменение невозможно");
        }
    }

    private void setPersonalData(PersonalData data) {
        this.recordBookNumber = data.recordBookNumber();
        this.lastName = data.lastName();
        this.firstName = data.firstName();
        this.middleName = data.middleName();
        this.groupName = data.groupName();
    }

    public Long getId() { return id; }
    public String getRecordBookNumber() { return recordBookNumber; }
    public String getLastName() { return lastName; }
    public String getFirstName() { return firstName; }
    public String getMiddleName() { return middleName; }
    public String getGroupName() { return groupName; }
    public int getCourse() { return course; }
    public StudentStatus getStatus() { return status; }
}
