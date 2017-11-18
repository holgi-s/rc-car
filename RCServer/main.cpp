#include <iostream>
#include "UDPServer.h"
#include <pigpiod_if2.h>
#include <arpa/inet.h>

#include "Data.h"
#include "CarController.h"

void processPacket(CarController& car, const std::vector<uint8_t>& packet) {
    const size_t packetSize = sizeof(RCData);
    size_t offset = 0;

    while(packet.size() >= offset + packetSize) {

        RCData* rcData = (RCData*)&packet[0];
        std::cout << "Left: " << (int)(rcData->left) << "; Right: " << (int)(rcData->right) << std::endl;
        std::cout << "LED: " << (int)(rcData->led) << "; Extra: " << (int)(rcData->stop) << std::endl;

        car.Control(rcData);

        offset += packetSize;
    }
}
static bool toggle = false;

void processIdle(CarController& car) {
    car.Stop();
    car.Led(toggle ? LED_MASK_LEFT : LED_MASK_RIGHT);
}

int main() {

    CarController car;

    unsigned short port = 9090;

    std::cout << "Hello RC Car!" << std::endl;

    std::cout << "UDP Server on port " << port << std::endl;

    CUDPServer udp(port, [&car](const std::vector<uint8_t>& packet) {
        std::cout << "UDP Packet: ";
        processPacket(car, packet);
    });

    std::cout << "Type x [Enter] to exit" << std::endl;

    while(true) {
        auto ch = getchar();
        if (ch == 'x') {
            break;
        }
    }

    udp.Close();

    std::cout << "Closing Server ..." << std::endl;
    std::cout << "Bye." << std::endl;

    return 0;
}