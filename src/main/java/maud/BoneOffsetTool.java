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
 * Stephen Gold's name may not be used to endorse or promote products
 derived from this software without specific prior written permission.

 THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND
 ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
 WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 DISCLAIMED. IN NO EVENT SHALL STEPHEN GOLD BE LIABLE FOR ANY
 DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES
 (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
 LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND
 ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */
package maud;

import com.jme3.app.Application;
import com.jme3.app.state.AppStateManager;
import com.jme3.math.FastMath;
import com.jme3.math.Transform;
import com.jme3.math.Vector3f;
import de.lessvoid.nifty.controls.Slider;
import java.util.logging.Logger;
import jme3utilities.nifty.BasicScreenController;

/**
 * The controller for the "Bone-Offset Tool" window in Maud's "3D View" screen.
 *
 * @author Stephen Gold sgold@sonic.net
 */
class BoneOffsetTool extends WindowController {
    // *************************************************************************
    // constants and loggers

    /**
     * logarithm base for the master slider
     */
    final private static float masterBase = 10f;
    /**
     * maximum scale for offsets (&gt;0)
     */
    final private static float maxScale = 100f;
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
            BoneOffsetTool.class.getName());
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
    /**
     * bone offsets (in bone units)
     */
    final private static float[] offsets = new float[numAxes];
    /*
     * animation time at previous update (in seconds)
     */
    private float previousUpdateTime = 0f;
    // *************************************************************************
    // constructors

    /**
     * Instantiate an uninitialized controller.
     *
     * @param screenController the controller of the screen that contains the
     * window (not null)
     */
    BoneOffsetTool(BasicScreenController screenController) {
        super(screenController, "boneOffsetTool", false);
    }
    // *************************************************************************
    // new methods exposed

    /**
     * If sliders are enabled, set all 3 offsets to 0.
     */
    void reset() {
        if (Maud.gui.bone.isSelected() && !Maud.gui.animation.isRunning()) {
            for (int iAxis = 0; iAxis < numAxes; iAxis++) {
                sliders[iAxis].enable();
                sliders[iAxis].setValue(0f);
            }
        }
    }

    /**
     * If sliders are enabled, set them to match the selected bone.
     */
    void set() {
        if (Maud.gui.bone.isSelected() && !Maud.gui.animation.isRunning()) {
            displayOffsets();
        }
    }
    // *************************************************************************
    // AppState methods

    /**
     * Initialize this controller prior to its 1st update.
     *
     * @param stateManager (not null)
     * @param application application which owns this screen (not null)
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
        if (!isInitialized()) {
            return;
        }

        if (Maud.gui.bone.isSelected()) {
            float newTime = Maud.gui.animation.getTime();
            if (newTime == previousUpdateTime) {
                Maud.gui.setButtonLabel("resetOffButton", "Reset");
                /*
                 * Read and apply offsets from sliders.
                 */
                for (int iAxis = 0; iAxis < numAxes; iAxis++) {
                    sliders[iAxis].enable();
                    offsets[iAxis] = updateStatus(iAxis);
                }
                Vector3f translation = new Vector3f(
                        offsets[0], offsets[1], offsets[2]);
                Maud.model.setBoneTranslation(translation);

            } else {
                Maud.gui.setButtonLabel("resetOffButton", "");
                previousUpdateTime = newTime;
                /*
                 * Display offsets from animation.
                 */
                for (int iAxis = 0; iAxis < numAxes; iAxis++) {
                    sliders[iAxis].disable();
                }
                displayOffsets();
            }

        } else {
            /*
             * No bone selected: clear the display.
             */
            Maud.gui.setButtonLabel("resetOffButton", "");
            for (int iAxis = 0; iAxis < numAxes; iAxis++) {
                sliders[iAxis].disable();
                sliders[iAxis].setValue(0f);

                String axisName = axisNames[iAxis];
                String statusName = axisName + "SliderStatus";
                Maud.gui.setStatusText(statusName, "");
            }
        }
    }
    // *************************************************************************
    // private methods

    /**
     * Calculate and display the offsets of the selected bone.
     */
    private void displayOffsets() {
        offsets();

        float newScale = readScale();
        for (int iAxis = 0; iAxis < numAxes; iAxis++) {
            float absOffset = FastMath.abs(offsets[iAxis]);
            if (absOffset > newScale) {
                newScale = absOffset;
            }
        }
        newScale = FastMath.clamp(newScale, minScale, maxScale);
        float masterSetting = FastMath.log(newScale, masterBase);
        masterSlider.setValue(masterSetting);

        for (int iAxis = 0; iAxis < numAxes; iAxis++) {
            float offset = offsets[iAxis];
            float setting = offset / newScale;
            sliders[iAxis].setValue(setting);
            updateStatus(iAxis);
        }
    }

    /**
     * Calculate offsets of the selected bone.
     */
    private void offsets() {
        int boneIndex = Maud.gui.bone.getSelectedIndex();
        Transform transform = Maud.gui.animation.copyBoneTransform(boneIndex);
        Vector3f translation = transform.getTranslation(null);
        translation.toArray(offsets);
    }

    /**
     * Read the scale factor from the master slider.
     */
    private float readScale() {
        float reading = masterSlider.getValue();
        float scale = FastMath.pow(masterBase, reading);

        return scale;
    }

    /**
     * Update the status label of the slider for the specified axis.
     *
     * @param iAxis index of the axis (&ge;0, &lt;3)
     * @return slider value (in bone units)
     */
    private float updateStatus(int iAxis) {
        String axisName = axisNames[iAxis];
        String sliderPrefix = axisName + "Off";
        float reading = Maud.gui.readSlider(sliderPrefix);
        float offset = reading * readScale();
        String statusText = String.format("%s = %.3f bu", sliderPrefix, offset);
        String statusName = sliderPrefix + "SliderStatus";
        Maud.gui.setStatusText(statusName, statusText);

        return offset;
    }
}
