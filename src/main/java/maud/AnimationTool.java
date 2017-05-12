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
package maud;

import de.lessvoid.nifty.controls.Slider;
import java.util.logging.Logger;
import jme3utilities.MyString;
import jme3utilities.nifty.BasicScreenController;
import jme3utilities.nifty.WindowController;

/**
 * The controller for the "Animation Tool" window in Maud's "3D View" screen.
 *
 * @author Stephen Gold sgold@sonic.net
 */
public class AnimationTool extends WindowController {
    // *************************************************************************
    // constants and loggers

    /**
     * message logger for this class
     */
    final private static Logger logger = Logger.getLogger(
            AnimationTool.class.getName());
    // *************************************************************************
    // fields

    /**
     * flag that causes this controller to temporarily ignore change events from
     * the time slider
     */
    private boolean ignoreTimeSliderChanges = false;
    // *************************************************************************
    // constructors

    /**
     * Instantiate an uninitialized controller.
     *
     * @param screenController
     */
    AnimationTool(BasicScreenController screenController) {
        super(screenController, "animationTool", false);
    }
    // *************************************************************************
    // new methods exposed

    /**
     * Delete the loaded animation and (if successful) load bind pose.
     */
    void delete() {
        boolean success = Maud.model.cgm.deleteAnimation();
        if (success) {
            Maud.model.animation.loadBindPose();
        }
    }

    /**
     * Update the MVC model based on the sliders.
     */
    void onSliderChanged() {
        float duration = Maud.model.animation.getDuration();
        float speed;
        if (duration > 0f) {
            Slider slider = Maud.gui.getSlider("speed");
            speed = slider.getValue();
            Maud.model.animation.setSpeed(speed);
        }

        boolean moving = Maud.model.animation.isMoving();
        if (!moving && !ignoreTimeSliderChanges) {
            Slider slider = Maud.gui.getSlider("time");
            float fraction = slider.getValue();
            float time = fraction * duration;
            Maud.model.animation.setTime(time);
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

        updateName();
        updateLooping();
        updateSpeed();
        updateTrackTime();
        updateTrackCounts();
    }
    // *************************************************************************
    // private methods

    /**
     * Update the loop check box and the pause button label.
     */
    private void updateLooping() {
        boolean looping = Maud.model.animation.willContinue();
        Maud.gui.setChecked("loop", looping);

        String pButton = "";
        float duration = Maud.model.animation.getDuration();
        if (duration > 0f) {
            boolean paused = Maud.model.animation.isPaused();
            if (paused) {
                pButton = "Resume";
            } else {
                pButton = "Pause";
            }
        }
        Maud.gui.setButtonLabel("togglePauseButton", pButton);
    }

    /**
     * Update the name label and rename button label.
     */
    private void updateName() {
        String nameText, rButton;
        String name = Maud.model.animation.getName();
        if (Maud.model.animation.isBindPoseLoaded()) {
            nameText = name;
            rButton = "";
        } else {
            nameText = MyString.quote(name);
            rButton = "Rename";
        }
        Maud.gui.setStatusText("animationName", " " + nameText);
        Maud.gui.setButtonLabel("animationRenameButton", rButton);
    }

    /**
     * Update the speed slider and its status label.
     */
    private void updateSpeed() {
        float duration = Maud.model.animation.getDuration();
        Slider slider = Maud.gui.getSlider("speed");
        if (duration > 0f) {
            slider.enable();
        } else {
            slider.disable();
        }

        float speed = Maud.model.animation.getSpeed();
        slider.setValue(speed);
        Maud.gui.updateSliderStatus("speed", speed, "x");
    }

    /**
     * Update the track counts.
     */
    private void updateTrackCounts() {
        int numBoneTracks = Maud.model.animation.countBoneTracks();
        String boneTracksText = String.format("%d", numBoneTracks);
        Maud.gui.setStatusText("boneTracks", " " + boneTracksText);

        int numTracks = Maud.model.animation.countTracks();
        int numOtherTracks = numTracks - numBoneTracks;
        String otherTracksText = String.format("%d", numOtherTracks);
        Maud.gui.setStatusText("otherTracks", " " + otherTracksText);
    }

    /**
     * Update the track-time slider and its status label.
     */
    private void updateTrackTime() {
        /*
         * slider
         */
        boolean moving = Maud.model.animation.isMoving();
        float duration = Maud.model.animation.getDuration();
        Slider slider = Maud.gui.getSlider("time");
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
            trackTime = Maud.model.animation.getTime();
            float fraction = trackTime / duration;
            ignoreTimeSliderChanges = true;
            slider.setValue(fraction);
            ignoreTimeSliderChanges = false;
        }
        /*
         * status label
         */
        String statusText;
        if (Maud.model.animation.isBindPoseLoaded()) {
            statusText = "time = n/a";
        } else {
            statusText = String.format("time = %.3f / %.3f sec",
                    trackTime, duration);
        }
        Maud.gui.setStatusText("trackTime", statusText);
    }
}
