package ru.dcaa.students.student;

import com.fasterxml.jackson.annotation.JsonUnwrapped;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

/** Тело приёма: данные студента и курс, в одном плоском JSON-объекте. */
record AdmitStudentRequest(
        @JsonUnwrapped @Valid PersonalData personalData,
        @Schema(example = "4") @NotNull @Min(1) @Max(6) Integer course) {
}
