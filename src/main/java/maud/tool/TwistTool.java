/*
 Copyright (c) 2017, Stephen Gold
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
package maud.tool;

import com.jme3.math.Quaternion;
import java.util.logging.Logger;
import jme3utilities.math.MyMath;
import jme3utilities.nifty.BasicScreenController;
import jme3utilities.nifty.SliderTransform;
import jme3utilities.nifty.WindowController;
import maud.Maud;
import maud.model.EditableMap;

/**
 * The controller for the "Twist Tool" window in Maud's editor screen.
 *
 * @author Stephen Gold sgold@sonic.net
 */
class TwistTool extends WindowController {
    // *************************************************************************
    // constants and loggers

    /**
     * number of coordinate axes
     */
    final private static int numAxes = 3;
    /**
     * message logger for this class
     */
    final private static Logger logger
            = Logger.getLogger(TwistTool.class.getName());
    /**
     * transform for the axis sliders
     */
    final private static SliderTransform axisSt = SliderTransform.None;
    /**
     * names of the coordinate axes
     */
    final private static String[] axisNames = {"x", "y", "z"};
    // *************************************************************************
    // constructors

    /**
     * Instantiate an uninitialized controller.
     *
     * @param screenController
     */
    TwistTool(BasicScreenController screenController) {
        super(screenController, "twistTool", false);
    }
    // *************************************************************************
    // new methods exposed

    /**
     * If active, update the MVC model based on the sliders.
     */
    void onSliderChanged() {
        EditableMap map = Maud.getModel().getMap();
        if (map.isBoneMappingSelected()) {
            float[] angles = new float[numAxes];
            for (int iAxis = 0; iAxis < numAxes; iAxis++) {
                String sliderName = axisNames[iAxis] + "Twist";
                float value = Maud.gui.readSlider(sliderName, axisSt);
                angles[iAxis] = value;
            }
            Quaternion twist = new Quaternion();
            twist.fromAngles(angles);
            map.setTwist(twist);
        }
    }
    // *************************************************************************
    // WindowController methods

    /**
     * Callback to update this window prior to rendering. (Invoked once per
     * render pass.)
     *
     * @param elapsedTime time interval between render passes (in seconds,
     * &ge;0)
     */
    @Override
    public void update(float elapsedTime) {
        super.update(elapsedTime);
        Maud.gui.setIgnoreGuiChanges(true);

        updateSelected();
        /*
         * the degrees/radians button
         */
        String dButton;
        if (Maud.getModel().getMisc().getAnglesInDegrees()) {
            dButton = "radians";
        } else {
            dButton = "degrees";
        }
        Maud.gui.setButtonLabel("degreesButton3", dButton);

        Maud.gui.setIgnoreGuiChanges(false);
    }
    // *************************************************************************
    // private methods

    /**
     * Zero all 3 sliders and clear their status labels.
     */
    private void clear() {
        for (int iAxis = 0; iAxis < numAxes; iAxis++) {
            String sliderName = axisNames[iAxis] + "Twist";
            Maud.gui.setSlider(sliderName, axisSt, 0f);
            Maud.gui.setStatusText(sliderName + "SliderStatus", "");
        }
    }

    /**
     * Disable all 3 sliders.
     */
    private void disableSliders() {
        for (int iAxis = 0; iAxis < numAxes; iAxis++) {
            String sliderName = axisNames[iAxis] + "Twist";
            Maud.gui.disableSlider(sliderName);
        }
    }

    /**
     * Enable all 3 sliders.
     */
    private void enableSliders() {
        for (int iAxis = 0; iAxis < numAxes; iAxis++) {
            String sliderName = axisNames[iAxis] + "Twist";
            Maud.gui.enableSlider(sliderName);
        }
    }

    /**
     * Set all 3 sliders (and their status labels) based on the mapping twist.
     */
    private void setSlidersToTwist() {
        Quaternion effTwist = Maud.getModel().getMap().copyTwist(null);
        float[] angles = effTwist.toAngles(null);
        boolean degrees = Maud.getModel().getMisc().getAnglesInDegrees();

        for (int iAxis = 0; iAxis < numAxes; iAxis++) {
            String sliderName = axisNames[iAxis] + "Twist";
            float angle = angles[iAxis];
            Maud.gui.setSlider(sliderName, axisSt, angle);

            if (degrees) {
                angle = MyMath.toDegrees(angle);
                Maud.gui.updateSliderStatus(sliderName, angle, " deg");
            } else {
                Maud.gui.updateSliderStatus(sliderName, angle, " rad");
            }
        }
    }

    /**
     * Update the twist sliders and reset button.
     */
    private void updateSelected() {
        String rButton, sButton;

        if (Maud.getModel().getMap().isBoneMappingSelected()) {
            setSlidersToTwist();
            rButton = "Reset";
            sButton = "Snap";
            enableSliders();
        } else {
            clear();
            rButton = "";
            sButton = "";
            disableSliders();
        }

        Maud.gui.setButtonLabel("resetTwistButton", rButton);
        Maud.gui.setButtonLabel("snapTwistButton", sButton);
        Maud.gui.setButtonLabel("snapXTwistButton", sButton);
        Maud.gui.setButtonLabel("snapYTwistButton", sButton);
        Maud.gui.setButtonLabel("snapZTwistButton", sButton);
    }
}
