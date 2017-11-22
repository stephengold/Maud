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

import com.jme3.math.Vector3f;
import java.util.logging.Logger;
import jme3utilities.math.MyMath;
import jme3utilities.math.MyVector3f;
import jme3utilities.nifty.BasicScreenController;
import jme3utilities.nifty.SliderTransform;
import jme3utilities.nifty.WindowController;
import maud.Maud;
import maud.model.cgm.SelectedSpatial;

/**
 * The controller for the "Spatial-Scale Tool" window in Maud's editor screen.
 *
 * @author Stephen Gold sgold@sonic.net
 */
class SpatialScaleTool extends WindowController {
    // *************************************************************************
    // constants and loggers

    /**
     * message logger for this class
     */
    final private static Logger logger
            = Logger.getLogger(SpatialScaleTool.class.getName());
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
     * Instantiate an uninitialized controller.
     *
     * @param screenController the controller of the screen that contains the
     * window (not null)
     */
    SpatialScaleTool(BasicScreenController screenController) {
        super(screenController, "spatialScaleTool", false);
    }
    // *************************************************************************
    // new methods exposed

    /**
     * Update the MVC model based on the sliders.
     */
    void onSliderChanged() {
        Vector3f scales = Maud.gui.readVectorBank("Ss", axisSt);
        /*
         * Avoid scale factors near zero.
         */
        scales.x = Math.max(scales.x, 0.001f);
        scales.y = Math.max(scales.y, 0.001f);
        scales.z = Math.max(scales.z, 0.001f);

        float masterScale = Maud.gui.readSlider("ssMaster", masterSt);
        scales.multLocal(masterScale);
        Maud.getModel().getTarget().setSpatialScale(scales);
    }
    // *************************************************************************
    // WindowController methods

    /**
     * Callback to update this state prior to rendering. (Invoked once per
     * render pass.)
     *
     * @param tpf time interval between render passes (in seconds, &ge;0)
     */
    @Override
    public void update(float tpf) {
        super.update(tpf);
        Maud.gui.setIgnoreGuiChanges(true);
        setSlidersToTransform();
        Maud.gui.setIgnoreGuiChanges(false);

    }
    // *************************************************************************
    // private methods

    /**
     * Set all 4 sliders (and their status labels) based on the local scale of
     * the selected spatial.
     */
    private void setSlidersToTransform() {
        SelectedSpatial spatial = Maud.getModel().getTarget().getSpatial();
        Vector3f vector = spatial.localScale(null);
        float maxScale = MyMath.max(vector.x, vector.y, vector.z);
        assert maxScale > 0f : maxScale;

        Maud.gui.setSlider("ssMaster", masterSt, maxScale);

        float[] scales = vector.toArray(null);
        for (int iAxis = 0; iAxis < MyVector3f.numAxes; iAxis++) {
            float scale = scales[iAxis];
            String sliderName = axisNames[iAxis] + "Ss";
            Maud.gui.setSlider(sliderName, axisSt, scale / maxScale);
            Maud.gui.updateSliderStatus(sliderName, scale, "x");
        }
    }
}
