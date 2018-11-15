/*
 Copyright (c) 2017-2018, Stephen Gold
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
import maud.Maud;
import maud.MaudUtil;
import maud.model.EditorModel;
import maud.model.cgm.SelectedSpatial;
import maud.model.option.RotationDisplayMode;

/**
 * The controller for the "Spatial-Rotation" tool in Maud's editor screen.
 *
 * @author Stephen Gold sgold@sonic.net
 */
class SpatialRotationTool extends Tool {
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
            = Logger.getLogger(SpatialRotationTool.class.getName());
    /**
     * transform for the axis sliders
     */
    final private static SliderTransform axisSt = SliderTransform.Reversed;
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
    SpatialRotationTool(GuiScreenController screenController) {
        super(screenController, "spatialRotation");
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
            String sliderName = axisNames[iAxis] + "Sa";
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
        float[] sliderPositions = new float[numAxes];
        for (int iAxis = 0; iAxis < numAxes; iAxis++) {
            String sliderName = axisNames[iAxis] + "Sa";
            float position = readSlider(sliderName, axisSt);
            sliderPositions[iAxis] = position;
        }

        Quaternion rotation = new Quaternion();
        RotationDisplayMode mode
                = Maud.getModel().getMisc().rotationDisplayMode();
        if (mode == RotationDisplayMode.QuatCoeff) {
            MaudUtil.setFromSliders(sliderPositions, rotation);
        } else {
            rotation.fromAngles(sliderPositions);
        }
        Maud.getModel().getTarget().setSpatialRotation(rotation);
    }

    /**
     * Update this tool prior to rendering. (Invoked once per frame while this
     * tool is displayed.)
     */
    @Override
    protected void toolUpdate() {
        setSlidersToTransform();
        updateSnapButtons();

        RotationDisplayMode mode
                = Maud.getModel().getMisc().rotationDisplayMode();
        String dButton = mode.toString();
        setButtonText("rotationMode2", dButton);
    }
    // *************************************************************************
    // private methods

    /**
     * Set all 3 sliders (and their status labels) based on the local rotation
     * of the selected spatial.
     */
    private void setSlidersToTransform() {
        EditorModel model = Maud.getModel();
        SelectedSpatial spatial = model.getTarget().getSpatial();
        Quaternion rotation = spatial.localRotation(null);

        RotationDisplayMode mode
                = Maud.getModel().getMisc().rotationDisplayMode();
        float[] statusValues = new float[numAxes];
        float[] sliderPositions = new float[numAxes];
        String unitSuffix = MaudUtil.displayRotation(rotation, mode,
                statusValues, sliderPositions);
        for (int iAxis = 0; iAxis < numAxes; iAxis++) {
            String sliderName = axisNames[iAxis] + "Sa";
            float position = sliderPositions[iAxis];
            setSlider(sliderName, axisSt, position);
            float value = statusValues[iAxis];
            updateSliderStatus(sliderName, value, unitSuffix);
        }
    }

    /**
     * Update the snap buttons.
     */
    private void updateSnapButtons() {
        String xyzButton = "";

        EditorModel model = Maud.getModel();
        RotationDisplayMode mode = model.getMisc().rotationDisplayMode();
        if (mode != RotationDisplayMode.QuatCoeff) {
            xyzButton = "Snap";
        }

        setButtonText("snapXSa", xyzButton);
        setButtonText("snapYSa", xyzButton);
        setButtonText("snapZSa", xyzButton);
    }
}
