//
// Created by Holger on 18.09.2017.
//

#include "TCPServer.h"

#include <iostream>
#include <iomanip>

# include <mutex>

# include <sys/socket.h>
# include <sys/select.h>
# include <netinet/in.h>
# include <unistd.h>

CTCPServer::CTCPServer(unsigned short port, const std::function<void(const std::vector<uint8_t>&)>& reader){
    cancelSocket = NotificationSocket::Create();
    buffer.reserve(packetSize);
    processing.reserve(packetSize);
    remaining.reserve(packetSize);
    serverThread = startServerAsync(port, reader);
}


std::thread CTCPServer::startServerAsync(unsigned short port, const std::function<void(const std::vector<uint8_t>&)>& reader) {
    return std::thread([=]() {
        startServer(port, reader);
    });
}

bool CTCPServer::startServer(unsigned short port, const std::function<void(const std::vector<uint8_t>&)>& reader) {

    std::cout << "CTCPServer::startServer - creating server socket on port " << port << std::endl;

    serverSocket = socket(AF_INET, SOCK_STREAM, 0);

    struct sockaddr_in servAddr;

    servAddr.sin_family = AF_INET;
    servAddr.sin_addr.s_addr = htonl(INADDR_ANY);
    servAddr.sin_port = htons(port);

    if(bind(serverSocket , (struct sockaddr*)&servAddr, sizeof(servAddr) ) < 0) {
        std::cerr << "CTCPServer::startServer - error creating tcp socket" << std::endl;
        return false;
    }

    std::cerr << "CTCPServer::startServer - bound to port " << port << std::endl;

    if (listen(serverSocket, 5) < 0) {
        std::cerr << "CTCPServer::startup - error listening socket " << std::endl;
        return false;
    }
    std::cerr << "CTCPServer::startServer - listening " << port << std::endl;

    fd_set readFds;
    fd_set readFds2;

    struct sockaddr_in cliAddr;

    struct timeval tv;

    int selectSocket = std::max<int>(serverSocket, cancelSocket) + 1;

    while(serverLoop) {

        tv.tv_sec = 1;
        tv.tv_usec = 0;

        FD_ZERO(&readFds);

        FD_SET(serverSocket, &readFds);
        if(cancelSocket != -1) {
            FD_SET(cancelSocket, &readFds);
        }

        if(int activity = select(selectSocket, &readFds, nullptr, nullptr, &tv)) {

            if (FD_ISSET(cancelSocket, &readFds)) {
                std::cerr << "CTCPServer::startServer - read set cancelSocket!" << std::endl;
                serverLoop = false;
                break;
            }

            socklen_t cliLen = sizeof(cliAddr);
            int sessionSocket = accept(serverSocket, (struct sockaddr *) &cliAddr, &cliLen);
            if (sessionSocket < 0) {
                std::cout << "CTCPServer::startServer - error accepting socket" << std::endl;
                continue;
            }
            std::cerr << "CTCPServer::startServer - client connected!" << std::endl;

            int selectSocket2 = std::max<int>(sessionSocket, cancelSocket) + 1;

            while(serverLoop) {

                tv.tv_sec = 5;
                tv.tv_usec = 0;

                FD_ZERO(&readFds2);

                FD_SET(sessionSocket, &readFds2);
                if(cancelSocket != -1) {
                    FD_SET(cancelSocket, &readFds2);
                }

                if(int activity = select(selectSocket2, &readFds2, nullptr, nullptr, &tv)) {
                    std::cerr << "CTCPServer::startServer - select!" << std::endl;

                    if (FD_ISSET(cancelSocket, &readFds2)) {
                        std::cerr << "CTCPServer::startServer - read set cancelSocket!" << std::endl;
                        serverLoop = false;
                        break;
                    }

                    if (FD_ISSET(sessionSocket, &readFds2)) {
                        std::cerr << "CTCPServer::startServer - read set serverSocket!" << std::endl;

                        if(!readPacket(sessionSocket, reader))
                        {
                            close(sessionSocket);
                            break;
                        }
                    }
                }
            }
        }
    }

    close(serverSocket);

    std::cout << "CTCPServer::startServer - complete!" << std::endl;

    return true;

}

void CTCPServer::Close() {

    serverLoop = false;
    NotificationSocket::Notify(cancelSocket);

    serverThread.join();
}

bool CTCPServer::readPacket(int serverSocket, const std::function<void(const std::vector<uint8_t> &)> &reader) {

    buffer.clear();
    buffer.resize(packetSize);

    std::cout << "CTCPServer::readPacket - start!" << std::endl;
    int selectSocket = std::max<int>(serverSocket, cancelSocket) + 1;

    if (int read = recv(serverSocket, (char *) &buffer[0], packetSize, 0)) {
        std::cout << "CTCPServer::readPacket - read:" << read << std::endl;
        buffer.resize(std::min(read, packetSize));
        processing.assign(std::begin(remaining), std::end(remaining));
        processing.assign(std::begin(buffer), std::end(buffer));
        int crop = processing.size() % sizeof(int) * 2;
        remaining.clear();
        if (crop) {
            std::cout << "CTCPServer::readPacket - crop: " << crop << std::endl;
            auto it = processing.end();
            std::advance(it, -crop);
            remaining.assign(it, std::end(processing));
            processing.erase(it, std::end(processing));
        }
        reader(processing);
        return true;
    }
    else
    {
        return false;
    }

}
