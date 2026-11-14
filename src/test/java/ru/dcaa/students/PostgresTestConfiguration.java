package ru.dcaa.students;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.context.annotation.Bean;
import org.testcontainers.postgresql.PostgreSQLContainer;

/**
 * Настоящая PostgreSQL в контейнере для интеграционных тестов.
 * {@link ServiceConnection} сам передаёт Spring адрес, логин и пароль контейнера,
 * поэтому тесты никогда не подключаются к рабочей БД из .env.
 */
@TestConfiguration(proxyBeanMethods = false)
public class PostgresTestConfiguration {

    @Bean
    @ServiceConnection
    PostgreSQLContainer postgres() {
        return new PostgreSQLContainer("postgres:18");
    }
}
