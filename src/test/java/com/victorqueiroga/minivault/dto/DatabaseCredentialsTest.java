package com.victorqueiroga.minivault.dto;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class DatabaseCredentialsTest {

    @Test
    void shouldSetAndGetAllProperties() {
        DatabaseCredentials creds = new DatabaseCredentials();

        creds.setHost("db.example.com");
        creds.setPort(5432);
        creds.setDatabaseName("mydb");
        creds.setUsername("admin");
        creds.setPassword("secret");
        creds.setDatabaseType("postgresql");

        assertEquals("db.example.com", creds.getHost());
        assertEquals(5432, creds.getPort());
        assertEquals("mydb", creds.getDatabaseName());
        assertEquals("admin", creds.getUsername());
        assertEquals("secret", creds.getPassword());
        assertEquals("postgresql", creds.getDatabaseType());
    }

    @Test
    void shouldDefaultPortToZero() {
        DatabaseCredentials creds = new DatabaseCredentials();
        assertEquals(0, creds.getPort());
    }

    @Test
    void shouldAllowNullDatabaseType() {
        DatabaseCredentials creds = new DatabaseCredentials();
        assertNull(creds.getDatabaseType());
    }
}
