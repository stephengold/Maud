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

import com.jme3.math.ColorRGBA;
import com.jme3.scene.Spatial;
import de.lessvoid.nifty.controls.Slider;
import java.util.logging.Logger;
import jme3utilities.debug.BoundsVisualizer;
import jme3utilities.nifty.BasicScreenController;
import jme3utilities.nifty.WindowController;
import maud.Maud;
import maud.model.BoundsOptions;
import maud.model.LoadedCgm;
import maud.view.SceneView;

/**
 * The controller for the "Bounds Tool" window in Maud's editor screen.
 *
 * @author Stephen Gold sgold@sonic.net
 */
class BoundsTool extends WindowController {
    // *************************************************************************
    // constants and loggers

    /**
     * message logger for this class
     */
    final private static Logger logger = Logger.getLogger(
            BoundsTool.class.getName());
    // *************************************************************************
    // constructors

    /**
     * Instantiate an uninitialized controller.
     *
     * @param screenController
     */
    BoundsTool(BasicScreenController screenController) {
        super(screenController, "boundsTool", false);
    }
    // *************************************************************************
    // new methods exposed

    /**
     * Update the MVC model based on the sliders.
     */
    void onSliderChanged() {
        BoundsOptions options = Maud.getModel().getScene().getBounds();
        float lineWidth = Maud.gui.readSlider("boundsLineWidth");
        options.setLineWidth(lineWidth);

        ColorRGBA color = Maud.gui.readColorBank("bounds");
        options.setColor(color);
    }

    /**
     * Update a CG model's visualizer based on the MVC model.
     *
     * @param cgm which CG model (not null)
     */
    void updateVisualizer(LoadedCgm cgm) {
        SceneView sceneView = cgm.getSceneView();
        BoundsVisualizer visualizer = sceneView.getBoundsVisualizer();
        visualizer.setEnabled(true);

        BoundsOptions options = Maud.getModel().getScene().getBounds();
        ColorRGBA color = options.copyColor(null);
        visualizer.setColor(color);

        boolean depthTestFlag = options.getDepthTestFlag();
        visualizer.setDepthTest(depthTestFlag);

        float lineWidth = options.getLineWidth();
        visualizer.setLineWidth(lineWidth);

        Spatial selectedSpatial = sceneView.selectedSpatial();
        visualizer.setSubject(selectedSpatial);
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
        BoundsOptions options = Maud.getModel().getScene().getBounds();
        Maud.gui.setIgnoreGuiChanges(true);

        ColorRGBA color = options.copyColor(null);
        Maud.gui.setColorBank("bounds", color);

        boolean depthTestFlag = options.getDepthTestFlag();
        Maud.gui.setChecked("boundsDepthTest", depthTestFlag);

        Slider slider = Maud.gui.getSlider("boundsLineWidth");
        float lineWidth = options.getLineWidth();
        slider.setValue(lineWidth);
        lineWidth = Math.round(lineWidth);
        Maud.gui.updateSliderStatus("boundsLineWidth", lineWidth, " pixels");

        Maud.gui.setIgnoreGuiChanges(false);
    }
}
