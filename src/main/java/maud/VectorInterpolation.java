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

import com.jme3.math.Vector3f;
import jme3utilities.Validate;
import jme3utilities.math.MyArray;
import static maud.Util.accumulateScaled;

/**
 * Enumerate and implement some interpolation techniques on time sequences of
 * Vector3f values. TODO savable/reusable spline slopes
 *
 * @author Stephen Gold sgold@sonic.net
 */
public enum VectorInterpolation {
    // *************************************************************************
    // values

    /**
     * acyclic finite-difference cubic-spline interpolation
     */
    FdcSpline,
    /**
     * acyclic linear (Lerp) interpolation
     */
    Lerp,
    /**
     * cyclic finite-difference cubic-spline interpolation
     */
    LoopFdcSpline,
    /**
     * cyclic linear (Lerp) interpolation
     */
    LoopLerp;
    // *************************************************************************
    // new methods exposed

    /**
     * Interpolate among vectors in an acyclic time sequence using cubic-spline
     * interpolation.
     *
     * @param time (&ge;times[0])
     * @param times (not null, unaffected, length&gt;0, in strictly ascending
     * order)
     * @param vectors function values (not null, unaffected, same length as
     * times)
     * @param storeResult (modified if not null)
     * @return an interpolated vector (either storeResult or a new instance)
     */
    public Vector3f cubicSpline(float time, float[] times,
            Vector3f[] vectors, Vector3f storeResult) {
        Validate.nonNull(times, "times");
        assert times.length > 0;
        assert time >= times[0] : time;
        Validate.nonNull(vectors, "vectors");
        assert times.length == vectors.length;
        if (storeResult == null) {
            storeResult = new Vector3f();
        }

        int index1 = MyArray.findPreviousIndex(time, times);
        Vector3f v1 = vectors[index1];
        int last = times.length - 1;

        if (index1 == last) {
            storeResult.set(v1);
            return storeResult;
        }
        int index2 = index1 + 1;
        Vector3f v2 = vectors[index2];
        float inter12 = times[index2] - times[index1];
        /*
         * Estimate slopes at either end of the central interval.
         */
        Vector3f m1, m2;
        switch (this) {
            case FdcSpline: // use finite differences
                if (index1 == 0) {
                    m1 = fdSlope(inter12, v1, v2, null);
                } else {
                    int index0 = index1 - 1;
                    Vector3f v0 = vectors[index0];
                    float inter01 = times[index1] - times[index0];
                    m1 = fdSlope(inter01, inter12, v0, v1, v2, null);
                }
                if (index2 == last) {
                    m2 = fdSlope(inter12, v1, v2, null);
                } else {
                    int index3 = index2 + 1;
                    Vector3f v3 = vectors[index3];
                    float inter23 = times[index3] - times[index2];
                    m2 = fdSlope(inter12, inter23, v1, v2, v3, null);
                }
                break;

            default:
                throw new IllegalStateException();
        }

        float t = (time - times[index1]) / inter12;
        cubicSpline(t, inter12, v1, v2, m1, m2, storeResult);

        return storeResult;
    }

    /**
     * Interpolate among vectors in a time sequence using this technique.
     *
     * @param time parameter value
     * @param times (not null, unaffected, length&gt;0, in strictly ascending
     * order)
     * @param cycleTime used for looping (&ge;times[last])
     * @param vectors function values (not null, unaffected, same length as
     * times)
     * @param storeResult (modified if not null)
     * @return an interpolated vector (either storeResult or a new instance)
     */
    public Vector3f interpolate(float time, float[] times, float cycleTime,
            Vector3f[] vectors, Vector3f storeResult) {
        Validate.nonNull(times, "times");
        assert times.length > 0;
        assert times.length == vectors.length;
        int last = times.length - 1;
        assert cycleTime >= times[last] : cycleTime;
        if (storeResult == null) {
            storeResult = new Vector3f();
        }

        if (last == 0 || time < times[0]) {
            storeResult.set(vectors[0]);
            return storeResult;
        }

        switch (this) {
            case FdcSpline:
                cubicSpline(time, times, vectors, storeResult);
                break;

            case Lerp:
                lerp(time, times, vectors, storeResult);
                break;

            case LoopFdcSpline:
                if (times[last] == cycleTime) {
                    if (last > 1) { // ignore the final point
                        loopCubicSpline(time, last - 1, times, cycleTime,
                                vectors, storeResult);
                    } else { // fall back on acyclic
                        cubicSpline(time, times, vectors, storeResult);
                    }
                } else {
                    loopCubicSpline(time, last, times, cycleTime, vectors,
                            storeResult);
                }
                break;

            case LoopLerp:
                if (times[last] == cycleTime) {
                    if (last > 1) { // ignore the final point
                        loopLerp(time, last - 1, times, cycleTime, vectors,
                                storeResult);
                    } else { // fall back on acyclic
                        lerp(time, times, vectors, storeResult);
                    }
                } else {
                    loopLerp(time, last, times, cycleTime, vectors,
                            storeResult);
                }
                break;

            default:
                throw new IllegalStateException();
        }

        return storeResult;
    }

