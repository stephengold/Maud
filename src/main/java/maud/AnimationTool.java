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
     * Toggle between paused and running.
     */
    void togglePause() {
        float duration = Maud.model.animation.getDuration();
        if (duration > 0f) {
            float speed = Maud.model.animation.getSpeed();
            if (speed > 0f) {
                speed = 0f;
            } else {
                speed = 1f;
            }
            Maud.model.animation.setSpeed(speed);
            setSliders();
        }
    }

    /**
     * Update this window after loading an animation.
     */
    public void updateAfterLoad() {
        setSliders();
        Maud.gui.boneAngle.setSliders();
        Maud.gui.boneOffset.setSliders();
        Maud.gui.boneScale.setSliders();
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
        updateSpeed();
        updateTrackTime();
    }
    // *************************************************************************
    // private methods

    /**
     * Set the sliders so that the MVC model's speed and track time will survive
     * the next update().
     */
    private void setSliders() {
        Slider slider = Maud.gui.getSlider("speed");
        float speed = Maud.model.animation.getSpeed();
        slider.setValue(speed);

        slider = Maud.gui.getSlider("time");
        float duration = Maud.model.animation.getDuration();
        if (duration == 0f) {
            slider.setValue(0f);
        } else {
            float time = Maud.model.animation.getTime();
            float fraction = time / duration;
            slider.setValue(fraction);
        }
    }

    /**
     * Update the name label, rename button, and update the track counts.
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

        int numBoneTracks = Maud.model.animation.countBoneTracks();
        String boneTracksText = String.format("%d", numBoneTracks);
        Maud.gui.setStatusText("boneTracks", " " + boneTracksText);

        int numTracks = Maud.model.animation.countTracks();
        int numOtherTracks = numTracks - numBoneTracks;
        String otherTracksText = String.format("%d", numOtherTracks);
        Maud.gui.setStatusText("otherTracks", " " + otherTracksText);
    }

    /**
     * Update the speed slider and its status label.
     */
    private void updateSpeed() {
        Slider slider = Maud.gui.getSlider("speed");
        float duration = Maud.model.animation.getDuration();
        float speed;
        if (duration > 0f) {
            slider.enable();
            speed = slider.getValue();
            Maud.model.animation.setSpeed(speed);
        } else {
            slider.disable();
            speed = Maud.model.animation.getSpeed();
            slider.setValue(speed);
        }
        Maud.gui.updateSliderStatus("speed", speed, "x");
    }

    /**
     * Update the track-time slider and its status label.
     */
    private void updateTrackTime() {
        /*
         * slider
         */
        Slider slider = Maud.gui.getSlider("time");
        float duration = Maud.model.animation.getDuration();
        float trackTime;
        if (duration == 0f) {
            slider.disable();
            trackTime = 0f;
            slider.setValue(0f);
        } else if (Maud.model.animation.isRunning()) {
            slider.disable();
            trackTime = Maud.model.animation.getTime();
            float fraction = trackTime / duration;
            slider.setValue(fraction);
        } else {
            slider.enable();
            float fraction = slider.getValue();
            trackTime = fraction * duration;
            Maud.model.animation.setTime(trackTime);
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
