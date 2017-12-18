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
import jme3utilities.nifty.SliderTransform;
import jme3utilities.nifty.WindowController;
import maud.EditorScreen;
import maud.Maud;
import maud.model.option.scene.PlatformType;
import maud.model.option.scene.SceneOptions;

/**
 * The controller for the "Platform Tool" window in Maud's editor screen.
 *
 * @author Stephen Gold sgold@sonic.net
 */
class PlatformTool extends WindowController {
    // *************************************************************************
    // constants and loggers

    /**
     * message logger for this class
     */
    final private static Logger logger
            = Logger.getLogger(PlatformTool.class.getName());
    /**
     * transform for the diameter slider
     */
    final private static SliderTransform diameterSt = SliderTransform.Log10;
    // *************************************************************************
    // constructors

    /**
     * Instantiate an uninitialized controller.
     *
     * @param screenController
     */
    PlatformTool(EditorScreen screenController) {
        super(screenController, "platformTool", false);
    }
    // *************************************************************************
    // new methods exposed

    /**
     * Update the MVC model based on the sliders.
     */
    void onSliderChanged() {
        float diameter = Maud.gui.readSlider("platformDiameter", diameterSt);
        Maud.getModel().getScene().setPlatformDiameter(diameter);
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
        SceneOptions options = Maud.getModel().getScene();

        PlatformType type = options.getPlatformType();
        String tButton = type.toString();
        Maud.gui.setButtonLabel("platformTypeButton", tButton);

        float diameter = options.getPlatformDiameter();
        Maud.gui.setSlider("platformDiameter", diameterSt, diameter);
        Maud.gui.updateSliderStatus("platformDiameter", diameter, "wu");

        Maud.gui.setIgnoreGuiChanges(false);
    }
}
