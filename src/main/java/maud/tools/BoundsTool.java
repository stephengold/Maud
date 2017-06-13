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
package maud.tools;

import com.jme3.app.Application;
import com.jme3.app.state.AppStateManager;
import com.jme3.math.ColorRGBA;
import com.jme3.scene.Spatial;
import de.lessvoid.nifty.controls.Slider;
import java.util.logging.Logger;
import jme3utilities.nifty.BasicScreenController;
import jme3utilities.nifty.WindowController;
import maud.BoundsVisualizer;
import maud.Maud;
import maud.model.BoundsStatus;
import maud.model.LoadedCGModel;

/**
 * The controller for the "Bounds Tool" window in Maud's "3D View" screen.
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
    // fields

    /**
     * SG control to display bounds of the source CG model
     */
    private BoundsVisualizer sourceVisualizer;
    /**
     * SG control to display bounds of the target CG model
     */
    private BoundsVisualizer targetVisualizer;
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
        float lineWidth = Maud.gui.readSlider("boundsLineWidth");
        Maud.model.bounds.setLineWidth(lineWidth);

        ColorRGBA color = Maud.gui.readColorBank("bounds");
        Maud.model.bounds.setColor(color);
    }

    /**
     * Update the BoundsVisualizer settings for both CG models.
     */
    void updateVisualizations() {
        updateBounds(Maud.model.source, sourceVisualizer);
        updateBounds(Maud.model.target, targetVisualizer);
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
        /*
         * Instantiate and add the visualizers.
         */
        sourceVisualizer = new BoundsVisualizer(assetManager);
        rootNode.addControl(sourceVisualizer);

        targetVisualizer = new BoundsVisualizer(assetManager);
        rootNode.addControl(targetVisualizer);
    }

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
        BoundsStatus model = Maud.model.bounds;
        Maud.gui.setIgnoreGuiChanges(true);

        ColorRGBA color = model.copyColor(null);
        Maud.gui.setColorBank("bounds", color);

        boolean depthTestFlag = model.getDepthTestFlag();
        Maud.gui.setChecked("boundsDepthTest", depthTestFlag);

        Slider slider = Maud.gui.getSlider("boundsLineWidth");
        float lineWidth = model.getLineWidth();
        slider.setValue(lineWidth);
        Maud.gui.updateSliderStatus("boundsLineWidth", lineWidth, " pixels");

        Maud.gui.setIgnoreGuiChanges(false);
    }
    // *************************************************************************
    // private methods

    /**
     * Update the BoundsVisualizer settings for a CG model.
     *
     * @param loadedCgm (not null)
     * @param visualizer (not null)
     */
    private void updateBounds(LoadedCGModel loadedCgm,
            BoundsVisualizer visualizer) {
        assert visualizer != null;

        if (loadedCgm.isLoaded()) {
            visualizer.setEnabled(true);

            ColorRGBA color = Maud.model.bounds.copyColor(null);
            visualizer.setColor(color);

            boolean depthTestFlag = Maud.model.bounds.getDepthTestFlag();
            visualizer.setDepthTest(depthTestFlag);

            float lineWidth = Maud.model.bounds.getLineWidth();
            visualizer.setLineWidth(lineWidth);

            Spatial cgmRoot = loadedCgm.view.getCgmRoot();
            Spatial selectedSpatial = loadedCgm.spatial.underRoot(cgmRoot);
            visualizer.setSubject(selectedSpatial);
        } else {
            visualizer.setEnabled(false);
        }
    }
}
