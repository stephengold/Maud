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

import com.jme3.app.Application;
import com.jme3.app.state.AppStateManager;
import com.jme3.math.FastMath;
import com.jme3.math.Vector3f;
import de.lessvoid.nifty.controls.Slider;
import java.util.logging.Logger;
import jme3utilities.nifty.BasicScreenController;
import jme3utilities.nifty.WindowController;
import maud.Maud;
import maud.model.EditableCgm;

/**
 * The controller for the "Bone-Translation Tool" window in Maud's editor
 * screen.
 *
 * @author Stephen Gold sgold@sonic.net
 */
class BoneTranslationTool extends WindowController {
    // *************************************************************************
    // constants and loggers

    /**
     * logarithm base for the master slider
     */
    final private static float masterBase = 10f;
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
    final private static Logger logger = Logger.getLogger(
            BoneTranslationTool.class.getName());
    /**
     * names of the coordinate axes
     */
    final private static String[] axisNames = {"x", "y", "z"};
    // *************************************************************************
    // fields

    /**
     * references to the per-axis sliders, set by
     * {@link #initialize(com.jme3.app.state.AppStateManager, com.jme3.app.Application)}
     */
    final private Slider sliders[] = new Slider[numAxes];
    /**
     * reference to the master slider, set by
     * {@link #initialize(com.jme3.app.state.AppStateManager, com.jme3.app.Application)}
     */
    private Slider masterSlider = null;
    // *************************************************************************
    // constructors

    /**
     * Instantiate an uninitialized controller.
     *
     * @param screenController the controller of the screen that contains the
     * window (not null)
     */
    BoneTranslationTool(BasicScreenController screenController) {
        super(screenController, "boneTranslationTool", false);
    }
    // *************************************************************************
    // new methods exposed

    /**
     * If active, update the MVC model based on the sliders.
     */
    void onSliderChanged() {
        EditableCgm target = Maud.getModel().target;
        if (target.bone.shouldEnableControls()) {
            Vector3f offsets = Maud.gui.readVectorBank("Off");

            float masterScale = readScale();
            offsets.multLocal(masterScale);

            int boneIndex = target.bone.getIndex();
            target.pose.getPose().setTranslation(boneIndex, offsets);
        }
    }
    // *************************************************************************
    // AppState methods

    /**
     * Initialize this controller prior to its 1st update.
     *
     * @param stateManager (not null)
     * @param application application that owns the window (not null)
     */
    @Override
    public void initialize(AppStateManager stateManager,
            Application application) {
        super.initialize(stateManager, application);

        for (int iAxis = 0; iAxis < numAxes; iAxis++) {
            String axisName = axisNames[iAxis];
            Slider slider = Maud.gui.getSlider(axisName + "Off");
            assert slider != null;
            sliders[iAxis] = slider;
        }
        masterSlider = Maud.gui.getSlider("offMaster");
    }

    /**
     * Callback to update this state prior to rendering. (Invoked once per
     * render pass.)
     *
     * @param tpf time interval between render passes (in seconds, &ge;0)
     */
    @Override
    public void update(float tpf) {
        super.update(tpf);

        if (Maud.getModel().target.bone.isSelected()) {
            setSlidersToPose();
            if (Maud.getModel().target.bone.shouldEnableControls()) {
                Maud.gui.setButtonLabel("resetOffAnimButton", "Animation");
                Maud.gui.setButtonLabel("resetOffBindButton", "Bind pose");
                enableSliders();
            } else {
                Maud.gui.setButtonLabel("resetOffAnimButton", "");
                Maud.gui.setButtonLabel("resetOffBindButton", "");
                disableSliders();
            }

        } else {
            clear();
            Maud.gui.setButtonLabel("resetOffAnimButton", "");
            Maud.gui.setButtonLabel("resetOffBindButton", "");
            disableSliders();
        }
    }
    // *************************************************************************
    // private methods

    /**
     * Reset the 3 axis sliders and clear their status labels.
     */
    private void clear() {
        for (int iAxis = 0; iAxis < numAxes; iAxis++) {
            sliders[iAxis].setValue(0f);

            String axisName = axisNames[iAxis];
            String statusName = axisName + "OffSliderStatus";
            Maud.gui.setStatusText(statusName, "");
        }
    }

    /**
     * Disable all 4 sliders.
     */
    private void disableSliders() {
        for (int iAxis = 0; iAxis < numAxes; iAxis++) {
            sliders[iAxis].disable();
        }
        masterSlider.disable();
    }

    /**
     * Enable all 4 sliders.
     */
    private void enableSliders() {
        for (int iAxis = 0; iAxis < numAxes; iAxis++) {
            sliders[iAxis].enable();
        }
        masterSlider.enable();
    }

    /**
     * Read the master slider.
     */
    private float readScale() {
        float reading = masterSlider.getValue();
        float result = FastMath.pow(masterBase, reading);

        return result;
    }

    /**
     * Set all 4 sliders (and their status labels) based on the pose.
     */
    private void setSlidersToPose() {
        Vector3f vector = Maud.getModel().target.bone.userTranslation(null);
        float[] offsets = vector.toArray(null);

        float scale = readScale();
        for (int iAxis = 0; iAxis < numAxes; iAxis++) {
            float absOffset = FastMath.abs(offsets[iAxis]);
            if (absOffset > scale) {
                scale = absOffset;
            }
        }
        scale = FastMath.clamp(scale, minScale, maxScale);
        float masterValue = FastMath.log(scale, masterBase);
        masterSlider.setValue(masterValue);

        for (int iAxis = 0; iAxis < numAxes; iAxis++) {
            float value = offsets[iAxis];
            sliders[iAxis].setValue(value / scale);

            String axisName = axisNames[iAxis];
            String sliderPrefix = axisName + "Off";
            Maud.gui.updateSliderStatus(sliderPrefix, value, " bu");
        }
    }
}
