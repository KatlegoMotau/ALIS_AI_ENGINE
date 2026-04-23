package com.alis.backend;

import org.springframework.web.bind.annotation.*;
import java.util.Map;

@RestController
@RequestMapping("/api/user")
@CrossOrigin("*")
public class UserController {

    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    // REGISTER
    @PostMapping("/register")
    public Object register(@RequestBody Map<String, String> request) {

        User user = userService.register(
                request.get("name"),
                request.get("password"),
                request.get("user_type")
        );

        return Map.of(
                "message", "User created",
                "user_id", user.getId(),
                "user_id", user.getUserIdentifier()
        );
    }

    // LOGIN
    @PostMapping("/login")
    public Object login(@RequestBody Map<String, String> request) {

        User user = userService.login(
                request.get("user_identifier"),
                request.get("password")
        );

        if (user == null) {
            return Map.of("error", "Invalid login");
        }

        return Map.of(
                "message", "Login successful",
                "user_id", user.getId(),
                "user_identifier", user.getUserIdentifier()
        );
    }
}
