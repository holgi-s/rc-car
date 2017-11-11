package com.holgis.rcclient.joystick;
/*
 * Copyright 2017 Holger Schmidt
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import android.util.Log;

import com.erz.joysticklibrary.JoyStick;

public class FourWayTranslator implements IJoystickTranslator {
    private final String TAG = FourWayTranslator.class.getName();
    private final double HALF_PI = Math.PI/2;
    private JoyStick joyStick;

    public FourWayTranslator(JoyStick joyStick){
        this.joyStick = joyStick;
    }

    @Override
    public DualSpeed getDualSpeed() {

        double power = this.joyStick.getPower();
        double angle = this.joyStick.getAngle();

        int left = 0;
        int right = 0;

        double center = (Math.abs(angle)-HALF_PI)/HALF_PI; //0 is center +/- 1.0

        if(angle >= 0) {
            if(center >= 0) {
                left = 100;
                right = 100 - (int) (center * 200.0);
            } else {
                right = 100;
                left = 100 - (int) (-center * 200.0);
            }
        } else {
            if(center >= 0) {
                right = -100;
                left = -100 + (int) (center * 200.0);
            } else {
                left = -100;
                right = -100 + (int) (-center * 200.0);
            }
        }

        left *= power / 100;
        right *= power / 100;

        if(angle < 0) {

        }

        return new DualSpeed(left, right);
    }
}
