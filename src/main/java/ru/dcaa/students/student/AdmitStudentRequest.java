package ru.dcaa.students.student;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

record AdmitStudentRequest(
        @Schema(example = "20231234") @NotBlank @Size(max = 20) String recordBookNumber,
        @Schema(example = "Иванов") @NotBlank @Size(max = 100) String lastName,
        @Schema(example = "Иван") @NotBlank @Size(max = 100) String firstName,
        @Schema(example = "Иванович") @Size(max = 100) String middleName,
        @Schema(example = "ПИбд-41") @NotBlank @Size(max = 20) String groupName,
        @Schema(example = "4") @NotNull @Min(1) @Max(6) Integer course) {
}
