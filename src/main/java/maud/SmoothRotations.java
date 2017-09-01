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

import com.jme3.math.FastMath;
import com.jme3.math.Quaternion;
import jme3utilities.Validate;
import jme3utilities.math.MyMath;

/**
 * Enumerate and implement some smoothing techniques on time sequences of
 * Quaternion values.
 *
 * @author Stephen Gold sgold@sonic.net
 */
public enum SmoothRotations {
    // *************************************************************************
    // values

    /**
     * cyclic normalized linear (Nlerp) smoothing
     */
    LoopNlerp,
    /**
     * acyclic normalized linear (Nlerp) smoothing
     */
    Nlerp;
    // *************************************************************************
    // new methods exposed

    /**
     * Smooth the rotations in a time sequence using this technique.
     *
     * @param times (not null, unaffected, length&gt;0, in strictly ascending
     * order)
     * @param cycleTime end time for looping (&ge;times[last])
     * @param samples input function values (not null, unaffected, same length
     * as times)
     * @param width width of time window (&ge;0, &le;cycleTime)
     * @param storeResult (distinct from samples, modified if not null)
     * @return array of smoothed quaternions (either storeResult or a new
     * instance)
     */
    public Quaternion[] smooth(float[] times, float cycleTime,
            Quaternion[] samples, float width, Quaternion[] storeResult) {
        assert times.length > 0;
        assert times.length == samples.length;
        int last = times.length - 1;
        assert cycleTime >= times[last] : cycleTime;
        Validate.inRange(width, "width", 0f, cycleTime);
        if (storeResult == null) {
            storeResult = new Quaternion[times.length];
        }

        switch (this) {
            case Nlerp:
                lerp(times, samples, width, storeResult);
                break;

            case LoopNlerp:
                if (times[last] == cycleTime) {
                    if (last > 1) { // ignore the final point
                        loopLerp(last - 1, times, cycleTime, samples, width,
                                storeResult);
                        storeResult[last] = storeResult[0].clone();
                    } else { // fall back on acyclic
                        lerp(times, samples, width, storeResult);
                    }
                } else {
                    loopLerp(last, times, cycleTime, samples, width,
                            storeResult);
                }
                break;

            default:
                throw new IllegalStateException();
        }

        return storeResult;
    }

    /**
     * Smooth the quaternions in an acyclic time sequence using normalized
     * linear (Nlerp) smoothing. TODO compare signs
     *
     * @param times (not null, unaffected, length&gt;0, in strictly ascending
     * order)
     * @param samples input function values (not null, unaffected, same length
     * as times, each not null)
     * @param width width of time window (&ge;0)
     * @param storeResult (modified if not null)
     * @return array of smoothed quaternions (either storeResult or a new
     * instance)
     */
    public static Quaternion[] lerp(float[] times, Quaternion[] samples,
            float width, Quaternion[] storeResult) {
        Validate.nonNull(times, "times");
        assert times.length > 0;
        Validate.nonNull(samples, "samples");
        assert times.length == samples.length;
        Validate.nonNegative(width, "width");
        if (storeResult == null) {
            storeResult = new Quaternion[times.length];
        }

        int last = times.length - 1;
        float halfWidth = width / 2f;
        for (int i = 0; i <= last; i++) {
            Quaternion sumQuaternion = storeResult[i];
            if (sumQuaternion == null) {
                sumQuaternion = new Quaternion();
                storeResult[i] = sumQuaternion;
            } else {
                sumQuaternion.set(0f, 0f, 0f, 0f);
            }
            float iTime = times[i];
            float sumWeight = 0f;
            for (int j = 0; j <= last; j++) {
                float jTime = times[j];
                float dt = iTime - jTime;
                dt = FastMath.abs(dt);
                if (dt < halfWidth) {
                    float weight = 1f - dt / halfWidth;
                    Util.accumulateScaled(sumQuaternion, samples[j], weight);
                    sumWeight += weight;
                }
            }
            assert sumWeight > 0f : sumWeight;
            sumQuaternion.normalizeLocal();
        }

        return storeResult;
    }

    /**
     * Smooth the quaternions in a cyclic time sequence using normalized linear
     * (Nlerp) smoothing. TODO compare signs
     *
     * @param last (index of the last point, &ge;1)
     * @param times (not null, unaffected, in strictly ascending order,
     * times[0]==0)
     * @param cycleTime cycle time (&gt;times[last])
     * @param samples input function values (not null, unaffected, each not
     * null)
     * @param width width of time window (&ge;0, &le;cycleTime)
     * @param storeResult (modified if not null)
     * @return array of smoothed quaternions (either storeResult or a new
     * instance)
     */
    public static Quaternion[] loopLerp(int last, float[] times, float cycleTime,
            Quaternion[] samples, float width, Quaternion[] storeResult) {
        Validate.positive(last, "last");
        Validate.nonNull(samples, "samples");
        assert times.length > last : times.length;
        assert samples.length > last : samples.length;
        assert cycleTime > times[last] : cycleTime;
        Validate.inRange(width, "width", 0f, cycleTime);
        if (storeResult == null) {
            storeResult = new Quaternion[times.length];
        }

        float halfWidth = width / 2f;
        for (int i = 0; i <= last; i++) {
            Quaternion sumQuaternion = storeResult[i];
            if (sumQuaternion == null) {
                sumQuaternion = new Quaternion();
                storeResult[i] = sumQuaternion;
            } else {
                sumQuaternion.set(0f, 0f, 0f, 0f);
            }
            float iTime = times[i];
            float sumWeight = 0f;
            for (int j = 0; j <= last; j++) {
                float jTime = times[j];
                float dt = MyMath.modulo(iTime - jTime, cycleTime);
                if (dt > cycleTime / 2f) {
                    dt -= cycleTime;
                }
                dt = FastMath.abs(dt);
                if (dt < halfWidth) {
                    float weight = 1f - dt / halfWidth;
                    Util.accumulateScaled(sumQuaternion, samples[j], weight);
                    sumWeight += weight;
                }
            }
            assert sumWeight > 0f : sumWeight;
            sumQuaternion.normalizeLocal();
        }

        return storeResult;
    }
}
