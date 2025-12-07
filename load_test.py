import requests
import random
import time
import sys
import hmac
import hashlib

# Ensure this matches your Spring Boot port (8000)
API_BASE_URL = "http://localhost:8000/api/leaderboard"
SECRET_KEY = "GOCOMET_GAME_SECRET" # MUST MATCH THE JAVA CONTROLLER

def generate_signature(user_id, score):
    """
    Creates an HMAC-SHA256 signature to prove this request is authentic.
    Matches the logic in LeaderboardController.java
    """
    message = f"{user_id}:{score}"
    signature = hmac.new(
        SECRET_KEY.encode('utf-8'), 
        message.encode('utf-8'), 
        hashlib.sha256
    ).hexdigest()
    return signature

def submit_score(user_id):
    score = random.randint(100, 1000000)
    
    # Generate the security signature
    signature = generate_signature(user_id, score)
    
    try:
        print(f"Submitting score for User {user_id}...", end='', flush=True)
        resp = requests.post(
            f"{API_BASE_URL}/submit", 
            json={"user_id": user_id, "score": score},
            headers={"X-HMAC-Signature": signature}, # Send signature in Header
            timeout=5 
        )
        if resp.status_code == 200:
            print("OK")
        else:
            # If 403, it means the signature failed
            print(f"(Status: {resp.status_code} - {resp.text})")
    except Exception as e:
        print(f"Error: {e}")

def get_top_players():
    try:
        print("Fetching Top 10...", end='', flush=True)
        resp = requests.get(f"{API_BASE_URL}/top", timeout=5)
        if resp.status_code == 200:
            print("Success")
            return resp.json()
        else:
            print(f"(Status: {resp.status_code})")
            return []
    except Exception as e:
        print(f"Error: {e}")
        return []

def get_user_rank(user_id):
    try:
        print(f"Checking rank for User {user_id}...", end='', flush=True)
        resp = requests.get(f"{API_BASE_URL}/rank/{user_id}", timeout=5)
        if resp.status_code == 200:
            print("Success")
            return resp.json()
        else:
            print(f"(Status: {resp.status_code})")
            return {}
    except Exception as e:
        print(f"Error: {e}")
        return {}

if __name__ == "__main__":
    # Check if requests is installed
    try:
        import requests
    except ImportError:
        print("Error: 'requests' library is missing. Run: pip3 install requests")
        sys.exit(1)

    print("Starting Secure Load Test...")
    while True:
        # Use a smaller range (1-20) if you want to see the Top 10 change live on Frontend
        # Use 1-1000000 for realistic random load
        user_id = random.randint(1, 20) 
        
        # 1. Submit (Now Secure)
        submit_score(user_id)
        
        # 2. Get Top Players
        top_players = get_top_players()
        if top_players:
            # FIX: Access the nested 'user' dictionary to get the 'id'
            top_user = top_players[0].get('user', {})
            user_id_display = top_user.get('id', 'Unknown')
            score_display = top_players[0].get('totalScore')
            
            print(f"Top Player: User {user_id_display} (Score: {score_display})")

        # 3. Get Rank
        rank_data = get_user_rank(user_id)
        if rank_data:
            print(f"User {user_id} is Rank #{rank_data.get('rank')}")

        print("-" * 30)
        time.sleep(random.uniform(0.5, 2))