    /**
     * Interpolate among vectors in an acyclic time sequence using linear (Lerp)
     * interpolation. This is essentially what AnimControl uses to interpolate
     * translations and scales in bone tracks.
     *
     * @param time parameter value (&ge;times[0])
     * @param times (not null, unaffected, length&gt;0, in strictly ascending
     * order)
     * @param vectors function values (not null, unaffected, same length as
     * times, each not null)
     * @param storeResult (modified if not null)
     * @return an interpolated vector (either storeResult or a new instance)
     */
    public static Vector3f lerp(float time, float[] times, Vector3f[] vectors,
            Vector3f storeResult) {
        Validate.nonNull(times, "times");
        assert times.length > 0;
        assert time >= times[0] : time;
        Validate.nonNull(vectors, "vectors");
        assert times.length == vectors.length;
        if (storeResult == null) {
            storeResult = new Vector3f();
        }

        int index1 = MyArray.findPreviousIndex(time, times);
        Vector3f v1 = vectors[index1];

        if (index1 >= times.length - 1) { // the last point
            storeResult.set(vectors[index1]);
        } else {
            int index2 = index1 + 1;
            float inter12 = times[index2] - times[index1];
            assert inter12 > 0f : inter12;
            float t = (time - times[index1]) / inter12;
            Vector3f v2 = vectors[index2];
            lerp(t, v1, v2, storeResult);
        }

        return storeResult;
    }

    /**
     * Interpolate among vectors in a cyclic time sequence using cubic-spline
     * interpolation.
     *
     * @param time parameter value (&ge;0, &le;cycleTime)
     * @param last (index of the last point, &ge;1)
     * @param times (not null, unaffected, in strictly ascending order,
     * times[0]==0)
     * @param cycleTime cycle time (&gt;times[last])
     * @param vectors function values (not null, unaffected)
     * @param storeResult (modified if not null)
     * @return an interpolated vector (either storeResult or a new instance)
     */
    public Vector3f loopCubicSpline(float time, int last, float[] times,
            float cycleTime, Vector3f[] vectors, Vector3f storeResult) {
        Validate.inRange(time, "time", 0f, cycleTime);
        Validate.positive(last, "last");
        Validate.nonNull(times, "times");
        Validate.nonNull(vectors, "vectors");
        assert times.length > last : times.length;
        assert vectors.length > last : vectors.length;
        assert cycleTime > times[last] : cycleTime;
        /*
         * Find 4 nearby points and calculate the 3 intervals.
         */
        int index1 = MyArray.findPreviousIndex(time, times);

        float inter01, inter12, inter23; // intervals between points
        int index0, index2, index3; // keyframe indices
        if (index1 > 0) {
            index0 = index1 - 1;
            inter01 = times[index1] - times[index0];
        } else {
            index0 = last;
            inter01 = cycleTime - times[index0];
        }
        assert inter01 > 0f : inter01;

        if (index1 < last) {
            index2 = index1 + 1;
            inter12 = times[index2] - times[index1];
        } else {
            index2 = 0;
            inter12 = cycleTime - times[last];
        }
        assert inter12 > 0f : inter12;

        if (index2 < last) {
            index3 = index2 + 1;
            inter23 = times[index3] - times[index2];
        } else {
            index3 = 0;
            inter23 = cycleTime - times[last];
        }
        assert inter23 > 0f : inter23;

        Vector3f v0 = vectors[index0];
        Vector3f v1 = vectors[index1];
        Vector3f v2 = vectors[index2];
        Vector3f v3 = vectors[index3];
        /*
         * Estimate slopes at either end of the central interval.
         */
        Vector3f m1, m2;
        switch (this) {
            case LoopFdcSpline: // use finite differences
                m1 = fdSlope(inter01, inter12, v0, v1, v2, null);
                m2 = fdSlope(inter12, inter23, v1, v2, v3, null);
                break;
            default:
                throw new IllegalStateException();
        }

        float t = (time - times[index1]) / inter12;
        storeResult = cubicSpline(t, inter12, v1, v2, m1, m2, storeResult);

        return storeResult;
    }

