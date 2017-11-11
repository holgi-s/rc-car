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

import com.erz.joysticklibrary.JoyStick;

public class DualTwoWayTranslator implements IJoystickTranslator {

    private JoyStick left;
    private JoyStick right;

    public DualTwoWayTranslator(JoyStick left, JoyStick right){
        this.left = left;
        this.right = right;
    }

    @Override
    public DualSpeed getDualSpeed() {

        double power = this.left.getPower();
        double angle = this.left.getAngle();
        final int cl = (int)(power * (angle < 0 ? -1 : 1));

        power = this.right.getPower();
        angle = this.right.getAngle();
        final int cr = (int)(power * (angle < 0 ? -1 : 1));

        return new DualSpeed(cl, cr);
    }
}
