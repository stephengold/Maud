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
import jme3utilities.math.MyMath;
import jme3utilities.math.MyVector3f;

/**
 * Enumerate and implement some interpolation techniques on time sequences of
 * Vector3f values.
 *
 * @author Stephen Gold sgold@sonic.net
 */
public enum TweenVectors {
    // *************************************************************************
    // values

    /**
     * acyclic uniform Catmull-Rom cubic-spline interpolation
     */
    CatmullRomSpline,
    /**
     * acyclic centripetal Catmull-Rom cubic-spline interpolation
     */
    CentripetalSpline,
    /**
     * acyclic finite-difference cubic-spline interpolation
     */
    FdcSpline,
    /**
     * acyclic linear (Lerp) interpolation
     */
    Lerp,
    /**
     * cyclic uniform Catmull-Rom cubic-spline interpolation
     */
    LoopCatmullRomSpline,
    /**
     * cyclic centripetal Catmull-Rom cubic-spline interpolation
     */
    LoopCentripetalSpline,
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
        float t = (time - times[index1]) / inter12;
        switch (this) {
            case CatmullRomSpline:
            case FdcSpline:
            case LoopCatmullRomSpline:
            case LoopFdcSpline:
                /*
                 * Estimate slopes at either end of the central interval.
                 */
                Vector3f m1;
                if (index1 == 0) {
                    m1 = slope(inter12, v1, v2, null);
                } else {
                    int index0 = index1 - 1;
                    Vector3f v0 = vectors[index0];
                    float inter01 = times[index1] - times[index0];
                    m1 = slope(inter01, inter12, v0, v1, v2, null);
                }
                Vector3f m2;
                if (index2 == last) {
                    m2 = slope(inter12, v1, v2, null);
                } else {
                    int index3 = index2 + 1;
                    Vector3f v3 = vectors[index3];
                    float inter23 = times[index3] - times[index2];
                    m2 = slope(inter12, inter23, v1, v2, v3, null);
                }

                cubicSpline(t, inter12, v1, v2, m1, m2, storeResult);
                break;

            case CentripetalSpline:
            case LoopCentripetalSpline:
                int index0;
                for (index0 = index1 - 1; index0 >= 0; index0--) {
                    if (MyVector3f.ne(vectors[index0], v1)) {
                        break;
                    }
                }
                Vector3f v0;
                if (index0 < 0) {
                    v0 = v1.mult(2f);
                    v0.subtractLocal(v2);
                } else {
                    v0 = vectors[index0];
                }
                int index3;
                for (index3 = index2 + 1; index3 <= last; index3++) {
                    if (MyVector3f.ne(vectors[index3], v2)) {
                        break;
                    }
                }
                Vector3f v3;
                if (index3 > last) {
                    v3 = v2.mult(2f);
                    v3.subtractLocal(v1);
                } else {
                    v3 = vectors[index3];
                }

                centripetal(t, v0, v1, v2, v3, storeResult);
                break;

            default:
                throw new IllegalStateException();
        }