    /**
     * Interpolate among vectors in a cyclic time sequence using linear (Lerp)
     * interpolation.
     *
     * @param time parameter value (&ge;0, &le;cycleTime)
     * @param last (index of the last point, &ge;1)
     * @param times (not null, unaffected, in strictly ascending order,
     * times[0]==0)
     * @param cycleTime cycle time (&gt;times[last])
     * @param vectors function values (not null, unaffected, each not null)
     * @param storeResult (modified if not null)
     * @return an interpolated vector (either storeResult or a new instance)
     */
    public static Vector3f loopLerp(float time, int last, float[] times,
            float cycleTime, Vector3f[] vectors, Vector3f storeResult) {
        Validate.inRange(time, "time", 0f, cycleTime);
        Validate.positive(last, "last");
        Validate.nonNull(times, "times");
        Validate.nonNull(vectors, "vectors");
        assert times.length > last : times.length;
        assert vectors.length > last : vectors.length;
        assert cycleTime > times[last] : cycleTime;
        if (storeResult == null) {
            storeResult = new Vector3f();
        }

        int index1 = MyArray.findPreviousIndex(time, times);
        int index2; // keyframe index
        float interval; // interval between keyframes
        if (index1 < last) {
            index2 = index1 + 1;
            interval = times[index2] - times[index1];
        } else {
            index2 = 0;
            interval = cycleTime - times[last];
        }
        assert interval > 0f : interval;

        float t = (time - times[index1]) / interval;
        Vector3f v1 = vectors[index1];
        Vector3f v2 = vectors[index2];
        lerp(t, v1, v2, storeResult);

        return storeResult;
    }
    // *************************************************************************
    // private methods

    /**
     * Interpolate between 2 vectors using a cubic spline in Hermite form.
     *
     * @param t descaled parameter value (&ge;0, &le;1)
     * @param interval length of the interval (&gt;0)
     * @param v1 function value at start of interval (not null, unaffected)
     * @param v2 function value at end of interval (not null, unaffected)
     * @param m1 1st derivative at start of interval (not null, unaffected)
     * @param m2 1st derivative at end of interval (not null, unaffected)
     * @param storeResult (modified if not null)
     * @return an interpolated vector (either storeResult or a new instance)
     */
    private static Vector3f cubicSpline(float t, float interval, Vector3f v1,
            Vector3f v2, Vector3f m1, Vector3f m2, Vector3f storeResult) {
        assert t >= 0f : t;
        assert t <= 1f : t;
        assert v1 != null;
        assert v2 != null;
        assert m1 != null;
        assert m2 != null;
        if (storeResult == null) {
            storeResult = new Vector3f();
        }
        /*
         * Evaluate the 4 Hermite basis functions, which are all cubic.
         */
        float u = 1f - t;
        float u2 = u * u;
        float h00 = (1f + 2f * t) * u2;
        float h01 = 1f - h00;
        float h10 = t * u2;
        float h11 = t * t * (t - 1f);

        storeResult.set(v1);
        storeResult.multLocal(h00);
        accumulateScaled(storeResult, v2, h01);
        accumulateScaled(storeResult, m1, interval * h10);
        accumulateScaled(storeResult, m2, interval * h11);

        return storeResult;
    }

