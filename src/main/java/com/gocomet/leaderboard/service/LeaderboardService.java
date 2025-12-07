package com.gocomet.leaderboard.service;

import com.gocomet.leaderboard.model.GameSession;
import com.gocomet.leaderboard.model.LeaderboardEntry;
import com.gocomet.leaderboard.model.User;
import com.gocomet.leaderboard.repository.GameSessionRepository;
import com.gocomet.leaderboard.repository.LeaderboardRepository;
import com.gocomet.leaderboard.repository.UserRepository;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ZSetOperations;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

@Service
public class LeaderboardService {

    @Autowired
    private LeaderboardRepository leaderboardRepo;

    @Autowired
    private GameSessionRepository gameSessionRepo; // ✅ Added this

    @Autowired
    private UserRepository userRepo; // ✅ Added this to fetch User entity

    @Autowired
    private RedisTemplate<String, String> redisTemplate;

    private static final String LEADERBOARD_KEY = "game_leaderboard";
    private boolean isWarmedUp = false;

    @PostConstruct
    public void init() {
        CompletableFuture.runAsync(() -> {
            System.out.println("[Redis] Starting background warm-up...");
            long start = System.currentTimeMillis();
            
            List<LeaderboardEntry> allEntries = leaderboardRepo.findAll();
            
            for (LeaderboardEntry entry : allEntries) {
                if (entry.getUser() != null) {
                   redisTemplate.opsForZSet().add(LEADERBOARD_KEY, 
                        String.valueOf(entry.getUser().getId()), 
                        entry.getTotalScore());
                }
            }

            long end = System.currentTimeMillis();
            isWarmedUp = true;
            System.out.println("[Redis] Warm-up complete! Loaded " + allEntries.size() + 
                               " records in " + (end - start) + "ms");
        });
    }

    /**
     * Requirement: Submit Score
     * 1. Update Redis (Real-time Rank)
     * 2. Insert into Game Sessions (History/Audit Trail)
     * 3. Update Leaderboard Table (Aggregate Score)
     */
    @Transactional
    public void submitScore(Long userId, int score) {
        // 1. Update Redis (Atomic by default)
        redisTemplate.opsForZSet().incrementScore(LEADERBOARD_KEY, String.valueOf(userId), score);

        // 2. Insert Game Session (Always Safe - it's an append)
        User user = userRepo.findById(userId).orElse(null);
        if (user == null) return; 

        GameSession session = new GameSession();
        session.setUser(user);
        session.setScore(score);
        session.setGameMode("solo"); 
        session.setTimestamp(LocalDateTime.now());
        gameSessionRepo.save(session);

        // 3. Update Leaderboard (Atomic DB Update)
        // Try to update existing row first (Fastest & Safe)
        int rowsUpdated = leaderboardRepo.incrementScore(userId, score);

        // If no row was updated, it means the user is new. Create the entry.
        if (rowsUpdated == 0) {
            try {
                LeaderboardEntry newEntry = new LeaderboardEntry(user, score);
                leaderboardRepo.save(newEntry);
            } catch (Exception e) {
                // Edge Case: Race condition on creation (Two threads tried to create user at once)
                // Since 'user_id' is UNIQUE in DB, one failed. 
                // We retry the increment for the one that failed.
                leaderboardRepo.incrementScore(userId, score);
            }
        }
    }

    public List<LeaderboardEntry> getTop10() {

        // For GET API performance analysis - hitting the DB directly everytime for top 10 players
        // if (true) { 
        //     return leaderboardRepo.findTop10ByOrderByTotalScoreDesc();
        // }

        if (!isWarmedUp) {
            return leaderboardRepo.findTop10ByOrderByTotalScoreDesc();
        }

        Set<ZSetOperations.TypedTuple<String>> top = 
            redisTemplate.opsForZSet().reverseRangeWithScores(LEADERBOARD_KEY, 0, 9);

        if (top == null) return Collections.emptyList();

        return top.stream().map(t -> {
            LeaderboardEntry entry = new LeaderboardEntry();
            entry.setTotalScore(t.getScore().longValue()); 
            
            // Mock user object for response
            User dummyUser = new User();
            dummyUser.setId(Long.valueOf(t.getValue()));
            entry.setUser(dummyUser);
            
            return entry; 
        }).collect(Collectors.toList());
    }

    public long getRank(Long userId) {
        Long rank = redisTemplate.opsForZSet().reverseRank(LEADERBOARD_KEY, String.valueOf(userId));

        if (rank != null) {
            return rank + 1; 
        }

        return leaderboardRepo.findByUserId(userId)
                .map(entry -> {
                    redisTemplate.opsForZSet().add(LEADERBOARD_KEY, String.valueOf(userId), entry.getTotalScore());
                    Long newRank = redisTemplate.opsForZSet().reverseRank(LEADERBOARD_KEY, String.valueOf(userId));
                    return (newRank != null ? newRank : -1L) + 1;
                })
                .orElse(-1L);
    }
}
