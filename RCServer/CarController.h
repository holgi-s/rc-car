//
// Created by Holger on 18.11.2017.
//

#ifndef RCSERVER_CARCONTROLLER_H
#define RCSERVER_CARCONTROLLER_H

#include <cstdint>

class RCData;

#define LED_MASK_LEFT           0x01
#define LED_MASK_RIGHT          0x02

#define GPIO_MOTOR_PWM_LEFT     20
#define GPIO_MOTOR_CTRL1_LEFT   5
#define GPIO_MOTOR_CTRL2_LEFT   6
#define GPIO_MOTOR_PWM_RIGHT    21
#define GPIO_MOTOR_CTRL1_RIGHT  19
#define GPIO_MOTOR_CTRL2_RIGHT  26
#define GPIO_LED_TOP_LEFT       27
#define GPIO_LED_BOTTOM_LEFT    10
#define GPIO_LED_TOP_RIGHT      22
#define GPIO_LED_BOTTOM_RIGHT   9

class CarController {

public:
    CarController();
    ~CarController();

    void Control(RCData* rcData);
    void Led(uint8_t ledMask);
    void Motor(int8_t left, int8_t right);

    void Stop();

private:
    void setup();
    void shutdown();


private :
    int _fd = -1;
};


#endif //RCSERVER_CARCONTROLLER_H
