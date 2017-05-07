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

import com.jme3.math.FastMath;
import de.lessvoid.nifty.controls.Slider;
import java.util.logging.Logger;
import jme3utilities.debug.AxesControl;
import jme3utilities.nifty.BasicScreenController;
import jme3utilities.nifty.WindowController;
import maud.model.AxesStatus;

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
     * which AxesControl is active in the view, or null for none
     */
    private AxesControl control = null;
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
     * Update the MVC model based on the sliders.
     */
    void onSliderChanged() {
        float value = Maud.gui.readSlider("axesLength");
        float axesLength = FastMath.pow(10f, value);
        Maud.model.axes.setLength(axesLength);

        float lineWidth = Maud.gui.readSlider("axesLineWidth");
        Maud.model.axes.setLineWidth(lineWidth);
    }

    /**
     * Update the view's AxesControl.
     */
    void updateAxesControl() {
        String mode = Maud.model.axes.getMode();
        AxesControl newControl;
        switch (mode) {
            case "bone":
                if (Maud.model.bone.isBoneSelected()) {
                    int boneIndex = Maud.model.bone.getIndex();
                    newControl = Maud.viewState.getBoneAxesControl(boneIndex);
                } else {
                    newControl = null;
                }
                break;
            case "model":
                newControl = Maud.viewState.getModelAxesControl();
                break;
            case "none":
                newControl = null;
                break;
            case "world":
                newControl = Maud.viewState.getWorldAxesControl();
                break;
            default:
                throw new IllegalArgumentException();
        }

        if (newControl != control) {
            if (control != null) {
                control.setEnabled(false);
            }
            control = newControl;
        }

        if (control != null) {
            boolean depthTestFlag = Maud.model.axes.getDepthTestFlag();
            float axisLength = Maud.model.axes.getLength();
            float lineWidth = Maud.model.axes.getLineWidth();

            control.setAxisLength(axisLength);
            control.setDepthTest(depthTestFlag);
            control.setEnabled(true);
            control.setLineWidth(lineWidth);
        }
    }
    // *************************************************************************
    // AppState methods

    /**
     * Callback to update this window prior to rendering. (Invoked once per
     * render pass.)
     *
     * @param elapsedTime time interval between render passes (in seconds,
     * &ge;0)
     */
    @Override
    public void update(float elapsedTime) {
        super.update(elapsedTime);

        float axesLength = Maud.model.axes.getLength();
        float value = FastMath.log(axesLength, 10f);
        Slider slider = Maud.gui.getSlider("axesLength");
        slider.setValue(value);

        boolean depthTestFlag = Maud.model.axes.getDepthTestFlag();
        Maud.gui.setChecked("axesDepthTest", depthTestFlag);

        float lineWidth = Maud.model.axes.getLineWidth();
        slider = Maud.gui.getSlider("axesLineWidth");
        slider.setValue(lineWidth);

        updateLabels();
    }
    // *************************************************************************
    // private methods

    /**
     * Update the status labels based on the MVC model.
     */
    private void updateLabels() {
        AxesStatus model = Maud.model.axes;

        String mode = model.getMode();
        String units;
        switch (mode) {
            case "bone":
                if (Maud.model.bone.isBoneSelected()) {
                    units = " bone units";
                } else {
                    units = " units";
                }
                break;
            case "model":
                units = " model units";
                break;
            case "none":
                units = " units";
                break;
            case "world":
                units = " world units";
                break;
            default:
                throw new IllegalStateException();
        }

        float axesLength = model.getLength();
        Maud.gui.updateSliderStatus("axesLength", axesLength, units);

        float lineWidth = model.getLineWidth();
        Maud.gui.updateSliderStatus("axesLineWidth", lineWidth, " pixels");
    }
}