    /**
     * Using finite differences, estimate the 1st derivative of an unknown
     * function between 2 indexed points.
     *
     * @param dt length of the interval (&gt;0)
     * @param v1 function value at the start point (not null, unaffected)
     * @param v2 function value at the end point (not null, unaffected)
     * @param storeResult (modified if not null)
     * @return a derivative vector (either storeResult or a new instance)
     */
    private static Vector3f fdSlope(float dt, Vector3f v1, Vector3f v2,
            Vector3f storeResult) {
        assert dt > 0f : dt;
        assert v1 != null;
        assert v2 != null;
        if (storeResult == null) {
            storeResult = new Vector3f();
        }

        storeResult.x = v2.x - v1.x;
        storeResult.y = v2.y - v1.y;
        storeResult.z = v2.z - v1.z;
        storeResult.divideLocal(dt);

        return storeResult;
    }

    /**
     * Using finite differences, estimate the 1st derivative of an unknown
     * function at the middle of 3 indexed points.
     *
     * @param dt01 length of the preceeding interval (&gt;0)
     * @param dt12 length of the following interval (&gt;0)
     * @param v0 function value at the previous point (not null, unaffected)
     * @param v1 function value at the current point (not null, unaffected)
     * @param v2 function value at the next point (not null, unaffected)
     * @param storeResult (modified if not null)
     * @return a derivative vector (either storeResult or a new instance)
     */
    private static Vector3f fdSlope(float dt01, float dt12, Vector3f v0,
            Vector3f v1, Vector3f v2, Vector3f storeResult) {
        assert dt01 > 0f : dt01;
        assert dt12 > 0f : dt12;
        assert v0 != null;
        assert v1 != null;
        assert v2 != null;
        if (storeResult == null) {
            storeResult = new Vector3f();
        }

        storeResult.x = (v1.x - v0.x) / dt01 + (v2.x - v1.x) / dt12;
        storeResult.y = (v1.y - v0.y) / dt01 + (v2.y - v1.y) / dt12;
        storeResult.z = (v1.z - v0.z) / dt01 + (v2.z - v1.z) / dt12;
        storeResult.divideLocal(2f);

        return storeResult;
    }

    /**
     * Interpolate between 2 unit single-precision values using linear (Lerp)
     * interpolation. Unlike
     * {@link com.jme3.math.FastMath#interpolateLinear(float, float, float)}, no
     * rounding error is introduced when y1==y2.
     *
     * @param t descaled parameter value (&ge;0, &le;1)
     * @param y1 function value at t=0
     * @param y2 function value at t=1
     * @return an interpolated function value
     */
    private static float lerp(float t, float y1, float y2) {
        assert t >= 0f : t;
        assert t <= 1f : t;

        float lerp;
        if (y1 == y2) {
            lerp = y1;
        } else {
            lerp = (1f - t) * y1 + t * y2;
        }

        return lerp;
    }

    /**
     * Interpolate between 2 vectors using linear (Lerp) interpolation. Unlike
     * {@link com.jme3.math.FastMath#interpolateLinear(float, com.jme3.math.Vector3f, com.jme3.math.Vector3f, com.jme3.math.Vector3f)},
     * no rounding error is introduced when v1==v2.
     *
     * @param t descaled parameter value (&ge;0, &le;1)
     * @param v0 function value at t=0 (not null, unaffected, norm=1)
     * @param v1 function value at t=1 (not null, unaffected, norm=1)
     * @param storeResult (modified if not null)
     * @return an interpolated vector (either storeResult or a new instance)
     */
    private static Vector3f lerp(float t, Vector3f v0, Vector3f v1,
            Vector3f storeResult) {
        Validate.inRange(t, "t", 0f, 1f);
        if (storeResult == null) {
            storeResult = new Vector3f();
        }

        storeResult.x = lerp(t, v0.x, v1.x);
        storeResult.y = lerp(t, v0.y, v1.y);
        storeResult.z = lerp(t, v0.z, v1.z);

        return storeResult;
    }
}
