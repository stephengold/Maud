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
import jme3utilities.nifty.BasicScreenController;
import jme3utilities.nifty.WindowController;
import maud.Maud;
import maud.model.EditableCgm;
import maud.model.LoadedCgm;
import maud.model.SelectedBone;
import maud.model.SelectedTrack;

/**
 * The controller for the "Keyframe Tool" window in Maud's editor screen.
 *
 * @author Stephen Gold sgold@sonic.net
 */
class KeyframeTool extends WindowController {
    // *************************************************************************
    // constants and loggers

    /**
     * message logger for this class
     */
    final private static Logger logger = Logger.getLogger(
            KeyframeTool.class.getName());
    // *************************************************************************
    // constructors

    /**
     * Instantiate an uninitialized controller.
     *
     * @param screenController
     */
    KeyframeTool(BasicScreenController screenController) {
        super(screenController, "keyframeTool", false);
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

        EditableCgm target = Maud.getModel().getTarget();
        String indexText, timeText;
        int numKeyframes = target.getTrack().countKeyframes();
        if (numKeyframes == 0) {
            if (target.getBone().hasTrack()) {
                indexText = "no keyframes";
                float time = target.getAnimation().getTime();
                timeText = String.format("%.3f", time);
            } else {
                indexText = "no track";
                timeText = "n/a";
            }

        } else {
            int index = target.getTrack().findKeyframeIndex();
            if (index == -1) {
                if (numKeyframes == 1) {
                    indexText = "one keyframe";
                } else {
                    indexText = String.format("%d keyframes", numKeyframes);
                }
            } else {
                int indexBase = Maud.getModel().getMisc().getIndexBase();
                indexText = String.format("#%d of %d", index + indexBase,
                        numKeyframes);
            }

            float time = target.getAnimation().getTime();
            timeText = String.format("%.3f", time);
        }

        Maud.gui.setStatusText("keyframeIndex", indexText);
        Maud.gui.setStatusText("keyframeTime", timeText);

        updateEditButtons();
        updateNavigationButtons();
        updateTrackDescription();
        updateTransforms();
    }
    // *************************************************************************
    // private methods

    /**
     * Update the delete/insert/set-to-pose/wrap buttons.
     */
    private void updateEditButtons() {
        String dButton = "";
        String iButton = "";
        String sButton = "";
        String wButton = "";

        SelectedTrack track = Maud.getModel().getTarget().getTrack();
        if (track.isTrackSelected()) {
            int index = track.findKeyframeIndex();
            if (index == -1) {
                iButton = "Insert";
            } else {
                iButton = "Replace";
            }
            if (index > 0) {
                dButton = "Delete";
            }
            sButton = "Set all to pose";
            wButton = "Wrap track";
        }

        Maud.gui.setButtonLabel("deleteSingleKeyframeButton", dButton);
        Maud.gui.setButtonLabel("insertSingleKeyframeButton", iButton);
        Maud.gui.setButtonLabel("rotationsToPoseKeyframeButton", sButton);
        Maud.gui.setButtonLabel("scalesToPoseKeyframeButton", sButton);
        Maud.gui.setButtonLabel("translationsToPoseKeyframeButton", sButton);
        Maud.gui.setButtonLabel("wrapTrackButton", wButton);
    }

    /**
     * Update the 4 navigation buttons.
     */
    private void updateNavigationButtons() {
        String firstButton = "";
        String previousButton = "";
        String nextButton = "";
        String lastButton = "";

        LoadedCgm target = Maud.getModel().getTarget();
        int numKeyframes = target.getTrack().countKeyframes();
        if (numKeyframes > 0) {
            float time = target.getAnimation().getTime();
            if (time != 0f) {
                firstButton = "First";
            }
            if (time > 0f) {
                previousButton = "Previous";
            }
            float lastKeyframeTime = target.getTrack().lastKeyframeTime();
            if (time < lastKeyframeTime) {
                nextButton = "Next";
            }
            if (time != lastKeyframeTime) {
                lastButton = "Last";
            }
        }

        Maud.gui.setButtonLabel("firstKeyframeButton", firstButton);
        Maud.gui.setButtonLabel("previousKeyframeButton", previousButton);
        Maud.gui.setButtonLabel("nextKeyframeButton", nextButton);
        Maud.gui.setButtonLabel("lastKeyframeButton", lastButton);
    }

    /**
     * Update the track description.
     */
    private void updateTrackDescription() {
        String trackDescription;

        LoadedCgm target = Maud.getModel().getTarget();
        SelectedBone bone = target.getBone();
        if (!target.getAnimation().isReal()) {
            trackDescription = "(load an animation)";
        } else if (bone.hasTrack()) {
            String boneName = bone.getName();
            String animName = target.getAnimation().getName();
            trackDescription = String.format("%s in %s", boneName, animName);
        } else if (bone.isSelected()) {
            String boneName = bone.getName();
            trackDescription = String.format("none for %s", boneName);
        } else {
            trackDescription = "(select a bone)";
        }

        Maud.gui.setStatusText("trackDescription", " " + trackDescription);
    }

    /**
     * Update transform information.
     */
    private void updateTransforms() {
        String translationCount = "";
        String rotationCount = "";
        String scaleCount = "";

        LoadedCgm target = Maud.getModel().getTarget();
        if (target.getBone().hasTrack()) {
            SelectedTrack track = target.getTrack();
            int numOffsets = track.countTranslations();
            translationCount = String.format("%d", numOffsets);

            int numRotations = track.countRotations();
            rotationCount = String.format("%d", numRotations);

            int numScales = track.countScales();
            scaleCount = String.format("%d", numScales);
        }

        Maud.gui.setStatusText("trackTranslationCount", translationCount);
        Maud.gui.setStatusText("trackRotationCount", rotationCount);
        Maud.gui.setStatusText("trackScaleCount", scaleCount);
    }
}
