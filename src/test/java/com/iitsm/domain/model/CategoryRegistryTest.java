package com.iitsm.domain.model;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.iitsm.domain.exception.ValidationException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * OOP: Proves the Open/Closed Principle for incident categories.
 * Demonstrates that new categories can be introduced dynamically at runtime without modifying enums.
 */
class CategoryRegistryTest {

    @Test
    @DisplayName("CategoryRegistry initializes all 9 standard enum categories")
    void testStandardCategoriesInitialized() {
        CategoryRegistry registry = new CategoryRegistry();

        for (IncidentCategory category : IncidentCategory.values()) {
            assertThat(registry.isValid(category.name())).isTrue();
            assertThat(registry.getDisplayName(category.name())).isEqualTo(category.getDisplayName());
        }
    }

    @Test
    @DisplayName("Open/Closed: Adding a new category requires registration only, without code modification")
    void testOpenClosedCategoryRegistration() {
        CategoryRegistry registry = new CategoryRegistry();

        String newCategoryCode = "DEVOPS";
        String newCategoryName = "DevOps & CI/CD";

        assertThat(registry.isValid(newCategoryCode)).isFalse();

        // Register new category at runtime
        registry.register(newCategoryCode, newCategoryName);

        assertThat(registry.isValid(newCategoryCode)).isTrue();
        assertThat(registry.getDisplayName(newCategoryCode)).isEqualTo(newCategoryName);
        assertThat(registry.getAllCodes()).contains("DEVOPS");
    }

    @Test
    @DisplayName("CategoryRegistry rejects invalid blank registrations")
    void testInvalidRegistrations() {
        CategoryRegistry registry = new CategoryRegistry();

        assertThatThrownBy(() -> registry.register("", "Display Name"))
                .isInstanceOf(ValidationException.class);

        assertThatThrownBy(() -> registry.register("CODE", null))
                .isInstanceOf(ValidationException.class);
    }
}

