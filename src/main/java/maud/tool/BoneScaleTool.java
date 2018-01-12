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

import com.jme3.math.Vector3f;
import java.util.logging.Logger;
import jme3utilities.math.MyMath;
import jme3utilities.nifty.GuiScreenController;
import jme3utilities.nifty.GuiWindowController;
import jme3utilities.nifty.SliderTransform;
import maud.Maud;
import maud.model.cgm.EditableCgm;
import maud.model.cgm.SelectedBone;

/**
 * The controller for the "Bone-Scale Tool" window in Maud's editor screen.
 *
 * @author Stephen Gold sgold@sonic.net
 */
class BoneScaleTool extends GuiWindowController {
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
            = Logger.getLogger(BoneScaleTool.class.getName());
    /**
     * transform for the axis sliders
     */
    final private static SliderTransform axisSt = SliderTransform.Reversed;
    /**
     * transform for the masterSlider
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
    BoneScaleTool(GuiScreenController screenController) {
        super(screenController, "boneScaleTool", false);
    }
    // *************************************************************************
    // new methods exposed

    /**
     * If active, update the MVC model based on the sliders.
     */
    void onSliderChanged() {
        EditableCgm target = Maud.getModel().getTarget();
        if (target.getBone().shouldEnableControls()) {
            Vector3f scales = Maud.gui.readVectorBank("Sca", axisSt);
            /*
             * Avoid scale factors near zero.
             */
            scales.x = Math.max(scales.x, 0.001f);
            scales.y = Math.max(scales.y, 0.001f);
            scales.z = Math.max(scales.z, 0.001f);

            float masterScale = readSlider("scaMaster", masterSt);
            scales.multLocal(masterScale);

            int boneIndex = target.getBone().getIndex();
            target.getPose().get().setScale(boneIndex, scales);
        }
    }
    // *************************************************************************
    // GuiWindowController methods

    /**
     * Callback to update this state prior to rendering. (Invoked once per
     * render pass.)
     *
     * @param tpf time interval between render passes (in seconds, &ge;0)
     */
    @Override
    public void update(float tpf) {
        super.update(tpf);

        boolean enableSliders = false;
        SelectedBone bone = Maud.getModel().getTarget().getBone();
        if (bone.isSelected()) {
            setSlidersToPose();
            if (bone.shouldEnableControls()) {
                setButtonText("resetScaAnim", "Animation");
                setButtonText("resetScaBind", "Bind pose");
                enableSliders = true;
            } else {
                setButtonText("resetScaAnim", "");
                setButtonText("resetScaBind", "");
            }
        } else {
            clear();
            setButtonText("resetScaAnim", "");
            setButtonText("resetScaBind", "");
        }
        setSlidersEnabled(enableSliders);
    }
    // *************************************************************************
    // private methods

    /**
     * Reset all 4 sliders and clear the status labels.
     */
    private void clear() {
        for (int iAxis = 0; iAxis < numAxes; iAxis++) {
            String sliderName = axisNames[iAxis] + "Sca";
            setSlider(sliderName, axisSt, 1f);
            setStatusText(sliderName + "SliderStatus", "");
        }
        setSlider("scaMaster", masterSt, 1f);
    }

    /**
     * Disable or enable all 4 sliders.
     *
     * @param newState true&rarr;enable the sliders, false&rarr;disable them
     */
    private void setSlidersEnabled(boolean newState) {
        for (int iAxis = 0; iAxis < numAxes; iAxis++) {
            String sliderName = axisNames[iAxis] + "Sca";
            setSliderEnabled(sliderName, newState);
        }
        setSliderEnabled("scaMaster", newState);
    }

    /**
     * Set all 4 sliders (and their status labels) based on the pose.
     */
    private void setSlidersToPose() {
        Vector3f vector = Maud.getModel().getTarget().getBone().userScale(null);
        float maxScale = MyMath.max(vector.x, vector.y, vector.z);
        assert maxScale > 0f : maxScale;
        setSlider("scaMaster", masterSt, maxScale);

        float[] scales = vector.toArray(null);

        for (int iAxis = 0; iAxis < numAxes; iAxis++) {
            String sliderName = axisNames[iAxis] + "Sca";
            float scale = scales[iAxis];
            setSlider(sliderName, axisSt, scale / maxScale);
            updateSliderStatus(sliderName, scale, "x");
        }
    }
}
