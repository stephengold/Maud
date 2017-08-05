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
 * unit quaternions. TODO savable/reusable spline parameters and temporaries
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
     * @param cycleTime used for looping (&ge;times[last])
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
        int last = times.length - 1;
        assert cycleTime >= times[last] : cycleTime;
        if (storeResult == null) {
            storeResult = new Quaternion();
        }

        if (last == 0 || time < times[0]) {
            storeResult.set(quaternions[0]);
            return storeResult;
        }

        switch (this) {
            case LoopNlerp:
            case LoopQuickSlerp:
            case LoopSlerp:
                if (times[last] == cycleTime) {
                    if (last > 1) { // ignore the final point
                        loopLerp(time, last - 1, times, cycleTime, quaternions,
                                storeResult);
                    } else { // fall back on acyclic
                        lerp(time, times, quaternions, storeResult);
                    }
                } else {
                    loopLerp(time, last, times, cycleTime, quaternions,
                            storeResult);
                }
                break;

            case LoopSpline:
                if (times[last] == cycleTime) {
                    if (last > 1) { // ignore the final point
                        loopSpline(time, last - 1, times, cycleTime,
                                quaternions, storeResult);
                    } else { // fall back on acyclic
                        spline(time, times, quaternions, storeResult);
                    }
                } else {
                    loopSpline(time, last, times, cycleTime, quaternions,
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

        if (index1 >= times.length - 1) { // the last point
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
     * @param last (index of the last point, &ge;1)
     * @param times (not null, unaffected, in strictly ascending order,
     * times[0]==0)
     * @param cycleTime cycle time (&gt;times[last])
     * @param quaternions function values (not null, unaffected, each norm=1)
     * @param storeResult (modified if not null)
     * @return an interpolated unit quaternion (either storeResult or a new
     * instance)
     */
    public Quaternion loopLerp(float time, int last, float[] times,
            float cycleTime, Quaternion[] quaternions, Quaternion storeResult) {
        Validate.inRange(time, "time", 0f, cycleTime);
        Validate.positive(last, "last");
        Validate.nonNull(times, "times");
        Validate.nonNull(quaternions, "quaternions");
        assert times.length > last : times.length;
        assert quaternions.length > last : quaternions.length;
        assert cycleTime > times[last] : cycleTime;
        if (storeResult == null) {
            storeResult = new Quaternion();
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
        Quaternion q1 = quaternions[index1];
        Quaternion q2 = quaternions[index2];
        lerp(t, q1, q2, storeResult);

        return storeResult;
    }

    /**
     * Interpolate among unit quaternions in a cyclic time sequence using spline
     * interpolation.
     *
     * @param time parameter value (&ge;0, &le;cycleTime)
     * @param last (index of the last point, &ge;1)
     * @param times (not null, unaffected, in strictly ascending order,
     * times[0]==0)
     * @param cycleTime cycle time (&gt;times[last])
     * @param quaternions function values (not null, unaffected, each norm=1)
     * @param storeResult (modified if not null)
     * @return an interpolated unit quaternion (either storeResult or a new
     * instance)
     */
    public Quaternion loopSpline(float time, int last, float[] times,
            float cycleTime, Quaternion[] quaternions, Quaternion storeResult) {
        Validate.inRange(time, "time", 0f, cycleTime);
        Validate.positive(last, "last");
        Validate.nonNull(times, "times");
        Validate.nonNull(quaternions, "quaternions");
        assert times.length > last : times.length;
        assert quaternions.length > last : quaternions.length;
        assert cycleTime > times[last] : cycleTime;
        if (storeResult == null) {
            storeResult = new Quaternion();
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
        int index0 = (index1 == 0) ? last : index1 - 1;
        int index3 = (index2 == last) ? 0 : index2 + 1;

        Quaternion q0 = quaternions[index0];
        Quaternion q1 = quaternions[index1];
        Quaternion q2 = quaternions[index2];
        Quaternion q3 = quaternions[index3];
        /*
         * Calculate Squad parameter "a" at either end of the central interval.
         */
        Quaternion a1 = squadA(q0, q1, q2, null);
        Quaternion a2 = squadA(q1, q2, q3, null);

        float t = (time - times[index1]) / interval;
        squad(t, q1, a1, a2, q2, storeResult);

        return storeResult;
    }

    /**
     * Interpolate among unit quaternions in an acyclic time sequence using
     * cubic-spline interpolation.
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
        int last = times.length - 1;

        if (index1 == last) {
            storeResult.set(q1);
            return storeResult;
        }
        // TODO try substituting q for a at the ends of the spline
        int index0 = (index1 == 0) ? 0 : index1 - 1;
        int index2 = index1 + 1;
        int index3 = (index2 == last) ? last : index2 + 1;
        Quaternion q0 = quaternions[index0];
        Quaternion q2 = quaternions[index2];
        Quaternion q3 = quaternions[index3];
        /*
         * Calculate Squad parameter "a" at either end of the central interval.
         */
        Quaternion a1 = squadA(q0, q1, q2, null);
        Quaternion a2 = squadA(q1, q2, q3, null);
        /*
         * Interpolate using the Squad function.
         */
        float inter12 = times[index2] - times[index1];
        float t = (time - times[index1]) / inter12;
        squad(t, q1, a1, a2, q2, storeResult);

        return storeResult;
    }

    /**
     * Interpolate between 4 unit quaternions using the Squad function.
     *
     * @param t descaled parameter value (&ge;0, &le;1)
     * @param p function value at t=0 (not null, unaffected, norm=1)
     * @param a 1st control point (not null, unaffected, norm=1)
     * @param b 2nd control point (not null, unaffected, norm=1)
     * @param q function value at t=1 (not null, unaffected, norm=1)
     * @param storeResult (modified if not null)
     * @return interpolated unit quaternion (either storeResult or a new
     * instance)
     */
    public static Quaternion squad(float t, Quaternion p, Quaternion a,
            Quaternion b, Quaternion q, Quaternion storeResult) {
        Validate.inRange(t, "t", 0f, 1f);
        Util.validateUnit(p, "p", 0.0001f);
        Util.validateUnit(a, "a", 0.0001f);
        Util.validateUnit(b, "b", 0.0001f);
        Util.validateUnit(q, "q", 0.0001f);
        if (storeResult == null) {
            storeResult = new Quaternion();
        }

        Quaternion qSlerp = slerp(t, p, q, null);
        Quaternion aSlerp = slerp(t, a, b, null);
        slerp(2f * t * (1f - t), qSlerp, aSlerp, storeResult);

        return storeResult;
    }
    // *************************************************************************
    // private methods

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
        Util.validateUnit(q0, "q0", 0.0001f);
        Util.validateUnit(q0, "q1", 0.0001f);
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
                    if (q0.dot(q1) < 0f) {
                        Quaternion negQ1 = q1.mult(-1f);
                        slerp(t, q0, negQ1, storeResult);
                    } else {
                        slerp(t, q0, q1, storeResult);
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
     * Interpolate between 2 unit quaternions using spherical linear (Slerp)
     * interpolation. This method is slower (but more accurate) than
     * {@link com.jme3.math.Quaternion#slerp(com.jme3.math.Quaternion, float)}
     * and doesn't trash q1. The caller is responsible for flipping the sign of
     * q0 or q1 when it's appropriate to do so.
     *
     * @param t descaled parameter value (&ge;0, &le;1)
     * @param q0 function value at t=0 (not null, unaffected, norm=1)
     * @param q1 function value at t=1 (not null, unaffected, norm=1)
     * @param storeResult (modified if not null)
     * @return an interpolated unit quaternion (either storeResult or a new
     * instance)
     */
    private static Quaternion slerp(float t, Quaternion q0, Quaternion q1,
            Quaternion storeResult) {
        Validate.inRange(t, "t", 0f, 1f);
        Util.validateUnit(q0, "q0", 0.0001f);
        Util.validateUnit(q1, "q1", 0.0001f);
        if (storeResult == null) {
            storeResult = new Quaternion();
        }

        Quaternion q0inverse = Util.conjugate(q0, null); // TODO less garbage
        Quaternion ratio = q0inverse.mult(q1);
        Quaternion power = Util.pow(ratio, t, null);
        storeResult.set(q0);
        storeResult.multLocal(power);

        assert storeResult.norm() > 0.9999f : storeResult;
        assert storeResult.norm() < 1.0001f : storeResult;
        return storeResult;
    }

    /**
     * Calculate Squad parameter "a" for a continuous 1st derivative at the
     * middle point of 3 specified control points.
     *
     * @param q0 previous control point (not null, unaffected, norm=1)
     * @param q1 current control point (not null, unaffected, norm=1)
     * @param q2 following control point (not null, unaffected, norm=1)
     * @param storeResult (modified if not null)
     * @return a unit quaternion for use as a Squad parameter (either
     * storeResult or a new instance)
     */
    private static Quaternion squadA(Quaternion q0, Quaternion q1,
            Quaternion q2, Quaternion storeResult) {
        if (storeResult == null) {
            storeResult = new Quaternion();
        }

        Quaternion q1c = Util.conjugate(q1, null); // TODO less garbage
        Quaternion turn0 = q1c.mult(q0);
        Quaternion logTurn0 = Util.log(turn0, null);
        Quaternion turn2 = q1c.mult(q2);
        Quaternion logTurn2 = Util.log(turn2, null);
        Quaternion sum = logTurn2.add(logTurn0);
        sum.multLocal(-0.25f);
        Quaternion exp = Util.exp(sum, null);
        storeResult.set(q1);
        storeResult.multLocal(exp);

        return storeResult;
    }
}
