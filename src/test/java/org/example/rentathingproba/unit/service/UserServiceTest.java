package org.example.rentathingproba.unit.service;

import org.example.rentathingproba.entities.User;
import org.example.rentathingproba.repository.UserRepository;
import org.example.rentathingproba.responses.UserResponseDTO;
import org.example.rentathingproba.service.application.UserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("UserService Unit Tests")
class UserServiceTest {

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private UserService userService;

    private User user1;
    private User user2;

    @BeforeEach
    void setUp() {
        user1 = User.builder().id(1L).username("dom").email("dom@example.com").password("enc").build();
        user2 = User.builder().id(2L).username("ana").email("ana@example.com").password("enc").build();
    }

    //Find all users
    @Test
    @DisplayName("findAllUsers: returns DTO list with id, username, and email for each user")
    void findAllUsers_returnsMappedDTOList() {
        when(userRepository.findAll()).thenReturn(List.of(user1, user2));

        List<UserResponseDTO> results = userService.findAllUsers();

        assertThat(results).hasSize(2);
        assertThat(results.get(0).getId()).isEqualTo(1L);
        assertThat(results.get(0).getUsername()).isEqualTo("dom@example.com");
        assertThat(results.get(0).getEmail()).isEqualTo("dom@example.com");
        assertThat(results.get(1).getId()).isEqualTo(2L);
        assertThat(results.get(1).getUsername()).isEqualTo("ana@example.com");
        verify(userRepository).findAll();
    }

    @Test
    @DisplayName("findAllUsers: returns empty list when no users exist")
    void findAllUsers_returnsEmptyListWhenNoUsersExist() {
        when(userRepository.findAll()).thenReturn(List.of());

        List<UserResponseDTO> results = userService.findAllUsers();

        assertThat(results).isEmpty();
    }

    @Test
    @DisplayName("findAllUsers: does not expose password in response")
    void findAllUsers_doesNotExposePassword() {
        when(userRepository.findAll()).thenReturn(List.of(user1));

        List<UserResponseDTO> results = userService.findAllUsers();

        assertThat(results.get(0)).isInstanceOf(UserResponseDTO.class);
        assertThat(results.get(0).getId()).isNotNull();
        assertThat(results.get(0).getUsername()).isNotNull();
        assertThat(results.get(0).getEmail()).isNotNull();
    }
}
