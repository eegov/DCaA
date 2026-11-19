package ru.dcaa.students.student;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Данные студента, которые задаются при приёме и меняются редактированием.
 * Длины совпадают со столбцами таблицы students (001-create-students.sql).
 */
record PersonalData(
        @Schema(example = "20231234") @NotBlank @Size(max = 20) String recordBookNumber,
        @Schema(example = "Иванов") @NotBlank @Size(max = 100) String lastName,
        @Schema(example = "Иван") @NotBlank @Size(max = 100) String firstName,
        @Schema(example = "Иванович") @Size(max = 100) String middleName,
        @Schema(example = "ПИбд-41") @NotBlank @Size(max = 20) String groupName) {
}
