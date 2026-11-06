package com.myCafeteria.orders;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;
import javax.sql.DataSource;
import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@ActiveProfiles("test")
public class MigrationIntegrationTest {
    @Autowired private DataSource dataSource;
    @Autowired private Flyway flyway;

    @Test
    public void initializesVersionedSchemaAndInventoryColumns() throws Exception {
        assertEquals("2", flyway.info().current().getVersion().toString());
        try (Connection connection = dataSource.getConnection();
             Statement stmt = connection.createStatement()) {
            try (ResultSet rs = stmt.executeQuery("SELECT COUNT(*) FROM food_menu WHERE stock IS NULL OR active IS NULL")) {
                assertTrue(rs.next());
                assertEquals(0L, rs.getLong(1));
            }
            try (ResultSet rs = stmt.executeQuery("SELECT COUNT(*) FROM cafeteria_orders")) {
                assertTrue(rs.next());
            }
            try (ResultSet rs = stmt.executeQuery("SELECT COUNT(*) FROM cafeteria_order_items")) {
                assertTrue(rs.next());
            }
        }
    }
}
