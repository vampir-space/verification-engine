#!/usr/bin/env python3
"""
Simple script to auto-drive a car non-stop in CARLA
"""
import carla
import random
import time

def main():
    try:
        print("⏳ Waiting for CARLA to start...")
        while True:
            try:
                client = carla.Client('carla', 2000)
                client.set_timeout(10.0)
                version = client.get_server_version()
                print(f"✅ Connected to CARLA version: {version}")
                break
            except Exception as e:
                print(f"⏳ Connection failed, {e}, retrying in 10 seconds...")
                time.sleep(10)

        world = client.get_world()
        print(f"Map: {world.get_map().name}")
        
        # Spawn a vehicle
        blueprint_library = world.get_blueprint_library()
        vehicle_bp = blueprint_library.filter('vehicle.*')[0]
        spawn_points = world.get_map().get_spawn_points()
        spawn_point = random.choice(spawn_points)
        
        vehicle = world.spawn_actor(vehicle_bp, spawn_point)
        print(f"🚗 Spawned {vehicle.type_id}")
        
        # Enable autopilot
        vehicle.set_autopilot(True)
        print("🤖 Auto-driving started! Press Ctrl+C to stop.")
        
        # Drive forever
        while True:
            time.sleep(1)
            location = vehicle.get_location()
            print(f"📍 x={location.x:.1f}, y={location.y:.1f}, z={location.z:.1f}")
            
    except KeyboardInterrupt:
        print("\n🛑 Stopping...")
        vehicle.destroy()
        print("✅ Vehicle destroyed")
    except Exception as e:
        print(f"❌ Error: {e}")

if __name__ == "__main__":
    main()