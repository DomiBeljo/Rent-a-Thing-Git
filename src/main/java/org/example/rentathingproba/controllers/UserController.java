package org.example.rentathingproba.controllers;

import org.example.rentathingproba.entities.User;
import org.example.rentathingproba.service.infrastrucure.UserService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
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
    public ResponseEntity<User> authenticatedUser(){
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            assert authentication != null;
            User currentUser = (User) authentication.getPrincipal();
        return ResponseEntity.ok(currentUser);

    }

    @GetMapping("/")
    public ResponseEntity<List<User>> allUsers(){
        List<User> users = userService.findAllUsers();
        return ResponseEntity.ok(users);
    }

}
