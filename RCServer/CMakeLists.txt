cmake_minimum_required(VERSION 3.6)
project(RCServer)

set(CMAKE_CXX_STANDARD 11)

set(SOURCE_FILES main.cpp UDPServer.cpp TCPServer.cpp NotificationSocket.cpp)
add_executable(RCServer ${SOURCE_FILES})
target_link_libraries(RCServer pigpiod_if2 pthread)