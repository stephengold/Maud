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

import java.util.logging.Logger;
import jme3utilities.nifty.GuiScreenController;
import jme3utilities.nifty.GuiWindowController;
import maud.Maud;
import maud.MaudUtil;
import maud.model.option.DisplaySettings;

/**
 * The controller for the "Display-Settings Tool" window in Maud's editor
 * screen.
 *
 * @author Stephen Gold sgold@sonic.net
 */
class DisplaySettingsTool extends GuiWindowController {
    // *************************************************************************
    // constants and loggers

    /**
     * message logger for this class
     */
    final private static Logger logger
            = Logger.getLogger(DisplaySettingsTool.class.getName());
    // *************************************************************************
    // constructors

    /**
     * Instantiate an uninitialized controller.
     *
     * @param screenController
     */
    DisplaySettingsTool(GuiScreenController screenController) {
        super(screenController, "displaySettingsTool", false);
    }
    // *************************************************************************
    // GuiWindowController methods

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
        Maud.gui.setIgnoreGuiChanges(true);

        boolean fullscreen = DisplaySettings.isFullscreen();
        setChecked("fullscreen", fullscreen);
        boolean gamma = DisplaySettings.isGammaCorrection();
        setChecked("gammaCorrection", gamma);
        boolean vSync = DisplaySettings.isVSync();
        setChecked("vSync", vSync);

        int width = DisplaySettings.getWidth();
        int height = DisplaySettings.getHeight();
        String dimensionsButton = MaudUtil.describeDimensions(width, height);
        setButtonText("displayDimensions", dimensionsButton);

        int msaaFactor = DisplaySettings.getMsaaFactor();
        String msaaButton = MaudUtil.describeMsaaFactor(msaaFactor);
        setButtonText("displayMsaa", msaaButton);

        String refreshRateButton = "";
        if (fullscreen) {
            int refreshRate = DisplaySettings.getRefreshRate();
            if (refreshRate <= 0) {
                refreshRateButton = "unknown";
            } else {
                refreshRateButton = String.format("%d Hz", refreshRate);
            }
        }
        setButtonText("refreshRate", refreshRateButton);

        int colorDepth = DisplaySettings.getColorDepth();
        String colorDepthButton = String.format("%d bpp", colorDepth);
        setButtonText("colorDepth", colorDepthButton);

        String applyButton = "";
        if (DisplaySettings.canApply() && !DisplaySettings.areApplied()) {
            applyButton = "Apply";
        }
        setButtonText("applyDisplaySettings", applyButton);

        String saveButton = "";
        if (DisplaySettings.areValid() && !DisplaySettings.areSaved()) {
            saveButton = "Save";
        }
        setButtonText("saveDisplaySettings", saveButton);

        Maud.gui.setIgnoreGuiChanges(false);
    }
}
