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

import de.lessvoid.nifty.controls.Slider;
import java.util.logging.Logger;
import jme3utilities.MyString;
import jme3utilities.nifty.BasicScreenController;
import jme3utilities.nifty.WindowController;
import maud.Maud;

/**
 * The controller for the "Source Animation Tool" window in Maud's "3D View"
 * screen.
 *
 * @author Stephen Gold sgold@sonic.net
 */
class SourceAnimationTool extends WindowController {
    // *************************************************************************
    // constants and loggers

    /**
     * message logger for this class
     */
    final private static Logger logger = Logger.getLogger(
            SourceAnimationTool.class.getName());
    // *************************************************************************
    // constructors

    /**
     * Instantiate an uninitialized controller.
     *
     * @param screenController
     */
    SourceAnimationTool(BasicScreenController screenController) {
        super(screenController, "sourceAnimationTool", false);
    }
    // *************************************************************************
    // new methods exposed

    /**
     * Update the MVC model based on the sliders.
     */
    void onSliderChanged() {
        float duration = Maud.model.source.animation.getDuration();
        float speed;
        if (duration > 0f) {
            Slider slider = Maud.gui.getSlider("sSpeed");
            speed = slider.getValue();
            Maud.model.source.animation.setSpeed(speed);
        }

        boolean moving = Maud.model.source.animation.isMoving();
        if (!moving) {
            Slider slider = Maud.gui.getSlider("sourceTime");
            float fraction = slider.getValue();
            float time = fraction * duration;
            Maud.model.source.animation.setTime(time);
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

        String hasTrackText;
        if (!Maud.model.source.isLoaded()) {
            hasTrackText = "no model";
        } else if (!Maud.model.source.bone.isSelected()) {
            hasTrackText = "no bone";
        } else if (!Maud.model.source.animation.isReal()) {
            hasTrackText = "";
        } else {
            if (Maud.model.source.bone.hasTrack()) {
                hasTrackText = "has track";
            } else {
                hasTrackText = "no track";
            }
        }
        Maud.gui.setStatusText("sourceAnimationHasTrack", " " + hasTrackText);

        updateIndex();
        updateLooping();
        updateName();
        updateSpeed();
        updateTrackTime();
        updateTrackCounts();

        Maud.gui.setIgnoreGuiChanges(false);
    }
    // *************************************************************************
    // private methods

    /**
     * Update the index status and previous/next/load buttons.
     */
    private void updateIndex() {
        String indexText;
        String lButton = "";
        String nButton = "";
        String pButton = "";

        if (Maud.model.source.isLoaded()) {
            lButton = "Load";
            int numAnimations = Maud.model.source.countAnimations();
            if (Maud.model.source.animation.isReal()) {
                int selectedIndex = Maud.model.source.animation.findIndex();
                indexText = String.format("#%d of %d", selectedIndex + 1,
                        numAnimations);
                nButton = "+";
                pButton = "-";

            } else {
                if (numAnimations == 0) {
                    indexText = "no animations";
                } else if (numAnimations == 1) {
                    indexText = "one animation";
                } else {
                    indexText = String.format("%d animations", numAnimations);
                }
            }
        } else {
            indexText = "no model";
        }

        Maud.gui.setButtonLabel("sourceAnimationPreviousButton", pButton);
        Maud.gui.setStatusText("sourceAnimationIndex", indexText);
        Maud.gui.setButtonLabel("sourceAnimationNextButton", nButton);
        Maud.gui.setButtonLabel("sourceAnimationLoadButton", lButton);
    }

    /**
     * Update the loop check box and the pause button label.
     */
    private void updateLooping() {
        boolean looping = Maud.model.source.animation.willContinue();
        Maud.gui.setChecked("loopSource", looping);

        String pButton = "";
        float duration = Maud.model.source.animation.getDuration();
        if (duration > 0f) {
            boolean paused = Maud.model.source.animation.isPaused();
            if (paused) {
                pButton = "Resume";
            } else {
                pButton = "Pause";
            }
        }
        Maud.gui.setButtonLabel("togglePauseSourceButton", pButton);
    }

    /**
     * Update the name label.
     */
    private void updateName() {
        String nameText;
        if (Maud.model.source.isLoaded()) {
            String name = Maud.model.source.animation.getName();
            if (Maud.model.source.animation.isReal()) {
                nameText = MyString.quote(name);
            } else {
                nameText = name;
            }
        } else {
            nameText = "";
        }
        Maud.gui.setStatusText("sourceAnimationName", " " + nameText);
    }

    /**
     * Update the speed slider and its status label.
     */
    private void updateSpeed() {
        float duration = Maud.model.source.animation.getDuration();
        Slider slider = Maud.gui.getSlider("sSpeed");
        if (duration > 0f) {
            slider.enable();
        } else {
            slider.disable();
        }

        float speed = Maud.model.source.animation.getSpeed();
        slider.setValue(speed);
        Maud.gui.updateSliderStatus("sSpeed", speed, "x");
    }

    /**
     * Update the track counts.
     */
    private void updateTrackCounts() {
        String boneTracksText, otherTracksText;
        if (Maud.model.source.isLoaded()) {
            int numBoneTracks = Maud.model.source.animation.countBoneTracks();
            boneTracksText = String.format("%d", numBoneTracks);
            int numTracks = Maud.model.source.animation.countTracks();
            int numOtherTracks = numTracks - numBoneTracks;
            otherTracksText = String.format("%d", numOtherTracks);
        } else {
            boneTracksText = "";
            otherTracksText = "";
        }

        Maud.gui.setStatusText("sourceBoneTracks", " " + boneTracksText);
        Maud.gui.setStatusText("sourceOtherTracks", " " + otherTracksText);
    }

    /**
     * Update the track-time slider and its status label.
     */
    private void updateTrackTime() {
        /*
         * slider
         */
        boolean moving = Maud.model.source.animation.isMoving();
        float duration = Maud.model.source.animation.getDuration();
        Slider slider = Maud.gui.getSlider("sourceTime");
        if (duration == 0f || moving) {
            slider.disable();
        } else {
            slider.enable();
        }

        float trackTime;
        if (duration == 0f) {
            trackTime = 0f;
            slider.setValue(0f);
        } else {
            trackTime = Maud.model.source.animation.getTime();
            float fraction = trackTime / duration;
            slider.setValue(fraction);
        }
        /*
         * status label
         */
        String statusText;
        if (!Maud.model.source.isLoaded()
                || !Maud.model.source.animation.isReal()) {
            statusText = "time = n/a";
        } else {
            statusText = String.format("time = %.3f / %.3f sec",
                    trackTime, duration);
        }
        Maud.gui.setStatusText("sourceTrackTime", statusText);
    }
}
