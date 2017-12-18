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
import jme3utilities.nifty.BasicScreenController;
import jme3utilities.nifty.SliderTransform;
import jme3utilities.nifty.WindowController;
import maud.Maud;
import maud.model.cgm.EditableCgm;
import maud.model.cgm.SelectedBone;

/**
 * The controller for the "Bone-Scale Tool" window in Maud's editor screen.
 *
 * @author Stephen Gold sgold@sonic.net
 */
class BoneScaleTool extends WindowController {
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
    BoneScaleTool(BasicScreenController screenController) {
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

            float masterScale = Maud.gui.readSlider("scaMaster", masterSt);
            scales.multLocal(masterScale);

            int boneIndex = target.getBone().getIndex();
            target.getPose().get().setScale(boneIndex, scales);
        }
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

        SelectedBone bone = Maud.getModel().getTarget().getBone();
        if (bone.isSelected()) {
            setSlidersToPose();
            if (bone.shouldEnableControls()) {
                Maud.gui.setButtonText("resetScaAnim", "Animation");
                Maud.gui.setButtonText("resetScaBind", "Bind pose");
                enableSliders();
            } else {
                Maud.gui.setButtonText("resetScaAnim", "");
                Maud.gui.setButtonText("resetScaBind", "");
                disableSliders();
            }

        } else {
            clear();
            Maud.gui.setButtonText("resetScaAnim", "");
            Maud.gui.setButtonText("resetScaBind", "");
            disableSliders();
        }
    }
    // *************************************************************************
    // private methods

    /**
     * Reset all 4 sliders and clear the status labels.
     */
    private void clear() {
        for (int iAxis = 0; iAxis < numAxes; iAxis++) {
            String sliderName = axisNames[iAxis] + "Sca";
            Maud.gui.setSlider(sliderName, axisSt, 1f);
            Maud.gui.setStatusText(sliderName + "SliderStatus", "");
        }
        Maud.gui.setSlider("scaMaster", masterSt, 1f);
    }

    /**
     * Disable all 4 sliders.
     */
    private void disableSliders() {
        for (int iAxis = 0; iAxis < numAxes; iAxis++) {
            String sliderName = axisNames[iAxis] + "Sca";
            Maud.gui.disableSlider(sliderName);
        }
        Maud.gui.disableSlider("scaMaster");
    }

    /**
     * Enable all 4 sliders.
     */
    private void enableSliders() {
        for (int iAxis = 0; iAxis < numAxes; iAxis++) {
            String sliderName = axisNames[iAxis] + "Sca";
            Maud.gui.enableSlider(sliderName);
        }
        Maud.gui.enableSlider("scaMaster");
    }

    /**
     * Set all 4 sliders (and their status labels) based on the pose.
     */
    private void setSlidersToPose() {
        Vector3f vector = Maud.getModel().getTarget().getBone().userScale(null);
        float maxScale = MyMath.max(vector.x, vector.y, vector.z);
        assert maxScale > 0f : maxScale;
        Maud.gui.setSlider("scaMaster", masterSt, maxScale);

        float[] scales = vector.toArray(null);

        for (int iAxis = 0; iAxis < numAxes; iAxis++) {
            String sliderName = axisNames[iAxis] + "Sca";
            float scale = scales[iAxis];
            Maud.gui.setSlider(sliderName, axisSt, scale / maxScale);
            Maud.gui.updateSliderStatus(sliderName, scale, "x");
        }
    }
}
