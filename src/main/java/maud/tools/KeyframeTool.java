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

import java.util.logging.Logger;
import jme3utilities.nifty.BasicScreenController;
import jme3utilities.nifty.WindowController;
import maud.Maud;

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

        String indexText, timeText;
        int numKeyframes = Maud.model.target.track.countKeyframes();
        if (numKeyframes == 0) {
            if (Maud.model.target.bone.hasTrack()) {
                indexText = "no keyframes";
                float time = Maud.model.target.animation.getTime();
                timeText = String.format("%.3f", time);
            } else {
                indexText = "no track";
                timeText = "n/a";
            }

        } else {
            int index = Maud.model.target.track.findKeyframe();
            if (index == -1) {
                if (numKeyframes == 1) {
                    indexText = "one keyframe";
                } else {
                    indexText = String.format("%d keyframes", numKeyframes);
                }
            } else {
                indexText = String.format("#%d of %d", index + 1,
                        numKeyframes);
            }

            float time = Maud.model.target.animation.getTime();
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
     * Update the delete/insert buttons.
     */
    private void updateEditButtons() {
        String dButton = "";
        String iButton = "";
        String wButton = "";

        if (Maud.model.target.track.isTrackSelected()) {
            int index = Maud.model.target.track.findKeyframe();
            if (index == -1) {
                iButton = "Insert";
            } else if (index > 0) {
                dButton = "Delete";
            }
            wButton = "Wrap track";
        }

        Maud.gui.setButtonLabel("deleteSingleKeyframeButton", dButton);
        Maud.gui.setButtonLabel("insertSingleKeyframeButton", iButton);
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

        int numKeyframes = Maud.model.target.track.countKeyframes();
        if (numKeyframes > 0) {
            float time = Maud.model.target.animation.getTime();
            if (time != 0f) {
                firstButton = "First";
            }
            if (time > 0f) {
                previousButton = "Previous";
            }
            float lastKeyframeTime = Maud.model.target.track.lastKeyframeTime();
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
        if (!Maud.model.target.animation.isReal()) {
            trackDescription = "(load an animation)";
        } else if (Maud.model.target.bone.hasTrack()) {
            String boneName = Maud.model.target.bone.getName();
            String animName = Maud.model.target.animation.getName();
            trackDescription = String.format("%s in %s", boneName, animName);
        } else if (Maud.model.target.bone.isSelected()) {
            String boneName = Maud.model.target.bone.getName();
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

        if (Maud.model.target.bone.hasTrack()) {
            int numOffsets = Maud.model.target.track.countTranslations();
            translationCount = String.format("%d", numOffsets);

            int numRotations = Maud.model.target.track.countRotations();
            rotationCount = String.format("%d", numRotations);

            int numScales = Maud.model.target.track.countScales();
            scaleCount = String.format("%d", numScales);
        }

        Maud.gui.setStatusText("trackTranslationCount", translationCount);
        Maud.gui.setStatusText("trackRotationCount", rotationCount);
        Maud.gui.setStatusText("trackScaleCount", scaleCount);
    }
}
