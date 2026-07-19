package com.victorqueiroga.minivault.dto;

import org.junit.jupiter.api.Test;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;

class PageResponseTest {

    @Test
    void shouldCreatePageResponseWithFirstPage() {
        List<String> content = List.of("a", "b");
        PageResponse<String> response = new PageResponse<>(content, 0, 10, 25, 3);

        assertEquals(content, response.getContent());
        assertEquals(0, response.getPage());
        assertEquals(10, response.getSize());
        assertEquals(25, response.getTotalElements());
        assertEquals(3, response.getTotalPages());
        assertTrue(response.isFirst());
        assertFalse(response.isLast());
    }

    @Test
    void shouldCreatePageResponseWithLastPage() {
        PageResponse<String> response = new PageResponse<>(List.of(), 2, 10, 25, 3);

        assertFalse(response.isFirst());
        assertTrue(response.isLast());
    }

    @Test
    void shouldCreatePageResponseWithMiddlePage() {
        PageResponse<String> response = new PageResponse<>(List.of(), 1, 10, 25, 3);

        assertFalse(response.isFirst());
        assertFalse(response.isLast());
    }

    @Test
    void shouldHandleSinglePage() {
        PageResponse<String> response = new PageResponse<>(List.of("x"), 0, 10, 1, 1);

        assertTrue(response.isFirst());
        assertTrue(response.isLast());
    }

    @Test
    void shouldHandleEmptyContent() {
        PageResponse<String> response = new PageResponse<>(List.of(), 0, 10, 0, 0);

        assertTrue(response.isFirst());
        assertTrue(response.isLast());
        assertTrue(response.getContent().isEmpty());
    }
}
