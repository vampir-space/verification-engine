#!/bin/bash

# CARLA 0.10.0 Startup Script for Docker
export DISPLAY=:99
export XDG_RUNTIME_DIR=/tmp

echo "Starting virtual framebuffer..."

# Kill any existing Xvfb processes and clean up
pkill -f "Xvfb :99" 2>/dev/null || true
rm -f /tmp/.X99-lock 2>/dev/null || true

# Start Xvfb
Xvfb :99 -screen 0 1920x1080x24 &
XVFB_PID=$!

echo "Waiting for Xvfb to initialize..."
sleep 5

echo "Starting CARLA server..."
cd /opt/carla

# Check if CarlaUnreal.sh exists
if [ ! -f "./CarlaUnreal.sh" ]; then
    echo "Error: CarlaUnreal.sh not found in $(pwd)"
    ls -la
    exit 1
fi

echo "Launching CARLA with GPU acceleration..."
echo "Command: ./CarlaUnreal.sh -opengl -carla-server -benchmark -carla-host 0.0.0.0"

# Run CARLA and capture output
./CarlaUnreal.sh -opengl -carla-server -benchmark -carla-host 0.0.0.0 2>&1 | tee /tmp/carla.log &
CARLA_PID=$!

# Wait and show if CARLA is still running
sleep 10
if kill -0 $CARLA_PID 2>/dev/null; then
    echo "✅ CARLA process is running (PID: $CARLA_PID)"
    
    # Check if port 2000 is listening using ss (instead of netstat)
    for i in {1..30}; do
        if ss -tln | grep -q ":2000 "; then
            echo "✅ CARLA server is listening on port 2000"
            break
        fi
        echo "⏳ Waiting for CARLA to listen on port 2000... ($i/30)"
        sleep 2
    done
else
    echo "❌ CARLA process has stopped"
    echo "Last few lines of CARLA log:"
    tail -20 /tmp/carla.log
    exit 1
fi

# Keep the script running
wait $CARLA_PID
