/*
 Copyright (c) 2017-2023, Stephen Gold
 All rights reserved.

 Redistribution and use in source and binary forms, with or without
 modification, are permitted provided that the following conditions are met:
 * Redistributions of source code must retain the above copyright
 notice, this list of conditions and the following disclaimer.
 * Redistributions in binary form must reproduce the above copyright
 notice, this list of conditions and the following disclaimer in the
 documentation and/or other materials provided with the distribution.
 * Neither the name of the copyright holder nor the names of its contributors
 may be used to endorse or promote products derived from this software without
 specific prior written permission.

 THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND
 ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
 WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE
 FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL
 DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR
 SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER
 CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY,
 OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
 OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */
package maud.action;

import com.jme3.input.InputManager;
import com.jme3.input.MouseInput;
import com.jme3.input.controls.AnalogListener;
import com.jme3.input.controls.MouseAxisTrigger;
import java.util.logging.Level;
import java.util.logging.Logger;
import jme3utilities.MyString;
import jme3utilities.Validate;
import maud.Maud;
import maud.model.cgm.Pov;

/**
 * The analog listener for the input device that zooms a POV. Currently, this
 * device is the scroll wheel on the mouse.
 *
 * @author Stephen Gold sgold@sonic.net
 */
class ZoomListener implements AnalogListener {
    // *************************************************************************
    // constants and loggers

    /**
     * message logger for this class
     */
    final private static Logger logger
            = Logger.getLogger(ZoomListener.class.getName());
    /**
     * analog event string to move a POV backward or zoom out
     */
    final private static String povBackward = "pov backward";
    /**
     * analog event string to move a POV forward or zoom in
     */
    final private static String povForward = "pov forward";
    // *************************************************************************
    // new methods exposed

    /**
     * Map the input device.
     */
    void map() {
        Maud application = Maud.getApplication();
        InputManager inputMgr = application.getInputManager();

        // Turning the mouse wheel up triggers positive Pov.moveBackward().
        boolean wheelUp = true;
        MouseAxisTrigger backwardTrigger
                = new MouseAxisTrigger(MouseInput.AXIS_WHEEL, wheelUp);
        inputMgr.addMapping(povBackward, backwardTrigger);
        inputMgr.addListener(this, povBackward);

        // Turning the mouse wheel down triggers negative Pov.moveBackward().
        boolean wheelDown = false;
        MouseAxisTrigger forwardTrigger
                = new MouseAxisTrigger(MouseInput.AXIS_WHEEL, wheelDown);
        inputMgr.addMapping(povForward, forwardTrigger);
        inputMgr.addListener(this, povForward);
    }

    /**
     * Unmap the input device.
     */
    static void unmap() {
        Maud application = Maud.getApplication();
        InputManager inputMgr = application.getInputManager();

        inputMgr.deleteMapping(povForward);
        inputMgr.deleteMapping(povBackward);
    }
    // *************************************************************************
    // AnalogListener methods

    /**
     * Process an analog event from the mouse.
     *
     * @param eventString textual description of the analog event (not null)
     * @param amount amount of the event (&ge;0)
     * @param ignored time interval between frames (in seconds, &ge;0)
     */
    @Override
    public void onAnalog(String eventString, float amount, float ignored) {
        Validate.nonNegative(amount, "amount");
        logger.log(Level.FINE, "Received analog event {0} with amount={1}",
                new Object[]{MyString.quote(eventString), amount});

        Pov pov = Maud.gui.mousePov();
        if (pov == null || amount == 0f) {
            return;
        }

        switch (eventString) {
            case povBackward:
                pov.moveBackward(+amount);
                break;

            case povForward:
                pov.moveBackward(-amount);
                break;

            default:
                logger.log(Level.WARNING, "unexpected analog event {0}",
                        MyString.quote(eventString));
        }
    }
}
