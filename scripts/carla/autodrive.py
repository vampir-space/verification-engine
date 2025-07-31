#!/usr/bin/env python3
import carla
import random
import time

def main():
    try:
        client = carla.Client('localhost', 2000)
        client.set_timeout(60.0)
        world = client.get_world()
        print(f"Map: {world.get_map().name}")
        
        blueprint_library = world.get_blueprint_library()
        print("Spawning test vehicle...")
        vehicle_bp = blueprint_library.filter('vehicle.*')[0]
        spawn_points = world.get_map().get_spawn_points()
        spawn_point = random.choice(spawn_points)
        
        vehicle = world.spawn_actor(vehicle_bp, spawn_point)
        print(f"Test vehicle {vehicle.type_id} spawned.")
        
        vehicle.set_autopilot(True)
        print("Auto-driving started.")
        
        # Drive forever
        while True:
            time.sleep(1)
            location = vehicle.get_location()
            print(f"Location (x,y,z): ({location.x:.1f}, {location.y:.1f}, {location.z:.1f})", end='\r')
            
    except KeyboardInterrupt:
        print("Stopping...")
        vehicle.destroy()
        print("Vehicle destroyed.")
    except Exception as e:
        print(f"Error: {e}")

if __name__ == "__main__":
    main()
