package org.example.rentathingproba.unit.entity;

import org.example.rentathingproba.entities.User;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("User Entity Unit Tests")
class UserEntityTest {

    private User buildUser() {
        return User.builder()
                .id(1L)
                .username("alice")
                .email("alice@example.com")
                .password("secret")
                .enabled(true)
                .build();
    }

    @Test
    @DisplayName("getUsername: returns email (Spring Security UserDetails override)")
    void getUsername_returnsEmail() {
        assertThat(buildUser().getUsername()).isEqualTo("alice@example.com");
    }

    @Test
    @DisplayName("getAuthorities: returns empty collection")
    void getAuthorities_isEmpty() {
        assertThat(buildUser().getAuthorities()).isEmpty();
    }

    @Test
    @DisplayName("UserDetails status methods all return true")
    void userDetailsStatusMethods_allReturnTrue() {
        User user = buildUser();
        assertThat(user.isAccountNonExpired()).isTrue();
        assertThat(user.isAccountNonLocked()).isTrue();
        assertThat(user.isCredentialsNonExpired()).isTrue();
        assertThat(user.isEnabled()).isTrue();
    }
}