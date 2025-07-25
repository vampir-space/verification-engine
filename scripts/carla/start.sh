#!/bin/bash

xvfb-run -a -s "-screen 0 1400x900x24" ./CarlaUnreal.sh -opengl -carla-server -carla-host 0.0.0.0 -carla-port 2000
# ./CarlaUnreal.sh -nullrhi -carla-server -carla-host 0.0.0.0 -carla-port 2000

