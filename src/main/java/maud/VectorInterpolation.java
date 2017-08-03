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
 * Enumerate (and implement) some interpolation techniques on Vector3f values.
 *
 * @author Stephen Gold sgold@sonic.net
 */
public enum VectorInterpolation {
    // *************************************************************************
    // values

    /**
     * non-looping finite-difference cubic spline interpolation
     */
    FdcSpline,
    /**
     * non-looping linear (Lerp) interpolation
     */
    Lerp,
    /**
     * looping finite-difference cubic spline interpolation
     */
    LoopFdcSpline,
    /**
     * looping linear (Lerp) interpolation
     */
    LoopLerp;
    // *************************************************************************
    // new methods exposed

    /**
     * Interpolate among vectors in a time sequence using non-looping
     * finite-difference cubic spline interpolation.
     *
     * @param time (&ge;times[0])
     * @param times (not null, unaffected, length&gt;0, in ascending order)
     * @param vectors (not null, unaffected, same length as times)
     * @param storeResult (modified if not null)
     * @return interpolated vector (either storeResult or a new instance)
     */
    public static Vector3f fdcSpline(float time, float[] times,
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
        assert index1 != -1;
        int last = times.length - 1;
        if (index1 == last) {
            storeResult.set(vectors[index1]);
            return storeResult;
        }
        int index2 = index1 + 1;
        float inter12 = times[index2] - times[index1];
        /*
         * Estimate slopes at each end of the central interval
         * using finite differences.
         */
        Vector3f m1;
        if (index1 == 0) {
            m1 = fdSlope(inter12, index1, index2, vectors, null);
        } else {
            int index0 = index1 - 1;
            float inter01 = times[index1] - times[index0];
            m1 = fdSlope(inter01, inter12, index0, index1, index2, vectors,
                    null);
        }

        Vector3f m2;
        if (index2 == last) {
            m2 = fdSlope(inter12, index1, index2, vectors, null);
        } else {
            int index3 = index2 + 1;
            float inter23 = times[index3] - times[index2];
            m2 = fdSlope(inter12, inter23, index1, index2, index3, vectors,
                    null);
        }

        float t = (time - times[index1]) / inter12;
        Vector3f v1 = vectors[index1];
        Vector3f v2 = vectors[index2];
        cSpline(t, inter12, v1, v2, m1, m2, storeResult);

        return storeResult;
    }

    /**
     * Interpolate among vectors in a time sequence using this technique.
     *
     * @param time the parameter value
     * @param times to interpolate among (not null, unaffected, length>0, in
     * ascending order)
     * @param duration used for looping (&ge;times[last])
     * @param vectors to interpolate among (not null, unaffected, same length as
     * times)
     * @param storeResult (modified if not null)
     * @return interpolated vector (either storeResult or a new instance)
     */
    public Vector3f interpolate(float time, float[] times, float duration,
            Vector3f[] vectors, Vector3f storeResult) {
        Validate.nonNull(times, "times");
        assert times.length > 0;
        assert times.length == vectors.length;
        int last = times.length - 1;
        assert duration >= times[last] : duration;
        if (storeResult == null) {
            storeResult = new Vector3f();
        }

        if (last == 0) {
            storeResult.set(vectors[0]);
            return storeResult;
        }

        switch (this) {
            case FdcSpline:
                fdcSpline(time, times, vectors, storeResult);
                break;

            case Lerp:
                lerp(time, times, vectors, storeResult);
                break;

            case LoopFdcSpline:
                if (times[last] == duration) {
                    if (last > 1) { // ignore the last point
                        loopFdcSpline(time, last - 1, times, duration, vectors,
                                storeResult);
                    } else { // fall back on non-looping
                        fdcSpline(time, times, vectors, storeResult);
                    }
                } else {
                    loopFdcSpline(time, last, times, duration, vectors,
                            storeResult);
                }
                break;

            case LoopLerp:
                if (times[last] == duration) {
                    if (last > 1) { // ignore the last point
                        loopLerp(time, last - 1, times, duration, vectors,
                                storeResult);
                    } else { // fall back on non-looping
                        lerp(time, times, vectors, storeResult);
                    }
                } else {
                    loopLerp(time, last, times, duration, vectors, storeResult);
                }
                break;

            default:
                throw new IllegalStateException();
        }

        return storeResult;
    }

