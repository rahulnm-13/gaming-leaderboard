import requests
import random
import time
import sys
import threading
import hmac
import hashlib
from concurrent.futures import ThreadPoolExecutor

# CONFIGURATION
API_BASE_URL = "http://localhost:8000/api/leaderboard"
TARGET_RPM = 10000
# Since our loop does 3 requests (Submit, Top, Rank), 
# we need about 56 iterations per second to hit 167 reqs/sec (10k RPM).
THREAD_COUNT = 50  # Adjust this: More threads = Higher RPM
SECRET_KEY = "GOCOMET_GAME_SECRET" # Matching with Java backend

# Global counters for monitoring
request_count = 0
start_time = time.time()
lock = threading.Lock()

def generate_signature(user_id, score):
    """
    Creates an HMAC-SHA256 signature to prove this request is authentic.
    """
    message = f"{user_id}:{score}"
    signature = hmac.new(
        SECRET_KEY.encode('utf-8'), 
        message.encode('utf-8'), 
        hashlib.sha256
    ).hexdigest()
    return signature

def simulate_user_session():
    """
    Each thread runs this function. 
    It creates its OWN Session (for connection reuse) and loops indefinitely.
    """
    # Create a persistent session for this thread (Performance boost)
    session = requests.Session()
    
    while True:
        # Focusing on users 1 to 20 so they fight for the top spots
        # For better visibility in the front-end dashboard
        user_id = random.randint(1, 20)
        
        try:
            # 1. Submit Score
            score = random.randint(100, 10000)
            
            # Generate the signature
            signature = generate_signature(user_id, score)

            resp1 = session.post(
                f"{API_BASE_URL}/submit", 
                json={"user_id": user_id, "score": score},
                headers={"X-HMAC-Signature": signature},
                timeout=5
            )
            increment_counter()

            # 2. Get Top Players
            resp2 = session.get(f"{API_BASE_URL}/top", timeout=5)
            increment_counter()

            # 3. Get User Rank
            resp3 = session.get(f"{API_BASE_URL}/rank/{user_id}", timeout=5)
            increment_counter()

        except Exception as e:
            # Silently ignoring errors to keep speed up
            pass
            
        # Tiny sleep to prevent 100% CPU usage if our server responds too fast
        time.sleep(0.01) 

def increment_counter():
    global request_count
    with lock:
        request_count += 1

def monitor_throughput():
    """
    Runs in a separate thread to print stats every second
    instead of printing every request.
    """
    global request_count, start_time
    while True:
        time.sleep(1)
        current_time = time.time()
        elapsed = current_time - start_time
        
        # Calculate current RPM
        if elapsed > 0:
            rpm = (request_count / elapsed) * 60
            print(f"Current Throughput: {rpm:.2f} RPM | Total Requests: {request_count}")
            
        # Reset counters every 10 seconds to keep the average "fresh"
        if elapsed > 10:
            with lock:
                request_count = 0
                start_time = time.time()

if __name__ == "__main__":
    print(f"Starting High-Load Test with {THREAD_COUNT} threads...")
    print("Press Ctrl+C to stop.")

    # 1. Start the monitor thread (for logs)
    monitor = threading.Thread(target=monitor_throughput, daemon=True)
    monitor.start()

    # 2. Start the worker threads (for load)
    with ThreadPoolExecutor(max_workers=THREAD_COUNT) as executor:
        # Launch 50 parallel "users"
        for _ in range(THREAD_COUNT):
            executor.submit(simulate_user_session)
