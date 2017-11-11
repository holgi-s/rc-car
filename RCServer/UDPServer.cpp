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

CUDPServer::CUDPServer(unsigned short port, const std::function<void(const std::vector<uint8_t>&)>& reader){
    cancelSocket = NotificationSocket::Create();
    buffer.reserve(packetSize);
    serverThread = startServerAsync(port, reader);
}


std::thread CUDPServer::startServerAsync(unsigned short port, const std::function<void(const std::vector<uint8_t>&)>& reader) {
    return std::thread([=]() {
        startServer(port, reader);
    });
}

bool CUDPServer::startServer(unsigned short port, const std::function<void(const std::vector<uint8_t>&)>& reader) {

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
    tv.tv_sec = 5;
    tv.tv_usec = 0;


    int selectSocket = std::max<int>(serverSocket, cancelSocket) + 1;

    while(serverLoop) {

        FD_ZERO(&readFds);

        FD_SET(serverSocket, &readFds);
        if(cancelSocket != -1) {
            FD_SET(cancelSocket, &readFds);
        }

        tv.tv_sec = 5;
        tv.tv_usec = 0;

        if(int activity = select(selectSocket, &readFds, nullptr, nullptr, &tv)) {

            if (FD_ISSET(cancelSocket, &readFds)) {
                std::cerr << "CUDPServer::startServer - read set cancelSocket!" << std::endl;
                break;
            }

            if (FD_ISSET(serverSocket, &readFds)) {
                std::cerr << "CUDPServer::startServer - read set serverSocket!" << std::endl;

                readPacket(serverSocket, reader);
            }
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

void CUDPServer::readPacket(int serverSocket, const std::function<void(const std::vector<uint8_t> &)> &reader) {

    struct sockaddr_in cliAddr;
    socklen_t cliLen = sizeof(cliAddr);

    buffer.clear();
    buffer.resize(packetSize);

    int selectSocket = std::max<int>(serverSocket, cancelSocket) + 1;

    if (int read = recvfrom(serverSocket, (char *) &buffer[0], packetSize, 0, (struct sockaddr *) &cliAddr, &cliLen)) {

        buffer.resize(read);
        reader(buffer);
    }
}
