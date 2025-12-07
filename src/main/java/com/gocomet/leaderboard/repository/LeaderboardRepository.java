package com.gocomet.leaderboard.repository;

import com.gocomet.leaderboard.model.LeaderboardEntry;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Repository
public interface LeaderboardRepository extends JpaRepository<LeaderboardEntry, Long> {

    Optional<LeaderboardEntry> findByUserId(Long userId);

    List<LeaderboardEntry> findTop10ByOrderByTotalScoreDesc();

    // Atomic Update
    // We execute the math INSIDE the database.
    @Modifying // Tells Spring this changes data
    @Transactional
    @Query("UPDATE LeaderboardEntry l SET l.totalScore = l.totalScore + :score WHERE l.user.id = :userId")
    int incrementScore(@Param("userId") Long userId, @Param("score") int score);
}
