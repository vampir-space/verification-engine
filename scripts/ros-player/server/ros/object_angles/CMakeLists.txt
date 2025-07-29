cmake_minimum_required(VERSION 3.5)
project(object_angles)

find_package(ament_cmake REQUIRED)
find_package(std_msgs REQUIRED)
find_package(rosidl_default_generators REQUIRED)
find_package(rosidl_typesupport_fastrtps_cpp REQUIRED)

rosidl_generate_interfaces(${PROJECT_NAME}
  "msg/ObjectAngles.msg"
  DEPENDENCIES std_msgs
)

ament_export_dependencies(rosidl_default_runtime)
ament_package()
