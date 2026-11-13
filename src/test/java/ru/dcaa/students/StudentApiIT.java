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
 * Каждый тест проверяет статус, тело ответа и, где запись должна измениться или нет, состояние БД.
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
        MvcTestResult result = post("/api/students", admitJson("100", 4));

        assertThat(result).hasStatus(HttpStatus.CREATED);
        assertThat(result).headers().hasValue("Location", "http://localhost/api/students/1");
        assertThat(result).bodyJson().extractingPath("$.status").isEqualTo("ACTIVE");
        assertThat(row(1)).containsEntry("last_name", "Иванов").containsEntry("course", 4);
    }

    @Test
    void admitWithInvalidFieldsReturns400AndListsFields() {
        String json = """
                {"recordBookNumber": "100", "lastName": " ", "firstName": "Иван",
                 "groupName": "ПИбд-41", "course": 7}
                """;

        MvcTestResult result = post("/api/students", json);

        assertThat(result).hasStatus(HttpStatus.BAD_REQUEST);
        assertThat(result).bodyJson().extractingPath("$.errors.lastName").isNotNull();
        assertThat(result).bodyJson().extractingPath("$.errors.course").isNotNull();
        assertThat(count()).isZero();
    }

    @Test
    void admitWithTakenRecordBookReturns409() {
        admit("100");

        MvcTestResult result = post("/api/students", admitJson("100", 1));

        assertThat(result).hasStatus(HttpStatus.CONFLICT);
        assertThat(count()).isEqualTo(1);
    }

    // --- Просмотр ---

    @Test
    void listReturnsAllStudentsInCreationOrderIncludingExpelled() {
        admit("100");
        long second = admit("200");
        post("/api/students/" + second + "/expel", null);

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
    }

    @Test
    void getUnknownReturns404() {
        assertThat(mvc.get().uri("/api/students/999").exchange()).hasStatus(HttpStatus.NOT_FOUND);
    }

    // --- Редактирование ---

    @Test
    void updateChangesPersonalDataAndGroup() {
        long id = admit("100");

        MvcTestResult result = put("/api/students/" + id, updateJson("101"));

        assertThat(result).hasStatusOk();
        assertThat(row(id)).containsEntry("record_book_number", "101")
                .containsEntry("last_name", "Петров")
                .containsEntry("group_name", "ПИбд-42")
                .containsEntry("course", 4);
    }

    @Test
    void updateWithCourseFieldReturns400AndKeepsRecord() {
        long id = admit("100");
        String json = updateJson("101").replace("}", ", \"course\": 1}");

        MvcTestResult result = put("/api/students/" + id, json);

        assertThat(result).hasStatus(HttpStatus.BAD_REQUEST);
        assertThat(row(id)).containsEntry("record_book_number", "100").containsEntry("course", 4);
    }

    @Test
    void updateExpelledReturns409() {
        long id = admit("100");
        post("/api/students/" + id + "/expel", null);

        MvcTestResult result = put("/api/students/" + id, updateJson("101"));

        assertThat(result).hasStatus(HttpStatus.CONFLICT);
        assertThat(row(id)).containsEntry("record_book_number", "100");
    }

    @Test
    void updateWithRecordBookOfAnotherStudentReturns409() {
        admit("100");
        long id = admit("200");

        MvcTestResult result = put("/api/students/" + id, updateJson("100"));

        assertThat(result).hasStatus(HttpStatus.CONFLICT);
        assertThat(row(id)).containsEntry("record_book_number", "200");
    }

    // --- Перевод на курс ---

    @Test
    void transferChangesCourseAndGroup() {
        long id = admit("100");

        MvcTestResult result = post("/api/students/" + id + "/transfer", "{\"course\": 3, \"groupName\": \"ПИбд-31\"}");

        assertThat(result).hasStatusOk();
        assertThat(result).bodyJson().extractingPath("$.course").isEqualTo(3);
        assertThat(row(id)).containsEntry("course", 3).containsEntry("group_name", "ПИбд-31");
    }

    @Test
    void transferExpelledReturns409() {
        long id = admit("100");
        post("/api/students/" + id + "/expel", null);

        MvcTestResult result = post("/api/students/" + id + "/transfer", "{\"course\": 3, \"groupName\": \"ПИбд-31\"}");

        assertThat(result).hasStatus(HttpStatus.CONFLICT);
        assertThat(row(id)).containsEntry("course", 4);
    }

    // --- Отчисление ---

    @Test
    void expelKeepsRecordWithCourseAndGroup() {
        long id = admit("100");

        MvcTestResult result = post("/api/students/" + id + "/expel", null);

        assertThat(result).hasStatusOk();
        assertThat(row(id)).containsEntry("status", "EXPELLED")
                .containsEntry("course", 4)
                .containsEntry("group_name", "ПИбд-41");
    }

    @Test
    void repeatedExpelReturns200() {
        long id = admit("100");
        post("/api/students/" + id + "/expel", null);

        MvcTestResult result = post("/api/students/" + id + "/expel", null);

        assertThat(result).hasStatusOk();
        assertThat(result).bodyJson().extractingPath("$.status").isEqualTo("EXPELLED");
    }

    // --- Удаление ---

    @Test
    void deleteRemovesRecord() {
        long id = admit("100");

        assertThat(mvc.delete().uri("/api/students/{id}", id).exchange()).hasStatus(HttpStatus.NO_CONTENT);
        assertThat(mvc.get().uri("/api/students/{id}", id).exchange()).hasStatus(HttpStatus.NOT_FOUND);
        assertThat(count()).isZero();
    }

    // --- Формат ошибок ---

    @Test
    void malformedJsonReturns400ProblemDetail() {
        MvcTestResult result = post("/api/students", "это не JSON");

        assertThat(result).hasStatus(HttpStatus.BAD_REQUEST).hasContentType(MediaType.APPLICATION_PROBLEM_JSON);
        assertThat(result).bodyJson().extractingPath("$.status").isEqualTo(400);
    }

    @Test
    void unsupportedMethodReturns405() {
        long id = admit("100");

        assertThat(mvc.patch().uri("/api/students/{id}", id).exchange()).hasStatus(HttpStatus.METHOD_NOT_ALLOWED);
    }

    // --- Документация API ---

    @Test
    void openApiDescribesStudentEndpoints() {
        MvcTestResult result = mvc.get().uri("/v3/api-docs").exchange();

        assertThat(result).hasStatusOk();
        assertThat(result).bodyJson().extractingPath("$.paths").asMap()
                .containsKeys("/api/students", "/api/students/{id}");
        assertThat(mvc.get().uri("/swagger-ui/index.html").exchange()).hasStatusOk();
    }

    // --- Вспомогательные методы ---

    private long admit(String recordBookNumber) {
        post("/api/students", admitJson(recordBookNumber, 4));
        return jdbc.queryForObject("select id from students where record_book_number = ?", Long.class,
                recordBookNumber);
    }

    private MvcTestResult post(String uri, String json) {
        var request = mvc.post().uri(uri);
        return json == null ? request.exchange() : request.contentType(MediaType.APPLICATION_JSON).content(json).exchange();
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

    private static String admitJson(String recordBookNumber, int course) {
        return """
                {"recordBookNumber": "%s", "lastName": "Иванов", "firstName": "Иван",
                 "middleName": "Иванович", "groupName": "ПИбд-41", "course": %d}
                """.formatted(recordBookNumber, course);
    }

    private static String updateJson(String recordBookNumber) {
        return """
                {"recordBookNumber": "%s", "lastName": "Петров", "firstName": "Пётр",
                 "middleName": null, "groupName": "ПИбд-42"}
                """.formatted(recordBookNumber);
    }
}
