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
package maud.model.cgm;

import java.util.logging.Logger;
import jme3utilities.Validate;

/**
 * The MVC model of animation playback options for a particular C-G model. For
 * the target model in retargeted pose, these options are disregarded.
 *
 * @author Stephen Gold sgold@sonic.net
 */
public class PlayOptions implements Cloneable {
    // *************************************************************************
    // constants and loggers

    /**
     * message logger for this class
     */
    final private static Logger logger
            = Logger.getLogger(PlayOptions.class.getName());
    // *************************************************************************
    // fields

    /**
     * true &rarr; play continuously ("loop"), false &rarr; play once-through
     * and then pause
     */
    private boolean continueFlag = true;
    /**
     * true &rarr; explicitly paused, false &rarr; running perhaps at speed=0
     */
    private boolean pausedFlag = false;
    /**
     * true &rarr; root bones pinned to bind transform, false &rarr; free to
     * transform
     */
    private boolean pinnedFlag = false;
    /**
     * true &rarr; reverse playback direction ("pong") at limits, false &rarr;
     * wrap time at limits
     */
    private boolean reverseFlag = false;
    /**
     * C-G model being played (set by {@link #setCgm(Cgm)})
     */
    private Cgm cgm = null;
    /**
     * current animation time for playback (in seconds, &ge;0)
     */
    private float currentTime = 0f;
    /**
     * lower animation-time limit (in seconds, &ge;0, &le;upperLimit)
     */
    private float lowerLimit = 0f;
    /**
     * relative playback speed and direction, when not paused (1 &rarr; forward
     * at normal speed)
     */
    private float speed = 1f;
    /**
     * upper animation-time limit (in seconds, &ge;lowerLimit)
     */
    private float upperLimit = Float.MAX_VALUE;
    // *************************************************************************
    // new methods exposed

    /**
     * Read the lower time limit.
     *
     * @return limit (in seconds, &ge;0, &le;upperLimit)
     */
    public float getLowerLimit() {
        assert lowerLimit >= 0f : lowerLimit;
        assert lowerLimit <= upperLimit : lowerLimit;
        return lowerLimit;
    }

    /**
     * Read the playback speed and direction.
     *
     * @return relative speed (1 &rarr; forward at normal speed)
     */
    public float getSpeed() {
        return speed;
    }

    /**
     * Read the animation time for playback.
     *
     * @return seconds since start (&ge;0)
     */
    public float getTime() {
        assert currentTime >= 0f : currentTime;
        return currentTime;
    }

    /**
     * Read the upper time limit.
     *
     * @return limit (in seconds, &ge;lowerLimit)
     */
    public float getUpperLimit() {
        assert upperLimit >= lowerLimit : upperLimit;
        return upperLimit;
    }

    /**
     * Test whether playback is explicitly paused.
     *
     * @return true if paused, false otherwise
     */
    public boolean isPaused() {
        return pausedFlag;
    }

    /**
     * Test whether the root bones are pinned to bind transform.
     *
     * @return true if pinned, false otherwise
     */
    public boolean isPinned() {
        return pinnedFlag;
    }

    /**
     * Reset the time limits.
     */
    public void resetLimits() {
        lowerLimit = 0f;
        upperLimit = Float.MAX_VALUE;
    }

    /**
     * Alter which C-G model displays the pose. (Invoked only during
     * initialization and cloning.)
     *
     * @param newCgm (not null, alias created)
     */
    void setCgm(Cgm newCgm) {
        assert newCgm != null;
        assert newCgm.getPlay() == this;

        cgm = newCgm;
    }

    /**
     * Alter whether the animation plays continuously.
     *
     * @param newSetting true &rarr; play continuously, false &rarr; play
     * once-through and then pause
     */
    public void setContinue(boolean newSetting) {
        continueFlag = newSetting;
    }

    /**
     * Alter the lower time limit.
     *
     * @param newLimit (in seconds, &ge;0)
     */
    public void setLowerLimit(float newLimit) {
        Validate.inRange(newLimit, "new limit", 0f, upperLimit);
        lowerLimit = newLimit;
    }

    /**
     * Alter whether playback is explicitly paused.
     *
     * @param newSetting true &rarr; paused, false &rarr; running
     */
    public void setPaused(boolean newSetting) {
        pausedFlag = newSetting;
    }

    /**
     * Alter whether the root bones are pinned to bind transform.
     *
     * @param newSetting true &rarr; pinned, false &rarr; free to translate
     */
    public void setPinned(boolean newSetting) {
        pinnedFlag = newSetting;
    }

    /**
     * Alter whether playback will reverse direction when it reaches a limit.
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
     * Alter the animation time and update the displayed pose unless it's
     * frozen. Has no effect in bind pose or if the loaded animation has zero
     * duration.
     *
     * @param newTime seconds since start (&ge;0, &le;duration)
     */
    public void setTime(float newTime) {
        float duration = cgm.getAnimation().duration();
        Validate.inRange(newTime, "new time", 0f, duration);

        if (duration > 0f) {
            currentTime = newTime;
            boolean frozen = cgm.getPose().isFrozen();
            if (!frozen) {
                cgm.getPose().setToAnimation();
            }
        }
    }

    /**
     * Alter the specified time.
     *
     * @param whichTime which time to alter (not null)
     * @param newValue (in seconds, &ge;0)
     */
    public void setTime(PlayTimes whichTime, float newValue) {
        Validate.nonNegative(newValue, "new value");

        switch (whichTime) {
            case Current:
                setTime(newValue);
                break;

            case LowerLimit:
                setLowerLimit(newValue);
                break;

            case UpperLimit:
                setUpperLimit(newValue);
                break;

            default:
                throw new IllegalArgumentException();
        }
    }

    /**
     * Alter the upper time limit.
     *
     * @param newLimit (in seconds)
     */
    public void setUpperLimit(float newLimit) {
        Validate.inRange(newLimit, "new limit", lowerLimit, Float.MAX_VALUE);
        upperLimit = newLimit;
    }

    /**
     * Toggle between explicitly paused and playing.
     */
    public void togglePaused() {
        setPaused(!pausedFlag);
    }

    /**
     * Test whether the playback will cycle continuously.
     *
     * @return true if continuous cycle, false if pause at limit
     */
    public boolean willContinue() {
        return continueFlag;
    }

    /**
     * Test whether the playback will reverse direction at limits.
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
     * @throws CloneNotSupportedException if the superclass isn't cloneable
     */
    @Override
    public PlayOptions clone() throws CloneNotSupportedException {
        PlayOptions clone = (PlayOptions) super.clone();
        return clone;
    }
}
