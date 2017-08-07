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
 * Enumerate and implement some interpolation techniques on time sequences of
 * unit quaternions.
 *
 * @author Stephen Gold sgold@sonic.net
 */
public enum TweenRotations {
    // *************************************************************************
    // values

    /**
     * cyclic normalized linear (Nlerp) interpolation
     */
    LoopNlerp,
    /**
     * cyclic spherical linear (Slerp) or "great arc" interpolation using
     * shortcuts
     */
    LoopQuickSlerp,
    /**
     * cyclic spherical linear (Slerp) or "great arc" interpolation
     */
    LoopSlerp,
    /**
     * cyclic cubic-spline interpolation based on the Squad function
     */
    LoopSpline,
    /**
     * acyclic normalized linear (Nlerp) interpolation
     */
    Nlerp,
    /**
     * acyclic spherical linear (Slerp) or "great arc" interpolation using
     * shortcuts
     */
    QuickSlerp,
    /**
     * acyclic spherical linear (Slerp) or "great arc" interpolation
     */
    Slerp,
    /**
     * acyclic cubic-spline interpolation based on the Squad function
     */
    Spline;
    // *************************************************************************
    // new methods exposed

    /**
     * Interpolate among unit quaternions in a time sequence using this
     * technique.
     *
     * @param time parameter value
     * @param times (not null, unaffected, length&gt;0, in strictly ascending
     * order)
     * @param cycleTime end time for looping (&ge;times[lastIndex])
     * @param quaternions function values (not null, unaffected, same length as
     * times, each norm=1)
     * @param storeResult (modified if not null)
     * @return interpolated unit quaternion (either storeResult or a new
     * instance)
     */
    public Quaternion interpolate(float time, float[] times, float cycleTime,
            Quaternion[] quaternions, Quaternion storeResult) {
        Validate.nonNull(times, "times");
        assert times.length > 0;
        assert times.length == quaternions.length;
        int lastIndex = times.length - 1;
        assert cycleTime >= times[lastIndex] : cycleTime;
        if (storeResult == null) {
            storeResult = new Quaternion();
        }

        if (lastIndex == 0 || time < times[0]) {
            storeResult.set(quaternions[0]);
            return storeResult;
        }

        switch (this) {
            case LoopNlerp:
            case LoopQuickSlerp:
            case LoopSlerp:
                if (times[lastIndex] == cycleTime) {
                    if (lastIndex > 1) { // ignore the final point
                        loopLerp(time, lastIndex - 1, times, cycleTime,
                                quaternions, storeResult);
                    } else { // fall back on acyclic
                        lerp(time, times, quaternions, storeResult);
                    }
                } else {
                    loopLerp(time, lastIndex, times, cycleTime, quaternions,
                            storeResult);
                }
                break;

            case LoopSpline:
                if (times[lastIndex] == cycleTime) {
                    if (lastIndex > 1) { // ignore the final point
                        loopSpline(time, lastIndex - 1, times, cycleTime,
                                quaternions, storeResult);
                    } else { // fall back on acyclic
                        spline(time, times, quaternions, storeResult);
                    }
                } else {
                    loopSpline(time, lastIndex, times, cycleTime, quaternions,
                            storeResult);
                }
                break;

            case Nlerp:
            case QuickSlerp:
            case Slerp:
                lerp(time, times, quaternions, storeResult);
                break;

            case Spline:
                spline(time, times, quaternions, storeResult);
                break;

            default:
                throw new IllegalStateException();
        }

        return storeResult;
    }

    /**
     * Interpolate among unit quaternions in a time sequence using this
     * technique and some precomputed parameters.
     *
     * @param time parameter value
     * @param curve curve parameters (not null, unaffected)
     * @param storeResult (modified if not null)
     * @return interpolated unit quaternion (either storeResult or a new
     * instance)
     */
    public Quaternion interpolate(float time, RotationCurve curve,
            Quaternion storeResult) {
        Validate.nonNull(curve, "curve");
        if (storeResult == null) {
            storeResult = new Quaternion();
        }

        switch (this) {
            case LoopNlerp:
            case LoopQuickSlerp:
            case LoopSlerp:
            case Nlerp:
            case QuickSlerp:
            case Slerp:
                float[] times = curve.getTimes();
                float cycleTime = curve.getCycleTime();
                Quaternion[] quaternions = curve.getQuaternions();
                interpolate(time, times, cycleTime, quaternions, storeResult);
                break;

            case LoopSpline:
                loopSpline(time, curve, storeResult);
                break;

            case Spline:
                spline(time, curve, storeResult);
                break;

            default:
                throw new IllegalStateException();
        }

        return storeResult;
    }

