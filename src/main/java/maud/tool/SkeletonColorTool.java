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
import java.util.BitSet;
import java.util.logging.Logger;
import jme3utilities.debug.SkeletonVisualizer;
import jme3utilities.nifty.BasicScreenController;
import jme3utilities.nifty.WindowController;
import maud.Maud;
import maud.model.Cgm;
import maud.model.option.SceneBones;
import maud.model.option.SkeletonOptions;

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
        SkeletonOptions options = Maud.getModel().getScene().getSkeleton();
        ColorRGBA color = Maud.gui.readColorBank("ske");
        options.setLinkColor(color);

        color = Maud.gui.readColorBank("bt");
        options.setTrackedColor(color);

        color = Maud.gui.readColorBank("bnt");
        options.setTracklessColor(color);
    }

    /**
     * Update a skeleton visualizer based on the MVC model.
     *
     * @param modelCgm which CG model's view to update (not null)
     */
    void updateVisualizer(Cgm modelCgm) {
        SkeletonVisualizer visualizer;
        visualizer = modelCgm.getSceneView().getSkeletonVisualizer();
        if (visualizer == null) {
            return;
        }

        SkeletonOptions options = Maud.getModel().getScene().getSkeleton();
        ColorRGBA color = options.copyLinkColor(null);
        visualizer.setLineColor(color);

        color = options.copyTracklessColor(null); // TODO avoid extra garbage
        visualizer.setPointColor(color);

        BitSet influencers = null;
        SceneBones sceneBones = options.bones();
        if (sceneBones == SceneBones.InfluencersOnly) {
            influencers = modelCgm.getSkeleton().listInfluencers(null);
        }

        options.copyTrackedColor(color);
        int numBones = modelCgm.getSkeleton().countBones();
        for (int boneIndex = 0; boneIndex < numBones; boneIndex++) {
            if (sceneBones == SceneBones.InfluencersOnly
                    && !influencers.get(boneIndex)) {
                ColorRGBA invisible = new ColorRGBA(0f, 0f, 0f, 0f);
                visualizer.setPointColor(boneIndex, invisible);
            } else if (modelCgm.getAnimation().isRetargetedPose()) {
                String name = modelCgm.getSkeleton().getBoneName(boneIndex);
                if (Maud.getModel().getMap().isBoneMapped(name)) {
                    visualizer.setPointColor(boneIndex, color);
                }
            } else if (modelCgm.getAnimation().hasTrackForBone(boneIndex)) {
                visualizer.setPointColor(boneIndex, color);
            } // else defaults to trackless/unmapped color
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

        SkeletonOptions options = Maud.getModel().getScene().getSkeleton();
        ColorRGBA color = options.copyLinkColor(null);
        Maud.gui.setColorBank("ske", color);

        color = options.copyTrackedColor(null);
        Maud.gui.setColorBank("bt", color);

        options.copyTracklessColor(color);
        Maud.gui.setColorBank("bnt", color);

        Maud.gui.setIgnoreGuiChanges(false);
    }
}
