import asyncio
import json
import rclpy
from rclpy.node import Node
from rosidl_runtime_py import message_to_ordereddict
from message_filters import Subscriber, ApproximateTimeSynchronizer
from std_msgs.msg import String
from nav_msgs.msg import Odometry
from vision_msgs.msg import Detection3DArray
from object_angles.msg import ObjectAngles
import websockets

class SyncNode(Node):
    def __init__(self, loop, websocket_port=9091):
        super().__init__('ros2_sync_node')

        self.sync = None
        self.subscribers = []
        self.topics = []
        self.topic_types = {
            '/ground_truth/odometry': Odometry,
            '/detections/pointpillars': Detection3DArray,
            '/detections/yolo': ObjectAngles,
        }

        self.loop = loop
        self.websocket_port = websocket_port
        self.websocket_clients = set()

    async def websocket_server(self):
        async def handler(ws):
            self.websocket_clients.add(ws)
            self.get_logger().info(f"Client connected: {ws.remote_address}")
            try:
                async for message in ws:
                    try:
                        config = json.loads(message)
                        topics = config.get('topics', [])
                        slop = float(config.get('slop', 0.05))
                        self.setup_synchronizer(topics, slop)
                    except Exception as e:
                        self.get_logger().error(f"Failed to parse config: {e}")
            except Exception as e:
                self.get_logger().warn(f"Client disconnected: {e}")
            finally:
                self.websocket_clients.remove(ws)

        server = await websockets.serve(handler, "0.0.0.0", self.websocket_port)
        self.get_logger().info(f"WebSocket server running on port {self.websocket_port}")
        await server.wait_closed()

    def setup_synchronizer(self, topics, slop):
        if self.sync:
            self.get_logger().info("Resetting synchronizer...")
            self.sync = None

        self.subscribers = []
        self.topics = topics
        for topic in topics:
            msg_type = self.topic_types.get(topic)
            if not msg_type:
                self.get_logger().warn(f"Unknown topic type for {topic}")
                continue
            self.subscribers.append(Subscriber(self, msg_type, topic))

        if len(self.subscribers) < 2:
            self.get_logger().warn("Need at least 2 topics to synchronize.")
            return

        self.sync = ApproximateTimeSynchronizer(self.subscribers, queue_size=20, slop=slop)
        self.sync.registerCallback(self.synced_callback)
        self.get_logger().info(f"Synchronizing topics: {topics} with slop={slop}s")

    def synced_callback(self, *msgs):
        stamp = msgs[0].header.stamp
        timestamp_sec = stamp.sec + stamp.nanosec * 1e-9
        sync_info = {
            "op": "synced_publish",
            "timestamp": timestamp_sec,
            "data": {self.topics[index]: message_to_ordereddict(msg) for index, msg in enumerate(msgs)}
        }
        message = json.dumps(sync_info)
        for ws in self.websocket_clients:
            asyncio.run_coroutine_threadsafe(ws.send(message), self.loop)


async def main_async():
    loop = asyncio.get_running_loop()
    rclpy.init()
    node = SyncNode(loop, websocket_port=9091)
    from threading import Thread
    def ros_spin():
        rclpy.spin(node)
    t = Thread(target=ros_spin, daemon=True)
    t.start()
    await node.websocket_server()

if __name__ == '__main__':
    asyncio.run(main_async())
