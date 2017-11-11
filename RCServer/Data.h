//
// Created by Holger on 02.11.2017.
//

#ifndef RCSERVER_DATA_H
#define RCSERVER_DATA_H

#pragma pack(push, 1)

struct RCData {
    int8_t left;     //  +/- 100 motor speed
    int8_t right;    //  +/- 100 motor speed
    uint8_t led;     //  led bit mask
    uint8_t stop;    //  extra bi mask bit 1 breaks;
};

#pragma pack(pop)

#endif //RCSERVER_DATA_H
