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

import com.jme3.animation.AnimControl;
import java.util.logging.Logger;
import jme3utilities.MyString;
import jme3utilities.nifty.BasicScreenController;
import jme3utilities.nifty.SliderTransform;
import jme3utilities.nifty.WindowController;
import maud.Maud;
import maud.model.cgm.Cgm;
import maud.model.cgm.LoadedAnimation;
import maud.model.cgm.PlayOptions;
import maud.model.cgm.SelectedAnimControl;
import maud.model.cgm.SelectedBone;

/**
 * The controller for the "Source-Animation Tool" window in Maud's editor
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
    final private static Logger logger
            = Logger.getLogger(SourceAnimationTool.class.getName());
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
    SourceAnimationTool(BasicScreenController screenController) {
        super(screenController, "sourceAnimationTool", false);
    }
    // *************************************************************************
    // new methods exposed

    /**
     * Update the MVC model based on the sliders.
     */
    void onSliderChanged() {
        Cgm source = Maud.getModel().getSource();
        LoadedAnimation animation = source.getAnimation();

        float duration = animation.getDuration();
        if (duration > 0f) {
            float speed = Maud.gui.readSlider("sSpeed", speedSt);
            source.getPlay().setSpeed(speed);
        }

        boolean moving = animation.isMoving();
        if (!moving) {
            float fraction = Maud.gui.readSlider("sourceTime", timeSt);
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

        Cgm source = Maud.getModel().getSource();
        int numAnimControls = source.countSgcs(AnimControl.class);
        if (numAnimControls > 0) {
            sButton = "Select AnimControl";

            SelectedAnimControl sac = source.getAnimControl();
            if (sac.isSelected()) {
                int selectedIndex = sac.findIndex();
                int indexBase = Maud.getModel().getMisc().getIndexBase();
                indexText = String.format("#%d of %d",
                        selectedIndex + indexBase, numAnimControls);
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

        } else if (source.isLoaded()) {
            indexText = "not animated";

        } else {
            indexText = "no model loaded";
        }

        Maud.gui.setButtonText("sourceAnimControlPrevious", pButton);
        Maud.gui.setStatusText("sourceAnimControlIndex", indexText);
        Maud.gui.setButtonText("sourceAnimControlNext", nButton);
        Maud.gui.setButtonText("sourceAnimControlSelect", sButton);
    }

    /**
     * Update the "has track" status of the selected bone.
     */
    private void updateHasTrack() {
        Cgm source = Maud.getModel().getSource();
        SelectedBone bone = source.getBone();
        String hasTrackText;
        if (!source.isLoaded()) {
            hasTrackText = "no model";
        } else if (!bone.isSelected()) {
            hasTrackText = "no bone";
        } else if (!source.getAnimation().isReal()) {
            hasTrackText = "";
        } else if (bone.hasTrack()) {
            hasTrackText = "has track";
        } else {
            hasTrackText = "no track";
        }
        Maud.gui.setStatusText("sourceAnimationHasTrack", " " + hasTrackText);
    }

    /**
     * Update the index status and previous/next/load buttons.
     */
    private void updateIndex() {
        String indexText;
        String lButton = "";
        String nButton = "";
        String pButton = "";

        Cgm source = Maud.getModel().getSource();
        SelectedAnimControl sac = source.getAnimControl();
        if (sac.isSelected()) {
            lButton = "Load";
            int numAnimations = sac.countAnimations();
            if (source.getAnimation().isReal()) {
                int selectedIndex = source.getAnimation().findIndex();
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
        } else if (source.isLoaded()) {
            indexText = "not selected";
        } else {
            indexText = "no model";
        }

        Maud.gui.setButtonText("sourceAnimationPrevious", pButton);
        Maud.gui.setStatusText("sourceAnimationIndex", indexText);
        Maud.gui.setButtonText("sourceAnimationNext", nButton);
        Maud.gui.setButtonText("sourceAnimationLoad", lButton);
    }

    /**
     * Update the loop/pin/pong check boxes and the pause button label.
     */
    private void updateLooping() {
        Cgm source = Maud.getModel().getSource();
        LoadedAnimation animation = source.getAnimation();
        boolean pinned = animation.isPinned();
        Maud.gui.setChecked("pinSource", pinned);

        PlayOptions options = source.getPlay();
        boolean looping = options.willContinue();
        Maud.gui.setChecked("loopSource", looping);
        boolean ponging = options.willReverse();
        Maud.gui.setChecked("pongSource", ponging);

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
        Maud.gui.setButtonText("togglePauseSource", pButton);
    }

    /**
     * Update the name label.
     */
    private void updateName() {
        String nameText;

        Cgm source = Maud.getModel().getSource();
        if (source.isLoaded()) {
            String name = source.getAnimation().getName();
            if (source.getAnimation().isReal()) {
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
        Cgm source = Maud.getModel().getSource();
        LoadedAnimation animation = source.getAnimation();

        float duration = animation.getDuration();
        if (duration > 0f) {
            Maud.gui.enableSlider("sSpeed");
        } else {
            Maud.gui.disableSlider("sSpeed");
        }

        float speed = source.getPlay().getSpeed();
        Maud.gui.setSlider("sSpeed", speedSt, speed);
        Maud.gui.updateSliderStatus("sSpeed", speed, "x");
    }

    /**
     * Update the track counts.
     */
    private void updateTrackCounts() {
        Cgm source = Maud.getModel().getSource();
        String boneTracksText, otherTracksText;
        if (source.isLoaded()) {
            int numBoneTracks = source.getAnimation().countBoneTracks();
            boneTracksText = String.format("%d", numBoneTracks);
            int numTracks = source.getAnimation().countTracks();
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
        Cgm source = Maud.getModel().getSource();
        LoadedAnimation animation = source.getAnimation();
        float duration = animation.getDuration();
        /*
         * slider
         */
        boolean moving = animation.isMoving();
        if (duration == 0f || moving) {
            Maud.gui.disableSlider("sourceTime");
        } else {
            Maud.gui.enableSlider("sourceTime");
        }

        float trackTime;
        if (duration == 0f) {
            trackTime = 0f;
            Maud.gui.setSlider("sourceTime", timeSt, 0f);
        } else {
            trackTime = animation.getTime();
            float fraction = trackTime / duration;
            Maud.gui.setSlider("sourceTime", timeSt, fraction);
        }
        /*
         * status label
         */
        String statusText;
        if (source.isLoaded() && animation.isReal()) {
            statusText = String.format("time = %.3f / %.3f sec",
                    trackTime, duration);
        } else {
            statusText = "time = n/a";
        }
        Maud.gui.setStatusText("sourceTrackTime", statusText);
    }
}
