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

import com.jme3.math.ColorRGBA;
import java.util.logging.Logger;
import jme3utilities.debug.SkeletonDebugControl;
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
        ColorRGBA color = Maud.gui.readColorBank("ske");
        Maud.model.skeleton.setLinkColor(color);

        color = Maud.gui.readColorBank("bt");
        Maud.model.skeleton.setTrackedColor(color);

        color = Maud.gui.readColorBank("bnt");
        Maud.model.skeleton.setTracklessColor(color);
    }

    /**
     * Update a SkeletonDebugControl based on the MVC model. TODO rename
     * updateVisualizer
     *
     * @param modelCgm which CG model's view to update (not null)
     */
    void updateSdc(LoadedCgm modelCgm) {
        SkeletonDebugControl control;
        control = modelCgm.getView().getSkeletonDebugControl();
        if (control == null) {
            return;
        }
        SkeletonStatus model = Maud.model.skeleton;

        ColorRGBA color = model.copyLinkColor(null);
        control.setLineColor(color);

        color = model.copyTracklessColor(null); // TODO avoid extra garbage
        control.setPointColor(color);

        model.copyTrackedColor(color);
        int numBones = modelCgm.bones.countBones();
        for (int boneIndex = 0; boneIndex < numBones; boneIndex++) {
            if (modelCgm.animation.isRetargetedPose()) {
                String name = modelCgm.bones.getBoneName(boneIndex);
                if (Maud.model.mapping.isBoneMapped(name)) {
                    control.setPointColor(boneIndex, color);
                }
            } else if (modelCgm.animation.hasTrackForBone(boneIndex)) {
                control.setPointColor(boneIndex, color);
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
        SkeletonStatus model = Maud.model.skeleton;
        Maud.gui.setIgnoreGuiChanges(true);

        ColorRGBA color = model.copyLinkColor(null);
        Maud.gui.setColorBank("ske", color);

        color = model.copyTrackedColor(null);
        Maud.gui.setColorBank("bt", color);

        model.copyTracklessColor(color);
        Maud.gui.setColorBank("bnt", color);

        Maud.gui.setIgnoreGuiChanges(false);
    }
}
