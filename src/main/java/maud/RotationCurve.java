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
 * Encapsulate a rotation curve. This is a performance optimization when
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
     * duration of each interval, or null if not using splines
     */
    private float[] intervalDurations;
    /**
     * sample times (not null, same length as quaternions)
     */
    final private float[] times;
    /**
     * control points for samples, or null if not using splines
     */
    private Quaternion[] controlPoints;
    /**
     * function values at sample times (not null, same length as times)
     */
    final private Quaternion[] quaternions;
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
        this.quaternions = quaternions;
        this.controlPoints = null;
        this.intervalDurations = null;
    }
    // *************************************************************************
    // new methods exposed

    /**
     * Access the control point that precedes the indexed sample.
     *
     * @param index which interval (&ge;0, &le;last)
     * @return the pre-existing instance (not null)
     */
    Quaternion getControlPoint(int index) {
        Quaternion controlPoint = controlPoints[index];
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
        int lastIndex = controlPoints.length - 1;
        assert lastIndex >= 0 : lastIndex;
        return lastIndex;
    }

    /**
     * Access the function values at sample times.
     *
     * @return the pre-existing instance (not null)
     */
    Quaternion[] getQuaternions() {
        assert quaternions != null;
        return quaternions;
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
     * Alter the index of the last point to use.
     *
     * @param newLastIndex new index (&ge;0)
     */
    void setLastIndex(int newLastIndex) {
        Validate.nonNegative(newLastIndex, "new last index");

        controlPoints = new Quaternion[newLastIndex + 1];
        intervalDurations = new float[newLastIndex + 1];
    }

    /**
     * Alter the spline parameters for the specified index.
     *
     * @param index sample or interval index (&ge;0, &le;last)
     * @param controlPoint control point for sample (not null, alias created)
     * @param intervalDuration duration of interval (&gt;0)
     */
    void setParameters(int index, Quaternion controlPoint,
            float intervalDuration) {
        Validate.nonNull(controlPoint, "control point");
        Validate.positive(intervalDuration, "interval duration");

        controlPoints[index] = controlPoint;
        intervalDurations[index] = intervalDuration;
    }
}
