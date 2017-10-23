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

import com.jme3.system.AppSettings;
import java.util.logging.Logger;
import jme3utilities.nifty.BasicScreenController;
import jme3utilities.nifty.WindowController;
import maud.Maud;
import maud.Util;
import maud.model.DisplaySettings;

/**
 * The controller for the "Display-Settings Tool" window in Maud's editor
 * screen.
 *
 * @author Stephen Gold sgold@sonic.net
 */
class DisplaySettingsTool extends WindowController {
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
    DisplaySettingsTool(BasicScreenController screenController) {
        super(screenController, "displaySettingsTool", false);
    }
    // *************************************************************************
    // WindowController methods

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
        AppSettings settings = DisplaySettings.get();
        Maud.gui.setIgnoreGuiChanges(true);

        boolean fullscreen = settings.isFullscreen();
        Maud.gui.setChecked("fullscreen", fullscreen);
        boolean gamma = settings.isGammaCorrection();
        Maud.gui.setChecked("gammaCorrection", gamma);
        boolean vsync = settings.isVSync();
        Maud.gui.setChecked("vsync", vsync);

        int width = settings.getWidth();
        int height = settings.getHeight();
        String resolution = String.format("%d x %d", width, height);
        Maud.gui.setButtonLabel("displayResolutionButton", resolution);

        int numSamples = settings.getSamples();
        String aaDescription = Util.aaDescription(numSamples);
        Maud.gui.setButtonLabel("displayAntiAliasingButton", aaDescription);

        int colorDepth = settings.getBitsPerPixel();
        String cdDescription = String.format("%d bits", colorDepth);
        Maud.gui.setButtonLabel("colorDepthButton", cdDescription);

        int refreshRate = settings.getFrequency();
        String rrDescription;
        if (!fullscreen || refreshRate <= 0) {
            rrDescription = "unknown";
        } else {
            rrDescription = String.format("%d Hz", refreshRate);
        }
        Maud.gui.setButtonLabel("refreshRateButton", rrDescription);

        Maud.gui.setIgnoreGuiChanges(false);
    }
}
