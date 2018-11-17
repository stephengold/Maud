/*
 Copyright (c) 2018, Stephen Gold
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

import com.jme3.animation.AnimControl;
import java.util.logging.Logger;
import jme3utilities.nifty.GuiScreenController;
import maud.DescribeUtil;
import maud.Maud;
import maud.model.cgm.Cgm;
import maud.model.cgm.LoadedAnimation;
import maud.model.cgm.LoadedCgm;
import maud.model.cgm.PlayOptions;
import maud.model.cgm.SelectedAnimControl;
import maud.model.cgm.SelectedTrack;

/**
 * The controller for the "Extract" tool in Maud's editor screen.
 *
 * @author Stephen Gold sgold@sonic.net
 */
class ExtractTool extends Tool {
    // *************************************************************************
    // constants and loggers

    /**
     * message logger for this class
     */
    final private static Logger logger
            = Logger.getLogger(ExtractTool.class.getName());
    // *************************************************************************
    // constructors

    /**
     * Instantiate an uninitialized tool.
     *
     * @param screenController the controller of the screen that will contain
     * the tool (not null)
     */
    ExtractTool(GuiScreenController screenController) {
        super(screenController, "extract");
    }
    // *************************************************************************
    // Tool methods

    /**
     * Update this tool prior to rendering. (Invoked once per frame while this
     * tool is displayed.)
     */
    @Override
    protected void toolUpdate() {
        LoadedCgm target = Maud.getModel().getTarget();

        String animControlButton = "";
        SelectedAnimControl animControl = target.getAnimControl();
        String targetAnimControlName = animControl.name();
        if (targetAnimControlName != null) {
            animControlButton = targetAnimControlName;
        } else {
            int numAnimControls = target.countSgcs(AnimControl.class);
            if (numAnimControls > 0) {
                animControlButton = "( none selected )";
            }
        }
        setButtonText("extractAnimControl", animControlButton);

        LoadedAnimation animation = target.getAnimation();
        String animationButton = animation.name();
        setButtonText("extractAnimation", animationButton);

        String trackButton = "";
        SelectedTrack track = target.getTrack();
        if (track.isSelected()) {
            trackButton = track.item().describe();
        } else if (animation.countTracks() > 0) {
            trackButton = "( none selected )";
        }
        setButtonText("extractTrack", trackButton);

        updateFeedback();
        updateRange();
    }
    // *************************************************************************
    // private methods

    /**
     * Update the feedback line and extract button.
     */
    private void updateFeedback() {
        String extractButton = "";
        String feedback;

        Cgm target = Maud.getModel().getTarget();
        if (!target.getAnimControl().isSelected()) {
            feedback = "select an anim control";
        } else if (!target.getAnimation().isReal()) {
            feedback = "load a real animation";
        } else {
            feedback = "";
            extractButton = "Extract";
        }

        setButtonText("extract", extractButton);
        setStatusText("extractFeedback", feedback);
    }

    /**
     * Update the range of frames/times to extract.
     */
    private void updateRange() {
        String startFrameButton = "";
        String startTimeButton = "";
        String endFrameButton = "";
        String endTimeButton = "";

        LoadedCgm target = Maud.getModel().getTarget();
        LoadedAnimation animation = target.getAnimation();
        SelectedTrack track = target.getTrack();

        if (animation.isReal()) {
            PlayOptions options = target.getPlay();
            float startTime = options.getLowerLimit();
            startTimeButton = String.format("%.3f sec", startTime);

            float upperLimit = options.getUpperLimit();
            float duration = target.getAnimation().duration();
            float endTime = Math.min(upperLimit, duration);
            endTimeButton = String.format("%.3f sec", endTime);

            if (track.isSelected()) {
                int startIndex = track.findKeyframeIndex(startTime);
                if (startIndex >= 0) {
                    startFrameButton
                            = "frame " + DescribeUtil.index(startIndex);
                } else {
                    startFrameButton = "not a keyframe";
                }
                int endIndex = track.findKeyframeIndex(endTime);
                if (endIndex >= 0) {
                    endFrameButton = "frame " + DescribeUtil.index(endIndex);
                } else {
                    endFrameButton = "not a keyframe";
                }
            }
        }

        setButtonText("extractStartFrame", startFrameButton);
        setButtonText("extractStartTime", startTimeButton);
        setButtonText("extractEndFrame", endFrameButton);
        setButtonText("extractEndTime", endTimeButton);
    }
}
