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
import java.util.logging.Logger;
import jme3utilities.Validate;

/**
 * Encapsulate a rotation curve. This is a performance optimization for
 * interpolating many times on a single spline.
 *
 * @author Stephen Gold sgold@sonic.net
 */
public class RotationCurve {
    // *************************************************************************
    // constants and loggers

    /**
     * message logger for this class
     */
    final private static Logger logger = Logger.getLogger(
            RotationCurve.class.getName());
    // *************************************************************************
    // fields

    /**
     * end time for looping
     */
    final private float cycleTime;
    /**
     * duration of each interval
     */
    private float[] intervalDurations;
    /**
     * start time of each interval (not null, same length as startValues)
     */
    final private float[] times;
    /**
     * 1st control point of each interval, or null if not using splines
     */
    private Quaternion[] controlPoint1s;
    /**
     * 2nd control point of each interval, or null if not using splines
     */
    private Quaternion[] controlPoint2s;
    /**
     * ending function value of each interval
     */
    private Quaternion[] endValues;
    /**
     * starting function value of each interval (not null, same length as times)
     */
    final private Quaternion[] startValues;
    // *************************************************************************
    // constructors

    /**
     * Instantiate a generic rotation curve.
     *
     * @param times sample times (not null, alias created)
     * @param cycleTime end time for looping
     * @param quaternions function values at sample times (not null, alias
     * created, same length as times)
     */
    public RotationCurve(float[] times, float cycleTime,
            Quaternion[] quaternions) {
        Validate.nonNull(times, "times");
        Validate.nonNull(quaternions, "quaternions");
        assert times.length == quaternions.length;

        this.times = times;
        this.cycleTime = cycleTime;
        this.startValues = quaternions;

        this.endValues = null;
        this.intervalDurations = null;

        this.controlPoint1s = null;
        this.controlPoint2s = null;
    }
    // *************************************************************************
    // new methods exposed

    /**
     * Access the 1st control point for the indexed interval.
     *
     * @param index which interval (&ge;0, &le;last)
     * @return the pre-existing instance (not null)
     */
    Quaternion getControlPoint1(int index) {
        Quaternion controlPoint = controlPoint1s[index];
        assert controlPoint != null;
        return controlPoint;
    }

    /**
     * Access the 2nd control point for the indexed interval.
     *
     * @param index which interval (&ge;0, &le;last)
     * @return the pre-existing instance (not null)
     */
    Quaternion getControlPoint2(int index) {
        Quaternion controlPoint = controlPoint2s[index];
        assert controlPoint != null;
        return controlPoint;
    }

    /**
     * Read the end time for looping.
     */
    float getCycleTime() {
        return cycleTime;
    }

    /**
     * Access the ending function value of the indexed interval.
     *
     * @return the pre-existing instance (not null)
     */
    Quaternion getEndValue(int index) {
        Quaternion value = endValues[index];
        assert value != null;
        return value;
    }

    /**
     * Read the duration of the indexed interval.
     *
     * @param index which interval (&ge;0, &le;last)
     * @return the duration (&gt;0)
     */
    float getIntervalDuration(int index) {
        float intervalDuration = intervalDurations[index];
        assert intervalDuration > 0f : intervalDuration;
        return intervalDuration;
    }

    /**
     * Read the index of the last point to use.
     *
     * @param newLastIndex new index (&ge;0)
     */
    int getLastIndex() {
        int lastIndex = controlPoint1s.length - 1;
        assert lastIndex >= 0 : lastIndex;
        return lastIndex;
    }

    /**
     * Access the function values at sample times.
     *
     * @return the pre-existing instance (not null)
     */
    Quaternion[] getQuaternions() {
        assert startValues != null;
        return startValues;
    }

    /**
     * Access the starting function value of the indexed interval.
     *
     * @return the pre-existing instance (not null)
     */
    Quaternion getStartValue(int index) {
        Quaternion value = startValues[index];
        assert value != null;
        return value;
    }

    /**
     * Access the sample times.
     *
     * @return the pre-existing instance (not null)
     */
    float[] getTimes() {
        assert times != null;
        return times;
    }

    /**
     * Alter the control points for the indexed interval. Used only for splines.
     *
     * @param index interval index (&ge;0, &le;last)
     * @param controlPoint1 1st control point for interval (not null, alias
     * created)
     * @param controlPoint2 2nd control point for interval (not null, alias
     * created)
     */
    void setControlPoints(int index, Quaternion controlPoint1,
            Quaternion controlPoint2) {
        Validate.nonNull(controlPoint1, "control point 1");
        Validate.nonNull(controlPoint2, "control point 2");

        controlPoint1s[index] = controlPoint1;
        controlPoint2s[index] = controlPoint2;
    }

    /**
     * Alter the index of the last point to use.
     *
     * @param newLastIndex new index (&ge;0)
     */
    void setLastIndex(int newLastIndex) {
        Validate.nonNegative(newLastIndex, "new last index");

        controlPoint1s = new Quaternion[newLastIndex + 1];
        controlPoint2s = new Quaternion[newLastIndex + 1];
        endValues = new Quaternion[newLastIndex + 1];
        intervalDurations = new float[newLastIndex + 1];
    }

    /**
     * Alter the curve parameters for the indexed interval.
     *
     * @param index interval index (&ge;0, &le;last)
     * @param endValue end value for interval (not null, alias created)
     * @param intervalDuration duration of interval (&gt;0)
     */
    void setParameters(int index, Quaternion endValue, float intervalDuration) {
        Validate.nonNull(endValue, "end value");
        Validate.positive(intervalDuration, "interval duration");

        endValues[index] = endValue;
        intervalDurations[index] = intervalDuration;
    }
}
