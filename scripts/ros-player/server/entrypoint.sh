#!/usr/bin/env bash
set -e

export PYTHONUNBUFFERED=1

source /opt/ros/setup.bash
ros2 launch rosbridge_server rosbridge_websocket_launch.xml &
ROS_PID=$!

python3 /opt/sync/sync_node.py &
SYNC_PID=$!

python3 /opt/web/app.py &
WEB_PID=$!

wait $ROS_PID $SYNC_PID $WEB_PID
