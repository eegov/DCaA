package ru.dcaa.students.student;

record StudentResponse(
        Long id,
        String recordBookNumber,
        String lastName,
        String firstName,
        String middleName,
        String groupName,
        int course,
        StudentStatus status) {

    static StudentResponse from(Student student) {
        return new StudentResponse(student.getId(), student.getRecordBookNumber(), student.getLastName(),
                student.getFirstName(), student.getMiddleName(), student.getGroupName(),
                student.getCourse(), student.getStatus());
    }
}
