FROM nvidia/cuda:12.9.1-devel-ubuntu22.04

RUN apt-get update && apt-get install -y \
    python3 \
    python3-pip \
    xvfb \
    libgl1-mesa-glx \
    libomp5 \
    sudo \
    iproute2 \
    net-tools \
    && rm -rf /var/lib/apt/lists/*

RUN useradd -m -s /bin/bash carla && \
    echo "carla ALL=(ALL) NOPASSWD:ALL" >> /etc/sudoers

COPY Carla-0.10.0-Linux-Shipping.tar.gz /tmp/
RUN tar -xzf /tmp/Carla-0.10.0-Linux-Shipping.tar.gz -C /opt && \
    rm /tmp/Carla-0.10.0-Linux-Shipping.tar.gz && \
    mv /opt/CARLA_0.10.0 /opt/carla 2>/dev/null || mv /opt/Carla-0.10.0-Linux-Shipping /opt/carla && \
    chown -R carla:carla /opt/carla

USER carla
WORKDIR /opt/carla
EXPOSE 2000 2001 2002