    /**
     * Interpolate among vectors in a time sequence using non-looping linear
     * (Lerp) interpolation. This is essentially what AnimControl uses to
     * interpolate translations and scales in bone tracks.
     *
     * @param time (&ge;times[0])
     * @param times (not null, unaffected, length&gt;0, in ascending order)
     * @param vectors (not null, unaffected, same length as times)
     * @param storeResult (modified if not null)
     * @return interpolated vector (either storeResult or a new instance)
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
        assert index1 != -1;
        storeResult.set(vectors[index1]);

        if (index1 < times.length - 1) { // not the last point
            int index2 = index1 + 1;
            float inter12 = times[index2] - times[index1];
            assert inter12 > 0f : inter12;
            float t = (time - times[index1]) / inter12;
            float u = 1f - t;
            Vector3f v2 = vectors[index2];
            if (storeResult.x != v2.x) {
                storeResult.x = u * storeResult.x + t * v2.x;
            }
            if (storeResult.y != v2.y) {
                storeResult.y = u * storeResult.y + t * v2.y;
            }
            if (storeResult.z != v2.z) {
                storeResult.z = u * storeResult.z + t * v2.z;
            }
        }

        return storeResult;
    }

    /**
     * Interpolate among vectors in a time sequence using looping
     * finite-difference cubic spline interpolation.
     *
     * @param time (&ge;0, &le;duration)
     * @param last (index of the last point, &ge;1)
     * @param times (not null, unaffected, in ascending order, times[0]==0)
     * @param duration (&gt;times[last])
     * @param vectors (not null, unaffected)
     * @param storeResult (modified if not null)
     * @return interpolated vector (either storeResult or a new instance)
     */
    public static Vector3f loopFdcSpline(float time, int last, float[] times,
            float duration, Vector3f[] vectors, Vector3f storeResult) {
        Validate.inRange(time, "time", 0f, duration);
        Validate.positive(last, "last");
        Validate.nonNull(times, "times");
        Validate.nonNull(vectors, "vectors");
        assert times.length > last : times.length;
        assert vectors.length > last : vectors.length;
        assert duration > times[last] : duration;
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
            inter01 = duration - times[index0];
        }
        assert inter01 > 0f : inter01;

        if (index1 < last) {
            index2 = index1 + 1;
            inter12 = times[index2] - times[index1];
        } else {
            index2 = 0;
            inter12 = duration - times[index1];
        }
        assert inter12 > 0f : inter12;

        if (index2 < last) {
            index3 = index2 + 1;
            inter23 = times[index3] - times[index2];
        } else {
            index3 = 0;
            inter23 = duration - times[index2];
        }
        assert inter23 > 0f : inter23;
        /*
         * Estimate slopes at each end of the central interval
         * using finite differences.
         */
        Vector3f m1, m2;
        m1 = fdSlope(inter01, inter12, index0, index1, index2, vectors, null);
        m2 = fdSlope(inter12, inter23, index1, index2, index3, vectors, null);

        float t = (time - times[index1]) / inter12;
        Vector3f v1 = vectors[index1];
        Vector3f v2 = vectors[index2];
        storeResult = cSpline(t, inter12, v1, v2, m1, m2, storeResult);

