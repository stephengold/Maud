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

import com.jme3.math.Quaternion;
import com.jme3.math.Transform;
import com.jme3.math.Vector3f;
import de.lessvoid.nifty.controls.Slider;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;
import jme3utilities.MyString;
import jme3utilities.Validate;
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
     * user transforms for the current (temporary) pose
     */
    final private List<Transform> currentPose = new ArrayList<>(30);
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
     * Copy the user transform of the indexed bone in the current pose.
     *
     * @param boneIndex
     * @return a new instance
     */
    public Transform copyBoneTransform(int boneIndex) {
        Transform result = currentPose.get(boneIndex);
        result = result.clone();

        return result;
    }

    /**
     * Delete the loaded animation and (if successful) load bind pose.
     */
    public void delete() {
        boolean success = Maud.model.deleteAnimation();
        if (success) {
            Maud.model.animation.loadBindPose();
        }
    }

    /**
     * Pose the loaded CG model per the loaded animation.
     */
    public void poseSkeleton() {
        int boneCount = Maud.model.countBones();
        for (int boneIndex = 0; boneIndex < boneCount; boneIndex++) {
            Transform transform = currentPose.get(boneIndex);
            Maud.model.animation.boneTransform(boneIndex, transform);
        }

        Maud.viewState.poseSkeleton(currentPose);
    }

    /**
     * Reset the pose to bind pose.
     */
    public void resetPose() {
        int boneCount = Maud.model.countBones();
        currentPose.clear();
        for (int boneIndex = 0; boneIndex < boneCount; boneIndex++) {
            Transform transform = new Transform();
            currentPose.add(transform);
        }
        Maud.viewState.poseSkeleton(currentPose);
    }

    /**
     * Alter the user rotation of the indexed bone.
     *
     * @param boneIndex which bone to rotate
     * @param rotation (not null, unaffected)
     */
    public void setBoneRotation(int boneIndex, Quaternion rotation) {
        Validate.nonNull(rotation, "rotation");

        Transform boneTransform = currentPose.get(boneIndex);
        boneTransform.setRotation(rotation);
        Maud.viewState.poseSkeleton(currentPose);
    }

    /**
     * Alter the user scale of the indexed bone.
     *
     * @param boneIndex which bone to scale
     * @param scale (not null, unaffected)
     */
    public void setBoneScale(int boneIndex, Vector3f scale) {
        Validate.nonNull(scale, "scale");

        Transform boneTransform = currentPose.get(boneIndex);
        boneTransform.setScale(scale);
        Maud.viewState.poseSkeleton(currentPose);
    }

    /**
     * Alter the user translation of the indexed bone.
     *
     * @param boneIndex which bone to translate
     * @param translation (not null, unaffected)
     */
    public void setBoneTranslation(int boneIndex, Vector3f translation) {
        Validate.nonNull(translation, "translation");

        Transform boneTransform = currentPose.get(boneIndex);
        boneTransform.setTranslation(translation);
        Maud.viewState.poseSkeleton(currentPose);
    }

    /**
     * Toggle between paused and running.
     */
    void togglePause() {
        float duration = Maud.model.animation.getDuration();
        if (duration > 0f) {
            Slider slider = Maud.gui.getSlider("speed");
            float speed = slider.getValue();
            if (speed > 0f) {
                speed = 0f;
            } else {
                speed = 1f;
            }
            slider.setValue(speed);

            update();
        }
    }

    /**
     * Update this window after a change to duration, speed, or time.
     */
    public void update() {
        /*
         * speed slider and its status
         */
        Slider slider = Maud.gui.getSlider("speed");
        float duration = Maud.model.animation.getDuration();
        if (duration > 0f) {
            slider.enable();
            float newSpeed = Maud.gui.updateSlider("speed", "x");
            Maud.model.animation.setSpeed(newSpeed);
        } else {
            slider.disable();
            float speed = Maud.model.animation.getSpeed();
            slider.setValue(speed);
            Maud.gui.updateSlider("speed", "x");
        }
        /*
         * track time slider
         */
        slider = Maud.gui.getSlider("time");
        if (duration == 0f) {
            slider.disable();
            slider.setValue(0f);
        } else if (Maud.model.animation.isRunning()) {
            slider.disable();
            float time = Maud.model.animation.getTime();
            float fraction = time / duration;
            slider.setValue(fraction);
        } else {
            slider.enable();
            float fraction = slider.getValue();
            float newTime = fraction * duration;
            Maud.model.animation.setTime(newTime);
        }
        /*
         * track time status
         */
        String status;
        if (Maud.model.animation.isBindPoseLoaded()) {
            status = "time = n/a";
        } else {
            float time = Maud.model.animation.getTime();
            status = String.format("time = %.3f / %.3f sec", time, duration);
        }
        Maud.gui.setStatusText("trackTime", status);
    }

    /**
     * Update this window after loading an animation.
     */
    public void updateAfterLoad() {
        setSliders();
        update();
        updateName();
        Maud.gui.boneAngle.set();
        Maud.gui.boneOffset.set();
        Maud.gui.boneScale.set();
    }

    /**
     * Update the name label and rename button after renaming or loading. Also
     * update the track counts.
     */
    public void updateName() {
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
    // *************************************************************************
    // private methods

    /**
     * Preset the sliders prior to update() during a load.
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
}
