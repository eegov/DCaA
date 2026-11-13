package ru.dcaa.students.student;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

record TransferStudentRequest(
        @Schema(example = "4") @NotNull @Min(1) @Max(6) Integer course,
        @Schema(example = "ПИбд-41") @NotBlank @Size(max = 20) String groupName) {
}
