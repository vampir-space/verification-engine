#!/usr/bin/env python3
"""
Simple script to connect to CARLA running in Docker container
"""

import carla
import random
import time

def main():
    try:
        # Connect to CARLA Docker container
        print("Connecting to CARLA Docker container...")
        client = carla.Client('carla', 2000)
        client.set_timeout(10.0)
        
        # Test connection
        version = client.get_server_version()
        print(f"✅ Connected to CARLA version: {version}")
        
        # Get world
        world = client.get_world()
        print(f"📍 Current map: {world.get_map().name}")
        
        # Get blueprint library
        blueprint_library = world.get_blueprint_library()
        
        # Try to spawn a vehicle
        print("\n🚗 Spawning a test vehicle...")
        vehicle_bp = blueprint_library.filter('vehicle.*')[0]
        spawn_points = world.get_map().get_spawn_points()
        
        if spawn_points:
            spawn_point = random.choice(spawn_points)
            vehicle = world.spawn_actor(vehicle_bp, spawn_point)
            print(f"✅ Spawned {vehicle.type_id} at {spawn_point.location}")
            
            # Wait a moment
            time.sleep(2)
            
            # Enable autopilot
            vehicle.set_autopilot(True)
            print("🤖 Autopilot enabled")
            
            # Let it drive for a few seconds
            print("🏎️  Vehicle driving for 5 seconds...")
            time.sleep(5)
            
            # Get vehicle location
            location = vehicle.get_location()
            print(f"📍 Vehicle location: x={location.x:.2f}, y={location.y:.2f}, z={location.z:.2f}")
            
            # Clean up
            vehicle.destroy()
            print("🗑️  Vehicle destroyed")
            
        else:
            print("❌ No spawn points available")
        
        print("\n🎉 Test completed successfully!")
        
    except Exception as e:
        print(f"❌ Error: {e}")
        print("\nTroubleshooting:")
        print("- Make sure Docker container is running: docker ps")
        print("- Check container logs: docker logs carla-carla-simulator-1")

if __name__ == "__main__":
    main()
