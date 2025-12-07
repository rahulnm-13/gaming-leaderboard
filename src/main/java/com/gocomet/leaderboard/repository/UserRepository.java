package com.gocomet.leaderboard.repository;

import com.gocomet.leaderboard.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {
    // Basic CRUD methods (findById, save, etc.) are included automatically
}
