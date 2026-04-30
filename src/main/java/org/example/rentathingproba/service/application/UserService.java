package org.example.rentathingproba.service.application;

import org.example.rentathingproba.entities.User;
import org.example.rentathingproba.repository.UserRepository;
import org.example.rentathingproba.responses.UserResponseDTO;
import org.example.rentathingproba.service.infrastructure.EmailService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

@Service
public class UserService {
    private final UserRepository userRepository;

    public UserService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    //Safe user returns
    @Transactional(readOnly = true)
    public List<UserResponseDTO> findAllUsers(){
        return StreamSupport.stream(userRepository.findAll().spliterator(), false)
                .map(u -> new UserResponseDTO(u.getId(), u.getUsername(), u.getEmail()))
                .collect(Collectors.toList());
    }
}
