//
// Created by Holger on 18.11.2017.
//

#include "CarController.h"
#include <pigpiod_if2.h>
#include "Data.h"
#include <algorithm>


CarController::CarController()
{
    _fd = pigpio_start(nullptr, nullptr);
    setup();

}
CarController::~CarController()
{
    shutdown();
    pigpio_stop(_fd);
}

void CarController::setup()
{
    set_mode(_fd, GPIO_MOTOR_PWM_LEFT, PI_OUTPUT);    //motor pwm left
    set_mode(_fd, GPIO_MOTOR_CTRL1_LEFT, PI_OUTPUT);     //motor control left
    set_mode(_fd, GPIO_MOTOR_CTRL2_LEFT, PI_OUTPUT);

    set_mode(_fd, GPIO_MOTOR_PWM_RIGHT, PI_OUTPUT);    //motor pwm right
    set_mode(_fd, GPIO_MOTOR_CTRL1_RIGHT, PI_OUTPUT);    //motor control right
    set_mode(_fd, GPIO_MOTOR_CTRL2_RIGHT, PI_OUTPUT);

    set_mode(_fd, GPIO_LED_TOP_LEFT, PI_OUTPUT);    //LED left
    set_mode(_fd, GPIO_LED_TOP_RIGHT, PI_OUTPUT);    //LED right

    set_mode(_fd, GPIO_LED_BOTTOM_LEFT, PI_OUTPUT);    //LED left
    set_mode(_fd, GPIO_LED_BOTTOM_RIGHT, PI_OUTPUT);     //LED right


    set_PWM_dutycycle(_fd, GPIO_MOTOR_PWM_LEFT, 0);   //stop pwm
    set_PWM_dutycycle(_fd, GPIO_MOTOR_PWM_RIGHT, 0);   //stop pwm

    set_PWM_frequency(_fd, GPIO_MOTOR_PWM_LEFT, 100); //set pwm freq to 100Hz
    set_PWM_frequency(_fd, GPIO_MOTOR_PWM_RIGHT, 100);

    set_PWM_range(_fd, GPIO_MOTOR_PWM_LEFT, 100);    //pwm range 0-100
    set_PWM_range(_fd, GPIO_MOTOR_PWM_RIGHT, 100);
}


void CarController::shutdown()
{
    set_PWM_dutycycle(_fd, GPIO_MOTOR_PWM_LEFT, 0); //stop pwm
    set_PWM_dutycycle(_fd, GPIO_MOTOR_PWM_RIGHT, 0);
}

void CarController::Control(RCData* rcData)
{
    Motor(rcData->left, rcData->right);
    Led(rcData->led);
}

void CarController::Led(uint8_t ledMask)
{
    //LED Right

    if(ledMask & LED_BOTH_RIGHT) {
        gpio_write(_fd, GPIO_LED_TOP_RIGHT, 1);
        gpio_write(_fd, GPIO_LED_BOTTOM_RIGHT, 1);
    } else {
        gpio_write(_fd, GPIO_LED_TOP_RIGHT, 0);
        gpio_write(_fd, GPIO_LED_BOTTOM_RIGHT, 0);
    }

    if(ledMask & LED_BOTH_LEFT) {
        gpio_write(_fd, GPIO_LED_TOP_LEFT, 1);
        gpio_write(_fd, GPIO_LED_BOTTOM_LEFT, 1);
    } else {
        gpio_write(_fd, GPIO_LED_TOP_LEFT, 0);
        gpio_write(_fd, GPIO_LED_BOTTOM_LEFT, 0);
    }
}

void CarController::Motor(int8_t left, int8_t right)
{

    if (left > 0) {
        gpio_write(_fd, GPIO_MOTOR_CTRL1_LEFT, 1);
        gpio_write(_fd, GPIO_MOTOR_CTRL2_LEFT, 0);
    } else if (left < 0) {
        gpio_write(_fd, GPIO_MOTOR_CTRL1_LEFT, 0);
        gpio_write(_fd, GPIO_MOTOR_CTRL2_LEFT, 1);
    } else {
        gpio_write(_fd, GPIO_MOTOR_CTRL1_LEFT, 0);
        gpio_write(_fd, GPIO_MOTOR_CTRL2_LEFT, 0);
    }

    if (right > 0) {
        gpio_write(_fd, GPIO_MOTOR_CTRL1_RIGHT, 1);
        gpio_write(_fd, GPIO_MOTOR_CTRL2_RIGHT, 0);
    } else if (right < 0) {
        gpio_write(_fd, GPIO_MOTOR_CTRL1_RIGHT, 0);
        gpio_write(_fd, GPIO_MOTOR_CTRL2_RIGHT, 1);
    } else {
        gpio_write(_fd, GPIO_MOTOR_CTRL1_RIGHT, 0);
        gpio_write(_fd, GPIO_MOTOR_CTRL2_RIGHT, 0);
    }

    left = abs(left);
    right = abs(right);

    left = std::max(20, std::min((int)left, 100));
    right = std::max(20, std::min((int)right, 100));

    set_PWM_dutycycle(_fd, GPIO_MOTOR_PWM_LEFT, left);
    set_PWM_dutycycle(_fd, GPIO_MOTOR_PWM_RIGHT, right);
}

void CarController::Stop()
{
    set_PWM_dutycycle(_fd, GPIO_MOTOR_PWM_LEFT, 0);
    set_PWM_dutycycle(_fd, GPIO_MOTOR_PWM_RIGHT, 0);

    gpio_write(_fd, GPIO_MOTOR_CTRL1_LEFT, 0);
    gpio_write(_fd, GPIO_MOTOR_CTRL2_LEFT, 0);

    gpio_write(_fd, GPIO_MOTOR_CTRL1_RIGHT, 0);
    gpio_write(_fd, GPIO_MOTOR_CTRL2_RIGHT, 0);
}