    /**
     * Interpolate among unit quaternions in an acyclic time sequence using
     * linear (Nlerp/Slerp) interpolation. Nlerp is essentially what AnimControl
     * uses to interpolate rotations in bone tracks.
     *
     * @param time parameter value (&ge;times[0])
     * @param times (not null, unaffected, length&gt;0, in strictly ascending
     * order)
     * @param quaternions function values (not null, unaffected, same length as
     * times, each norm=1)
     * @param storeResult (modified if not null)
     * @return an interpolated unit quaternion (either storeResult or a new
     * instance)
     */
    public Quaternion lerp(float time, float[] times, Quaternion[] quaternions,
            Quaternion storeResult) {
        Validate.nonNull(times, "times");
        assert times.length > 0;
        assert time >= times[0] : time;
        Validate.nonNull(quaternions, "quaternions");
        assert times.length == quaternions.length;
        if (storeResult == null) {
            storeResult = new Quaternion();
        }

        int index1 = MyArray.findPreviousIndex(time, times);
        Quaternion q1 = quaternions[index1];

        if (index1 >= times.length - 1) { // the last point to use
            storeResult.set(q1);
        } else {
            int index2 = index1 + 1;
            float inter12 = times[index2] - times[index1];
            assert inter12 > 0f : inter12;
            float t = (time - times[index1]) / inter12;
            Quaternion q2 = quaternions[index2];
            lerp(t, q1, q2, storeResult);
        }

        return storeResult;
    }

    /**
     * Interpolate among unit quaternions in a cyclic time sequence using linear
     * (Nlerp/Slerp) interpolation.
     *
     * @param time parameter value (&ge;0, &le;cycleTime)
     * @param lastIndex (index of the last point to use, &ge;1)
     * @param times (not null, unaffected, in strictly ascending order,
     * times[0]==0)
     * @param cycleTime cycle time (&gt;times[lastIndex])
     * @param quaternions function values (not null, unaffected, each norm=1)
     * @param storeResult (modified if not null)
     * @return an interpolated unit quaternion (either storeResult or a new
     * instance)
     */
    public Quaternion loopLerp(float time, int lastIndex, float[] times,
            float cycleTime, Quaternion[] quaternions, Quaternion storeResult) {
        Validate.inRange(time, "time", 0f, cycleTime);
        Validate.positive(lastIndex, "last index");
        Validate.nonNull(times, "times");
        Validate.nonNull(quaternions, "quaternions");
        assert times.length > lastIndex : times.length;
        assert quaternions.length > lastIndex : quaternions.length;
        assert cycleTime > times[lastIndex] : cycleTime;
        if (storeResult == null) {
            storeResult = new Quaternion();
        }

        int index1 = MyArray.findPreviousIndex(time, times);
        int index2; // keyframe index
        float interval; // interval between keyframes
        if (index1 < lastIndex) {
            index2 = index1 + 1;
            interval = times[index2] - times[index1];
        } else {
            index2 = 0;
            interval = cycleTime - times[lastIndex];
        }
        assert interval > 0f : interval;

        float t = (time - times[index1]) / interval;
        Quaternion q1 = quaternions[index1];
        Quaternion q2 = quaternions[index2];
        lerp(t, q1, q2, storeResult);

        return storeResult;
    }

    /**
     * Interpolate among unit quaternions in a cyclic time sequence using
     * cubic-spline interpolation based on the Squad function.
     *
     * @param time parameter value (&ge;0, &le;cycleTime)
     * @param lastIndex (index of the last point to use, &ge;1)
     * @param times (not null, unaffected, in strictly ascending order,
     * times[0]==0)
     * @param cycleTime cycle time (&gt;times[lastIndex])
     * @param quaternions function values (not null, unaffected, each norm=1)
     * @param storeResult (modified if not null)
     * @return an interpolated unit quaternion (either storeResult or a new
     * instance)
     */
    public static Quaternion loopSpline(float time, int lastIndex,
            float[] times, float cycleTime, Quaternion[] quaternions,
            Quaternion storeResult) {
        Validate.inRange(time, "time", 0f, cycleTime);
        Validate.positive(lastIndex, "last index");
        Validate.nonNull(times, "times");
        Validate.nonNull(quaternions, "quaternions");
        assert times.length > lastIndex : times.length;
        assert quaternions.length > lastIndex : quaternions.length;
        assert cycleTime > times[lastIndex] : cycleTime;
        if (storeResult == null) {
            storeResult = new Quaternion();
        }

        int index1 = MyArray.findPreviousIndex(time, times);
        int index2; // keyframe index
        float interval; // interval between keyframes
        if (index1 < lastIndex) {
            index2 = index1 + 1;
            interval = times[index2] - times[index1];
        } else {
            index2 = 0;
            interval = cycleTime - times[lastIndex];
        }
        assert interval > 0f : interval;
        float t = (time - times[index1]) / interval;
        int index0 = (index1 == 0) ? lastIndex : index1 - 1;
        int index3 = (index2 == lastIndex) ? 0 : index2 + 1;
        Quaternion q0 = quaternions[index0];
        Quaternion q1 = quaternions[index1];
        Quaternion q2 = quaternions[index2];
        Quaternion q3 = quaternions[index3];
        flipSpline(t, q0, q1, q2, q3, storeResult);

        return storeResult;
    }

