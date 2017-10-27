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

import de.lessvoid.nifty.controls.Slider;
import java.util.logging.Logger;
import jme3utilities.debug.SkeletonVisualizer;
import jme3utilities.nifty.BasicScreenController;
import jme3utilities.nifty.WindowController;
import maud.Maud;
import maud.model.Cgm;
import maud.model.option.ShowBones;
import maud.model.option.scene.SkeletonOptions;

/**
 * The controller for the "Skeleton Tool" window in Maud's editor screen.
 *
 * @author Stephen Gold sgold@sonic.net
 */
class SkeletonTool extends WindowController {
    // *************************************************************************
    // constants and loggers

    /**
     * message logger for this class
     */
    final private static Logger logger
            = Logger.getLogger(SkeletonTool.class.getName());
    // *************************************************************************
    // constructors

    /**
     * Instantiate an uninitialized controller.
     *
     * @param screenController
     */
    SkeletonTool(BasicScreenController screenController) {
        super(screenController, "skeletonTool", false);
    }
    // *************************************************************************
    // new methods exposed

    /**
     * Update the MVC model based on the sliders.
     */
    void onSliderChanged() {
        SkeletonOptions options = Maud.getModel().getScene().getSkeleton();
        float lineWidth = Maud.gui.readSlider("skeletonLineWidth");
        options.setLineWidth(lineWidth);

        float pointSize = Maud.gui.readSlider("skeletonPointSize");
        options.setPointSize(pointSize);
    }

    /**
     * Update a skeleton visualizer based on the MVC model.
     *
     * @param modelCgm which C-G model's view to update (not null)
     */
    void updateVisualizer(Cgm modelCgm) {
        SkeletonVisualizer visualizer;
        visualizer = modelCgm.getSceneView().getSkeletonVisualizer();
        if (visualizer == null) {
            return;
        }
        SkeletonOptions options = Maud.getModel().getScene().getSkeleton();
        ShowBones showBones = options.getShowBones();
        visualizer.setEnabled(showBones != ShowBones.None);

        float lineWidth = options.getLineWidth();
        visualizer.setLineWidth(lineWidth);

        if (visualizer.supportsPointSize()) {
            float pointSize = options.getPointSize();
            visualizer.setPointSize(pointSize);
        }
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
        Maud.gui.setIgnoreGuiChanges(true);

        SkeletonOptions options = Maud.getModel().getScene().getSkeleton();
        ShowBones showBones = options.getShowBones();
        String bLabel = showBones.toString();
        Maud.gui.setButtonLabel("skeletonShowBonesButton", bLabel);

        float lineWidth = options.getLineWidth();
        Slider slider = Maud.gui.getSlider("skeletonLineWidth");
        slider.setValue(lineWidth);
        lineWidth = Math.round(lineWidth);
        Maud.gui.updateSliderStatus("skeletonLineWidth", lineWidth, " pixels");

        float pointSize = options.getPointSize();
        slider = Maud.gui.getSlider("skeletonPointSize");
        slider.setValue(pointSize);
        pointSize = Math.round(pointSize);
        Maud.gui.updateSliderStatus("skeletonPointSize", pointSize, " pixels");

        Maud.gui.setIgnoreGuiChanges(false);
    }
}
