package ru.dcaa.students.student;

import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

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

    Student(String recordBookNumber, String lastName, String firstName, String middleName,
            String groupName, int course) {
        updatePersonalData(recordBookNumber, lastName, firstName, middleName, groupName);
        this.course = course;
        this.status = StudentStatus.ACTIVE;
    }

    void updatePersonalData(String recordBookNumber, String lastName, String firstName,
                            String middleName, String groupName) {
        this.recordBookNumber = recordBookNumber;
        this.lastName = lastName;
        this.firstName = firstName;
        this.middleName = middleName;
        this.groupName = groupName;
    }

    void transfer(int course, String groupName) {
        this.course = course;
        this.groupName = groupName;
    }

    void expel() {
        this.status = StudentStatus.EXPELLED;
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