    /**
     * Generate a rotation curve.
     *
     * @param times (not null, unaffected, length&gt;0, in strictly ascending
     * order)
     * @param cycleTime end time of loop (&ge;times[lastIndex])
     * @param quaternions function values (not null, unaffected, same length as
     * times, each norm==1)
     * @return a new instance
     */
    public RotationCurve precompute(float[] times, float cycleTime,
            Quaternion[] quaternions) {
        Validate.nonNull(times, "times");
        assert times.length > 0;
        assert times.length == quaternions.length;
        int lastIndex = times.length - 1;
        assert cycleTime >= times[lastIndex] : cycleTime;

        RotationCurve result = new RotationCurve(times, cycleTime, quaternions);
        switch (this) {
            case LoopNlerp:
            case LoopQuickSlerp:
            case LoopSlerp:
            case Nlerp:
            case QuickSlerp:
            case Slerp:
                break;

            case LoopSpline:
                if (times[lastIndex] == cycleTime) {
                    if (lastIndex > 1) { // ignore the final point
                        precomputeLoopSpline(result, lastIndex - 1);
                    } else { // fall back on acyclic
                        precomputeSpline(result);
                    }
                } else {
                    precomputeLoopSpline(result, lastIndex);
                }
                break;

            case Spline:
                precomputeSpline(result);
                break;

            default:
                throw new IllegalStateException();
        }

        return result;
    }

    /**
     * Interpolate among unit quaternions in an acyclic time sequence using
     * cubic-spline interpolation based on the Squad function.
     *
     * @param time parameter value (&ge;times[0])
     * @param times (not null, unaffected, length&gt;0, in strictly ascending
     * order)
     * @param quaternions function values (not null, unaffected, same length as
     * times, norm=1)
     * @param storeResult (modified if not null)
     * @return an interpolated unit quaternion (either storeResult or a new
     * instance)
     */
    public static Quaternion spline(float time, float[] times,
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
        Quaternion q1 = quaternions[index1];
        int lastIndex = times.length - 1;

        if (index1 == lastIndex) {
            storeResult.set(q1);
            return storeResult;
        }
        // TODO try substituting q for a at the ends of the spline
        int index0 = (index1 == 0) ? 0 : index1 - 1;
        int index2 = index1 + 1;
        int index3 = (index2 == lastIndex) ? lastIndex : index2 + 1;
        float inter12 = times[index2] - times[index1];
        float t = (time - times[index1]) / inter12;
        Quaternion q0 = quaternions[index0];
        Quaternion q2 = quaternions[index2];
        Quaternion q3 = quaternions[index3];
        flipSpline(t, q0, q1, q2, q3, storeResult);

        return storeResult;
    }
    // *************************************************************************
    // private methods

    /**
     * Interpolate between the 2 middle unit quaternions in a sequence of 4
     * using cubic-spline interpolation based on the Squad function.
     *
     * @param t descaled parameter value (&ge;0, &le;1)
     * @param q0 function value preceding q1 (not null, unaffected, norm=1)
     * @param q1 function value at start of interval (not null, unaffected,
     * norm=1)
     * @param q2 function value at end of interval (not null, unaffected,
     * norm=1)
     * @param q3 function value following q1 (not null, unaffected, norm=1)
     * @param storeResult (modified if not null)
     * @return an interpolated unit quaternion (either storeResult or a new
     * instance)
     */
    private static Quaternion flipSpline(float t, Quaternion q0, Quaternion q1,
            Quaternion q2, Quaternion q3, Quaternion storeResult) {
        if (storeResult == null) {
            storeResult = new Quaternion();
        }
        /*
         * Flip signs as necessary to make dot products of successive
         * sampled values non-negative.
         */
        if (q0.dot(q1) < 0f) {
            q0 = q0.mult(-1f);
        }
        if (q1.dot(q2) < 0f) {
            q2 = q2.mult(-1f);
        }
        if (q2.dot(q3) < 0f) {
            q3 = q3.mult(-1f);
        }
        /*
         * Calculate Squad parameter "a" at either end of the central interval.
         */
        Quaternion a1 = Util.squadA(q0, q1, q2, null);
        Quaternion a2 = Util.squadA(q1, q2, q3, null);
        Util.squad(t, q1, a1, a2, q2, storeResult);

        return storeResult;
    }

