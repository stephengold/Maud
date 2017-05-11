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
package maud.model;

import com.jme3.animation.Animation;
import com.jme3.animation.BoneTrack;
import com.jme3.animation.Track;
import com.jme3.math.Transform;
import java.util.logging.Logger;
import jme3utilities.MyAnimation;
import jme3utilities.Validate;
import maud.Maud;
import maud.Util;

/**
 * The MVC model of the loaded animation in the Maud application. For loading
 * purposes, the bind pose is treated as an animation.
 *
 * @author Stephen Gold sgold@sonic.net
 */
public class LoadedAnimation implements Cloneable {
    // *************************************************************************
    // constants and loggers

    /**
     * message logger for this class
     */
    final private static Logger logger = Logger.getLogger(
            LoadedAnimation.class.getName());
    /**
     * dummy animation name used to indicate bind pose, that is, no animation
     * loaded
     */
    final public static String bindPoseName = "( bind pose )";
    // *************************************************************************
    // fields

    /**
     * true &rarr; play continuously ("loop"), false &rarr; play once-through
     * and then pause
     */
    private boolean continueFlag = true;
    /**
     * true &rarr; explicitly paused, false &rarr; running
     */
    private boolean pausedFlag = false;
    /**
     * true &rarr; reverse playback direction ("pong") at limits, false &rarr;
     * wrap time at limits
     */
    private boolean reverseFlag = false;
    /**
     * playback speed and direction when not paused (1 &rarr; forward at normal
     * speed)
     */
    private float speed = 1f;
    /**
     * current animation time (in seconds, &ge;0)
     */
    private float time = 0f;
    /**
     * name of the loaded animation, or bindPoseName
     */
    private String loadedName = null;
    // *************************************************************************
    // new methods exposed

    /**
     * Calculate the current transform of the indexed bone.
     *
     * @param boneIndex which bone to calculate
     * @param storeResult (modified if not null)
     * @return transform (either storeResult or a new instance)
     */
    public Transform boneTransform(int boneIndex, Transform storeResult) {
        if (storeResult == null) {
            storeResult = new Transform();
        }

        Animation animation = getLoadedAnimation();
        if (animation == null) {
            storeResult.loadIdentity();
        } else {
            BoneTrack track = MyAnimation.findTrack(animation, boneIndex);
            if (track == null) {
                storeResult.loadIdentity();
            } else {
                Util.boneTransform(track, time, storeResult);
            }
        }

        return storeResult;
    }

    /**
     * Count the number of bone tracks in the loaded animation.
     *
     * @return count (&ge;0)
     */
    public int countBoneTracks() {
        int count = 0;
        Animation animation = getLoadedAnimation();
        if (animation != null) {
            Track[] tracks = animation.getTracks();
            for (Track track : tracks) {
                if (track instanceof BoneTrack) {
                    count++;
                }
            }
        }

        assert count >= 0 : count;
        return count;
    }

    /**
     * Count the total number of tracks in the loaded animation.
     *
     * @return count (&ge;0)
     */
    public int countTracks() {
        int count;
        Animation animation = getLoadedAnimation();
        if (animation == null) {
            count = 0;
        } else {
            Track[] tracks = animation.getTracks();
            count = tracks.length;
        }

        assert count >= 0 : count;
        return count;
    }

    /**
     * Access the loaded animation.
     *
     * @return the pre-existing instance, or null if in bind pose
     */
    Animation getLoadedAnimation() {
        Animation result;
        if (isBindPoseLoaded()) {
            result = null;
        } else {
            result = Maud.model.cgm.getAnimation(loadedName);
        }

        return result;
    }

