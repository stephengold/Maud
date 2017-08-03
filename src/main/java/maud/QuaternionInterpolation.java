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
import jme3utilities.Validate;
import jme3utilities.math.MyArray;
import jme3utilities.math.MyQuaternion;

/**
 * Enumerate (and implement) some interpolation techniques on Quaternion values.
 *
 * @author Stephen Gold sgold@sonic.net
 */
public enum QuaternionInterpolation {
    // *************************************************************************
    // values

    /**
     * looping normalized linear (Nlerp) interpolation
     */
    LoopNlerp,
    /**
     * looping spherical (Slerp) interpolation
     */
    LoopSlerp,
    /**
     * non-looping normalized linear (Nlerp) interpolation
     */
    Nlerp,
    /**
     * non-looping spherical (Slerp) interpolation
     */
    Slerp;
    // *************************************************************************
    // new methods exposed

    /**
     * Interpolate among quaternions in a time sequence using this technique.
     *
     * @param time the parameter value
     * @param times to interpolate among (not null, unaffected, length>0, in
     * ascending order)
     * @param duration used for looping (&ge;times[last])
     * @param quaternions to interpolate among (not null, unaffected, same
     * length as times)
     * @param storeResult (modified if not null)
     * @return quaternion (either storeResult or a new instance)
     */
    public Quaternion interpolate(float time, float[] times, float duration,
            Quaternion[] quaternions, Quaternion storeResult) {
        Validate.nonNull(times, "times");
        assert times.length > 0;
        assert times.length == quaternions.length;
        int last = times.length - 1;
        assert duration >= times[last] : duration;
        if (storeResult == null) {
            storeResult = new Quaternion();
        }

        if (last == 0) {
            storeResult.set(quaternions[0]);
            return storeResult;
        }

        switch (this) {
            case LoopNlerp:
                if (times[last] == duration) {
                    if (last > 1) { // ignore the last point
                        loopNlerp(time, last - 1, times, duration, quaternions,
                                storeResult);
                    } else { // fall back on non-looping
                        nlerp(time, times, quaternions, storeResult);
                    }
                } else {
                    loopNlerp(time, last, times, duration, quaternions,
                            storeResult);
                }
                break;

            case LoopSlerp:
                if (times[last] == duration) {
                    if (last > 1) { // ignore the last point
                        loopSlerp(time, last - 1, times, duration, quaternions,
                                storeResult);
                    } else { // fall back on non-looping
                        slerp(time, times, quaternions, storeResult);
                    }
                } else {
                    loopSlerp(time, last, times, duration, quaternions,
                            storeResult);
                }
                break;

            case Nlerp:
                nlerp(time, times, quaternions, storeResult);
                break;

            case Slerp:
                slerp(time, times, quaternions, storeResult);
                break;

            default:
                throw new IllegalStateException();
        }

        return storeResult;
    }

    /**
     * Interpolate among quaternions in a time sequence using looping normalized
     * linear (Nlerp) interpolation.
     *
     * @param time (in seconds, &ge;0, &le;duration)
     * @param last (index of the last point, &ge;1)
     * @param times (not null, unaffected)
     * @param duration (in seconds, &gt;times[last])
     * @param quaternions (not null, unaffected)
     * @param storeResult (modified if not null)
     * @return interpolated quaternion (either storeResult or a new instance)
     */
    public static Quaternion loopNlerp(float time, int last, float[] times,
            float duration, Quaternion[] quaternions, Quaternion storeResult) {
        Validate.inRange(time, "time", 0f, duration);
        Validate.positive(last, "last");
        Validate.nonNull(times, "times");
        Validate.nonNull(quaternions, "quaternions");
        assert times.length > last : times.length;
        assert quaternions.length > last : quaternions.length;
        assert duration > times[last] : duration;
        if (storeResult == null) {
            storeResult = new Quaternion();
        }

        int index1 = MyArray.findPreviousIndex(time, times);
        storeResult.set(quaternions[index1]);

        int index2; // keyframe index
        float interval; // interval between keyframes
        if (index1 < last) {
            index2 = index1 + 1;
            interval = times[index2] - times[index1];
        } else {
            index2 = 0;
            interval = duration - times[index1];
        }
        assert interval > 0f : interval;

        float t = (time - times[index1]) / interval;
        Quaternion q2 = quaternions[index2];
        if (MyQuaternion.ne(storeResult, q2)) {
            storeResult.nlerp(q2, t);
        }

        return storeResult;
    }

