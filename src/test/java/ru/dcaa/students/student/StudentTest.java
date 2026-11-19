package ru.dcaa.students.student;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;
import ru.dcaa.students.error.ConflictException;

/** Правила смены состояния студента без Spring и БД: запускаются обычным ./mvnw test. */
class StudentTest {

    private final Student student = new Student(data("100", "ПИбд-41"), 4);

    @Test
    void newStudentIsActive() {
        assertThat(student.getStatus()).isEqualTo(StudentStatus.ACTIVE);
    }

    @Test
    void transferChangesCourseAndGroup() {
        student.transfer(3, "ПИбд-31");

        assertThat(student.getCourse()).isEqualTo(3);
        assertThat(student.getGroupName()).isEqualTo("ПИбд-31");
    }

    @Test
    void transferToCurrentCourseIsRejected() {
        assertThatThrownBy(() -> student.transfer(4, "ПИбд-42")).isInstanceOf(ConflictException.class);
        assertThat(student.getGroupName()).isEqualTo("ПИбд-41");
    }

    @Test
    void expelledStudentCannotBeTransferredOrEdited() {
        student.expel();

        assertThatThrownBy(() -> student.transfer(3, "ПИбд-31")).isInstanceOf(ConflictException.class);
        assertThatThrownBy(() -> student.updatePersonalData(data("101", "ПИбд-42")))
                .isInstanceOf(ConflictException.class);
        assertThat(student.getCourse()).isEqualTo(4);
        assertThat(student.getRecordBookNumber()).isEqualTo("100");
    }

    @Test
    void expelKeepsCourseAndGroupAndCanBeRepeated() {
        student.expel();
        student.expel();

        assertThat(student.getStatus()).isEqualTo(StudentStatus.EXPELLED);
        assertThat(student.getCourse()).isEqualTo(4);
        assertThat(student.getGroupName()).isEqualTo("ПИбд-41");
    }

    private static PersonalData data(String recordBookNumber, String groupName) {
        return new PersonalData(recordBookNumber, "Иванов", "Иван", null, groupName);
    }
}
