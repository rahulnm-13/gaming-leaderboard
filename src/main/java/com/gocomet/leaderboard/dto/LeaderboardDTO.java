package com.gocomet.leaderboard.dto;

public class LeaderboardDTO {
    private Long userId;
    private Long score;
    private Long rank;

    // Default Constructor
    public LeaderboardDTO() {}

    // All-Args Constructor
    public LeaderboardDTO(Long userId, Long score, Long rank) {
        this.userId = userId;
        this.score = score;
        this.rank = rank;
    }

    // Getters and Setters
    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }

    public Long getScore() { return score; }
    public void setScore(Long score) { this.score = score; }

    public Long getRank() { return rank; }
    public void setRank(Long rank) { this.rank = rank; }
}
