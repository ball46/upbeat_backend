package com.example.upbeat_backend.controller;

import com.example.upbeat_backend.model.User;
import com.example.upbeat_backend.repository.UserRepository;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/users")
public class UserController {
    private final UserRepository userRepository;

    public UserController(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @GetMapping
    public String test() {
        User user = new User();
        user.setUsername("test");
        user.setPassword("test");
        user.setEmail("test@gmail.com");
        userRepository.save(user);
        return "User created";
    }

    @GetMapping("/all")
    public List<User> getAllUsers() {
        return userRepository.findAll();
    }
}
