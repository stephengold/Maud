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
import java.util.logging.Logger;
import jme3utilities.Validate;

/**
 * Encapsulate a vector curve. This is a performance optimization for
 * interpolating many times on a single spline.
 *
 * @author Stephen Gold sgold@sonic.net
 */
public class VectorCurve {
    // *************************************************************************
    // constants and loggers

    /**
     * message logger for this class
     */
    final private static Logger logger = Logger.getLogger(
            VectorCurve.class.getName());
    // *************************************************************************
    // fields

    /**
     * end time for looping (&ge;0)
     */
    final private float cycleTime;
    /**
     * square root of preceding interval's distance for each interval, or null
     * if not using centripetal splines
     */
    private float[] dt01s;
    /**
     * square root of distance for each interval, or null if not using
     * centripetal splines
     */
    private float[] dt12s;
    /**
     * square root of following interval's distance for each interval, or null
     * if not using centripetal splines
     */
    private float[] dt23s;
    /**
     * time duration for each interval
     */
    private float[] intervalDurations;
    /**
     * start time for each interval (not null, same length as startValues)
     */
    final private float[] times;
    /**
     * 1st auxiliary slope/value for each interval, or null if not using splines
     */
    private Vector3f[] aux1s;
    /**
     * 2nd auxiliary slope/value for each interval, or null if not using splines
     */
    private Vector3f[] aux2s;
    /**
     * ending function value for each interval
     */
    private Vector3f[] endValues;
    /**
     * starting function value for each interval (not null, same length as
     * times)
     */
    final private Vector3f[] startValues;
    // *************************************************************************
    // constructors

    /**
     * Instantiate a generic rotation curve.
     *
     * @param times sample times (not null, alias created)
     * @param cycleTime end time for looping (&ge;0)
     * @param values function values at sample times (not null, same length as
     * times, alias created)
     */
    public VectorCurve(float[] times, float cycleTime, Vector3f[] values) {
        Validate.nonNull(times, "times");
        Validate.nonNegative(cycleTime, "cycle time");
        Validate.nonNull(values, "values");
        assert times.length == values.length;

        this.times = times;
        this.cycleTime = cycleTime;
        this.startValues = values;

        this.endValues = null;
        this.intervalDurations = null;

        this.aux1s = null;
        this.aux2s = null;

        this.dt01s = null;
        this.dt12s = null;
        this.dt23s = null;
    }
    // *************************************************************************
    // new methods exposed

    /**
     * Access the 1st auxiliary slope/value for the indexed interval. Used only
     * for splines.
     *
     * @param index which interval (&ge;0, &le;lastIndex)
     * @return the pre-existing instance (not null)
     */
    Vector3f getAux1(int index) {
        Vector3f aux1 = aux1s[index];
        assert aux1 != null;
        return aux1;
    }

    /**
     * Access the 2nd auxiliary slope/value for the indexed interval. Used only
     * for splines.
     *
     * @param index which interval (&ge;0, &le;lastIndex)
     * @return the pre-existing instance (not null)
     */
    Vector3f getAux2(int index) {
        Vector3f aux2 = aux2s[index];
        assert aux2 != null;
        return aux2;
    }

    /**
     * Read the end time for looping.
     */
    float getCycleTime() {
        return cycleTime;
    }

    /**
     * Read the square root of the preceding interval's distance. Used only for
     * centripetal splines.
     *
     * @param index which interval (&ge;0, &le;lastIndex)
     * @return the square root of the distance (&ge;0)
     */
    float getDt01(int index) {
        float value = dt01s[index];
        return value;
    }

    /**
     * Read the square root of the indexed interval's distance. Used only for
     * centripetal splines.
     *
     * @param index which interval (&ge;0, &le;lastIndex)
     * @return the square root of the distance (&ge;0)
     */
    float getDt12(int index) {
        float value = dt12s[index];
        return value;
    }

    /**
     * Read the square root of the following interval's distance. Used only for
     * centripetal splines.
     *
     * @param index which interval (&ge;0, &le;lastIndex)
     * @return the square root of the distance (&ge;0)
     */
    float getDt23(int index) {
        float value = dt23s[index];
        return value;
    }

    /**
     * Access the ending function value of the indexed interval.
     *
     * @param index which interval (&ge;0, &le;lastIndex)
     * @return the pre-existing instance (not null)
     */
    Vector3f getEndValue(int index) {
        Vector3f value = endValues[index];
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
     * Read the index of the last sample to use.
     *
     * @return the index (&ge;0)
     */
    int getLastIndex() {
        int lastIndex = aux1s.length - 1;
        assert lastIndex >= 0 : lastIndex;
        return lastIndex;
    }

    /**
     * Access the starting function value of the indexed interval.
     *
     * @return the pre-existing instance (not null)
     */
    Vector3f getStartValue(int index) {
        Vector3f value = startValues[index];
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
     * Access the function values for all sample times.
     *
     * @return the pre-existing instance (not null)
     */
    Vector3f[] getValues() {
        assert startValues != null;
        return startValues;
    }

    /**
     * Alter the auxiliary auxiliary slopes/values for the indexed interval.
     * Used only for splines.
     *
     * @param index which interval (&ge;0, &le;lastIndex)
     * @param auxPoint1 1st control point for interval (not null, alias created)
     * @param auxPoint2 2nd control point for interval (not null, alias created)
     */
    void setAuxPoints(int index, Vector3f auxPoint1, Vector3f auxPoint2) {
        Validate.nonNull(auxPoint1, "aux point 1");
        Validate.nonNull(auxPoint2, "aux point 2");

        aux1s[index] = auxPoint1;
        aux2s[index] = auxPoint2;
    }

    /**
     * Alter the root distances for the indexed interval. Used only for
     * centripetal splines.
     *
     * @param index which interval (&ge;0, &le;lastIndex)
     * @param dt01 square root of preceding interval's distance (&ge;0)
     * @param dt12 square root of the interval's distance (&ge;0)
     * @param dt23 square root of following interval's distance (&ge;0)
     */
    void setDts(int index, float dt01, float dt12, float dt23) {
        dt01s[index] = dt01;
        dt12s[index] = dt12;
        dt23s[index] = dt23;
    }

    /**
     * Alter the range of sample points to use.
     *
     * @param newLastIndex new last index (&ge;0)
     * @param allocateDts true for a centripetal spline, otherwise false
     */
    void setLastIndex(int newLastIndex, boolean allocateDts) {
        Validate.nonNegative(newLastIndex, "new last index");

        int numSamples = newLastIndex + 1;
        aux1s = new Vector3f[numSamples];
        aux2s = new Vector3f[numSamples];
        endValues = new Vector3f[numSamples];
        intervalDurations = new float[numSamples];
        if (allocateDts) {
            dt01s = new float[numSamples];
            dt12s = new float[numSamples];
            dt23s = new float[numSamples];
        }
    }

    /**
     * Alter the curve parameters for the indexed interval.
     *
     * @param index interval index (&ge;0, &le;last)
     * @param endValue end value for the interval (not null, alias created)
     * @param intervalDuration time duration of the interval (&gt;0)
     */
    void setParameters(int index, Vector3f endValue, float intervalDuration) {
        Validate.nonNull(endValue, "end value");
        Validate.positive(intervalDuration, "interval duration");

        endValues[index] = endValue;
        intervalDurations[index] = intervalDuration;
    }
}
