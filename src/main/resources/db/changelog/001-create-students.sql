--liquibase formatted sql

--changeset dcaa:001-create-students
CREATE TABLE students (
    id                 BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    record_book_number VARCHAR(20)  NOT NULL,
    last_name          VARCHAR(100) NOT NULL,
    first_name         VARCHAR(100) NOT NULL,
    middle_name        VARCHAR(100),
    group_name         VARCHAR(20)  NOT NULL,
    course             INTEGER      NOT NULL,
    status             VARCHAR(16)  NOT NULL,
    CONSTRAINT uq_students_record_book UNIQUE (record_book_number),
    CONSTRAINT ck_students_course CHECK (course BETWEEN 1 AND 6),
    CONSTRAINT ck_students_status CHECK (status IN ('ACTIVE', 'EXPELLED'))
);
--rollback DROP TABLE students;
