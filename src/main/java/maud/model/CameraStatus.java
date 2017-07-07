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
package maud.model;

import com.jme3.math.FastMath;
import java.util.logging.Level;
import java.util.logging.Logger;
import jme3utilities.MyString;
import maud.Maud;

/**
 * The status of the camera in Maud's editor screen.
 *
 * @author Stephen Gold sgold@sonic.net
 */
public class CameraStatus implements Cloneable {
    // *************************************************************************
    // constants and loggers

    /**
     * vertical angle of the frustum (in degrees of arc, &gt;0)
     */
    final private static float frustumYDegrees = 45f;
    /**
     * Disorientation occurs when the camera looks straight up, so we limit its
     * elevation angle to just under 90 degrees. (orbit mode only, in radians)
     */
    final private static float maxElevationAngle = 1.5f;
    /**
     * minimum elevation angle (orbit mode only, in radians)
     */
    final private static float minElevationAngle = -0.5f;
    /**
     * message logger for this class
     */
    final private static Logger logger = Logger.getLogger(
            CameraStatus.class.getName());
    // *************************************************************************
    // fields

    /**
     * movement mode: true &rarr; orbit, false &rarr; fly
     */
    private boolean orbitMode = true;
    /**
     * projection mode: true &rarr; parallel/orthographic, false &rarr;
     * perspective
     */
    private boolean parallelMode = false;
    /**
     * movement rate (fly mode only, world units per scroll wheel notch)
     */
    private float flyRate = 0.1f;
    /**
     * distance to the far plane of the view frustum (in world units, &gt;0)
     */
    private float frustumFar = 100f;
    /**
     * distance to the near plane of the view frustum (in world units, &gt;0)
     */
    private float frustumNear = 0.01f;
    /**
     * maximum distance of camera from the 3D cursor (orbit mode only, in world
     * units, &gt;0)
     */
    private float maxRange = 10f;
    /**
     * minimum distance of camera from the 3D cursor (orbit mode only, in world
     * units, &gt;0)
     */
    private float minRange = 0.2f;
    // *************************************************************************
    // new methods exposed

    /**
     * Clamp the elevation angle of the camera in orbit mode.
     *
     * @param angle desired elevation angle (in radians)
     * @return clamped angle (in radians)
     */
    float clampElevation(float angle) {
        float result;
        result = FastMath.clamp(angle, minElevationAngle, maxElevationAngle);
        return result;
    }

    /**
     * Clamp the distance of the camera from the 3D cursor in orbit mode.
     *
     * @param range desired distance (in world units)
     * @return clamped distance (in world units)
     */
    float clampRange(float range) {
        float result = FastMath.clamp(range, minRange, maxRange);
        return result;
    }

    /**
     * Read the movement rate for fly mode.
     *
     * @return rate (in world units per scroll wheel notch, &gt;0)
     */
    float getFlyRate() {
        assert flyRate > 0f : flyRate;
        return flyRate;
    }

    /**
     * Read the distance to the far plane of the camera's frustum.
     *
     * @return distance (in world units, &gt;0)
     */
    public float getFrustumFar() {
        assert frustumFar > 0f : frustumFar;
        assert frustumFar > frustumNear : frustumFar;
        return frustumFar;
    }

    /**
     * Read the distance to the near plane of the camera's frustum.
     *
     * @return distance (in world units, &gt;0)
     */
    public float getFrustumNear() {
        assert frustumNear > 0f : frustumNear;
        assert frustumFar > frustumNear : frustumFar;
        return frustumNear;
    }

    /**
     * Read the vertical angle of the camera's frustum.
     *
     * @return angle (in degrees of arc, &gt;0, &lt;180)
     */
    public float getFrustumYDegrees() {
        assert frustumYDegrees > 0f : frustumYDegrees;
        assert frustumYDegrees < 180f : frustumYDegrees;
        return frustumYDegrees;
    }

    /**
     * Test whether the camera is in orbit mode.
     *
     * @return true if in orbit mode, otherwise false
     */
    public boolean isOrbitMode() {
        return orbitMode;
    }

    /**
     * Test whether the camera is in parallel-projection mode.
     *
     * @return true if in parallel-projection mode, otherwise false
     */
    public boolean isParallelProjection() {
        return parallelMode;
    }

    /**
     * Alter one of the camera's modes.
     *
     * @param modeName "fly", "orbit", "parallel", or "perspective"
     */
    public void setMode(String modeName) {
        switch (modeName) {
            case "fly":
                orbitMode = false;
                break;
            case "orbit":
                orbitMode = true;
                break;
            case "parallel":
                parallelMode = true;
                break;
            case "perspective":
                parallelMode = false;
                break;
            default:
                logger.log(Level.SEVERE, "newMode={0}",
                        MyString.quote(modeName));
                throw new IllegalArgumentException();
        }

        if (orbitMode) {
            Maud.model.source.scenePov.aim();
            Maud.model.target.scenePov.aim();
        }
    }

    /**
     * Toggle the projection mode.
     */
    public void toggleProjection() {
        parallelMode = !parallelMode;
    }
    // *************************************************************************
    // Object methods

    /**
     * Create a copy of this object.
     *
     * @return a new object, equivalent to this one
     * @throws CloneNotSupportedException if superclass isn't cloneable
     */
    @Override
    public CameraStatus clone() throws CloneNotSupportedException {
        CameraStatus clone = (CameraStatus) super.clone();
        return clone;
    }
}