    /**
     * Interpolate among quaternions in a time sequence using looping spherical
     * (Slerp) interpolation.
     *
     * @param time (in seconds, &ge;0, &le;duration)
     * @param last (index of the last point, &ge;1)
     * @param times (not null, unaffected)
     * @param duration (in seconds, &gt;times[last])
     * @param quaternions (not null, unaffected)
     * @param storeResult (modified if not null)
     * @return interpolated quaternion (either storeResult or a new instance)
     */
    public static Quaternion loopSlerp(float time, int last, float[] times,
            float duration, Quaternion[] quaternions, Quaternion storeResult) {
        Validate.inRange(time, "time", 0f, duration);
        Validate.positive(last, "last");
        Validate.nonNull(times, "times");
        Validate.nonNull(quaternions, "quaternions");
        assert times.length > last : times.length;
        assert quaternions.length > last : quaternions.length;
        assert duration > times[last] : duration;
        if (storeResult == null) {
            storeResult = new Quaternion();
        }

        int index1 = MyArray.findPreviousIndex(time, times);
        storeResult.set(quaternions[index1]);

        int index2; // keyframe index
        float interval; // interval between keyframes
        if (index1 < last) {
            index2 = index1 + 1;
            interval = times[index2] - times[index1];
        } else {
            index2 = 0;
            interval = duration - times[index1];
        }
        assert interval > 0f : interval;

        float t = (time - times[index1]) / interval;
        Quaternion q2 = quaternions[index2];
        if (MyQuaternion.ne(storeResult, q2)) {
            q2 = q2.clone(); // slerp() may modify q2!
            storeResult.slerp(q2, t);
        }

        return storeResult;
    }

    /**
     * Interpolate among quaternions in a time sequence using non-looping
     * normalized linear (Nlerp) interpolation. This is essentially what
     * AnimControl uses to interpolate rotations in bone tracks.
     *
     * @param time (&ge;times[0])
     * @param times (not null, unaffected, length&gt;0, in ascending order)
     * @param quaternions (not null, unaffected, same length as times)
     * @param storeResult (modified if not null)
     * @return interpolated quaternion (either storeResult or a new instance)
     */
    public static Quaternion nlerp(float time, float[] times,
            Quaternion[] quaternions, Quaternion storeResult) {
        Validate.nonNull(times, "times");
        assert times.length > 0;
        assert time >= times[0] : time;
        Validate.nonNull(quaternions, "quaternions");
        assert times.length == quaternions.length;
        if (storeResult == null) {
            storeResult = new Quaternion();
        }

        int index1 = MyArray.findPreviousIndex(time, times);
        assert index1 != -1;
        storeResult.set(quaternions[index1]);

        if (index1 < times.length - 1) { // not the last point
            int index2 = index1 + 1;
            float inter12 = times[index2] - times[index1];
            assert inter12 > 0f : inter12;
            float t = (time - times[index1]) / inter12;
            Quaternion q2 = quaternions[index2];
            if (MyQuaternion.ne(storeResult, q2)) {
                storeResult.nlerp(q2, t);
            }
        }

        return storeResult;
    }

    /**
     * Interpolate among quaternions in a time sequence using non-looping
     * spherical (Slerp) interpolation.
     *
     * @param time (&ge;times[0])
     * @param times (not null, unaffected, length&gt;0, in ascending order)
     * @param quaternions (not null, unaffected, same length as times)
     * @param storeResult (modified if not null)
     * @return interpolated quaternion (either storeResult or a new instance)
     */
    public static Quaternion slerp(float time, float[] times,
            Quaternion[] quaternions, Quaternion storeResult) {
        Validate.nonNull(times, "times");
        assert times.length > 0;
        assert time >= times[0] : time;
        Validate.nonNull(quaternions, "quaternions");
        assert times.length == quaternions.length;
        if (storeResult == null) {
            storeResult = new Quaternion();
        }

        int index1 = MyArray.findPreviousIndex(time, times);
        assert index1 != -1;
        storeResult.set(quaternions[index1]);

        if (index1 < times.length - 1) { // not the last point
            int index2 = index1 + 1;
            float inter12 = times[index2] - times[index1];
            assert inter12 > 0f : inter12;
            float t = (time - times[index1]) / inter12;
            Quaternion q2 = quaternions[index2];
            if (MyQuaternion.ne(storeResult, q2)) {
                q2 = q2.clone(); // slerp() may modify q2!
                storeResult.slerp(q2, t);
            }
        }

        return storeResult;
    }
}
