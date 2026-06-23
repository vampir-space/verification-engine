#!/usr/bin/env python3

import asyncio
import json
import threading

import rclpy
from rclpy.node import Node

from sensor_msgs.msg import NavSatFix
from sensor_msgs.msg import Imu

from object_angles.msg import ObjectAngles

import websockets


class TopicRelay(Node):

    def __init__(self):
        super().__init__("topic_relay")

        self.ws_clients = set()

        self.loop = asyncio.new_event_loop()
        threading.Thread(
            target=self.websocket_thread,
            daemon=True
        ).start()

        self.create_subscription(
            NavSatFix,
            "/ground_truth/gps",
            self.gps_cb,
            10
        )

        self.create_subscription(
            NavSatFix,
            "/ground_truth/gps_ublox",
            self.gps_cbu,
            10
        )

        self.create_subscription(
            Imu,
            "/ground_truth/imu",
            self.imu_cb,
            10
        )

        self.create_subscription(
            ObjectAngles,
            "/detections/yolo",
            self.detections_cb,
            10
        )

    def websocket_thread(self):
        asyncio.set_event_loop(self.loop)

        async def handler(ws):
            self.ws_clients.add(ws)
            try:
                await ws.wait_closed()
            finally:
                self.ws_clients.discard(ws)

        async def server():
            async with websockets.serve(
                handler,
                "0.0.0.0",
                9090,ping_interval=None,   # Disable automatic pings from the server side
                ping_timeout=None    # Disable the timeout clock
            ):
                await asyncio.Future()

        self.loop.run_until_complete(server())

    async def broadcast(self, payload):
        if not self.ws_clients:
            return

        msg = json.dumps(payload)

        await asyncio.gather(
            *[c.send(msg) for c in self.ws_clients],
            return_exceptions=True
        )

    def send(self, payload):
        asyncio.run_coroutine_threadsafe(
            self.broadcast(payload),
            self.loop
        )

    def gps_cb(self, msg):

        self.send({
            "op": "publish",
            "topic": "/ground_truth/gps",
            "msg": {
                "header": {
                    "stamp": {
                        "sec": msg.header.stamp.sec,
                        "nanosec": msg.header.stamp.nanosec
                    },
                    "frame_id": msg.header.frame_id
                },
                "status": {
                    "status": msg.status.status,
                    "service": msg.status.service
                },
                "latitude": msg.latitude,
                "longitude": msg.longitude,
                "altitude": msg.altitude,
                "position_covariance": list(msg.position_covariance),
                "position_covariance_type": msg.position_covariance_type
            }
        })

    def gps_cbu(self, msg):

        self.send({
            "op": "publish",
            "topic": "/ground_truth/gps_ublox",
            "msg": {
                "header": {
                    "stamp": {
                        "sec": msg.header.stamp.sec,
                        "nanosec": msg.header.stamp.nanosec
                    },
                    "frame_id": msg.header.frame_id
                },
                "status": {
                    "status": msg.status.status,
                    "service": msg.status.service
                },
                "latitude": msg.latitude,
                "longitude": msg.longitude,
                "altitude": msg.altitude,
                "position_covariance": list(msg.position_covariance),
                "position_covariance_type": msg.position_covariance_type
            }
        })

    def imu_cb(self, msg):

        self.send({
            "op": "publish",
            "topic": "/ground_truth/imu",
            "msg": {
                "header": {
                    "stamp": {
                        "sec": msg.header.stamp.sec,
                        "nanosec": msg.header.stamp.nanosec
                    },
                    "frame_id": msg.header.frame_id
                },
                "orientation": {
                    "x": msg.orientation.x,
                    "y": msg.orientation.y,
                    "z": msg.orientation.z,
                    "w": msg.orientation.w
                },
                "orientation_covariance": list(msg.orientation_covariance),
                "angular_velocity": {
                    "x": msg.angular_velocity.x,
                    "y": msg.angular_velocity.y,
                    "z": msg.angular_velocity.z
                },
                "angular_velocity_covariance": list(msg.angular_velocity_covariance),
                "linear_acceleration": {
                    "x": msg.linear_acceleration.x,
                    "y": msg.linear_acceleration.y,
                    "z": msg.linear_acceleration.z
                },
                "linear_acceleration_covariance": list(msg.linear_acceleration_covariance)
            }
        })

    def detections_cb(self, msg):

        self.send({
            "op": "publish",
            "topic": "/detections/yolo",
            "msg": {
                "header": {
                    "stamp": {
                        "sec": msg.header.stamp.sec,
                        "nanosec": msg.header.stamp.nanosec
                    },
                    "frame_id": msg.header.frame_id
                },
                "type": list(msg.type),
                "angle": list(msg.angle),
                "confidence": list(msg.confidence)
            }
        })


def main():
    rclpy.init()

    node = TopicRelay()

    try:
        rclpy.spin(node)
    finally:
        node.destroy_node()
        rclpy.shutdown()


if __name__ == "__main__":
    main()