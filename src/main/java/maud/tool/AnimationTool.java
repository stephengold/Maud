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

import com.jme3.animation.AnimControl;
import java.util.logging.Logger;
import jme3utilities.MyString;
import jme3utilities.nifty.GuiScreenController;
import jme3utilities.nifty.GuiWindowController;
import jme3utilities.nifty.SliderTransform;
import maud.Maud;
import maud.MaudUtil;
import maud.model.cgm.Cgm;
import maud.model.cgm.LoadedAnimation;
import maud.model.cgm.PlayOptions;
import maud.model.cgm.SelectedAnimControl;
import maud.model.cgm.SelectedBone;

/**
 * The controller for the "Animation Tool" window in Maud's editor screen.
 *
 * @author Stephen Gold sgold@sonic.net
 */
class AnimationTool extends GuiWindowController {
    // *************************************************************************
    // constants and loggers

    /**
     * message logger for this class
     */
    final private static Logger logger
            = Logger.getLogger(AnimationTool.class.getName());
    /**
     * transform for the speed slider
     */
    final private static SliderTransform speedSt = SliderTransform.None;
    /**
     * transform for the time slider
     */
    final private static SliderTransform timeSt = SliderTransform.None;
    // *************************************************************************
    // constructors

    /**
     * Instantiate an uninitialized controller.
     *
     * @param screenController
     */
    AnimationTool(GuiScreenController screenController) {
        super(screenController, "animationTool", false);
    }
    // *************************************************************************
    // new methods exposed

    /**
     * Update the MVC model based on the sliders.
     */
    void onSliderChanged() {
        Cgm target = Maud.getModel().getTarget();
        LoadedAnimation animation = target.getAnimation();
        if (animation.isRetargetedPose()) {
            animation = Maud.getModel().getSource().getAnimation();
        }

        float duration = animation.getDuration();
        if (duration > 0f) {
            float speed = readSlider("speed", speedSt);
            target.getPlay().setSpeed(speed);
        }

        boolean moving = animation.isMoving();
        if (!moving) {
            float fraction = readSlider("time", timeSt);
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
        int numAnimControls = target.countSgcs(AnimControl.class);
        if (numAnimControls > 0) {
            sButton = "Select AnimControl";
            SelectedAnimControl sac = target.getAnimControl();
            if (sac.isSelected()) {
                int selectedIndex = sac.findIndex();
                indexText = MaudUtil.formatIndex(selectedIndex);
                indexText
                        = String.format("%s of %d", indexText, numAnimControls);
                nButton = "+";
                pButton = "-";
            } else {
                if (numAnimControls == 1) {
                    indexText = "one AnimControl";
                } else {
                    indexText
                            = String.format("%d AnimControls", numAnimControls);
                }
            }

        } else {
            indexText = "not animated";
        }

        setButtonText("animControlPrevious", pButton);
        setStatusText("animControlIndex", indexText);
        setButtonText("animControlNext", nButton);
        setButtonText("animControlSelect", sButton);
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
        setStatusText("animationHasTrack", " " + hasTrackText);
    }

    /**
     * Update the index status and previous/next/load buttons.
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
                indexText = MaudUtil.formatIndex(selectedIndex);
                indexText = String.format("%s of %d", indexText, numAnimations);
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

        setStatusText("animationIndex", indexText);
        setButtonText("animationNext", nButton);
        setButtonText("animationPrevious", pButton);
        setButtonText("animationLoad", lButton);
    }

    /**
     * Update the freeze/loop/pin/pong check boxes and the pause button label.
     */
    private void updateLooping() {
        Cgm target = Maud.getModel().getTarget();
        boolean pinned = target.getAnimation().isPinned();
        setChecked("pin", pinned);

        Cgm cgm;
        if (target.getAnimation().isRetargetedPose()) {
            cgm = Maud.getModel().getSource();
        } else {
            cgm = target;
        }
        LoadedAnimation animation = cgm.getAnimation();
        boolean frozen = cgm.getPose().isFrozen();
        setChecked("freeze", frozen);

        PlayOptions options = cgm.getPlay();
        boolean looping = options.willContinue();
        setChecked("loop", looping);
        boolean ponging = options.willReverse();
        setChecked("pong", ponging);

        String pButton = "";
        float duration = animation.getDuration();
        if (duration > 0f) {
            boolean paused = options.isPaused();
            if (paused) {
                pButton = "Resume";
            } else {
                pButton = "Pause";
            }
        }
        setButtonText("togglePause", pButton);
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

        setStatusText("animationName", " " + nameText);
        setButtonText("animationRename", rButton);
    }

    /**
     * Update the speed slider and its status label.
     */
    private void updateSpeed() {
        Cgm target = Maud.getModel().getTarget();
        LoadedAnimation animation = target.getAnimation();
        if (animation.isRetargetedPose()) {
            animation = Maud.getModel().getSource().getAnimation();
        }

        float duration = animation.getDuration();
        setSliderEnabled("speed", duration > 0f);

        float speed = target.getPlay().getSpeed();
        setSlider("speed", speedSt, speed);
        updateSliderStatus("speed", speed, "x");
    }

    /**
     * Update the track counts.
     */
    private void updateTrackCounts() {
        LoadedAnimation animation = Maud.getModel().getTarget().getAnimation();
        int numBoneTracks = animation.countBoneTracks();
        String boneTracksText = Integer.toString(numBoneTracks);
        setStatusText("boneTracks", boneTracksText);

        int numSpatialTracks = animation.countSpatialTracks();
        String spatialTracksText = Integer.toString(numSpatialTracks);
        setStatusText("spatialTracks", spatialTracksText);

        int numTracks = animation.countTracks();
        int numOtherTracks = numTracks - numBoneTracks - numSpatialTracks;
        String otherTracksText = String.format("%d", numOtherTracks);
        setStatusText("otherTracks", otherTracksText);
    }

    /**
     * Update the track-time slider and its status label.
     */
    private void updateTrackTime() {
        LoadedAnimation animation = Maud.getModel().getTarget().getAnimation();
        if (animation.isRetargetedPose()) {
            animation = Maud.getModel().getSource().getAnimation();
        }
        float duration = animation.getDuration();
        /*
         * slider
         */
        boolean moving = animation.isMoving();
        setSliderEnabled("time", duration == 0f || moving);

        float fraction, trackTime;
        if (duration == 0f) {
            trackTime = 0f;
            fraction = 0f;
        } else {
            trackTime = animation.getTime();
            fraction = trackTime / duration;
        }
        setSlider("time", timeSt, fraction);
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
        setStatusText("trackTime", statusText);
    }
}
