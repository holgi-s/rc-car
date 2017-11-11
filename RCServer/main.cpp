#include <iostream>
#include "UDPServer.h"
#include "TCPServer.h"
#include <pigpiod_if2.h>
#include <arpa/inet.h>

#include "Data.h"

int startup() {

    int fd = pigpio_start(nullptr, nullptr);

    set_mode(fd, 5, PI_OUTPUT);     //motor control left
    set_mode(fd, 6, PI_OUTPUT);
    set_mode(fd, 19, PI_OUTPUT);    //motor control right
    set_mode(fd, 26, PI_OUTPUT);
    set_mode(fd, 20, PI_OUTPUT);    //motor pwm left
    set_mode(fd, 21, PI_OUTPUT);    //motor pwm right

    set_mode(fd, 27, PI_OUTPUT);    //LED left
    set_mode(fd, 22, PI_OUTPUT);    //LED right

    set_mode(fd, 10, PI_OUTPUT);    //LED left
    set_mode(fd, 9, PI_OUTPUT);     //LED right


    set_PWM_dutycycle(fd, 20, 0);   //stop pwm
    set_PWM_dutycycle(fd, 21, 0);   //stop pwm

    set_PWM_frequency(fd, 20, 100); //setup pwm
    set_PWM_frequency(fd, 21, 100);
    set_PWM_range(fd, 20, 100);
    set_PWM_range(fd, 21, 100);

    return fd;
}

void shutdown(int fd) {
    set_PWM_dutycycle(fd, 20, 0); //stop pwm
    set_PWM_dutycycle(fd, 21, 0);
    pigpio_stop(fd);
}

void control(int fd, RCData* rcData) {

    //BREAK
    if (rcData->stop & 0x01) {
        set_PWM_dutycycle(fd, 20, 0);
        set_PWM_dutycycle(fd, 21, 0);
        gpio_write(fd, 5, 1);
        gpio_write(fd, 6, 1);
        gpio_write(fd, 19, 1);
        gpio_write(fd, 26, 1);
    } else {

        //Motor
        int8_t left = rcData->left;
        int8_t right = rcData->right;

        if (left > 0) {
            gpio_write(fd, 5, 1);
            gpio_write(fd, 6, 0);
        } else if (left < 0) {
            gpio_write(fd, 5, 0);
            gpio_write(fd, 6, 1);
        } else {
            gpio_write(fd, 5, 0);
            gpio_write(fd, 6, 0);
        }

        if (right > 0) {
            gpio_write(fd, 19, 1);
            gpio_write(fd, 26, 0);
        } else if (right < 0) {
            gpio_write(fd, 19, 0);
            gpio_write(fd, 26, 1);
        } else {
            gpio_write(fd, 19, 0);
            gpio_write(fd, 26, 0);
        }

        left = abs(left);
        right = abs(right);

        left = std::max(20, std::min((int)left, 100));
        right = std::max(20, std::min((int)right, 100));

        set_PWM_dutycycle(fd, 20, left);
        set_PWM_dutycycle(fd, 21, right);
    }

    //LED Right
    if(rcData->led & 0x01){
        gpio_write(fd, 27, 1);
        gpio_write(fd, 10, 1);

    } else{
        gpio_write(fd, 27, 0);
        gpio_write(fd, 10, 0);
    }

    //LED Left
    if(rcData->led & 0x02){
        gpio_write(fd, 22, 1);
        gpio_write(fd, 9, 1);
    } else{
        gpio_write(fd, 22, 0);
        gpio_write(fd, 9, 0);
    }

}

void readPacket(int fd, const std::vector<uint8_t>& packet){
    const size_t packetSize = sizeof(RCData);
    size_t offset = 0;
    while(packet.size() >= offset + packetSize){

        RCData* rcData = (RCData*)&packet[0];
        std::cout << "Left: " << (int)(rcData->left) << "; Right: " << (int)(rcData->right) << std::endl;
        std::cout << "LED: " << (int)(rcData->led) << "; Extra: " << (int)(rcData->stop) << std::endl;

        control(fd, rcData);
        offset += packetSize;
    }
}

int main() {

    int fd = startup();
    unsigned short port = 9090;
    std::cout << "Hello, Server on port " << port << std::endl;

    CUDPServer udp(port, [fd](const std::vector<uint8_t>& packet) {
        std::cout << "UDP Packet: ";
        readPacket(fd, packet);
    });

    CTCPServer tcp(port, [fd](const std::vector<uint8_t>& packet) {
        std::cout << "TCP Packet: ";
        readPacket(fd, packet);
    });

    std::cout << "Type x [Enter] to exit" << std::endl;

    while(true) {
        auto ch = getchar();
        if (ch == 'x') {
            break;
        }
    }

    std::cout << "Closing Server ..." << std::endl;

    udp.Close();
    tcp.Close();

    shutdown(fd);

    std::cout << "Bye." << std::endl;

    return 0;
}