    /**
     * Interpolate between 2 unit quaternions using linear (Nlerp/Slerp)
     * interpolation.
     *
     * @param t descaled parameter value (&ge;0, &le;1)
     * @param q0 function value at t=0 (not null, unaffected, norm=1)
     * @param q1 function value at t=1 (not null, unaffected, norm=1)
     * @param storeResult (modified if not null)
     * @return an interpolated unit quaternion (either storeResult or a new
     * instance)
     */
    private Quaternion lerp(float t, Quaternion q0, Quaternion q1,
            Quaternion storeResult) {
        Validate.inRange(t, "t", 0f, 1f);
        MyQuaternion.validateUnit(q0, "q0", 0.0001f);
        MyQuaternion.validateUnit(q0, "q1", 0.0001f);
        if (storeResult == null) {
            storeResult = new Quaternion();
        }

        if (MyQuaternion.ne(q0, q1)) {
            switch (this) {
                case LoopNlerp:
                case Nlerp:
                    storeResult.set(q0);
                    storeResult.nlerp(q1, t);
                    break;
                case LoopQuickSlerp:
                case QuickSlerp:
                    Quaternion q2copy = q1.clone();
                    storeResult.slerp(q0, q2copy, t);
                    break;
                case LoopSlerp:
                case Slerp:
                    /*
                     * Flip signs as necessary to make dot product
                     * of the sampled values non-negative.
                     */
                    if (q0.dot(q1) < 0f) {
                        Quaternion negQ1 = q1.mult(-1f);
                        Util.slerp(t, q0, negQ1, storeResult);
                    } else {
                        Util.slerp(t, q0, q1, storeResult);
                    }
                    break;
                default:
                    throw new IllegalStateException();
            }
        } else {
            storeResult.set(q0);
        }

        return storeResult;
    }

    /**
     * Interpolate among unit quaternions in a cyclic time sequence using
     * cubic-spline interpolation based on the Squad function.
     *
     * @param time parameter value (&ge;0, &le;cycleTime)
     * @param curve rotation curve (not null, unaffected)
     * @param storeResult (modified if not null)
     * @return an interpolated unit quaternion (either storeResult or a new
     * instance)
     */
    private static Quaternion loopSpline(float time, RotationCurve curve,
            Quaternion storeResult) {
        assert time >= 0f : time;
        if (storeResult == null) {
            storeResult = new Quaternion();
        }

        float cycleTime = curve.getCycleTime();
        Validate.inRange(time, "time", 0f, cycleTime);
        float[] times = curve.getTimes();
        int index1 = MyArray.findPreviousIndex(time, times);
        Quaternion q1 = curve.getStartValue(index1);
        /*
         * Interpolate using the Squad function.
         */
        float intervalDuration = curve.getIntervalDuration(index1);
        float t = (time - times[index1]) / intervalDuration;
        Quaternion a1 = curve.getControlPoint1(index1);
        Quaternion a2 = curve.getControlPoint2(index1);
        Quaternion q2 = curve.getEndValue(index1);
        Util.squad(t, q1, a1, a2, q2, storeResult);

        return storeResult;
    }

