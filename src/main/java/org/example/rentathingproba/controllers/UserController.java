package org.example.rentathingproba.controllers;

import org.example.rentathingproba.entities.User;
import org.example.rentathingproba.responses.UserResponseDTO;
import org.example.rentathingproba.service.application.UserService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RequestMapping("/users")
@RestController
public class UserController {
    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    @GetMapping("/me")
    public ResponseEntity<UserResponseDTO> authenticatedUser(@AuthenticationPrincipal User currentUser){
        return  ResponseEntity.ok(new UserResponseDTO(
                currentUser.getId(),
                currentUser.getUsername(),
                currentUser.getEmail()
        ));

    }

    @GetMapping
    public ResponseEntity<List<UserResponseDTO>> allUsers(){
        return ResponseEntity.ok(userService.findAllUsers());
    }

}