    /**
     * Read the duration of the loaded animation.
     *
     * @return time (in seconds, &ge;0)
     */
    public float getDuration() {
        float result;
        if (isBindPoseLoaded()) {
            result = 0f;
        } else {
            Animation animation = getLoadedAnimation();
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
    public String getName() {
        assert loadedName != null;
        return loadedName;
    }

    /**
     * Read the animation speed.
     *
     * @return relative speed (1 &rarr; normal)
     */
    public float getSpeed() {
        return speed;
    }

    /**
     * Read the animation time.
     *
     * @return seconds since start (&ge;0)
     */
    public float getTime() {
        assert time >= 0f : time;
        return time;
    }

    /**
     * Test whether bind pose is loaded.
     *
     * @return true if it's loaded, false if an animation is loaded
     */
    public boolean isBindPoseLoaded() {
        if (loadedName.equals(bindPoseName)) {
            return true;
        } else {
            return false;
        }
    }

    /**
     * Test whether the track time is changing.
     *
     * @return true time is changing, false otherwise
     */
    public boolean isMoving() {
        boolean running;

        if (pausedFlag) {
            running = false;
        } else if (speed == 0f) {
            running = false;
        } else {
            running = true;
        }

        return running;
    }

    /**
     * Test whether the loaded animation is explicitly paused.
     *
     * @return true if paused, false otherwise
     */
    public boolean isPaused() {
        return pausedFlag;
    }

    /**
     * Load the named animation at t=0 with the default speed.
     *
     * @param name which animation (not null)
     */
    public void load(String name) {
        Validate.nonNull(name, "animation name");

        if (name.equals(bindPoseName)) {
            /*
             * Load bind pose.
             */
            loadBindPose();

        } else {
            float duration = Maud.model.cgm.getDuration(name);
            float playSpeed;
            if (duration == 0f) {
                /*
                 * The animation consists of a single pose: set speed to zero.
                 */
                playSpeed = 0f;
            } else {
                /*
                 * Start the animation looping at normal speed.
                 */
                playSpeed = 1f;
            }
            loadAnimation(name, playSpeed);
        }
    }

    /**
     * Load the named animation (not bind pose) at t=0 with the specified
     * playback speed.
     *
     * @param name which animation (not null)
     * @param newSpeed playback speed
     */
    public void loadAnimation(String name, float newSpeed) {
        Validate.nonNull(name, "animation name");
        assert !name.equals(bindPoseName);

        loadedName = name;
        speed = newSpeed;
        time = 0f;

        Maud.model.pose.setToAnimation();
    }

    /**
     * Load the bind pose.
     */
    public void loadBindPose() {
        loadedName = bindPoseName;
        speed = 0f;
        time = 0f;

        Maud.model.pose.resetToBind();
    }

    /**
     * Add a copy of the loaded animation to the CG model.
     *
     * @param animationName name for the new animation (not null, not empty, not
     * bindPoseName, not in use)
     */
    public void newCopy(String animationName) {
        Validate.nonEmpty(animationName, "animation name");
        assert !animationName.equals(bindPoseName) : animationName;
        assert !Maud.model.cgm.hasAnimation(animationName) : animationName;

        Animation loaded = Maud.model.animation.getLoadedAnimation();
        float duration = Maud.model.animation.getDuration();
        Animation copyAnim = new Animation(animationName, duration);
        if (loaded != null) {
            Track[] loadedTracks = loaded.getTracks();
            for (Track track : loadedTracks) {
                Track clone = track.clone();
                copyAnim.addTrack(clone);
            }
        }
        Maud.model.cgm.addAnimation(copyAnim);
    }

    /**
     * Add a new pose animation to the CG model. The new animation has zero
     * duration, a single keyframe at t=0, and all the tracks are BoneTracks,
     * set to the current pose.
     *
     * @param animationName name for the new animation (not null, not empty, not
     * bindPoseName, not in use)
     */
    public void newPose(String animationName) {
        Validate.nonEmpty(animationName, "animation name");
        assert !animationName.equals(bindPoseName) : animationName;
        assert !Maud.model.cgm.hasAnimation(animationName) : animationName;

        Animation poseAnim = Maud.model.pose.capture(animationName);
        Maud.model.cgm.addAnimation(poseAnim);
    }

    /**
     * Rename the loaded animation.
     *
     * @param newName (not null)
     */
    void rename(String newName) {
        loadedName = newName;
    }

    /**
     * Alter whether the loaded animation will play continuously.
     *
     * @param newSetting true &rarr; play continuously, false &rarr; play
     * once-through and then pause
     */
    public void setContinue(boolean newSetting) {
        continueFlag = newSetting;
    }

    /**
     * Alter whether the loaded animation is explicitly paused.
     *
     * @param newSetting true &rarr; paused, false &rarr; running
     */
    public void setPaused(boolean newSetting) {
        pausedFlag = newSetting;
    }

    /**
     * Alter whether the loaded animation will reverse direction when it reaches
     * a limit.
     *
     * @param newSetting true &rarr; reverse, false &rarr; wrap
     */
    public void setReverse(boolean newSetting) {
        reverseFlag = newSetting;
    }

    /**
     * Alter the playback speed and/or direction.
     *
     * @param newSpeed (1 &rarr; forward at normal speed)
     */
    public void setSpeed(float newSpeed) {
        speed = newSpeed;
    }

    /**
     * Alter the animation time. Has no effect in bind pose or if the loaded
     * animation has zero duration.
     *
     * @param newTime seconds since start (&ge;0, &le;duration)
     */
    public void setTime(float newTime) {
        float duration = getDuration();
        Validate.inRange(newTime, "animation time", 0f, duration);

        if (duration > 0f) {
            time = newTime;
            Maud.model.pose.setToAnimation();
        }
    }

    /**
     * Toggle between paused and running.
     */
    public void togglePaused() {
        setPaused(!pausedFlag);
    }

    /**
     * Test whether the loaded animation will play continuously.
     *
     * @return true if continuous loop, false otherwise
     */
    public boolean willContinue() {
        return continueFlag;
    }

    /**
     * Test whether the loaded animation will reverse direction at limits.
     *
     * @return true if it will reverse, false otherwise
     */
    public boolean willReverse() {
        return reverseFlag;
    }
    // *************************************************************************
    // Object methods

    /**
     * Create a deep copy of this object.
     *
     * @return a new object, equivalent to this one
     * @throws CloneNotSupportedException if superclass isn't cloneable
     */
    @Override
    public Object clone() throws CloneNotSupportedException {
        LoadedAnimation clone = (LoadedAnimation) super.clone();
        return clone;
    }
}
