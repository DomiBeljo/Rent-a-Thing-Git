package org.example.rentathingproba.unit.notification;

import org.example.rentathingproba.notification.ListingEmailTemplate;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("ListingEmailTemplate Unit Tests")
class ListingEmailTemplateTest {

    private ListingEmailTemplate template;

    @BeforeEach
    void setUp() {
        template = new ListingEmailTemplate();
    }

    @Test
    @DisplayName("buildCreateHtml: contains username and listing ID")
    void buildCreateHtml_containsUsernameAndId() {
        String html = template.buildCreateHtml("Alice", 42L);

        assertThat(html).contains("Alice");
        assertThat(html).contains("42");
        assertThat(html).contains("created");
    }

    @Test
    @DisplayName("buildCreateHtml: returns non-empty HTML string")
    void buildCreateHtml_returnsNonEmptyHtml() {
        String html = template.buildCreateHtml("Bob", 1L);

        assertThat(html).isNotBlank();
        assertThat(html).contains("<div");
    }

    @Test
    @DisplayName("buildUpdateHtml: contains username and listing ID")
    void buildUpdateHtml_containsUsernameAndId() {
        String html = template.buildUpdateHtml("Charlie", 99L);

        assertThat(html).contains("Charlie");
        assertThat(html).contains("99");
        assertThat(html).contains("updated");
    }

    @Test
    @DisplayName("buildUpdateHtml: returns non-empty HTML string")
    void buildUpdateHtml_returnsNonEmptyHtml() {
        String html = template.buildUpdateHtml("Dave", 5L);

        assertThat(html).isNotBlank();
        assertThat(html).contains("<div");
    }

    @Test
    @DisplayName("buildDeleteHtml: contains username and listing ID")
    void buildDeleteHtml_containsUsernameAndId() {
        String html = template.buildDeleteHtml("Eve", 7L);

        assertThat(html).contains("Eve");
        assertThat(html).contains("7");
        assertThat(html).contains("removed");
    }

    @Test
    @DisplayName("buildDeleteHtml: returns non-empty HTML string")
    void buildDeleteHtml_returnsNonEmptyHtml() {
        String html = template.buildDeleteHtml("Frank", 3L);

        assertThat(html).isNotBlank();
        assertThat(html).contains("<div");
    }
}