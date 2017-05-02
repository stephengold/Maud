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
public class LoadedAnimation {
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
     * animation speed (0 &rarr; paused, 1 &rarr; normal speed)
     */
    private float speed = 0f;
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
        BoneTrack track = MyAnimation.findTrack(animation, boneIndex);
        if (track == null) {
            storeResult.loadIdentity();
        } else {
            Util.boneTransform(track, time, storeResult);
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
            result = Maud.model.getAnimation(loadedName);
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
     * Test whether an animation is running.
     *
     * @return true if an animation is running, false otherwise
     */
    public boolean isRunning() {
        if (speed == 0f) {
            return false;
        } else {
            return true;
        }
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
            float duration = Maud.model.getDuration(name);
            float speed;
            if (duration == 0f) {
                /*
                 * The animation consists of a single pose: set speed to zero.
                 */
                speed = 0f;
            } else {
                /*
                 * Start the animation looping at normal speed.
                 */
                speed = 1f;
            }
            loadAnimation(name, speed);
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

        Maud.gui.animation.poseSkeleton();
        Maud.gui.animation.updateAfterLoad();
    }

    /**
     * Load the bind pose.
     */
    public void loadBindPose() {
        loadedName = bindPoseName;
        speed = 0f;
        time = 0f;
        Maud.gui.animation.resetPose();
        Maud.gui.animation.updateAfterLoad();
    }

    /**
     * Rename the loaded animation.
     *
     * @param newName (not null)
     */
    void rename(String newName) {
        loadedName = newName;
        Maud.gui.animation.updateName();
    }

    /**
     * Alter the animation speed.
     *
     * @param newSpeed animation speed (0 &rarr; paused, 1 &rarr; normal speed)
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
            Maud.gui.animation.poseSkeleton();
            if (isRunning()) {
                Maud.gui.animation.update();
            }
        }
    }
}
