//
// Created by Holger on 18.09.2017.
//

#include "UDPServer.h"

#include <iostream>
#include <iomanip>

# include <mutex>

# include <sys/socket.h>
# include <sys/select.h>
# include <netinet/in.h>
# include <unistd.h>

#define UDP_DATA_TIMEOUT_SEC 0
#define UDP_DATA_TIMEOUT_USEC 500000


CUDPServer::CUDPServer(unsigned short port,
                       const std::function<void(const std::vector<uint8_t>&)>& onDataPacket,
                       const std::function<void(void)>& onIdle) {
    cancelSocket = NotificationSocket::Create();
    buffer.reserve(packetSize);
    serverThread = startServerAsync(port, onDataPacket, onIdle);
}


std::thread CUDPServer::startServerAsync(unsigned short port,
                                         const std::function<void(const std::vector<uint8_t>&)>& onDataPacket,
                                         const std::function<void(void)>& onIdle) {
    return std::thread([=]() {
        startServer(port, onDataPacket, onIdle);
    });
}

bool CUDPServer::startServer(unsigned short port,
                             const std::function<void(const std::vector<uint8_t>&)>& onDataPacket,
                             const std::function<void(void)>& onIdle) {

    std::cout << "CUDPServer::startServer - creating server socket on port " << port << std::endl;

    serverSocket = socket(AF_INET, SOCK_DGRAM, IPPROTO_UDP);

    struct sockaddr_in servAddr;

    servAddr.sin_family = AF_INET;
    servAddr.sin_addr.s_addr = htonl(INADDR_ANY);
    servAddr.sin_port = htons(port);

    if(bind(serverSocket , (struct sockaddr*)&servAddr, sizeof(servAddr) ) < 0) {
        std::cerr << "CUDPServer::startServer - error creating udp socket" << std::endl;
        return false;
    }

    std::cerr << "CUDPServer::startServer - bound to port " << port << std::endl;

    fd_set readFds;

    struct timeval tv;
    tv.tv_sec = UDP_DATA_TIMEOUT_SEC;
    tv.tv_usec = UDP_DATA_TIMEOUT_USEC;


    int selectSocket = std::max<int>(serverSocket, cancelSocket) + 1;

    while(serverLoop) {

        FD_ZERO(&readFds);

        FD_SET(serverSocket, &readFds);
        if(cancelSocket != -1) {
            FD_SET(cancelSocket, &readFds);
        }

        tv.tv_sec = UDP_DATA_TIMEOUT_SEC;
        tv.tv_usec = UDP_DATA_TIMEOUT_USEC;

        int activity = select(selectSocket, &readFds, nullptr, nullptr, &tv);
        if(activity > 0) {
            if (FD_ISSET(cancelSocket, &readFds)) {
                std::cerr << "CUDPServer::startServer - read set cancelSocket!" << std::endl;
                break;
            } else if (FD_ISSET(serverSocket, &readFds)) {
                std::cout << "CUDPServer::startServer - read set serverSocket!" << std::endl;
                readPacket(serverSocket, onDataPacket);
            }
        } else if (activity == 0) {
            std::cout << "CUDPServer::startServer - timeout!" << std::endl;
            if(onIdle) {
                onIdle();
            }
        } else {
            std::cerr << "CUDPServer::startServer - error: " << errno << std::endl;
        }
    }

    close(serverSocket);

    std::cout << "CUDPServer::startServer - complete!" << std::endl;

    return true;

}

void CUDPServer::Close() {

    serverLoop = false;
    NotificationSocket::Notify(cancelSocket);

    serverThread.join();
}

void CUDPServer::readPacket(int serverSocket, const std::function<void(const std::vector<uint8_t> &)> &onDataPacket) {

    struct sockaddr_in cliAddr;
    socklen_t cliLen = sizeof(cliAddr);

    buffer.clear();
    buffer.resize(packetSize);

    int selectSocket = std::max<int>(serverSocket, cancelSocket) + 1;

    if (int read = recvfrom(serverSocket, (char *) &buffer[0], packetSize, 0, (struct sockaddr *) &cliAddr, &cliLen)) {

        buffer.resize(read);
        onDataPacket(buffer);
    }
}
