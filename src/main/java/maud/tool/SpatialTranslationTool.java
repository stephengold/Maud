/*
 Copyright (c) 2017-2021, Stephen Gold
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

import com.jme3.math.FastMath;
import com.jme3.math.Vector3f;
import java.util.List;
import java.util.logging.Logger;
import jme3utilities.nifty.GuiScreenController;
import jme3utilities.nifty.SliderTransform;
import jme3utilities.nifty.Tool;
import maud.Maud;
import maud.model.cgm.SelectedSpatial;

/**
 * The controller for the "Spatial-Translation" tool in Maud's editor screen.
 *
 * @author Stephen Gold sgold@sonic.net
 */
class SpatialTranslationTool extends Tool {
    // *************************************************************************
    // constants and loggers

    /**
     * maximum scale for offsets (&gt;0)
     */
    final private static float maxScale = 1000f;
    /**
     * minimum scale for offsets (&gt;0)
     */
    final private static float minScale = 0.01f;
    /**
     * number of coordinate axes
     */
    final private static int numAxes = 3;
    /**
     * message logger for this class
     */
    final private static Logger logger
            = Logger.getLogger(SpatialTranslationTool.class.getName());
    /**
     * transform for the axis sliders
     */
    final private static SliderTransform axisSt = SliderTransform.Reversed;
    /**
     * transform for the master slider
     */
    final private static SliderTransform masterSt = SliderTransform.Log10;
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
    SpatialTranslationTool(GuiScreenController screenController) {
        super(screenController, "spatialTranslation");
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
            String sliderName = axisNames[iAxis] + "So";
            result.add(sliderName);
        }
        result.add("soMaster");

        return result;
    }

    /**
     * Update the MVC model based on the sliders.
     *
     * @param sliderName the name (unique id prefix) of the slider (not null)
     */
    @Override
    public void onSliderChanged(String sliderName) {
        Vector3f offsets = readVectorBank("So", axisSt, null);
        float masterScale = readSlider("soMaster", masterSt);
        offsets.multLocal(masterScale);
        Maud.getModel().getTarget().setSpatialTranslation(offsets);
    }

    /**
     * Update this tool prior to rendering. (Invoked once per frame while this
     * tool is displayed.)
     */
    @Override
    protected void toolUpdate() {
        setSlidersToTransform();

        String buttonText = "";
        boolean isNode = Maud.getModel().getTarget().getSpatial().isNode();
        if (isNode) {
            buttonText = "Smart set";
        }
        setButtonText("spatialTranslationSmartSet", buttonText);
    }
    // *************************************************************************
    // private methods

    /**
     * Set all 4 sliders (and their status labels) based on the local
     * translation of the selected spatial.
     */
    private void setSlidersToTransform() {
        SelectedSpatial spatial = Maud.getModel().getTarget().getSpatial();
        Vector3f vector = spatial.localTranslation(null);
        float[] offsets = vector.toArray(null);

        float scale = readSlider("soMaster", masterSt);
        for (int iAxis = 0; iAxis < numAxes; iAxis++) {
            float absOffset = FastMath.abs(offsets[iAxis]);
            if (absOffset > scale) {
                scale = absOffset;
            }
        }
        scale = FastMath.clamp(scale, minScale, maxScale);
        setSlider("soMaster", masterSt, scale);

        for (int iAxis = 0; iAxis < numAxes; iAxis++) {
            float value = offsets[iAxis];
            String sliderName = axisNames[iAxis] + "So";
            setSlider(sliderName, axisSt, value / scale);
            updateSliderStatus(sliderName, value, " lu");
        }
    }
}
