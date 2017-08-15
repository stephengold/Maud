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
import maud.model.LoadedCgm;

/**
 * The controller for the "Source Animation Tool" window in Maud's editor
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
        LoadedCgm source = Maud.getModel().getSource();
        float duration = source.animation.getDuration();
        float speed;
        if (duration > 0f) {
            Slider slider = Maud.gui.getSlider("sSpeed");
            speed = slider.getValue();
            source.animation.setSpeed(speed);
        }

        boolean moving = source.animation.isMoving();
        if (!moving) {
            Slider slider = Maud.gui.getSlider("sourceTime");
            float fraction = slider.getValue();
            float time = fraction * duration;
            source.animation.setTime(time);
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

        LoadedCgm source = Maud.getModel().getSource();
        String hasTrackText;
        if (!source.isLoaded()) {
            hasTrackText = "no model";
        } else if (!source.bone.isSelected()) {
            hasTrackText = "no bone";
        } else if (!source.animation.isReal()) {
            hasTrackText = "";
        } else {
            if (source.bone.hasTrack()) {
                hasTrackText = "has track";
            } else {
                hasTrackText = "no track";
            }
        }
        Maud.gui.setStatusText("sourceAnimationHasTrack", " " + hasTrackText);

        updateControlIndex();
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
     * Update the control index status and previous/next/select buttons.
     */
    private void updateControlIndex() {
        String indexText;
        String sButton = "";
        String nButton = "";
        String pButton = "";

        LoadedCgm source = Maud.getModel().getSource();
        if (source.countAnimControls() > 0) {
            sButton = "Select AnimControl";
            int numAnimControls = source.countAnimControls();
            if (source.isAnimControlSelected()) {
                int selectedIndex = source.findAnimControlIndex();
                indexText = String.format("#%d of %d", selectedIndex + 1,
                        numAnimControls);
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

        } else if (source.isLoaded()) {
            indexText = "not animated";

        } else {
            indexText = "no model loaded";
        }

        Maud.gui.setButtonLabel("sourceAnimControlPreviousButton", pButton);
        Maud.gui.setStatusText("sourceAnimControlIndex", indexText);
        Maud.gui.setButtonLabel("sourceAnimControlNextButton", nButton);
        Maud.gui.setButtonLabel("sourceAnimControlSelectButton", sButton);
    }

    /**
     * Update the index status and previous/next/load buttons.
     */
    private void updateIndex() {
        String indexText;
        String lButton = "";
        String nButton = "";
        String pButton = "";

        LoadedCgm source = Maud.getModel().getSource();
        if (source.isAnimControlSelected()) {
            lButton = "Load";
            int numAnimations = source.countAnimations();
            if (source.animation.isReal()) {
                int selectedIndex = source.animation.findIndex();
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
        } else if (source.isLoaded()) {
            indexText = "not selected";
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
        LoadedCgm source = Maud.getModel().getSource();
        boolean looping = source.animation.willContinue();
        Maud.gui.setChecked("loopSource", looping);

        String pButton = "";
        float duration = source.animation.getDuration();
        if (duration > 0f) {
            boolean paused = source.animation.isPaused();
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
        LoadedCgm source = Maud.getModel().getSource();
        if (source.isLoaded()) {
            String name = source.animation.getName();
            if (source.animation.isReal()) {
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
        float duration = Maud.getModel().getSource().animation.getDuration();
        Slider slider = Maud.gui.getSlider("sSpeed");
        if (duration > 0f) {
            slider.enable();
        } else {
            slider.disable();
        }

        float speed = Maud.getModel().getSource().animation.getSpeed();
        slider.setValue(speed);
        Maud.gui.updateSliderStatus("sSpeed", speed, "x");
    }

    /**
     * Update the track counts.
     */
    private void updateTrackCounts() {
        LoadedCgm source = Maud.getModel().getSource();
        String boneTracksText, otherTracksText;
        if (source.isLoaded()) {
            int numBoneTracks = source.animation.countBoneTracks();
            boneTracksText = String.format("%d", numBoneTracks);
            int numTracks = source.animation.countTracks();
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
        LoadedCgm source = Maud.getModel().getSource();
        /*
         * slider
         */
        boolean moving = source.animation.isMoving();
        float duration = source.animation.getDuration();
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
            trackTime = source.animation.getTime();
            float fraction = trackTime / duration;
            slider.setValue(fraction);
        }
        /*
         * status label
         */
        String statusText;
        if (!source.isLoaded() || !source.animation.isReal()) {
            statusText = "time = n/a";
        } else {
            statusText = String.format("time = %.3f / %.3f sec",
                    trackTime, duration);
        }
        Maud.gui.setStatusText("sourceTrackTime", statusText);
    }
}