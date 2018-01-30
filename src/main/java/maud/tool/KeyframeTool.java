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
import maud.Maud;
import maud.MaudUtil;
import maud.model.cgm.Cgm;
import maud.model.cgm.EditableCgm;
import maud.model.cgm.SelectedBone;
import maud.model.cgm.SelectedTrack;

/**
 * The controller for the "Keyframe" tool in Maud's editor screen.
 *
 * @author Stephen Gold sgold@sonic.net
 */
class KeyframeTool extends Tool {
    // *************************************************************************
    // constants and loggers

    /**
     * message logger for this class
     */
    final private static Logger logger
            = Logger.getLogger(KeyframeTool.class.getName());
    // *************************************************************************
    // constructors

    /**
     * Instantiate an uninitialized tool.
     *
     * @param screenController the controller of the screen that contains the
     * tool (not null)
     */
    KeyframeTool(GuiScreenController screenController) {
        super(screenController, "keyframe");
    }
    // *************************************************************************
    // Tool methods

    /**
     * Callback to update this tool prior to rendering. (Invoked once per render
     * pass while the tool is displayed.)
     */
    @Override
    void toolUpdate() {
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
                indexText = MaudUtil.formatIndex(index);
                indexText = String.format("%s of %d", indexText, numKeyframes);
            }

            float time = target.getAnimation().getTime();
            timeText = String.format("%.3f", time);
        }

        setStatusText("keyframeIndex", indexText);
        setStatusText("keyframeTime", timeText);

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
        }

        setButtonText("deleteSingleKeyframe", dButton);
        setButtonText("insertSingleKeyframe", iButton);
        setButtonText("rotationsToPoseKeyframe", sButton);
        setButtonText("scalesToPoseKeyframe", sButton);
        setButtonText("translationsToPoseKeyframe", sButton);
    }

    /**
     * Update the 4 navigation buttons.
     */
    private void updateNavigationButtons() {
        String firstButton = "";
        String previousButton = "";
        String nearestButton = "";
        String nextButton = "";
        String lastButton = "";

        Cgm target = Maud.getModel().getTarget();
        int numKeyframes = target.getTrack().countKeyframes();
        if (numKeyframes > 0) {
            float time = target.getAnimation().getTime();
            if (time != 0f) {
                firstButton = "First";
            }
            if (time > 0f) {
                previousButton = "Previous";
            }
            int frameIndex = target.getTrack().findKeyframeIndex();
            if (frameIndex == -1) {
                nearestButton = "Nearest";
            }
            float lastKeyframeTime = target.getTrack().lastKeyframeTime();
            if (time < lastKeyframeTime) {
                nextButton = "Next";
            }
            if (time != lastKeyframeTime) {
                lastButton = "Last";
            }
        }

        setButtonText("firstKeyframe", firstButton);
        setButtonText("previousKeyframe", previousButton);
        setButtonText("nearestKeyframe", nearestButton);
        setButtonText("nextKeyframe", nextButton);
        setButtonText("lastKeyframe", lastButton);
    }

    /**
     * Update the track description.
     */
    private void updateTrackDescription() {
        String trackDescription;

        Cgm target = Maud.getModel().getTarget();
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

        setStatusText("trackDescription", " " + trackDescription);
    }

    /**
     * Update transform information.
     */
    private void updateTransforms() {
        String translationCount = "";
        String rotationCount = "";
        String scaleCount = "";

        Cgm target = Maud.getModel().getTarget();
        if (target.getBone().hasTrack()) {
            SelectedTrack track = target.getTrack();
            int numOffsets = track.countTranslations();
            translationCount = String.format("%d", numOffsets);

            int numRotations = track.countRotations();
            rotationCount = String.format("%d", numRotations);

            int numScales = track.countScales();
            scaleCount = String.format("%d", numScales);
        }

        setStatusText("trackTranslationCount", translationCount);
        setStatusText("trackRotationCount", rotationCount);
        setStatusText("trackScaleCount", scaleCount);
    }
}
