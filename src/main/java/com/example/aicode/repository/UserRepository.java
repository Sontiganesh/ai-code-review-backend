package com.example.aicode.repository;

import com.example.aicode.model.User;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserRepository extends JpaRepository<User, Long> {

    User findByUsername(String username);
    
    // 🔹 Added method to support login by email
    User findByEmail(String email);
}
