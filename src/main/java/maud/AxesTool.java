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

import java.util.logging.Logger;
import jme3utilities.debug.AxesControl;
import jme3utilities.nifty.BasicScreenController;

/**
 * The controller for the "Axes Tool" window in Maud's "3D View" screen.
 *
 * @author Stephen Gold sgold@sonic.net
 */
class AxesTool extends WindowController {
    // *************************************************************************
    // constants and loggers

    /**
     * message logger for this class
     */
    final private static Logger logger = Logger.getLogger(
            AxesTool.class.getName());
    // *************************************************************************
    // fields

    /**
     * which control is active, or null for none
     */
    private AxesControl control = null;
    /**
     * flag to enable depth test for axes
     */
    private boolean depthTestFlag = false;
    /**
     * length of axes (units depend on mode, &ge;0)
     */
    private float axisLength = 1f;
    /**
     * line width for axes (in pixels, &ge;1)
     */
    private float lineWidth = 1f;
    /**
     * which set of axes is active (either "none", "world", "model", or "bone")
     */
    private String mode = "bone";
    // *************************************************************************
    // constructors

    /**
     * Instantiate an uninitialized controller.
     *
     * @param screenController the controller of the screen that contains the
     * window (not null)
     */
    AxesTool(BasicScreenController screenController) {
        super(screenController, "axesTool", false);
    }
    // *************************************************************************
    // new methods exposed

    /**
     * Change the mode and update this window.
     */
    void setMode(String newMode) {
        mode = newMode;
        update();
    }

    /**
     * Update this window after a change.
     */
    void update() {
        lineWidth = Maud.gui.updateSlider("axesLineWidth", " pixels");
        depthTestFlag = Maud.gui.isChecked("axesDepthTest");

        AxesControl newControl;
        String units;
        switch (mode) {
            case "bone":
                if (Maud.gui.bone.isBoneSelected()) {
                    int boneIndex = Maud.gui.bone.getBoneIndex();
                    newControl = Maud.viewState.getBoneAxesControl(boneIndex);
                    units = " bone units";
                } else {
                    newControl = null;
                    units = " units";
                }
                break;
            case "model":
                newControl = Maud.viewState.getModelAxesControl();
                units = " model units";
                break;
            case "none":
                newControl = null;
                units = " units";
                break;
            case "world":
                newControl = Maud.viewState.getWorldAxesControl();
                units = " world units";
                break;
            default:
                throw new IllegalArgumentException();
        }
        axisLength = Maud.gui.updateLogSlider("axesLength", 10f, units);

        if (newControl != control) {
            if (control != null) {
                control.setEnabled(false);
            }
            control = newControl;
        }

        if (control != null) {
            control.setAxisLength(axisLength);
            control.setDepthTest(depthTestFlag);
            control.setEnabled(true);
            control.setLineWidth(lineWidth);
        }
    }
}
