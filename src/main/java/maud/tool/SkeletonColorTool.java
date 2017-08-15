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
import java.util.logging.Logger;
import jme3utilities.debug.SkeletonVisualizer;
import jme3utilities.nifty.BasicScreenController;
import jme3utilities.nifty.WindowController;
import maud.Maud;
import maud.model.LoadedCgm;
import maud.model.SkeletonStatus;

/**
 * The controller for the "Skeleton Color Tool" window in Maud's editor screen.
 *
 * @author Stephen Gold sgold@sonic.net
 */
class SkeletonColorTool extends WindowController {
    // *************************************************************************
    // constants and loggers

    /**
     * message logger for this class
     */
    final private static Logger logger = Logger.getLogger(
            SkeletonColorTool.class.getName());
    // *************************************************************************
    // constructors

    /**
     * Instantiate an uninitialized controller.
     *
     * @param screenController
     */
    SkeletonColorTool(BasicScreenController screenController) {
        super(screenController, "skeletonColorTool", false);
    }
    // *************************************************************************
    // new methods exposed

    /**
     * Update the MVC model based on the sliders.
     */
    void onSliderChanged() {
        SkeletonStatus status = Maud.getModel().scene.getSkeleton();
        ColorRGBA color = Maud.gui.readColorBank("ske");
        status.setLinkColor(color);

        color = Maud.gui.readColorBank("bt");
        status.setTrackedColor(color);

        color = Maud.gui.readColorBank("bnt");
        status.setTracklessColor(color);
    }

    /**
     * Update a skeleton visualizer based on the MVC model.
     *
     * @param modelCgm which CG model's view to update (not null)
     */
    void updateVisualizer(LoadedCgm modelCgm) {
        SkeletonVisualizer visualizer;
        visualizer = modelCgm.getSceneView().getSkeletonVisualizer();
        if (visualizer == null) {
            return;
        }

        SkeletonStatus status = Maud.getModel().scene.getSkeleton();
        ColorRGBA color = status.copyLinkColor(null);
        visualizer.setLineColor(color);

        color = status.copyTracklessColor(null); // TODO avoid extra garbage
        visualizer.setPointColor(color);

        status.copyTrackedColor(color);
        int numBones = modelCgm.bones.countBones();
        for (int boneIndex = 0; boneIndex < numBones; boneIndex++) {
            if (modelCgm.animation.isRetargetedPose()) {
                String name = modelCgm.bones.getBoneName(boneIndex);
                if (Maud.getModel().getMap().isBoneMapped(name)) {
                    visualizer.setPointColor(boneIndex, color);
                }
            } else if (modelCgm.animation.hasTrackForBone(boneIndex)) {
                visualizer.setPointColor(boneIndex, color);
            }
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
        Maud.gui.setIgnoreGuiChanges(true);

        SkeletonStatus status = Maud.getModel().scene.getSkeleton();
        ColorRGBA color = status.copyLinkColor(null);
        Maud.gui.setColorBank("ske", color);

        color = status.copyTrackedColor(null);
        Maud.gui.setColorBank("bt", color);

        status.copyTracklessColor(color);
        Maud.gui.setColorBank("bnt", color);

        Maud.gui.setIgnoreGuiChanges(false);
    }
}