    /**
     * Precompute curve parameters for middle of 3 intervals in a spline.
     *
     * @param curve (not null, modified)
     * @param index1
     * @param inter12 duration of interval (&gt;0)
     * @param q0 function value preceding q1 (not null, unaffected, norm=1)
     * @param q1 function value at start of interval (not null, unaffected,
     * norm=1)
     * @param q2 function value at end of interval (not null, unaffected,
     * norm=1)
     * @param q3 function value following q1 (not null, unaffected, norm=1)
     */
    private static void precomputeFlipSpline(RotationCurve curve, int index1,
            float inter12, Quaternion q0, Quaternion q1, Quaternion q2,
            Quaternion q3) {
        /*
         * Flip signs as necessary to make dot products of successive
         * sampled values non-negative.
         */
        if (q0.dot(q1) < 0f) {
            q0 = q0.mult(-1f);
        }
        if (q1.dot(q2) < 0f) {
            q2 = q2.mult(-1f);
        }
        if (q2.dot(q3) < 0f) {
            q3 = q3.mult(-1f);
        }
        curve.setParameters(index1, q2, inter12);

        Quaternion a = Util.squadA(q0, q1, q2, null);
        Quaternion b = Util.squadA(q1, q2, q3, null);
        curve.setControlPoints(index1, a, b);
    }

    /**
     * Precompute curve parameters for a cyclic spline.
     *
     * @param curve (not null, modified)
     * @param lastIndex index of the last point to use (&ge;0)
     */
    private void precomputeLoopSpline(RotationCurve curve, int lastIndex) {
        curve.setLastIndex(lastIndex);

        float[] times = curve.getTimes();
        float cycleTime = curve.getCycleTime();
        Quaternion[] quaternions = curve.getQuaternions();

        for (int index1 = 0; index1 <= lastIndex; index1++) {
            int index0 = (index1 == 0) ? lastIndex : index1 - 1;
            int index2;
            float inter12; // interval between keyframes
            if (index1 < lastIndex) {
                index2 = index1 + 1;
                inter12 = times[index2] - times[index1];
            } else {
                index2 = 0;
                inter12 = cycleTime - times[lastIndex];
            }
            assert inter12 > 0f : inter12;
            int index3 = (index2 == lastIndex) ? 0 : index2 + 1;
            Quaternion q0 = quaternions[index0];
            Quaternion q1 = quaternions[index1];
            Quaternion q2 = quaternions[index2];
            Quaternion q3 = quaternions[index3];
            precomputeFlipSpline(curve, index1, inter12, q0, q1, q2, q3);
        }
    }

    /**
     * Precompute curve parameters for an acyclic spline.
     *
     * @param curve (not null, modified)
     */
    private void precomputeSpline(RotationCurve curve) {
        Quaternion[] quaternions = curve.getQuaternions();
        int lastIndex = quaternions.length - 1;
        curve.setLastIndex(lastIndex);

        float[] times = curve.getTimes();

        for (int index1 = 0; index1 <= lastIndex; index1++) {
            // TODO try substituting q for a at the ends of the spline
            int index0;
            if (index1 == 0) {
                index0 = 0;
            } else {
                index0 = index1 - 1;
            }
            float inter12;
            int index2;
            if (index1 == lastIndex) {
                index2 = lastIndex;
                inter12 = 1f;
            } else {
                index2 = index1 + 1;
                inter12 = times[index2] - times[index1];
            }
            int index3 = (index2 == lastIndex) ? lastIndex : index2 + 1;

            Quaternion q0 = quaternions[index0];
            Quaternion q1 = quaternions[index1];
            Quaternion q2 = quaternions[index2];
            Quaternion q3 = quaternions[index3];
            precomputeFlipSpline(curve, index1, inter12, q0, q1, q2, q3);
        }
    }

    /**
     * Interpolate among unit quaternions in an acyclic time sequence using
     * cubic-spline interpolation based on the Squad function.
     *
     * @param time parameter value (&ge;times[0])
     * @param curve rotation curve (not null, unaffected)
     * @param storeResult (modified if not null)
     * @return an interpolated unit quaternion (either storeResult or a new
     * instance)
     */
    private static Quaternion spline(float time, RotationCurve curve,
            Quaternion storeResult) {
        if (storeResult == null) {
            storeResult = new Quaternion();
        }

        float[] times = curve.getTimes();
        assert time >= times[0] : time;
        int index1 = MyArray.findPreviousIndex(time, times);
        Quaternion q1 = curve.getStartValue(index1);
        int lastIndex = curve.getLastIndex();
        if (index1 == lastIndex) {
            storeResult.set(q1);
        } else {
            /*
             * Interpolate using the Squad function.
             */
            float intervalDuration = curve.getIntervalDuration(index1);
            float t = (time - times[index1]) / intervalDuration;
            Quaternion a1 = curve.getControlPoint1(index1);
            Quaternion a2 = curve.getControlPoint2(index1);
            Quaternion q2 = curve.getEndValue(index1);
            Util.squad(t, q1, a1, a2, q2, storeResult);
        }

        return storeResult;
    }
}
