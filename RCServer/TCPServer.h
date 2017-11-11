//
// Created by Holger on 18.09.2017.
//

#ifndef RCSERVER_TCPSERVER_H
#define RCSERVER_TCPSERVER_H

#include "NotificationSocket.h"

#include <thread>
#include <vector>

class CTCPServer {

public:
    CTCPServer(unsigned short port, const std::function<void(const std::vector<uint8_t>&)>& reader);
    void Close();

private:
    bool startServer(unsigned short port, const std::function<void(const std::vector<uint8_t>&)>& reader);
    std::thread startServerAsync(unsigned short port, const std::function<void(const std::vector<uint8_t>&)>& reader);

    bool readPacket(int serverSocket, const std::function<void(const std::vector<uint8_t> &)> &reader);

    int serverSocket = 0;
    int cancelSocket = 0;

    bool serverLoop = true;

    const int packetSize = 1024;
    std::vector<uint8_t> buffer;
    std::vector<uint8_t> processing;
    std::vector<uint8_t> remaining;

    std::thread serverThread;
};


#endif //RCSERVER_TCPSERVER_H