        return storeResult;
    }

    /**
     * Interpolate among vectors in a time sequence using this technique.
     *
     * @param time parameter value
     * @param times (not null, unaffected, length&gt;0, in strictly ascending
     * order)
     * @param cycleTime end time for looping (&ge;times[last])
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
            case CentripetalSpline:
            case CatmullRomSpline:
            case FdcSpline:
                cubicSpline(time, times, vectors, storeResult);
                break;

            case Lerp:
                lerp(time, times, vectors, storeResult);
                break;

            case LoopCentripetalSpline:
            case LoopCatmullRomSpline:
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
     * Interpolate among vectors in a time sequence using this technique and
     * precomputed parameters.
     *
     * @param time parameter value
     * @param curve curve parameters (not null, unaffected)
     * @param storeResult (modified if not null)
     * @return an interpolated vector (either storeResult or a new instance)
     */
    public Vector3f interpolate(float time, VectorCurve curve,
            Vector3f storeResult) {
        Validate.nonNull(curve, "curve");
        if (storeResult == null) {
            storeResult = new Vector3f();
        }

        switch (this) {
            case CatmullRomSpline:
            case CentripetalSpline:
            case FdcSpline:
            case LoopCatmullRomSpline:
            case LoopCentripetalSpline:
            case LoopFdcSpline:
                spline(time, curve, storeResult);
                break;

            case Lerp:
            case LoopLerp:
                float[] times = curve.getTimes();
                float cycleTime = curve.getCycleTime();
                Vector3f[] vectors = curve.getValues();
                interpolate(time, times, cycleTime, vectors, storeResult);
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
            MyVector3f.lerp(t, v1, v2, storeResult);
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

        float inter12;
        int index2;
        if (index1 < last) {
            index2 = index1 + 1;
            inter12 = times[index2] - times[index1];
        } else {
            index2 = 0;
            inter12 = cycleTime - times[last];
        }
        assert inter12 > 0f : inter12;

        Vector3f v1 = vectors[index1];
        Vector3f v2 = vectors[index2];
        Vector3f v0, v3;
        int index0, index3;
        float t = (time - times[index1]) / inter12;

        switch (this) {
            case LoopCatmullRomSpline:
            case LoopFdcSpline:
                /*
                 * Estimate slopes at either end of the central interval.
                 */
                float inter01;
                if (index1 > 0) {
                    index0 = index1 - 1;
                    inter01 = times[index1] - times[index0];
                } else {
                    index0 = last;
                    inter01 = cycleTime - times[index0];
                }
                assert inter01 > 0f : inter01;

                float inter23;
                if (index2 < last) {
                    index3 = index2 + 1;
                    inter23 = times[index3] - times[index2];
                } else {
                    index3 = 0;
                    inter23 = cycleTime - times[last];
                }
                assert inter23 > 0f : inter23;

                v0 = vectors[index0];
                v3 = vectors[index3];
                Vector3f m1 = slope(inter01, inter12, v0, v1, v2, null);
                Vector3f m2 = slope(inter12, inter23, v1, v2, v3, null);

                storeResult = cubicSpline(t, inter12, v1, v2, m1, m2,
                        storeResult);
                break;

            case LoopCentripetalSpline:
                int numVectors = last + 1;
                for (index0 = MyMath.modulo(index1 - 1, numVectors);
                        index0 != index1;
                        index0 = MyMath.modulo(index0 - 1, numVectors)) {
                    if (MyVector3f.ne(vectors[index0], v1)) {
                        break;
                    }
                }
                for (index3 = MyMath.modulo(index2 + 1, numVectors);
                        index3 != index2;
                        index3 = MyMath.modulo(index3 + 1, numVectors)) {
                    if (MyVector3f.ne(vectors[index3], v2)) {
                        break;
                    }
                }

                v0 = vectors[index0];
                v3 = vectors[index3];
                storeResult = centripetal(t, v0, v1, v2, v3, storeResult);
                break;

            default:
                throw new IllegalStateException();
        }

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
        MyVector3f.lerp(t, v1, v2, storeResult);

        return storeResult;
    }

    /**
     * Precompute a curve from vectors in a time sequence.
     *
     * @param times (not null, unaffected, length&gt;0, in strictly ascending
     * order)
     * @param cycleTime end time of loop (&ge;times[lastIndex])
     * @param vectors function values (not null, unaffected, same length as
     * times, each norm==1)
     * @return a new instance
     */
    public VectorCurve precompute(float[] times, float cycleTime,
            Vector3f[] vectors) {
        Validate.nonNull(times, "times");
        assert times.length > 0;
        assert times.length == vectors.length;
        int lastIndex = times.length - 1;
        assert cycleTime >= times[lastIndex] : cycleTime;

        VectorCurve result = new VectorCurve(times, cycleTime, vectors);
        switch (this) {
            case CatmullRomSpline:
            case CentripetalSpline:
            case FdcSpline:
                precomputeSpline(result);
                break;

            case LoopCatmullRomSpline:
            case LoopCentripetalSpline:
            case LoopFdcSpline:
                if (times[lastIndex] == cycleTime) {
                    if (lastIndex > 1) {
                        // ignore the final point
                        precomputeLoopSpline(result, lastIndex - 1);
                    } else {
                        // fall back on acyclic
                        precomputeSpline(result);
                    }
                } else {
                    precomputeLoopSpline(result, lastIndex);
                }
                break;

            case Lerp:
            case LoopLerp:
                // don't precompute anything
                break;

            default:
                throw new IllegalStateException();
        }

        return result;
    }
    // *************************************************************************
    // private methods

    /**
     * Interpolate between 2 vectors using a centripetal Catmull-Rom spline.
     *
     * @param tt descaled parameter value (&ge;0, &le;1)
     * @param v0 function value preceding v1 (not null, != v1, unaffected)
     * @param v1 function value at start of interval (not null, unaffected)
     * @param v2 function value at end of interval (not null, unaffected)
     * @param v3 function value following v2 (not null, != v2, unaffected)
     * @param storeResult (modified if not null)
     * @return an interpolated vector (either storeResult or a new instance)
     */
    private static Vector3f centripetal(float tt, Vector3f v0, Vector3f v1,
            Vector3f v2, Vector3f v3, Vector3f storeResult) {
        assert tt >= 0f : tt;
        assert tt <= 1f : tt;
        assert v0 != null;
        assert v1 != null;
        assert v2 != null;
        assert v3 != null;
        if (storeResult == null) {
            storeResult = new Vector3f();
        }

        double ds12 = MyVector3f.distanceSquared(v1, v2);
        if (ds12 == 0.0) {
            storeResult.set(v1);
        } else {
            float dt12 = (float) MyMath.fourthRoot(ds12);
            if (dt12 == 0f) {
                storeResult.set(v1);
            } else {
                double ds01 = MyVector3f.distanceSquared(v0, v1);
                double ds23 = MyVector3f.distanceSquared(v2, v3);
                float dt01 = (float) MyMath.fourthRoot(ds01);
                float dt23 = (float) MyMath.fourthRoot(ds23);
                centripetal(tt, v0, v1, v2, v3, dt01, dt12, dt23, storeResult);
            }
        }

        return storeResult;
    }

    /**
     * Interpolate between 2 vectors using a centripetal Catmull-Rom spline.
     *
     * @param tt descaled parameter value (&ge;0, &le;1)
     * @param v0 function value preceding v1 (not null, unaffected)
     * @param v1 function value at start of interval (not null, unaffected)
     * @param v2 function value at end of interval (not null, unaffected)
     * @param v3 function value following v2 (not null, unaffected)
     * @param dt01 square root of distance from v0 to v1 (&gt;0)
     * @param dt12 square root of distance from v1 to v2 (&gt;0)
     * @param dt23 square root of distance from v2 to v3 (&gt;0)
     * @param storeResult (modified if not null)
     * @return an interpolated vector (either storeResult or a new instance)
     */
    private static Vector3f centripetal(float tt, Vector3f v0, Vector3f v1,
            Vector3f v2, Vector3f v3, float dt01, float dt12, float dt23,
            Vector3f storeResult) {
        assert tt >= 0f : tt;
        assert tt <= 1f : tt;
        assert v0 != null;
        assert v1 != null;
        assert v2 != null;
        assert v3 != null;
        assert dt01 > 0f : dt01;
        assert dt12 > 0f : dt12;
        assert dt23 > 0f : dt23;
        if (storeResult == null) {
            storeResult = new Vector3f();
        }

        float t = tt * dt12;

        Vector3f a1 = MyVector3f.lerp((t + dt01) / dt01, v0, v1, null);
        Vector3f a2 = MyVector3f.lerp(t / dt12, v1, v2, null);
        Vector3f a3 = MyVector3f.lerp((t - dt12) / dt23, v2, v3, null);

        Vector3f b1 = MyVector3f.lerp((t + dt01) / (dt01 + dt12), a1, a2, null);
        Vector3f b2 = MyVector3f.lerp(t / (dt12 + dt23), a2, a3, null);

        MyVector3f.lerp(t, b1, b2, storeResult);

        return storeResult;
    }

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
        MyVector3f.accumulateScaled(storeResult, v2, h01);
        MyVector3f.accumulateScaled(storeResult, m1, interval * h10);
        MyVector3f.accumulateScaled(storeResult, m2, interval * h11);

        return storeResult;
    }

    /**
     * Precompute a curve for a cyclic spline.
     *
     * @param curve (not null, modified)
     * @param lastIndex (&ge;0)
     */
    private void precomputeLoopSpline(VectorCurve curve, int lastIndex) {
        setLastIndex(curve, lastIndex);

        float[] times = curve.getTimes();
        float cycleTime = curve.getCycleTime();
        Vector3f[] vectors = curve.getValues();

        for (int index1 = 0; index1 <= lastIndex; index1++) {
            float inter12;
            int index2;
            if (index1 < lastIndex) {
                index2 = index1 + 1;
                inter12 = times[index2] - times[index1];
            } else {
                index2 = 0;
                inter12 = cycleTime - times[lastIndex];
            }
            assert inter12 > 0f : inter12;

            Vector3f v1 = vectors[index1];
            Vector3f v2 = vectors[index2];
            curve.setParameters(index1, v2, inter12);

            Vector3f v0, v3;
            int index0, index3;

            switch (this) {
                case LoopCatmullRomSpline:
                case LoopFdcSpline:
                    /*
                     * Estimate slopes at either end of the central interval.
                     */
                    float inter01;
                    if (index1 > 0) {
                        index0 = index1 - 1;
                        inter01 = times[index1] - times[index0];
                    } else {
                        index0 = lastIndex;
                        inter01 = cycleTime - times[index0];
                    }
                    assert inter01 > 0f : inter01;

                    float inter23;
                    if (index2 < lastIndex) {
                        index3 = index2 + 1;
                        inter23 = times[index3] - times[index2];
                    } else {
                        index3 = 0;
                        inter23 = cycleTime - times[lastIndex];
                    }
                    assert inter23 > 0f : inter23;
                    v0 = vectors[index0];
                    v3 = vectors[index3];
                    Vector3f m1 = slope(inter01, inter12, v0, v1, v2, null);
                    Vector3f m2 = slope(inter12, inter23, v1, v2, v3, null);
                    curve.setAuxPoints(index1, m1, m2);
                    break;

                case LoopCentripetalSpline:
                    int numVectors = lastIndex + 1;
                    for (index0 = MyMath.modulo(index1 - 1, numVectors);
                            index0 != index1;
                            index0 = MyMath.modulo(index0 - 1, numVectors)) {
                        if (MyVector3f.ne(vectors[index0], v1)) {
                            break;
                        }
                    }
                    for (index3 = MyMath.modulo(index2 + 1, numVectors);
                            index3 != index2;
                            index3 = MyMath.modulo(index3 + 1, numVectors)) {
                        if (MyVector3f.ne(vectors[index3], v2)) {
                            break;
                        }
                    }
                    v0 = vectors[index0];
                    v3 = vectors[index3];
                    curve.setAuxPoints(index1, v0, v3);

                    double ds12 = MyVector3f.distanceSquared(v1, v2);
                    if (ds12 > 0.0) {
                        float dt12 = (float) MyMath.fourthRoot(ds12);
                        if (dt12 > 0f) {
                            double ds01 = MyVector3f.distanceSquared(v0, v1);
                            double ds23 = MyVector3f.distanceSquared(v2, v3);
                            float dt01 = (float) MyMath.fourthRoot(ds01);
                            float dt23 = (float) MyMath.fourthRoot(ds23);
                            curve.setDts(index1, dt01, dt12, dt23);
                        } else {
                            curve.setDts(index1, 0f, 0f, 0f);
                        }
                    } else {
                        curve.setDts(index1, 0f, 0f, 0f);
                    }
                    break;

                default:
                    throw new IllegalStateException();
            }
        }
    }

    /**
     * Precompute a curve for an acyclic spline.
     *
     * @param curve (not null, modified)
     */
    private void precomputeSpline(VectorCurve curve) {
        Vector3f[] vectors = curve.getValues();
        int lastIndex = vectors.length - 1;
        setLastIndex(curve, lastIndex);
        float[] times = curve.getTimes();

        for (int index1 = 0; index1 <= lastIndex; index1++) {
            Vector3f v1 = vectors[index1];
            int index2;
            float inter12;
            if (index1 == lastIndex) {
                index2 = lastIndex;
                inter12 = curve.getCycleTime() - times[index1] + 0.001f;
            } else {
                index2 = index1 + 1;
                inter12 = times[index2] - times[index1];
            }
            Vector3f v2 = vectors[index2];
            curve.setParameters(index1, v2, inter12);

            switch (this) {
                case CatmullRomSpline:
                case FdcSpline:
                case LoopCatmullRomSpline:
                case LoopFdcSpline:
                    /*
                     * Estimate slopes at either end of the central interval.
                     */
                    Vector3f m1;
                    if (index1 == 0) {
                        m1 = slope(inter12, v1, v2, null);
                    } else {
                        int index0 = index1 - 1;
                        Vector3f v0 = vectors[index0];
                        float inter01 = times[index1] - times[index0];
                        m1 = slope(inter01, inter12, v0, v1, v2, null);
                    }
                    Vector3f m2;
                    if (index2 == lastIndex) {
                        m2 = slope(inter12, v1, v2, null);
                    } else {
                        int index3 = index2 + 1;
                        Vector3f v3 = vectors[index3];
                        float inter23 = times[index3] - times[index2];
                        m2 = slope(inter12, inter23, v1, v2, v3, null);
                    }
                    curve.setAuxPoints(index1, m1, m2);
                    break;

                case CentripetalSpline:
                case LoopCentripetalSpline:
                    int index0;
                    for (index0 = index1 - 1; index0 >= 0; index0--) {
                        if (MyVector3f.ne(vectors[index0], v1)) {
                            break;
                        }
                    }
                    Vector3f v0;
                    if (index0 < 0) {
                        v0 = v1.mult(2f);
                        v0.subtractLocal(v2);
                    } else {
                        v0 = vectors[index0];
                    }
                    int index3;
                    for (index3 = index2 + 1; index3 <= lastIndex; index3++) {
                        if (MyVector3f.ne(vectors[index3], v2)) {
                            break;
                        }
                    }
                    Vector3f v3;
                    if (index3 > lastIndex) {
                        v3 = v2.mult(2f);
                        v3.subtractLocal(v1);
                    } else {
                        v3 = vectors[index3];
                    }
                    curve.setAuxPoints(index1, v0, v3);

                    double ds12 = MyVector3f.distanceSquared(v1, v2);
                    if (ds12 > 0.0) {
                        float dt12 = (float) MyMath.fourthRoot(ds12);
                        if (dt12 > 0f) {
                            double ds01 = MyVector3f.distanceSquared(v0, v1);
                            double ds23 = MyVector3f.distanceSquared(v2, v3);
                            float dt01 = (float) MyMath.fourthRoot(ds01);
                            float dt23 = (float) MyMath.fourthRoot(ds23);
                            curve.setDts(index1, dt01, dt12, dt23);
                        } else {
                            curve.setDts(index1, 0f, 0f, 0f);
                        }
                    } else {
                        curve.setDts(index1, 0f, 0f, 0f);
                    }
                    break;

                default:
                    throw new IllegalStateException();
            }
        }
    }

    /**
     * Alter the number of samples in the specified curve.
     *
     * @param curve (not null, modified)
     * @param lastIndex new value (&ge;0)
     */
    private void setLastIndex(VectorCurve curve, int lastIndex) {
        boolean allocateDts;
        switch (this) {
            case CatmullRomSpline:
            case FdcSpline:
            case LoopCatmullRomSpline:
            case LoopFdcSpline:
                allocateDts = false;
                curve.setLastIndex(lastIndex, allocateDts);
                break;
            case CentripetalSpline:
            case LoopCentripetalSpline:
                allocateDts = true;
                curve.setLastIndex(lastIndex, allocateDts);
                break;
            default:
                throw new IllegalStateException();
        }
    }

    /**
     * Estimate the 1st derivative of an unknown function between 2 indexed
     * points.
     *
     * @param dt length of the interval (&gt;0)
     * @param v1 function value at the start point (not null, unaffected)
     * @param v2 function value at the end point (not null, unaffected)
     * @param storeResult (modified if not null)
     * @return a derivative vector (either storeResult or a new instance)
     */
    private Vector3f slope(float dt, Vector3f v1, Vector3f v2,
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
     * Estimate the 1st derivative of an unknown function at the middle of 3
     * indexed points.
     *
     * @param dt01 length of the preceeding interval (&gt;0)
     * @param dt12 length of the following interval (&gt;0)
     * @param v0 function value at the previous point (not null, unaffected)
     * @param v1 function value at the current point (not null, unaffected)
     * @param v2 function value at the next point (not null, unaffected)
     * @param storeResult (modified if not null)
     * @return a derivative vector (either storeResult or a new instance)
     */
    private Vector3f slope(float dt01, float dt12, Vector3f v0,
            Vector3f v1, Vector3f v2, Vector3f storeResult) {
        assert dt01 > 0f : dt01;
        assert dt12 > 0f : dt12;
        assert v0 != null;
        assert v1 != null;
        assert v2 != null;
        if (storeResult == null) {
            storeResult = new Vector3f();
        }

        switch (this) {
            case CatmullRomSpline:
            case LoopCatmullRomSpline:
                float dt02 = dt01 + dt12;
                storeResult.x = (v2.x - v0.x) / dt02;
                storeResult.y = (v2.y - v0.y) / dt02;
                storeResult.z = (v2.z - v0.z) / dt02;
                break;

            case FdcSpline:
            case LoopFdcSpline:
                storeResult.x = (v1.x - v0.x) / dt01 + (v2.x - v1.x) / dt12;
                storeResult.y = (v1.y - v0.y) / dt01 + (v2.y - v1.y) / dt12;
                storeResult.z = (v1.z - v0.z) / dt01 + (v2.z - v1.z) / dt12;
                storeResult.divideLocal(2f);
                break;

            default:
                throw new IllegalStateException();
        }

        return storeResult;
    }

    /**
     * Interpolate among vectors in a time sequence using spline interpolation
     * and precomputed parameters.
     *
     * @param time parameter value (&ge;0, &le;cycleTime)
     * @param curve vector curve (not null, unaffected)
     * @param storeResult (modified if not null)
     * @return an interpolated vector (either storeResult or a new instance)
     */
    private Vector3f spline(float time, VectorCurve curve,
            Vector3f storeResult) {
        assert time >= 0f : time;
        if (storeResult == null) {
            storeResult = new Vector3f();
        }
        float cycleTime = curve.getCycleTime();
        assert time <= cycleTime : time;

        int lastIndex = curve.getLastIndex();
        float[] times = curve.getTimes();
        int index1 = MyArray.findPreviousIndex(time, times);
        if (index1 > lastIndex) {
            index1 = lastIndex;
        }

        float intervalDuration = curve.getIntervalDuration(index1);
        float t = (time - times[index1]) / intervalDuration;
        assert t <= 1f : t;
        Vector3f v1 = curve.getStartValue(index1);
        Vector3f v2 = curve.getEndValue(index1);
        switch (this) {
            case CatmullRomSpline:
            case FdcSpline:
            case LoopCatmullRomSpline:
            case LoopFdcSpline:
                Vector3f m1 = curve.getAux1(index1);
                Vector3f m2 = curve.getAux2(index1);
                cubicSpline(t, intervalDuration, v1, v2, m1, m2, storeResult);
                break;

            case CentripetalSpline:
            case LoopCentripetalSpline:
                float dt12 = curve.getDt12(index1);
                if (dt12 == 0f) {
                    storeResult.set(v1);
                } else {
                    Vector3f v0 = curve.getAux1(index1);
                    Vector3f v3 = curve.getAux2(index1);
                    float dt01 = curve.getDt01(index1);
                    float dt23 = curve.getDt23(index1);
                    centripetal(t, v0, v1, v2, v3, dt01, dt12, dt23,
                            storeResult);
                }
                break;

            default:
                throw new IllegalStateException();
        }

        return storeResult;
    }
}
