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
package maud;

import com.jme3.app.Application;
import com.jme3.app.state.AppStateManager;
import com.jme3.math.Transform;
import com.jme3.math.Vector3f;
import de.lessvoid.nifty.controls.Slider;
import java.util.logging.Logger;
import jme3utilities.nifty.BasicScreenController;
import jme3utilities.nifty.WindowController;

/**
 * The controller for the "Bone-Scale Tool" window in Maud's "3D View" screen.
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
    final private static Logger logger = Logger.getLogger(
            BoneScaleTool.class.getName());
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
     * bone scale factors
     */
    final private static float[] scales = new float[numAxes];
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
    BoneScaleTool(BasicScreenController screenController) {
        super(screenController, "boneScaleTool", false);
    }
    // *************************************************************************
    // new methods exposed

    /**
     * If sliders are enabled, set all 3 scale factors to 1.
     */
    void reset() {
        if (Maud.model.bone.isBoneSelected() && !Maud.model.animation.isRunning()) {
            for (int iAxis = 0; iAxis < numAxes; iAxis++) {
                sliders[iAxis].enable();
                sliders[iAxis].setValue(1f);
            }
        }
    }

    /**
     * If sliders are enabled, set all 3 sliders to match the selected bone.
     */
    void setSliders() {
        if (Maud.model.bone.isBoneSelected() && !Maud.model.animation.isRunning()) {
            scales();
            for (int iAxis = 0; iAxis < numAxes; iAxis++) {
                float scale = scales[iAxis];
                sliders[iAxis].setValue(scale);
            }
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
            Slider slider = Maud.gui.getSlider(axisName + "Sca");
            assert slider != null;
            sliders[iAxis] = slider;
        }
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

        if (Maud.model.bone.isBoneSelected()) {
            float newTime = Maud.model.animation.getTime();
            if (newTime == previousUpdateTime) {
                Maud.gui.setButtonLabel("resetScaButton", "Reset");
                /*
                 * Read and apply scales from sliders.
                 */
                for (int iAxis = 0; iAxis < numAxes; iAxis++) {
                    sliders[iAxis].enable();
                    scales[iAxis] = updateStatus(iAxis);
                }
                Vector3f scale = new Vector3f(
                        scales[0], scales[1], scales[2]);
                Maud.model.cgm.setBoneScale(scale);

            } else {
                Maud.gui.setButtonLabel("resetScaButton", "");
                previousUpdateTime = newTime;
                /*
                 * Display scale factors from animation.
                 */
                scales();
                for (int iAxis = 0; iAxis < numAxes; iAxis++) {
                    sliders[iAxis].disable();
                    float scale = scales[iAxis];
                    sliders[iAxis].setValue(scale);
                    updateStatus(iAxis);
                }
            }

        } else {
            /*
             * No bone selected: clear the display.
             */
            Maud.gui.setButtonLabel("resetScaButton", "");
            for (int iAxis = 0; iAxis < numAxes; iAxis++) {
                sliders[iAxis].disable();
                sliders[iAxis].setValue(1f);

                String axisName = axisNames[iAxis];
                String statusName = axisName + "ScaSliderStatus";
                Maud.gui.setStatusText(statusName, "");
            }
        }
    }
    // *************************************************************************
    // private methods

    /**
     * Calculate scale factors of the selected bone.
     */
    private void scales() {
        int boneIndex = Maud.model.bone.getIndex();
        Transform transform = Maud.model.pose.copyTransform(boneIndex, null);
        Vector3f scale = transform.getScale(null);
        scale.toArray(scales);
    }

    /**
     * Update the status label of the slider for the specified axis.
     *
     * @param iAxis index of the axis (&ge;0, &lt;3)
     * @return slider value (&ge;0)
     */
    private float updateStatus(int iAxis) {
        String axisName = axisNames[iAxis];
        String sliderPrefix = axisName + "Sca";
        float scale = Maud.gui.updateSlider(sliderPrefix, "x");

        assert scale >= 0f : scale;
        return scale;
    }
}
