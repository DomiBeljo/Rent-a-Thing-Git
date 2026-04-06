package org.example.rentathingproba.service;

import org.example.rentathingproba.entities.User;
import org.example.rentathingproba.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
public class UserService {
    private final UserRepository userRepository;

    public UserService(UserRepository userRepository,EmailService emailService) {
        this.userRepository = userRepository;

    }

    public List<User> findAllUsers(){
        List<User> users = new ArrayList<>();
        userRepository.findAll().forEach(users::add);
        return users;
    }
}
