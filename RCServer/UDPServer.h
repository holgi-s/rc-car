//
// Created by Holger on 18.09.2017.
//

#ifndef RCSERVER_NETSERVER_H
#define RCSERVER_NETSERVER_H

#include "NotificationSocket.h"

#include <thread>
#include <vector>

class CUDPServer {

public:
    CUDPServer(unsigned short port,
               const std::function<void(const std::vector<uint8_t>&)>& onDataPacket,
               const std::function<void(void)>& onIdle = nullptr);

    void Close();

private:
    bool startServer(unsigned short port,
                     const std::function<void(const std::vector<uint8_t>&)>& onDataPacket,
                     const std::function<void(void)>& onIdle = nullptr);

    std::thread startServerAsync(unsigned short port,
                                 const std::function<void(const std::vector<uint8_t>&)>& onDataPacket,
                                 const std::function<void(void)>& onIdle = nullptr);

    void readPacket(int serverSocket, const std::function<void(const std::vector<uint8_t> &)> &onDataPacket);

    int serverSocket = 0;
    int cancelSocket = 0;

    bool serverLoop = true;

    const int packetSize = 512;
    std::vector<uint8_t> buffer;

    std::thread serverThread;
};


#endif //RCSERVER_NETSERVER_H
