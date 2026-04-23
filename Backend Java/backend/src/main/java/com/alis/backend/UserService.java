package com.alis.backend;

import org.springframework.stereotype.Service;
import java.util.Random;

@Service
public class UserService {

    private final UserRepository userRepository;

    public UserService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    // Generate ID: Katlego_4821
    public String generateUserId(String name) {
        int random = new Random().nextInt(9000) + 1000;
        return name.replaceAll(" ", "") + "_" + random;
    }

    public User register(String name, String password, String userType) {

        User user = new User();
        user.setName(name);
        user.setPassword(password);
        user.setUserIdentifier(generateUserId(name));
        user.setUserType(userType);

        return userRepository.save(user);
    }

    public User login(String userId, String password) {

        return userRepository.findByUserIdentifier(userId)
                .filter(u -> u.getPassword().equals(password))
                .orElse(null);
    }
}
