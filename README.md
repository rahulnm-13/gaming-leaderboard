# High-Frequency Gaming Leaderboard

A low-latency, real-time leaderboard system handling 1M+ users and 5M+ records using Java Spring Boot, Redis, and PostgreSQL.

## Tech Stack
* **Backend:** Java 17, Spring Boot 3
* **Database:** PostgreSQL (Storage), Redis (Caching/Ranking)
* **DevOps:** Docker Compose, New Relic APM
* **Frontend:** HTML/JS (Live Dashboard)

## How to Run
1.  **Clone the repo:**
    ```bash
    git clone [https://github.com/your-username/gaming-leaderboard.git](https://github.com/your-username/gaming-leaderboard.git)
    cd gaming-leaderboard
    ```

2.  **Start Infrastructure (DB & Redis):**
    ```bash
    docker-compose up -d
    ```

3.  **Run the Application:**
    ```bash
    ./mvnw spring-boot:run
    ```

4.  **Access the Dashboard:**
    Open `http://localhost:8000` in your browser.

5.  **Run Load Test:**
    ```bash
    python3 load_test.py
    ```
