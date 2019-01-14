/*
 Copyright (c) 2017-2019, Stephen Gold
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
import java.util.List;
import java.util.logging.Logger;
import jme3utilities.nifty.GuiScreenController;
import jme3utilities.nifty.SliderTransform;
import jme3utilities.nifty.Tool;
import maud.Maud;
import maud.MaudUtil;
import maud.model.EditableMap;
import maud.model.EditorModel;
import maud.model.option.RotationDisplayMode;

/**
 * The controller for the "Twist" tool in Maud's editor screen.
 *
 * @author Stephen Gold sgold@sonic.net
 */
class TwistTool extends Tool {
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
     * Instantiate an uninitialized tool.
     *
     * @param screenController the controller of the screen that will contain
     * the tool (not null)
     */
    TwistTool(GuiScreenController screenController) {
        super(screenController, "twist");
    }
    // *************************************************************************
    // Tool methods

    /**
     * Enumerate this tool's sliders.
     *
     * @return a new list of names (unique id prefixes)
     */
    @Override
    protected List<String> listSliders() {
        List<String> result = super.listSliders();
        for (int iAxis = 0; iAxis < numAxes; iAxis++) {
            String sliderName = axisNames[iAxis] + "Twist";
            result.add(sliderName);
        }

        return result;
    }

    /**
     * Update the MVC model based on the sliders.
     *
     * @param name the name (unique id prefix) of the slider (not null)
     */
    @Override
    public void onSliderChanged(String name) {
        EditableMap map = Maud.getModel().getMap();
        if (map.isBoneMappingSelected()) {
            float[] sliderPositions = new float[numAxes];
            for (int iAxis = 0; iAxis < numAxes; iAxis++) {
                String sliderName = axisNames[iAxis] + "Twist";
                float position = readSlider(sliderName, axisSt);
                sliderPositions[iAxis] = position;
            }

            Quaternion twist = new Quaternion();
            RotationDisplayMode mode
                    = Maud.getModel().getMisc().rotationDisplayMode();
            if (mode == RotationDisplayMode.QuatCoeff) {
                MaudUtil.setFromSliders(sliderPositions, twist);
            } else {
                twist.fromAngles(sliderPositions);
            }
            map.setTwist(twist);
        }
    }

    /**
     * Update this tool prior to rendering. (Invoked once per frame while this
     * tool is displayed.)
     */
    @Override
    protected void toolUpdate() {
        updateSelected();

        RotationDisplayMode mode
                = Maud.getModel().getMisc().rotationDisplayMode();
        String dButton = mode.toString();
        setButtonText("rotationMode3", dButton);
    }
    // *************************************************************************
    // private methods

    /**
     * Zero all 3 sliders and clear their status labels.
     */
    private void clear() {
        for (int iAxis = 0; iAxis < numAxes; iAxis++) {
            String sliderName = axisNames[iAxis] + "Twist";
            setSlider(sliderName, axisSt, 0f);
            setStatusText(sliderName + "SliderStatus", "");
        }
    }

    /**
     * Set all 3 sliders (and their status labels) based on the mapping twist.
     */
    private void setSlidersToTwist() {
        Quaternion effTwist = Maud.getModel().getMap().copyTwist(null);
        RotationDisplayMode mode
                = Maud.getModel().getMisc().rotationDisplayMode();
        float[] statusValues = new float[numAxes];
        float[] sliderPositions = new float[numAxes];
        String unitSuffix = MaudUtil.displayRotation(effTwist, mode,
                statusValues, sliderPositions);
        for (int iAxis = 0; iAxis < numAxes; iAxis++) {
            String sliderName = axisNames[iAxis] + "Twist";
            float position = sliderPositions[iAxis];
            setSlider(sliderName, axisSt, position);
            float value = statusValues[iAxis];
            updateSliderStatus(sliderName, value, unitSuffix);
        }
    }

    /**
     * Update the twist sliders and reset/snap buttons.
     */
    private void updateSelected() {
        boolean enableSliders = false;
        String rButton = "", sButton = "", xyzButton = "";

        EditorModel model = Maud.getModel();
        if (model.getMap().isBoneMappingSelected()) {
            setSlidersToTwist();
            rButton = "Reset";
            sButton = "Snap";
            RotationDisplayMode mode = model.getMisc().rotationDisplayMode();
            if (mode != RotationDisplayMode.QuatCoeff) {
                xyzButton = "Snap";
            }
            enableSliders = true;
        } else {
            clear();
        }

        setButtonText("resetTwist", rButton);
        setButtonText("snapTwist", sButton);
        setButtonText("snapXTwist", xyzButton);
        setButtonText("snapYTwist", xyzButton);
        setButtonText("snapZTwist", xyzButton);
        setSlidersEnabled(enableSliders);
    }
}
