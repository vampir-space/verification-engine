FROM osrf/ros:kilted-desktop-full
RUN apt update

# ROS setup
RUN apt install -y ros-kilted-rosbridge-suite
RUN echo "source /opt/ros/kilted/setup.bash" >> "/opt/ros/setup.bash"
COPY ./ros /opt/ros/custom/src
RUN /bin/bash -c "source /opt/ros/setup.bash && \
  colcon build --install-base /opt/ros/custom --merge-install --base-paths /opt/ros/custom/src"
RUN echo "source /opt/ros/custom/setup.bash" >> "/opt/ros/setup.bash"
RUN echo 'export AMENT_PREFIX_PATH="/opt/ros/custom:$AMENT_PREFIX_PATH"' >> "/opt/ros/setup.bash"
RUN echo "source /opt/ros/setup.bash" >> ~/.bashrc
EXPOSE 9090

# Syncronizer setup
RUN apt install -y python3-pip
RUN pip3 install websockets --break-system-packages
COPY ./sync /opt/sync
EXPOSE 9091

# Web setup
RUN apt install -y python3-flask python3-watchdog
COPY ./web /opt/web
EXPOSE 4000

COPY entrypoint.sh /entrypoint.sh
RUN chmod +x /entrypoint.sh
ENTRYPOINT ["/entrypoint.sh"]
