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
 * Stephen Gold's name may not be used to endorse or promote products
 derived from this software without specific prior written permission.

 THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND
 ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
 WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 DISCLAIMED. IN NO EVENT SHALL STEPHEN GOLD BE LIABLE FOR ANY
 DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES
 (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
 LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND
 ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */
package maud;

import com.jme3.animation.Animation;
import com.jme3.animation.BoneTrack;
import com.jme3.math.Quaternion;
import com.jme3.math.Transform;
import com.jme3.math.Vector3f;
import de.lessvoid.nifty.controls.Slider;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;
import jme3utilities.MyAnimation;
import jme3utilities.MyString;
import jme3utilities.nifty.BasicScreenController;

/**
 * The controller for the "Animation Tool" window in Maud's "3D View" screen.
 *
 * @author Stephen Gold sgold@sonic.net
 */
class AnimationTool extends WindowController {
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
     * animation speed (0 &rarr; paused, 1 &rarr; normal speed)
     */
    private float speed = 0f;
    /**
     * animation time (in seconds, &ge;0)
     */
    private float time = 0f;
    /**
     * user transforms for the current (temporary) pose
     */
    final private List<Transform> currentPose = new ArrayList<>(30);
    /**
     * name of the loaded animation, or bindPoseName
     */
    private String loadedName = null;
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
     */
    Transform copyBoneTransform(int boneIndex) {
        Transform result = currentPose.get(boneIndex);
        result = result.clone();

        return result;
    }

    /**
     * Delete the loaded animation and (if successful) load bind pose.
     */
    void delete() {
        boolean success = Maud.model.deleteAnimation();
        if (success) {
            loadBindPose();
        }
    }

    /**
     * Read the duration of the loaded animation.
     *
     * @return time (in seconds, &ge;0)
     */
    float getDuration() {
        float result;
        if (isBindPoseLoaded()) {
            result = 0f;
        } else {
            Animation animation;
            animation = Maud.model.getAnimation(loadedName);
            result = animation.getLength();
        }

        assert result >= 0f : result;
        return result;
    }

    /**
     * Read the name of the loaded animation.
     *
     * @return the name, or bindPoseName if in bind pose (not null)
     */
    String getName() {
        assert loadedName != null;
        return loadedName;
    }

    /**
     * Read the animation speed.
     *
     * @return relative speed (1 &rarr; normal)
     */
    float getSpeed() {
        return speed;
    }

    /**
     * Read the animation time.
     *
     * @return seconds since start (&ge;0)
     */
    float getTime() {
        assert time >= 0f : time;
        return time;
    }

    /**
     * Test whether bind pose is loaded.
     *
     * @return true if it's loaded, false if an animation is loaded
     */
    boolean isBindPoseLoaded() {
        if (loadedName.equals(DddGui.bindPoseName)) {
            return true;
        } else {
            return false;
        }
    }

    /**
     * Test whether an animation is running.
     *
     * @return true if an animation is running, false otherwise
     */
    boolean isRunning() {
        if (speed == 0f) {
            return false;
        } else {
            return true;
        }
    }

    /**
     * Load the named animation (not bind pose) at t=0 and set the playback
     * speed.
     *
     * @para name which animation (not null)
     * @param newSpeed
     */
    void loadAnimation(String name, float newSpeed) {
        assert name != null;
        assert !name.equals(DddGui.bindPoseName);

        loadedName = name;
        speed = newSpeed;
        time = 0f;

        poseSkeleton();
        setSliders();
        update();
        updateDescription();
        Maud.gui.boneAngle.set();
        Maud.gui.boneOffset.set();
        Maud.gui.boneScale.set();
    }

    /**
     * Load the bind pose.
     */
    void loadBindPose() {
        loadedName = DddGui.bindPoseName;
        speed = 0f;
        time = 0f;

        int boneCount = Maud.model.getBoneCount();
        currentPose.clear();
        for (int boneIndex = 0; boneIndex < boneCount; boneIndex++) {
            Transform transform = new Transform();
            currentPose.add(transform);
        }
        Maud.viewState.poseSkeleton(currentPose);

        setSliders();
        update();
        updateDescription();
        Maud.gui.boneAngle.set();
        Maud.gui.boneOffset.set();
        Maud.gui.boneScale.set();
    }

    /**
     * Pose the loaded model per the loaded animation.
     */
    void poseSkeleton() {
        Animation animation = Maud.model.getLoadedAnimation();
        int boneCount = Maud.model.getBoneCount();
        for (int boneIndex = 0; boneIndex < boneCount; boneIndex++) {
            Transform transform = currentPose.get(boneIndex);
            BoneTrack track = MyAnimation.findTrack(animation, boneIndex);
            if (track == null) {
                transform.loadIdentity();
            } else {
                Util.boneTransform(track, time, transform);
            }
        }

        Maud.viewState.poseSkeleton(currentPose);
    }

    /**
     * Rename the loaded animation.
     *
     * @param newName new name (not null)
     */
    void rename(String newName) {
        loadedName = newName;
        updateDescription();
    }

    /**
     * Alter the user rotation of the indexed bone.
     */
    void setBoneRotation(int boneIndex, Quaternion rotation) {
        assert rotation != null;

        Transform boneTransform = currentPose.get(boneIndex);
        boneTransform.setRotation(rotation);
        Maud.viewState.poseSkeleton(currentPose);
    }

    /**
     * Alter the user scale of the indexed bone.
     */
    void setBoneScale(int boneIndex, Vector3f scale) {
        assert scale != null;

        Transform boneTransform = currentPose.get(boneIndex);
        boneTransform.setScale(scale);
        Maud.viewState.poseSkeleton(currentPose);
    }

    /**
     * Alter the user translation of the indexed bone.
     */
    void setBoneTranslation(int boneIndex, Vector3f translation) {
        assert translation != null;

        Transform boneTransform = currentPose.get(boneIndex);
        boneTransform.setTranslation(translation);
        Maud.viewState.poseSkeleton(currentPose);
    }

    /**
     * Alter the animation time. No effect in bind pose or if the loaded
     * animation has zero duration.
     *
     * @param newTime seconds since start (&ge;0, &le;duration)
     */
    void setTime(float newTime) {
        assert newTime >= 0f : newTime;
        assert newTime <= getDuration() : newTime;

        if (!isBindPoseLoaded() && getDuration() > 0f) {
            time = newTime;
            poseSkeleton();
            update();
        }
    }

    /**
     * Update this window after a change to duration, speed, or time.
     */
    void update() {
        /*
         * speed slider and its status
         */
        Slider slider = Maud.gui.getSlider("speed");
        float duration = getDuration();
        if (duration > 0f) {
            slider.enable();
            float newSpeed = Maud.gui.updateSlider("speed", "x");
            speed = newSpeed;
        } else {
            slider.disable();
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
        } else if (speed == 0f) {
            slider.enable();
            float fraction = slider.getValue();
            float newTime = fraction * duration;
            time = newTime;
            poseSkeleton();
        } else {
            slider.disable();
            float fraction = time / duration;
            slider.setValue(fraction);
        }
        /*
         * track time status
         */
        String status;
        if (isBindPoseLoaded()) {
            status = "time = n/a";
        } else {
            status = String.format("time = %.3f / %.3f sec", time, duration);
        }
        Maud.gui.setStatusText("trackTime", status);
    }
    // *************************************************************************
    // private methods

    /**
     * Preset the sliders prior to update() during a load.
     */
    private void setSliders() {
        Slider slider = Maud.gui.getSlider("speed");
        slider.setValue(speed);

        slider = Maud.gui.getSlider("time");
        float duration = getDuration();
        if (duration == 0f) {
            slider.setValue(0f);
        } else {
            float fraction = time / duration;
            slider.setValue(fraction);
        }
    }

    /**
     * Update the description after changing the name.
     */
    private void updateDescription() {
        String description;
        if (isBindPoseLoaded()) {
            description = DddGui.bindPoseName;
        } else {
            description = "Loaded " + MyString.quote(loadedName);
        }
        Maud.gui.setStatusText("animationDescription", description);
    }
}
