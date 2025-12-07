package com.gocomet.leaderboard.controller;

import com.gocomet.leaderboard.model.LeaderboardEntry;
import com.gocomet.leaderboard.service.LeaderboardService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.HexFormat; // Java 17+ feature

@RestController
@RequestMapping("/api/leaderboard")
public class LeaderboardController {

    @Autowired
    private LeaderboardService leaderboardService;

    // The Secret Key (In production, we will load this from env variables)
    private static final String SECRET_KEY = "GOCOMET_GAME_SECRET";

    // 1. Submit Score
    @PostMapping("/submit")
    public ResponseEntity<?> submitScore(
            @RequestBody Map<String, Object> payload,
            @RequestHeader(value = "X-HMAC-Signature", required = false) String clientSignature
    ) {
        Long userId = ((Number) payload.get("user_id")).longValue();
        int score = ((Number) payload.get("score")).intValue();

        // SECURITY CHECK: Verify the signature
        if (clientSignature == null || !isValidSignature(userId, score, clientSignature)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(Map.of("error", "Security Violation: Invalid or missing signature. Tampering detected."));
        }

        leaderboardService.submitScore(userId, score);
        return ResponseEntity.ok(Map.of("message", "Score submitted successfully"));
    }

    // Helper to Verify HMAC
    private boolean isValidSignature(Long userId, int score, String clientSignature) {
        try {
            // Data to sign: "userID:score" (e.g., "12345:500")
            String data = userId + ":" + score;
            
            Mac sha256_HMAC = Mac.getInstance("HmacSHA256");
            SecretKeySpec secret_key = new SecretKeySpec(SECRET_KEY.getBytes(StandardCharsets.UTF_8), "HmacSHA256");
            sha256_HMAC.init(secret_key);

            byte[] rawHmac = sha256_HMAC.doFinal(data.getBytes(StandardCharsets.UTF_8));
            
            // Convert to Hex String to compare with client header
            String serverSignature = HexFormat.of().formatHex(rawHmac);
            
            return serverSignature.equals(clientSignature);
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    // 2. Get Top 10 Players (No change)
    @GetMapping("/top")
    public ResponseEntity<List<LeaderboardEntry>> getTopPlayers() {
        return ResponseEntity.ok(leaderboardService.getTop10());
    }

    // 3. Get Player Rank (No change)
    @GetMapping("/rank/{userId}")
    public ResponseEntity<?> getRank(@PathVariable Long userId) {
        long rank = leaderboardService.getRank(userId);
        return ResponseEntity.ok(Map.of("user_id", userId, "rank", rank));
    }
}


// SELECT COUNT(*) from leaderboard where score > target_score

// inputs -> names
// ranks vs. names

// "har" -> "harsh", "harshit", "harshita", .... (Trie data structure)

// Node