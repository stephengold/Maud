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

import de.lessvoid.nifty.controls.Slider;
import java.util.logging.Logger;
import jme3utilities.MyString;
import jme3utilities.nifty.BasicScreenController;
import jme3utilities.nifty.WindowController;
import maud.Maud;
import maud.model.Cgm;
import maud.model.LoadedAnimation;
import maud.model.SelectedAnimControl;
import maud.model.SelectedBone;

/**
 * The controller for the "Animation Tool" window in Maud's editor screen.
 *
 * @author Stephen Gold sgold@sonic.net
 */
class AnimationTool extends WindowController {
    // *************************************************************************
    // constants and loggers

    /**
     * message logger for this class
     */
    final private static Logger logger
            = Logger.getLogger(AnimationTool.class.getName());
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
     * Update the MVC model based on the sliders.
     */
    void onSliderChanged() {
        LoadedAnimation animation = Maud.getModel().getTarget().getAnimation();
        if (animation.isRetargetedPose()) {
            animation = Maud.getModel().getSource().getAnimation();
        }

        float duration = animation.getDuration();
        float speed;
        if (duration > 0f) {
            Slider slider = Maud.gui.getSlider("speed");
            speed = slider.getValue();
            animation.setSpeed(speed);
        }

        boolean moving = animation.isMoving();
        if (!moving) {
            Slider slider = Maud.gui.getSlider("time");
            float fraction = slider.getValue();
            float time = fraction * duration;
            animation.setTime(time);
        }
    }
    // *************************************************************************
    // WindowController methods

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

        updateControlIndex();
        updateHasTrack();
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
     * Update the anim control index status and previous/next/select buttons.
     */
    private void updateControlIndex() {
        String indexText;
        String sButton = "";
        String nButton = "";
        String pButton = "";

        Cgm target = Maud.getModel().getTarget();
        if (target.countAnimControls() > 0) {
            sButton = "Select AnimControl";
            int numAnimControls = target.countAnimControls();
            SelectedAnimControl sac = target.getAnimControl();
            if (sac.isSelected()) {
                int selectedIndex = sac.findIndex();
                int indexBase = Maud.getModel().getMisc().getIndexBase();
                indexText = String.format("#%d of %d",
                        selectedIndex + indexBase, numAnimControls);
                nButton = "+";
                pButton = "-";
            } else {
                if (numAnimControls == 0) {
                    indexText = "no AnimControls";
                } else if (numAnimControls == 1) {
                    indexText = "one AnimControl";
                } else {
                    indexText = String.format("%d AnimControls",
                            numAnimControls);
                }
            }

        } else {
            indexText = "not animated";
        }

        Maud.gui.setButtonLabel("animControlPreviousButton", pButton);
        Maud.gui.setStatusText("animControlIndex", indexText);
        Maud.gui.setButtonLabel("animControlNextButton", nButton);
        Maud.gui.setButtonLabel("animControlSelectButton", sButton);
    }

    /**
     * Update the "has track" status of the selected bone.
     */
    private void updateHasTrack() {
        Cgm target = Maud.getModel().getTarget();
        SelectedBone bone = target.getBone();
        String hasTrackText;
        if (!bone.isSelected()) {
            hasTrackText = "no bone";
        } else if (target.getAnimation().isReal()) {
            if (bone.hasTrack()) {
                hasTrackText = "has track";
            } else {
                hasTrackText = "no track";
            }
        } else if (target.getAnimation().isRetargetedPose()) {
            String boneName = bone.getName();
            if (Maud.getModel().getMap().isBoneMapped(boneName)) {
                hasTrackText = "is mapped";
            } else {
                hasTrackText = "not mapped";
            }
        } else {
            hasTrackText = "";
        }
        Maud.gui.setStatusText("animationHasTrack", " " + hasTrackText);
    }

