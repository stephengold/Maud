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
import java.util.List;
import java.util.logging.Logger;
import jme3utilities.MyString;
import jme3utilities.nifty.GuiScreenController;
import jme3utilities.nifty.SliderTransform;
import maud.Maud;
import maud.MaudUtil;
import maud.model.cgm.Cgm;
import maud.model.cgm.LoadedAnimation;
import maud.model.cgm.PlayOptions;
import maud.model.cgm.SelectedAnimControl;

/**
 * The controller for the "Animation" tool in Maud's editor screen.
 *
 * @author Stephen Gold sgold@sonic.net
 */
class AnimationTool extends Tool {
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
     * Instantiate an uninitialized tool.
     *
     * @param screenController the controller of the screen that contains the
     * tool (not null)
     */
    AnimationTool(GuiScreenController screenController) {
        super(screenController, "animation");
    }
    // *************************************************************************
    // Tool methods

    /**
     * Enumerate this tool's check boxes.
     *
     * @return a new list of names (unique id prefixes)
     */
    @Override
    List<String> listCheckBoxes() {
        List<String> result = super.listCheckBoxes();
        result.add("freeze");
        result.add("loop");
        result.add("pin");
        result.add("pong");

        return result;
    }

    /**
     * Enumerate this tool's sliders.
     *
     * @return a new list of names (unique id prefixes)
     */
    @Override
    List<String> listSliders() {
        List<String> result = super.listSliders();
        result.add("speed");
        result.add("time");

        return result;
    }

    /**
     * Update the MVC model based on a check-box event.
     *
     * @param name the name (unique id prefix) of the check box
     * @param isChecked the new state of the check box (true&rarr;checked,
     * false&rarr;unchecked)
     */
    @Override
    public void onCheckBoxChanged(String name, boolean isChecked) {
        Cgm target = Maud.getModel().getTarget();
        LoadedAnimation animation = target.getAnimation();
        Cgm cgm;
        if (animation.isRetargetedPose()) {
            cgm = Maud.getModel().getSource();
        } else {
            cgm = target;
        }

        switch (name) {
            case "freeze":
                cgm.getPose().setFrozen(isChecked);
                break;

            case "loop":
                cgm.getPlay().setContinue(isChecked);
                break;

            case "pin":
                animation.setPinned(isChecked);
                break;

            case "pong":
                cgm.getPlay().setReverse(isChecked);
                break;

            default:
                super.onCheckBoxChanged(name, isChecked);
        }
    }

    /**
     * Update the MVC model based on the sliders.
     */
    @Override
    public void onSliderChanged() {
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

    /**
     * Callback to update this tool prior to rendering. (Invoked once per render
     * pass while this tool is displayed.)
     */
    @Override
    void toolUpdate() {
        updateControlIndex();
        updateIndex();
        updateLooping();
        updateName();
        updateSpeed();
        updateTrackTime();
        updateTrackCounts();
    }
    // *************************************************************************
    // private methods

    /**
     * Update the anim control index status and previous/next/select buttons.
     */
    private void updateControlIndex() {
        String indexStatus;
        String selectButton = "";
        String nextButton = "";
        String previousButton = "";

        Cgm target = Maud.getModel().getTarget();
        int numAnimControls = target.countSgcs(AnimControl.class);
        if (numAnimControls > 0) {
            selectButton = "Select animControl";
            SelectedAnimControl sac = target.getAnimControl();
            if (sac.isSelected()) {
                int selectedIndex = sac.findIndex();
                indexStatus = MaudUtil.formatIndex(selectedIndex);
                indexStatus = String.format("%s of %d", indexStatus,
                        numAnimControls);
                if (numAnimControls > 1) {
                    nextButton = "+";
                    previousButton = "-";
                }
            } else {
                if (numAnimControls == 1) {
                    indexStatus = "one AnimControl";
                } else {
                    indexStatus
                            = String.format("%d AnimControls", numAnimControls);
                }
            }

        } else {
            indexStatus = "not animated";
        }

        setButtonText("animControlPrevious", previousButton);
        setStatusText("animControlIndex", indexStatus);
        setButtonText("animControlNext", nextButton);
        setButtonText("animControlSelect", selectButton);
    }

    /**
     * Update the index status and previous/next/add/load buttons.
     */
    private void updateIndex() {
        String indexStatus;
        String addButton = "";
        String loadButton = "";
        String nextButton = "";
        String previousButton = "";

        Cgm target = Maud.getModel().getTarget();
        SelectedAnimControl animControl = target.getAnimControl();
        if (animControl.isSelected()) {
            addButton = "Add new";
            loadButton = "Load animation";
            int numAnimations = animControl.countAnimations();
            if (target.getAnimation().isReal()) {
                int selectedIndex = target.getAnimation().findIndex();
                indexStatus = MaudUtil.formatIndex(selectedIndex);
                indexStatus
                        = String.format("%s of %d", indexStatus, numAnimations);
                if (numAnimations > 1) {
                    nextButton = "+";
                    previousButton = "-";
                }
            } else {
                if (numAnimations == 0) {
                    indexStatus = "no animations";
                } else if (numAnimations == 1) {
                    indexStatus = "one animation";
                } else {
                    indexStatus = String.format("%d animations", numAnimations);
                }
            }
        } else {
            indexStatus = "not selected";
        }

        setButtonText("animationAdd", addButton);
        setButtonText("animationLoad", loadButton);
        setButtonText("animationNext", nextButton);
        setButtonText("animationPrevious", previousButton);
        setStatusText("animationIndex", indexStatus);
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
     * Update the name status and edit/rename button labels.
     */
    private void updateName() {
        String eButton, nameText, rButton;

        LoadedAnimation animation = Maud.getModel().getTarget().getAnimation();
        String name = animation.getName();
        if (animation.isReal()) {
            eButton = "Edit";
            nameText = MyString.quote(name);
            rButton = "Rename";
        } else {
            eButton = "";
            nameText = name;
            rButton = "";
        }

        setButtonText("animationEdit", eButton);
        setButtonText("animationRename", rButton);
        setStatusText("animationName", " " + nameText);
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
        LoadedAnimation master = Maud.getModel().getTarget().getAnimation();
        if (master.isRetargetedPose()) {
            master = Maud.getModel().getSource().getAnimation();
        }
        float duration = master.getDuration();
        /*
         * slider
         */
        boolean moving = master.isMoving();
        setSliderEnabled("time", duration != 0f && !moving);

        float fraction, trackTime;
        if (duration == 0f) {
            trackTime = 0f;
            fraction = 0f;
        } else {
            trackTime = master.getTime();
            fraction = trackTime / duration;
        }
        setSlider("time", timeSt, fraction);
        /*
         * status label
         */
        String statusText;
        if (master.isReal()) {
            statusText = String.format("time = %.3f / %.3f sec",
                    trackTime, duration);
        } else {
            statusText = "time = n/a";
        }
        setStatusText("trackTime", statusText);
    }
}
