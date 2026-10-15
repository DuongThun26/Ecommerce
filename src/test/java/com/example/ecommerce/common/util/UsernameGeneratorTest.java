package com.example.ecommerce.common.util;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

class UsernameGeneratorTest {

    @Test
    @DisplayName("normalizeBaseName should strip Vietnamese diacritics and convert to lowercase")
    void normalizeBaseNameShouldStripVietnameseDiacritics() {
        assertThat(UsernameGenerator.normalizeBaseName("Nguyễn Văn Anh")).isEqualTo("nguyenvananh");
        assertThat(UsernameGenerator.normalizeBaseName("Đặng Đức Thuận")).isEqualTo("dangducthuan");
        assertThat(UsernameGenerator.normalizeBaseName("Lê Thị Thảo Đan")).isEqualTo("lethithaodan");
        assertThat(UsernameGenerator.normalizeBaseName("  Trần   Hữu   Đức  ")).isEqualTo("tranhuuduc");
        assertThat(UsernameGenerator.normalizeBaseName("John Doe 123!@#")).isEqualTo("johndoe123");
    }

    @Test
    @DisplayName("normalizeBaseName should handle empty or null name with fallback 'customer'")
    void normalizeBaseNameShouldHandleNullOrEmpty() {
        assertThat(UsernameGenerator.normalizeBaseName(null)).isEqualTo("customer");
        assertThat(UsernameGenerator.normalizeBaseName("")).isEqualTo("customer");
        assertThat(UsernameGenerator.normalizeBaseName("   ")).isEqualTo("customer");
        assertThat(UsernameGenerator.normalizeBaseName("!@#$%^&*")).isEqualTo("customer");
    }

    @Test
    @DisplayName("generateUniqueUsername should append random digits and check uniqueness")
    void generateUniqueUsernameShouldEnsureUniqueness() {
        Set<String> existingUsernames = new HashSet<>();
        // Simulate existing collisions
        String base = UsernameGenerator.normalizeBaseName("Nguyễn An");
        existingUsernames.add(base + "1000");
        existingUsernames.add(base + "1001");

        String username = UsernameGenerator.generateUniqueUsername("Nguyễn An", existingUsernames::contains);

        assertThat(username).startsWith(base);
        assertThat(existingUsernames.contains(username)).isFalse();
    }
}
