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

import com.jme3.shadow.EdgeFilteringMode;
import java.util.logging.Logger;
import jme3utilities.nifty.GuiScreenController;
import jme3utilities.nifty.GuiWindowController;
import jme3utilities.nifty.SliderTransform;
import maud.Maud;
import maud.model.option.scene.RenderOptions;
import maud.model.option.scene.TriangleMode;

/**
 * The controller for the "Render Tool" window in Maud's editor screen.
 *
 * @author Stephen Gold sgold@sonic.net
 */
class RenderTool extends GuiWindowController {
    // *************************************************************************
    // constants and loggers

    /**
     * message logger for this class
     */
    final private static Logger logger
            = Logger.getLogger(RenderTool.class.getName());
    /**
     * transform for the size slider
     */
    final private static SliderTransform sizeSt = SliderTransform.Log2;
    /**
     * transform for the splits slider
     */
    final private static SliderTransform splitsSt = SliderTransform.None;
    // *************************************************************************
    // constructors

    /**
     * Instantiate an uninitialized controller.
     *
     * @param screenController
     */
    RenderTool(GuiScreenController screenController) {
        super(screenController, "renderTool", false);
    }
    // *************************************************************************
    // new methods exposed

    /**
     * Update the MVC model based on the sliders.
     */
    void onSliderChanged() {
        RenderOptions options = Maud.getModel().getScene().getRender();

        float mapSize = readSlider("mapSize", sizeSt);
        int newSize = Math.round(mapSize);
        options.setShadowsMapSize(newSize);

        float mapSplits = readSlider("mapSplits", splitsSt);
        int newNumSplits = Math.round(mapSplits);
        options.setNumSplits(newNumSplits);
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
        RenderOptions options = Maud.getModel().getScene().getRender();

        boolean shadowsFlag = options.areShadowsRendered();
        setChecked("shadows", shadowsFlag);

        boolean renderFlag = options.isPhysicsRendered();
        setChecked("physics", renderFlag);

        TriangleMode mode = options.getTriangleMode();
        String modeName = mode.toString();
        setButtonText("triangles", modeName);

        int numSplits = options.getNumSplits();
        setSlider("mapSplits", splitsSt, numSplits);
        updateSliderStatus("mapSplits", numSplits, "");

        int mapSize = options.getShadowMapSize();
        setSlider("mapSize", sizeSt, mapSize);
        updateSliderStatus("mapSize", mapSize, " px");

        EdgeFilteringMode edgeFilter = options.getEdgeFilter();
        modeName = edgeFilter.toString();
        setButtonText("edgeFilter", modeName);

        Maud.gui.setIgnoreGuiChanges(false);
    }
}
