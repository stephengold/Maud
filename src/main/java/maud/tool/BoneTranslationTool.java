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

import com.jme3.math.FastMath;
import com.jme3.math.Vector3f;
import java.util.logging.Logger;
import jme3utilities.nifty.GuiScreenController;
import jme3utilities.nifty.GuiWindowController;
import jme3utilities.nifty.SliderTransform;
import maud.Maud;
import maud.model.cgm.EditableCgm;
import maud.model.cgm.SelectedBone;

/**
 * The controller for the "Bone-Translation Tool" window in Maud's editor
 * screen.
 *
 * @author Stephen Gold sgold@sonic.net
 */
class BoneTranslationTool extends GuiWindowController {
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
            = Logger.getLogger(BoneTranslationTool.class.getName());
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
    BoneTranslationTool(GuiScreenController screenController) {
        super(screenController, "boneTranslationTool", false);
    }
    // *************************************************************************
    // new methods exposed

    /**
     * If active, update the MVC model based on the sliders.
     */
    void onSliderChanged() {
        EditableCgm target = Maud.getModel().getTarget();
        if (target.getBone().shouldEnableControls()) {
            Vector3f offsets = Maud.gui.readVectorBank("Off", axisSt);
            float masterScale = readSlider("offMaster", masterSt);
            offsets.multLocal(masterScale);

            int boneIndex = target.getBone().getIndex();
            target.getPose().get().setTranslation(boneIndex, offsets);
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
        Maud.gui.setIgnoreGuiChanges(true);

        boolean enableSliders = false;
        SelectedBone bone = Maud.getModel().getTarget().getBone();
        if (bone.isSelected()) {
            setSlidersToPose();
            if (bone.shouldEnableControls()) {
                setButtonText("resetOffAnim", "Animation");
                setButtonText("resetOffBind", "Bind pose");
                enableSliders = true;
            } else {
                setButtonText("resetOffAnim", "");
                setButtonText("resetOffBind", "");
            }
        } else {
            clear();
            setButtonText("resetOffAnim", "");
            setButtonText("resetOffBind", "");
        }
        setSlidersEnabled(enableSliders);

        Maud.gui.setIgnoreGuiChanges(false);
    }
    // *************************************************************************
    // private methods

    /**
     * Reset the 3 axis sliders and clear their status labels.
     */
    private void clear() {
        for (int iAxis = 0; iAxis < numAxes; iAxis++) {
            String sliderName = axisNames[iAxis] + "Off";
            setSlider(sliderName, axisSt, 0f);
            setStatusText(sliderName + "SliderStatus", "");
        }
    }

    /**
     * Disable or enable all 4 sliders.
     *
     * @param newState true&rarr;enable the sliders, false&rarr;disable them
     */
    private void setSlidersEnabled(boolean newState) {
        for (int iAxis = 0; iAxis < numAxes; iAxis++) {
            String sliderName = axisNames[iAxis] + "Off";
            setSliderEnabled(sliderName, newState);
        }
        setSliderEnabled("offMaster", newState);
    }

    /**
     * Set all 4 sliders (and their status labels) based on the pose.
     */
    private void setSlidersToPose() {
        EditableCgm target = Maud.getModel().getTarget();
        Vector3f vector = target.getBone().userTranslation(null);
        float[] offsets = vector.toArray(null);

        float scale = readSlider("offMaster", masterSt);
        for (int iAxis = 0; iAxis < numAxes; iAxis++) {
            float absOffset = FastMath.abs(offsets[iAxis]);
            if (absOffset > scale) {
                scale = absOffset;
            }
        }
        scale = FastMath.clamp(scale, minScale, maxScale);
        setSlider("offMaster", masterSt, scale);

        for (int iAxis = 0; iAxis < numAxes; iAxis++) {
            float value = offsets[iAxis];
            String sliderName = axisNames[iAxis] + "Off";
            setSlider(sliderName, axisSt, value / scale);
            updateSliderStatus(sliderName, value, " bu");
        }
    }
}
