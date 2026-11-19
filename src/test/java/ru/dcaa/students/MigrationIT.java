package ru.dcaa.students;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;

/** Сценарий «Старт на пустой БД» из спецификации service-runtime. */
@SpringBootTest
@Import(PostgresTestConfiguration.class)
class MigrationIT {

    @Autowired
    JdbcTemplate jdbc;

    @Test
    void liquibaseCreatesOnlyStudentsTableAndItsServiceTables() {
        var tables = jdbc.queryForList(
                "select table_name from information_schema.tables where table_schema = 'public'", String.class);

        assertThat(tables).containsExactlyInAnyOrder("students", "databasechangelog", "databasechangeloglock");
    }

    @Test
    void changesetIsRecordedInChangelog() {
        var applied = jdbc.queryForList("select id from databasechangelog", String.class);

        assertThat(applied).containsExactly("001-create-students");
    }
}
