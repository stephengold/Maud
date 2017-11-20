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

import java.util.logging.Logger;
import jme3utilities.nifty.BasicScreenController;
import jme3utilities.nifty.SliderTransform;
import jme3utilities.nifty.WindowController;
import maud.Maud;
import maud.model.option.scene.AxesDragEffect;
import maud.model.option.scene.AxesOptions;
import maud.model.option.scene.AxesSubject;

/**
 * The controller for the "Axes Tool" window in Maud's editor screen.
 *
 * @author Stephen Gold sgold@sonic.net
 */
class AxesTool extends WindowController {
    // *************************************************************************
    // constants and loggers

    /**
     * message logger for this class
     */
    final private static Logger logger
            = Logger.getLogger(AxesTool.class.getName());
    /**
     * transform for the width slider
     */
    final private static SliderTransform widthSt = SliderTransform.None;
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
     * Update the MVC model based on the slider.
     */
    void onSliderChanged() {
        float lineWidth = Maud.gui.readSlider("axesLineWidth", widthSt);
        Maud.getModel().getScene().getAxes().setLineWidth(lineWidth);
    }
    // *************************************************************************
    // WindowController methods

    /**
     * Callback to update this window prior to rendering. (Invoked once per
     * render pass while the window is displayed.)
     *
     * @param elapsedTime time interval between render passes (in seconds,
     * &ge;0)
     */
    @Override
    public void update(float elapsedTime) {
        super.update(elapsedTime);

        AxesOptions options = Maud.getModel().getScene().getAxes();
        boolean depthTestFlag = options.getDepthTestFlag();
        Maud.gui.setChecked("axesDepthTest", depthTestFlag);

        float lineWidth = options.getLineWidth();
        Maud.gui.setSlider("axesLineWidth", widthSt, lineWidth);

        updateLabels();
    }
    // *************************************************************************
    // private methods

    /**
     * Update buttons and status labels based on the MVC model.
     */
    private void updateLabels() {
        AxesOptions options = Maud.getModel().getScene().getAxes();

        float lineWidth = options.getLineWidth();
        lineWidth = Math.round(lineWidth);
        Maud.gui.updateSliderStatus("axesLineWidth", lineWidth, " pixels");

        AxesSubject subject = options.getMode();
        String buttonLabel = subject.toString();
        Maud.gui.setButtonLabel("axesSubjectButton", buttonLabel);

        AxesDragEffect effect = options.getDragEffect();
        buttonLabel = effect.toString();
        Maud.gui.setButtonLabel("axesDragButton", buttonLabel);
    }
}
