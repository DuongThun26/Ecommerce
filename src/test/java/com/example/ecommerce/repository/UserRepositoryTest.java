package com.example.ecommerce.repository;

import com.example.ecommerce.auth.repository.UserRepository;
import com.example.ecommerce.auth.entity.UserEntity;
import com.example.ecommerce.testdata.UserTestDataBuilder;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.context.ActiveProfiles;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@ActiveProfiles("test")
class UserRepositoryTest {

    @Autowired
    private UserRepository userRepository;

    @Test
    @DisplayName("existsByUsername should return true for existing username")
    void existsByUsernameShouldWork() {
        userRepository.save(UserTestDataBuilder.aUser().withUsername("alice").withEmail("alice@example.com").build());

        assertThat(userRepository.existsByUsername("alice")).isTrue();
        assertThat(userRepository.existsByUsername("bob")).isFalse();
    }

    @Test
    @DisplayName("existsByEmail should return true for existing email")
    void existsByEmailShouldWork() {
        userRepository.save(UserTestDataBuilder.aUser().withUsername("john").withEmail("john@example.com").build());

        assertThat(userRepository.existsByEmail("john@example.com")).isTrue();
        assertThat(userRepository.existsByEmail("none@example.com")).isFalse();
    }

    @Test
    @DisplayName("findByUsername should return matching user")
    void findByUsernameShouldWork() {
        UserEntity saved = userRepository.save(UserTestDataBuilder.aUser().withUsername("search-me").withEmail("s@example.com").build());

        var result = userRepository.findByUsername("search-me");

        assertThat(result).isPresent();
        assertThat(result.get().getId()).isEqualTo(saved.getId());
    }
}