    /**
     * Update the animation index status and previous/next buttons.
     */
    private void updateIndex() {
        String indexText;
        String lButton = "";
        String nButton = "";
        String pButton = "";

        Cgm target = Maud.getModel().getTarget();
        SelectedAnimControl sac = target.getAnimControl();
        if (sac.isSelected()) {
            lButton = "Load";
            int numAnimations = sac.countAnimations();
            if (target.getAnimation().isReal()) {
                int selectedIndex = target.getAnimation().findIndex();
                int indexBase = Maud.getModel().getMisc().getIndexBase();
                indexText = String.format("#%d of %d",
                        selectedIndex + indexBase, numAnimations);
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
            indexText = "not selected";
        }

        Maud.gui.setStatusText("animationIndex", indexText);
        Maud.gui.setButtonLabel("animationNextButton", nButton);
        Maud.gui.setButtonLabel("animationPreviousButton", pButton);
        Maud.gui.setButtonLabel("animationLoadButton", lButton);
    }

    /**
     * Update the freeze/loop/pin/pong check boxes and the pause button label.
     */
    private void updateLooping() {
        Cgm target = Maud.getModel().getTarget();
        boolean pinned = target.getAnimation().isPinned();
        Maud.gui.setChecked("pin", pinned);

        Cgm cgm;
        if (target.getAnimation().isRetargetedPose()) {
            cgm = Maud.getModel().getSource();
        } else {
            cgm = target;
        }
        LoadedAnimation animation = cgm.getAnimation();
        boolean frozen = cgm.getPose().isFrozen();
        Maud.gui.setChecked("freeze", frozen);
        boolean looping = animation.willContinue();
        Maud.gui.setChecked("loop", looping);
        boolean ponging = animation.willReverse();
        Maud.gui.setChecked("pong", ponging);

        String pButton = "";
        float duration = animation.getDuration();
        if (duration > 0f) {
            boolean paused = animation.isPaused();
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

        LoadedAnimation animation = Maud.getModel().getTarget().getAnimation();
        String name = animation.getName();
        if (animation.isReal()) {
            nameText = MyString.quote(name);
            rButton = "Rename";
        } else {
            nameText = name;
            rButton = "";
        }

        Maud.gui.setStatusText("animationName", " " + nameText);
        Maud.gui.setButtonLabel("animationRenameButton", rButton);
    }

    /**
     * Update the speed slider and its status label.
     */
    private void updateSpeed() {
        LoadedAnimation animation = Maud.getModel().getTarget().getAnimation();
        if (animation.isRetargetedPose()) {
            animation = Maud.getModel().getSource().getAnimation();
        }

        float duration = animation.getDuration();
        Slider slider = Maud.gui.getSlider("speed");
        if (duration > 0f) {
            slider.enable();
        } else {
            slider.disable();
        }

        float speed = animation.getSpeed();
        slider.setValue(speed);
        Maud.gui.updateSliderStatus("speed", speed, "x");
    }

    /**
     * Update the track counts.
     */
    private void updateTrackCounts() {
        LoadedAnimation animation = Maud.getModel().getTarget().getAnimation();
        int numBoneTracks = animation.countBoneTracks();
        String boneTracksText = Integer.toString(numBoneTracks);
        Maud.gui.setStatusText("boneTracks", boneTracksText);

        int numSpatialTracks = animation.countSpatialTracks();
        String spatialTracksText = Integer.toString(numSpatialTracks);
        Maud.gui.setStatusText("spatialTracks", spatialTracksText);

        int numTracks = animation.countTracks();
        int numOtherTracks = numTracks - numBoneTracks - numSpatialTracks;
        String otherTracksText = String.format("%d", numOtherTracks);
        Maud.gui.setStatusText("otherTracks", otherTracksText);
    }

    /**
     * Update the track-time slider and its status label.
     */
    private void updateTrackTime() {
        LoadedAnimation animation = Maud.getModel().getTarget().getAnimation();
        if (animation.isRetargetedPose()) {
            animation = Maud.getModel().getSource().getAnimation();
        }
        /*
         * slider
         */
        boolean moving = animation.isMoving();
        float duration = animation.getDuration();
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
            trackTime = animation.getTime();
            float fraction = trackTime / duration;
            slider.setValue(fraction);
        }
        /*
         * status label
         */
        String statusText;
        if (animation.isReal()) {
            statusText = String.format("time = %.3f / %.3f sec",
                    trackTime, duration);
        } else {
            statusText = "time = n/a";
        }
        Maud.gui.setStatusText("trackTime", statusText);
    }
}
