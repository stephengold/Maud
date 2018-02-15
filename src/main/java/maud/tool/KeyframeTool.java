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
import maud.model.cgm.SelectedTrack;
import maud.model.cgm.TrackItem;

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
        String indexText, timeText;

        EditableCgm target = Maud.getModel().getTarget();
        float time = target.getPlay().getTime();
        int numKeyframes = target.getTrack().countKeyframes();
        if (numKeyframes == 0) {
            if (target.getTrack().isSelected()) {
                indexText = "no keyframes";
                timeText = String.format("%.3f", time);
            } else {
                indexText = "no track";
                timeText = "n/a";
            }
        } else {
            int index = target.getFrame().findIndex();
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
            timeText = String.format("%.3f", time);
        }

        setStatusText("keyframeIndex", indexText);
        setStatusText("keyframeTime", timeText);

        updateEditButtons();
        updateNavigationButtons();
        updateTrackDescription();
    }
    // *************************************************************************
    // private methods

    /**
     * Update the delete/insert/set-to-pose/wrap buttons.
     */
    private void updateEditButtons() {
        String deleteButton = "";
        String insertButton = "";

        Cgm target = Maud.getModel().getTarget();
        if (target.getTrack().isSelected()) {
            int index = target.getFrame().findIndex();
            if (index == -1) {
                insertButton = "Insert";
            } else {
                insertButton = "Replace";
            }
            if (index > 0) {
                deleteButton = "Delete";
            }
        }

        setButtonText("deleteSingleKeyframe", deleteButton);
        setButtonText("insertSingleKeyframe", insertButton);
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
            float time = target.getPlay().getTime();
            if (time != 0f) {
                firstButton = "First";
            }
            if (time > 0f) {
                previousButton = "Previous";
            }
            if (!target.getFrame().isSelected()) {
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
        String status;

        Cgm target = Maud.getModel().getTarget();
        SelectedTrack track = target.getTrack();
        if (!target.getAnimation().isReal()) {
            status = "(load an animation)";
        } else if (track.isSelected()) {
            TrackItem item = track.item();
            status = item.describe();
        } else {
            status = "(select a track)";
        }

        setStatusText("trackDescription2", " " + status);
    }
}
