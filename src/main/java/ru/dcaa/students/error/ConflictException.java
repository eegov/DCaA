package ru.dcaa.students.error;

/** Запрос корректен, но противоречит текущему состоянию данных (HTTP 409). */
public class ConflictException extends RuntimeException {

    public ConflictException(String message) {
        super(message);
    }
}
