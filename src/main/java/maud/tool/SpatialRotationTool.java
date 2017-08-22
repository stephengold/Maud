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
import com.jme3.math.Quaternion;
import de.lessvoid.nifty.controls.Slider;
import java.util.logging.Logger;
import jme3utilities.math.MyMath;
import jme3utilities.nifty.BasicScreenController;
import jme3utilities.nifty.WindowController;
import maud.Maud;
import maud.model.SelectedSpatial;

/**
 * The controller for the "Spatial-Rotation Tool" window in Maud's editor
 * screen.
 *
 * @author Stephen Gold sgold@sonic.net
 */
class SpatialRotationTool extends WindowController {
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
            SpatialRotationTool.class.getName());
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
    // *************************************************************************
    // constructors

    /**
     * Instantiate an uninitialized controller.
     *
     * @param screenController the controller of the screen that contains the
     * window (not null)
     */
    SpatialRotationTool(BasicScreenController screenController) {
        super(screenController, "spatialRotationTool", false);
    }
    // *************************************************************************
    // new methods exposed

    /**
     * If active, update the MVC model based on the sliders.
     */
    void onSliderChanged() {
        float[] angles = new float[numAxes];
        for (int iAxis = 0; iAxis < numAxes; iAxis++) {
            float value = sliders[iAxis].getValue();
            angles[iAxis] = value;
        }
        Quaternion rot = new Quaternion();
        rot.fromAngles(angles);
        Maud.getModel().getTarget().setSpatialRotation(rot);
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
            Slider slider = Maud.gui.getSlider(axisName + "Sa");
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
        Maud.gui.setIgnoreGuiChanges(true);

        setSlidersToTransform();
        String dButton;
        if (Maud.getModel().getMisc().getAnglesInDegrees()) {
            dButton = "radians";
        } else {
            dButton = "degrees";
        }
        Maud.gui.setButtonLabel("degreesButton2", dButton);

        Maud.gui.setIgnoreGuiChanges(false);
    }
    // *************************************************************************
    // private methods

    /**
     * Set all 3 sliders (and their status labels) based on the local rotation
     * of the selected spatial.
     */
    private void setSlidersToTransform() {
        SelectedSpatial spatial = Maud.getModel().getTarget().getSpatial();
        Quaternion rotation = spatial.localRotation(null);
        float[] angles = rotation.toAngles(null);
        boolean degrees = Maud.getModel().getMisc().getAnglesInDegrees();

        for (int iAxis = 0; iAxis < numAxes; iAxis++) {
            float angle = angles[iAxis];
            sliders[iAxis].setValue(angle);

            String axisName = axisNames[iAxis];
            String sliderPrefix = axisName + "Sa";
            if (degrees) {
                angle = MyMath.toDegrees(angle);
                Maud.gui.updateSliderStatus(sliderPrefix, angle, " deg");
            } else {
                Maud.gui.updateSliderStatus(sliderPrefix, angle, " rad");
            }
        }
    }
}
