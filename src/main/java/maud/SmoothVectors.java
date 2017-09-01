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
import com.jme3.math.Vector3f;
import jme3utilities.Validate;
import jme3utilities.math.MyMath;
import jme3utilities.math.MyVector3f;

/**
 * Enumerate and implement some smoothing techniques on time sequences of
 * Vector3f values.
 *
 * @author Stephen Gold sgold@sonic.net
 */
public enum SmoothVectors {
    // *************************************************************************
    // values

    /**
     * acyclic linear (Lerp) smoothing
     */
    Lerp,
    /**
     * cyclic linear (Lerp) smoothing
     */
    LoopLerp;
    // *************************************************************************
    // new methods exposed

    /**
     * Smooth the vectors in a time sequence using this technique.
     *
     * @param times (not null, unaffected, length&gt;0, in strictly ascending
     * order)
     * @param cycleTime end time for looping (&ge;times[last])
     * @param samples input function values (not null, unaffected, same length
     * as times)
     * @param width width of time window (&ge;0, &le;cycleTime)
     * @param storeResult (distinct from samples, modified if not null)
     * @return array of smoothed vectors (either storeResult or a new instance)
     */
    public Vector3f[] smooth(float[] times, float cycleTime, Vector3f[] samples,
            float width, Vector3f[] storeResult) {
        assert times.length > 0;
        assert times.length == samples.length;
        int last = times.length - 1;
        assert cycleTime >= times[last] : cycleTime;
        Validate.inRange(width, "width", 0f, cycleTime);
        if (storeResult == null) {
            storeResult = new Vector3f[times.length];
        }

        switch (this) {
            case Lerp:
                lerp(times, samples, width, storeResult);
                break;

            case LoopLerp:
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
     * Smooth the vectors in an acyclic time sequence using linear (Lerp)
     * smoothing.
     *
     * @param times (not null, unaffected, length&gt;0, in strictly ascending
     * order)
     * @param samples input function values (not null, unaffected, same length
     * as times, each not null)
     * @param width width of time window (&ge;0)
     * @param storeResult (distinct from samples, modified if not null)
     * @return array of smoothed vectors (either storeResult or a new instance)
     */
    public static Vector3f[] lerp(float[] times, Vector3f[] samples,
            float width, Vector3f[] storeResult) {
        Validate.nonNull(times, "times");
        assert times.length > 0;
        Validate.nonNull(samples, "samples");
        assert times.length == samples.length;
        Validate.nonNegative(width, "width");
        if (storeResult == null) {
            storeResult = new Vector3f[times.length];
        }

        int last = times.length - 1;
        float halfWidth = width / 2f;
        for (int i = 0; i <= last; i++) {
            Vector3f sumVector = storeResult[i];
            if (sumVector == null) {
                sumVector = new Vector3f();
                storeResult[i] = sumVector;
            } else {
                sumVector.zero();
            }
            float iTime = times[i];
            float sumWeight = 0f;
            for (int j = 0; j <= last; j++) {
                float jTime = times[j];
                float dt = iTime - jTime;
                dt = FastMath.abs(dt);
                if (dt < halfWidth) {
                    float weight = 1f - dt / halfWidth;
                    MyVector3f.accumulateScaled(sumVector, samples[j], weight);
                    sumWeight += weight;
                }
            }
            assert sumWeight > 0f : sumWeight;
            sumVector.divideLocal(sumWeight);
        }

        return storeResult;
    }

    /**
     * Smooth the vectors in a cyclic time sequence using linear (Lerp)
     * smoothing.
     *
     * @param last (index of the last point, &ge;1)
     * @param times (not null, unaffected, in strictly ascending order,
     * times[0]==0)
     * @param cycleTime cycle time (&gt;times[last])
     * @param samples input function values (not null, unaffected, each not
     * null)
     * @param width width of time window (&ge;0, &le;cycleTime)
     * @param storeResult (distinct from samples, modified if not null)
     * @return array of smoothed vectors (either storeResult or a new instance)
     */
    public static Vector3f[] loopLerp(int last, float[] times, float cycleTime,
            Vector3f[] samples, float width, Vector3f[] storeResult) {
        Validate.positive(last, "last");
        Validate.nonNull(samples, "samples");
        assert times.length > last : times.length;
        assert samples.length > last : samples.length;
        assert cycleTime > times[last] : cycleTime;
        Validate.inRange(width, "width", 0f, cycleTime);
        if (storeResult == null) {
            storeResult = new Vector3f[times.length];
        }

        float halfWidth = width / 2f;
        for (int i = 0; i <= last; i++) {
            Vector3f sumVector = storeResult[i];
            if (sumVector == null) {
                sumVector = new Vector3f();
                storeResult[i] = sumVector;
            } else {
                sumVector.zero();
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
                    MyVector3f.accumulateScaled(sumVector, samples[j], weight);
                    sumWeight += weight;
                }
            }
            assert sumWeight > 0f : sumWeight;
            sumVector.divideLocal(sumWeight);
        }

        return storeResult;
    }
}
