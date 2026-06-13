package com.tutor.marketplaceservice.subject;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.jdbc.core.JdbcTemplate;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class SubjectSchemaTest {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    void descriptionColumnIsNotNullable() {
        String nullable = jdbcTemplate.queryForObject("""
                select is_nullable
                from information_schema.columns
                where table_name = 'SUBJECTS'
                  and column_name = 'DESCRIPTION'
                """, String.class);

        assertThat(nullable).isEqualTo("NO");
    }
}
