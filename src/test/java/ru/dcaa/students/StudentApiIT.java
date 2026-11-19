package ru.dcaa.students;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.assertj.MockMvcTester;
import org.springframework.test.web.servlet.assertj.MvcTestResult;

/**
 * Сценарии спецификаций student-records и service-runtime (документация API).
 * Каждый тест проверяет статус, тело ответа и состояние БД: изменилась запись или осталась прежней.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Import(PostgresTestConfiguration.class)
class StudentApiIT {

    @Autowired
    MockMvcTester mvc;

    @Autowired
    JdbcTemplate jdbc;

    @BeforeEach
    void cleanTable() {
        jdbc.execute("TRUNCATE students RESTART IDENTITY");
    }

    // --- Приём студента ---

    @Test
    void admitCreatesActiveStudent() {
        MvcTestResult result = post("/api/students", admitJson("100"));

        assertThat(result).hasStatus(HttpStatus.CREATED);
        assertThat(result).headers().hasValue("Location", "http://localhost/api/students/1");
        assertThat(result).bodyJson().extractingPath("$.id").isEqualTo(1);
        assertThat(result).bodyJson().extractingPath("$.status").isEqualTo("ACTIVE");
        assertThat(row(1)).containsEntry("last_name", "Иванов").containsEntry("course", 4)
                .containsEntry("status", "ACTIVE");
    }

    @Test
    void admitWithInvalidFieldsReturns400AndListsFields() {
        String json = """
                {"recordBookNumber": "100", "lastName": " ", "firstName": "Иван",
                 "groupName": "ПИбд-41", "course": 7}
                """;

        MvcTestResult result = post("/api/students", json);

        assertProblem(result, HttpStatus.BAD_REQUEST);
        assertThat(result).bodyJson().extractingPath("$.errors").asMap().containsOnlyKeys("lastName", "course");
        assertThat(count()).isZero();
    }

    @Test
    void admitWithTakenRecordBookReturns409() {
        admit("100");

        MvcTestResult result = post("/api/students", admitJson("100"));

        assertProblem(result, HttpStatus.CONFLICT);
        assertThat(count()).isEqualTo(1);
    }

    // --- Просмотр ---

    @Test
    void listReturnsAllStudentsInCreationOrderIncludingExpelled() {
        admit("100");
        long second = admit("200");
        expel(second);

        MvcTestResult result = mvc.get().uri("/api/students").exchange();

        assertThat(result).hasStatusOk();
        assertThat(result).bodyJson().extractingPath("$[*].recordBookNumber").asArray().containsExactly("100", "200");
        assertThat(result).bodyJson().extractingPath("$[1].status").isEqualTo("EXPELLED");
    }

    @Test
    void getReturnsStudent() {
        long id = admit("100");

        MvcTestResult result = mvc.get().uri("/api/students/{id}", id).exchange();

        assertThat(result).hasStatusOk();
        assertThat(result).bodyJson().extractingPath("$.recordBookNumber").isEqualTo("100");
        assertThat(result).bodyJson().extractingPath("$.groupName").isEqualTo("ПИбд-41");
    }

    @Test
    void getReturnsExpelledStudent() {
        long id = admit("100");
        expel(id);

        MvcTestResult result = mvc.get().uri("/api/students/{id}", id).exchange();

        assertThat(result).hasStatusOk();
        assertThat(result).bodyJson().extractingPath("$.status").isEqualTo("EXPELLED");
    }

    @Test
    void getUnknownReturns404() {
        assertProblem(mvc.get().uri("/api/students/999").exchange(), HttpStatus.NOT_FOUND);
    }

    @Test
    void modifyingUnknownStudentReturns404() {
        assertProblem(put("/api/students/999", updateJson("101")), HttpStatus.NOT_FOUND);
        assertProblem(transfer(999, 3), HttpStatus.NOT_FOUND);
        assertProblem(expel(999), HttpStatus.NOT_FOUND);
        assertProblem(mvc.delete().uri("/api/students/999").exchange(), HttpStatus.NOT_FOUND);
        assertThat(count()).isZero();
    }

    // --- Редактирование ---

    @Test
    void updateChangesPersonalDataAndGroup() {
        long id = admit("100");

        MvcTestResult result = put("/api/students/" + id, updateJson("101"));

        assertThat(result).hasStatusOk();
        assertThat(result).bodyJson().extractingPath("$.lastName").isEqualTo("Петров");
        assertThat(result).bodyJson().extractingPath("$.course").isEqualTo(4);
        assertThat(row(id)).containsEntry("record_book_number", "101")
                .containsEntry("last_name", "Петров")
                .containsEntry("group_name", "ПИбд-42")
                .containsEntry("course", 4);
    }

    @Test
    void updateWithInvalidFieldsReturns400AndKeepsRecord() {
        long id = admit("100");
        String json = updateJson("101").replace("\"Петров\"", "\" \"");

        MvcTestResult result = put("/api/students/" + id, json);

        assertProblem(result, HttpStatus.BAD_REQUEST);
        assertThat(result).bodyJson().extractingPath("$.errors").asMap().containsOnlyKeys("lastName");
        assertThat(row(id)).containsEntry("record_book_number", "100").containsEntry("last_name", "Иванов");
    }

    @Test
    void updateWithCourseFieldReturns400AndKeepsRecord() {
        assertUpdateWithExtraFieldIsRejected("\"course\": 1");
    }

    @Test
    void updateWithStatusFieldReturns400AndKeepsRecord() {
        assertUpdateWithExtraFieldIsRejected("\"status\": \"ACTIVE\"");
    }

    @Test
    void updateExpelledReturns409() {
        long id = admit("100");
        expel(id);

        assertProblem(put("/api/students/" + id, updateJson("101")), HttpStatus.CONFLICT);
        assertThat(row(id)).containsEntry("record_book_number", "100");
    }

    @Test
    void updateWithRecordBookOfAnotherStudentReturns409() {
        admit("100");
        long id = admit("200");

        assertProblem(put("/api/students/" + id, updateJson("100")), HttpStatus.CONFLICT);
        assertThat(row(id)).containsEntry("record_book_number", "200");
    }

    // --- Перевод на курс ---

    @Test
    void transferChangesCourseAndGroup() {
        long id = admit("100");

        MvcTestResult result = transfer(id, 3);

        assertThat(result).hasStatusOk();
        assertThat(result).bodyJson().extractingPath("$.course").isEqualTo(3);
        assertThat(result).bodyJson().extractingPath("$.groupName").isEqualTo("ПИбд-31");
        assertThat(row(id)).containsEntry("course", 3).containsEntry("group_name", "ПИбд-31");
    }

    @Test
    void transferWithInvalidFieldsReturns400AndKeepsRecord() {
        long id = admit("100");

        MvcTestResult result = transfer(id, 7);

        assertProblem(result, HttpStatus.BAD_REQUEST);
        assertThat(result).bodyJson().extractingPath("$.errors").asMap().containsOnlyKeys("course");
        assertThat(row(id)).containsEntry("course", 4).containsEntry("group_name", "ПИбд-41");
    }

    @Test
    void transferToCurrentCourseReturns409() {
        long id = admit("100");

        assertProblem(transfer(id, 4), HttpStatus.CONFLICT);
        assertThat(row(id)).containsEntry("course", 4).containsEntry("group_name", "ПИбд-41");
    }

    @Test
    void transferExpelledReturns409() {
        long id = admit("100");
        expel(id);

        assertProblem(transfer(id, 3), HttpStatus.CONFLICT);
        assertThat(row(id)).containsEntry("course", 4);
    }

    // --- Отчисление ---

    @Test
    void expelKeepsRecordWithCourseAndGroup() {
        long id = admit("100");

        MvcTestResult result = expel(id);

        assertThat(result).hasStatusOk();
        assertThat(result).bodyJson().extractingPath("$.status").isEqualTo("EXPELLED");
        assertThat(row(id)).containsEntry("status", "EXPELLED")
                .containsEntry("course", 4)
                .containsEntry("group_name", "ПИбд-41");
    }

    @Test
    void repeatedExpelReturns200AndKeepsRecord() {
        long id = admit("100");
        expel(id);
        Map<String, Object> before = row(id);

        MvcTestResult result = expel(id);

        assertThat(result).hasStatusOk();
        assertThat(result).bodyJson().extractingPath("$.status").isEqualTo("EXPELLED");
        assertThat(row(id)).isEqualTo(before);
    }

    // --- Удаление ---

    @Test
    void deleteRemovesRecord() {
        long id = admit("100");

        assertThat(mvc.delete().uri("/api/students/{id}", id).exchange()).hasStatus(HttpStatus.NO_CONTENT);
        assertProblem(mvc.get().uri("/api/students/{id}", id).exchange(), HttpStatus.NOT_FOUND);
        assertThat(count()).isZero();
    }

    @Test
    void deleteRemovesExpelledRecord() {
        long id = admit("100");
        expel(id);

        assertThat(mvc.delete().uri("/api/students/{id}", id).exchange()).hasStatus(HttpStatus.NO_CONTENT);
        assertThat(count()).isZero();
    }

    // --- Формат ошибок ---

    @Test
    void malformedJsonReturns400() {
        assertProblem(post("/api/students", "это не JSON"), HttpStatus.BAD_REQUEST);
        assertThat(count()).isZero();
    }

    @Test
    void unknownFieldReturns400() {
        String json = admitJson("100").replace("}", ", \"extra\": 1}");

        assertProblem(post("/api/students", json), HttpStatus.BAD_REQUEST);
        assertThat(count()).isZero();
    }

    @Test
    void fractionalCourseOnAdmitReturns400() {
        assertProblem(post("/api/students", admitJson("100").replace("\"course\": 4", "\"course\": 2.9")),
                HttpStatus.BAD_REQUEST);
        assertThat(count()).isZero();
    }

    @Test
    void stringCourseOnAdmitReturns400() {
        assertProblem(post("/api/students", admitJson("100").replace("\"course\": 4", "\"course\": \"3\"")),
                HttpStatus.BAD_REQUEST);
        assertThat(count()).isZero();
    }

    @Test
    void fractionalCourseOnTransferReturns400() {
        long id = admit("100");

        assertProblem(post("/api/students/" + id + "/transfer", "{\"course\": 2.9, \"groupName\": \"ПИбд-21\"}"),
                HttpStatus.BAD_REQUEST);
        assertThat(row(id)).containsEntry("course", 4).containsEntry("group_name", "ПИбд-41");
    }

    @Test
    void stringCourseOnTransferReturns400() {
        long id = admit("100");

        assertProblem(post("/api/students/" + id + "/transfer", "{\"course\": \"3\", \"groupName\": \"ПИбд-31\"}"),
                HttpStatus.BAD_REQUEST);
        assertThat(row(id)).containsEntry("course", 4).containsEntry("group_name", "ПИбд-41");
    }

    @Test
    void unsupportedMethodReturns405() {
        long id = admit("100");

        assertProblem(mvc.patch().uri("/api/students/{id}", id).exchange(), HttpStatus.METHOD_NOT_ALLOWED);
    }

    // --- Документация API ---

    @Test
    void swaggerAndOpenApiAreAvailable() {
        assertThat(mvc.get().uri("/swagger-ui.html").exchange())
                .hasStatus(HttpStatus.FOUND).hasRedirectedUrl("/swagger-ui/index.html");
        assertThat(mvc.get().uri("/swagger-ui/index.html").exchange()).hasStatusOk();

        MvcTestResult openApi = mvc.get().uri("/v3/api-docs").exchange();
        assertThat(openApi).hasStatusOk();
        assertThat(openApi).bodyJson().extractingPath("$.paths").asMap()
                .containsKeys("/api/students", "/api/students/{id}");
    }

    // --- Вспомогательные методы ---

    /** Ошибка в формате RFC 9457: нужный статус, тип application/problem+json, поля status, title, detail. */
    private static void assertProblem(MvcTestResult result, HttpStatus status) {
        assertThat(result).hasStatus(status).hasContentType(MediaType.APPLICATION_PROBLEM_JSON);
        assertThat(result).bodyJson().extractingPath("$.status").isEqualTo(status.value());
        assertThat(result).bodyJson().extractingPath("$.title").isNotNull();
        assertThat(result).bodyJson().extractingPath("$.detail").isNotNull();
    }

    private void assertUpdateWithExtraFieldIsRejected(String extraField) {
        long id = admit("100");
        String json = updateJson("101").replace("}", ", " + extraField + "}");

        assertProblem(put("/api/students/" + id, json), HttpStatus.BAD_REQUEST);
        assertThat(row(id)).containsEntry("record_book_number", "100").containsEntry("course", 4);
    }

    private long admit(String recordBookNumber) {
        assertThat(post("/api/students", admitJson(recordBookNumber))).hasStatus(HttpStatus.CREATED);
        return jdbc.queryForObject("select id from students where record_book_number = ?", Long.class,
                recordBookNumber);
    }

    private MvcTestResult expel(long id) {
        return mvc.post().uri("/api/students/{id}/expel", id).exchange();
    }

    private MvcTestResult transfer(long id, int course) {
        return post("/api/students/" + id + "/transfer",
                "{\"course\": %d, \"groupName\": \"ПИбд-%d1\"}".formatted(course, course));
    }

    private MvcTestResult post(String uri, String json) {
        return mvc.post().uri(uri).contentType(MediaType.APPLICATION_JSON).content(json).exchange();
    }

    private MvcTestResult put(String uri, String json) {
        return mvc.put().uri(uri).contentType(MediaType.APPLICATION_JSON).content(json).exchange();
    }

    private Map<String, Object> row(long id) {
        return jdbc.queryForMap("select * from students where id = ?", id);
    }

    private long count() {
        return jdbc.queryForObject("select count(*) from students", Long.class);
    }

    private static String admitJson(String recordBookNumber) {
        return """
                {"recordBookNumber": "%s", "lastName": "Иванов", "firstName": "Иван",
                 "middleName": "Иванович", "groupName": "ПИбд-41", "course": 4}
                """.formatted(recordBookNumber);
    }

    private static String updateJson(String recordBookNumber) {
        return """
                {"recordBookNumber": "%s", "lastName": "Петров", "firstName": "Пётр",
                 "middleName": null, "groupName": "ПИбд-42"}
                """.formatted(recordBookNumber);
    }
}