        return storeResult;
    }

    /**
     * Interpolate among vectors in a time sequence using looping linear (Lerp)
     * interpolation.
     *
     * @param time (in seconds, &ge;0, &le;duration)
     * @param last (index of the last point, &ge;1)
     * @param times (not null, unaffected)
     * @param duration (in seconds, &gt;times[last])
     * @param vectors (not null, unaffected)
     * @param storeResult (modified if not null)
     * @return interpolated vector (either storeResult or a new instance)
     */
    public static Vector3f loopLerp(float time, int last, float[] times,
            float duration, Vector3f[] vectors, Vector3f storeResult) {
        Validate.inRange(time, "time", 0f, duration);
        Validate.positive(last, "last");
        Validate.nonNull(times, "times");
        Validate.nonNull(vectors, "vectors");
        assert times.length > last : times.length;
        assert vectors.length > last : vectors.length;
        assert duration > times[last] : duration;
        if (storeResult == null) {
            storeResult = new Vector3f();
        }

        int index1 = MyArray.findPreviousIndex(time, times);
        storeResult.set(vectors[index1]);

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
        float u = 1f - t;
        Vector3f v2 = vectors[index2];
        if (storeResult.x != v2.x) {
            storeResult.x = u * storeResult.x + t * v2.x;
        }
        if (storeResult.y != v2.y) {
            storeResult.y = u * storeResult.y + t * v2.y;
        }
        if (storeResult.z != v2.z) {
            storeResult.z = u * storeResult.z + t * v2.z;
        }

        return storeResult;
    }
    // *************************************************************************
    // private methods

    /**
     * Interpolate a cubic spline in Hermite form.
     *
     * @param t scaled parameter (&ge;0, &le;1)
     * @param interval interval duration (&gt;0)
     * @param v1 function value at start of interval (not null, unaffected)
     * @param v2 function value at end of interval (not null, unaffected)
     * @param m1 derivative at start of interval (not null, unaffected)
     * @param m2 derivative at end of interval (not null, unaffected)
     * @param storeResult (modified if not null)
     * @return interpolated vector (either storeResult or a new instance)
     */
    private static Vector3f cSpline(float t, float interval, Vector3f v1,
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
     * Using finite differences, estimate the derivative of an unknown function
     * at 2 indexed points.
     *
     * @param dt interval (&gt;0)
     * @param index1 index of the 1st point (&ge;0)
     * @param index2 index of the 2nd point (&ge;0)
     * @param vectors data for points (not null, unaffected)
     * @param storeResult (modified if not null)
     * @return derivative vector (either storeResult or a new instance)
     */
    private static Vector3f fdSlope(float dt, int index1, int index2,
            Vector3f[] vectors, Vector3f storeResult) {
        assert dt > 0f : dt;
        if (storeResult == null) {
            storeResult = new Vector3f();
        }

        Vector3f v1 = vectors[index1];
        Vector3f v2 = vectors[index2];

        storeResult.x = (v2.x - v1.x) / dt;
        storeResult.y = (v2.y - v1.y) / dt;
        storeResult.z = (v2.z - v1.z) / dt;

        return storeResult;
    }

    /**
     * Using finite differences, estimate the derivative of an unknown function
     * at the middle of 3 indexed points.
     *
     * @param dt01 previous interval (&gt;0)
     * @param dt12 following interval (&gt;0)
     * @param index0 index of the previous point (&ge;0)
     * @param index1 index of the current point (&ge;0)
     * @param index2 index of the following point (&ge;0)
     * @param vectors data for points (not null, unaffected)
     * @param storeResult (modified if not null)
     * @return derivative vector (either storeResult or a new instance)
     */
    private static Vector3f fdSlope(float dt01, float dt12, int index0,
            int index1, int index2, Vector3f[] vectors, Vector3f storeResult) {
        assert dt01 > 0f : dt01;
        assert dt12 > 0f : dt12;
        if (storeResult == null) {
            storeResult = new Vector3f();
        }

        Vector3f v0 = vectors[index0];
        Vector3f v1 = vectors[index1];
        Vector3f v2 = vectors[index2];

        storeResult.x = 0.5f * ((v1.x - v0.x) / dt01 + (v2.x - v1.x) / dt12);
        storeResult.y = 0.5f * ((v1.y - v0.y) / dt01 + (v2.y - v1.y) / dt12);
        storeResult.z = 0.5f * ((v1.z - v0.z) / dt01 + (v2.z - v1.z) / dt12);

        return storeResult;
    }
}
