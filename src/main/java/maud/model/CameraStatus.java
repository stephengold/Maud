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
import com.jme3.math.Quaternion;
import com.jme3.math.Vector3f;
import java.util.logging.Level;
import java.util.logging.Logger;
import jme3utilities.MyString;
import jme3utilities.Validate;
import jme3utilities.math.MyVector3f;
import maud.Maud;

/**
 * The status of the camera in Maud's "3D View" screen.
 *
 * @author Stephen Gold sgold@sonic.net
 */
public class CameraStatus implements Cloneable {
    // *************************************************************************
    // constants and loggers

    /**
     * rate to dolly in/out (orbit mode only, percentage points per wheel notch)
     */
    final private static float dollyInOutRate = 15f;
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
    /**
     * local copy of {@link com.jme3.math.Vector3f#UNIT_Y}
     */
    final private static Vector3f yAxis = new Vector3f(0f, 1f, 0f);
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
    /**
     * direction the camera points (unit vector in world coordinates)
     */
    private Vector3f direction = new Vector3f(1f, 0f, 0f);
    /**
     * location of the camera (in world coordinates)
     */
    private Vector3f location = new Vector3f();
    // *************************************************************************
    // new methods exposed

    /**
     * Aim the camera at the 3D cursor.
     */
    public void aim() {
        setLocation(location.clone());
    }

    /**
     * Calculate the camera's azimuth angle from the 3D cursor.
     *
     * @return azimuth angle of the camera, measured clockwise from +X around
     * the 3D cursor's +Y axis (in radians)
     */
    public float azimuthAngle() {
        Vector3f cursorLocation = Maud.model.cursor.copyLocation(null);
        Vector3f offset = location.subtract(cursorLocation);
        float azimuthAngle;
        if (MyVector3f.isZero(offset)) {
            azimuthAngle = 0f;
        } else {
            azimuthAngle = MyVector3f.azimuth(offset);
        }

        return azimuthAngle;
    }

    /**
     * Copy the location of the camera.
     *
     * @param storeResult (modified if not null)
     * @return world coordinates (either storeResult or a new vector)
     */
    public Vector3f copyLocation(Vector3f storeResult) {
        if (storeResult == null) {
            storeResult = new Vector3f();
        }
        storeResult.set(location);

        return storeResult;
    }

    /**
     * Copy the orientation of the camera.
     *
     * @param storeResult (modified if not null)
     * @return rotation relative to world coordinates (either storeResult or a
     * new instance)
     */
    public Quaternion copyOrientation(Quaternion storeResult) {
        if (storeResult == null) {
            storeResult = new Quaternion();
        }
        storeResult.lookAt(direction, yAxis);

        return storeResult;
    }

    /**
     * Calculate the camera's elevation angle from the 3D cursor.
     *
     * @return elevation angle of camera, measured upward from the 3D cursor's
     * X-Z plane (in radians)
     */
    public float elevationAngle() {
        Vector3f cursorLocation = Maud.model.cursor.copyLocation(null);
        Vector3f offset = location.subtract(cursorLocation);
        float elevationAngle;
        if (MyVector3f.isZero(offset)) {
            elevationAngle = 0f;
        } else {
            elevationAngle = MyVector3f.altitude(offset);
        }

        return elevationAngle;
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
     * Move/turn the camera to a horizontal orientation.
     */
    public void goHorizontal() {
        if (orbitMode) {
            float azimuthAngle = azimuthAngle();
            float range = range();
            setOrbit(0f, azimuthAngle, range);

        } else {
            if (direction.x != 0f || direction.z != 0f) {
                direction.y = 0f;
                direction.normalizeLocal();
            }
        }
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
     * Move the camera forward/backward when the scroll wheel is turned.
     *
     * @param amount scroll wheel notches
     */
    public void moveBackward(float amount) {
        if (orbitMode) {
            float rate = 1f + dollyInOutRate / 100f;
            float factor = FastMath.pow(rate, amount);
            float range = range();
            range = FastMath.clamp(range * factor, minRange, maxRange);

            float elevationAngle = elevationAngle();
            float azimuthAngle = azimuthAngle();
            setOrbit(elevationAngle, azimuthAngle, range);

        } else {
            Vector3f offset = direction.mult(amount * flyRate);
            location.addLocal(offset);
        }
    }

    /**
     * Move the camera left/right when the mouse is dragged from left/right.
     *
     * @param amount drag component
     */
    public void moveLeft(float amount) {
        if (orbitMode) {
            float azimuthAngle = azimuthAngle();
            azimuthAngle += 2f * amount;

            float elevationAngle = elevationAngle();
            float range = range();
            setOrbit(elevationAngle, azimuthAngle, range);

        } else {
            Quaternion rotate = new Quaternion();
            rotate.fromAngleAxis(amount, yAxis);
            rotate.multLocal(direction);
        }
    }

    /**
     * Move the camera up/down when the mouse is dragged up/down.
     *
     * @param amount drag component
     */
    public void moveUp(float amount) {
        if (orbitMode) {
            float elevationAngle = elevationAngle();
            elevationAngle += amount;
            elevationAngle = FastMath.clamp(elevationAngle, minElevationAngle,
                    maxElevationAngle);

            float azimuthAngle = azimuthAngle();
            float range = range();
            setOrbit(elevationAngle, azimuthAngle, range);

        } else {
            Vector3f pitchAxis = pitchAxis();
            Quaternion rotate = new Quaternion();
            rotate.fromAngleAxis(amount, pitchAxis);
            rotate.multLocal(direction);
        }
    }

    /**
     * Calculate the camera's pitch axis.
     *
     * @return a new unit vector
     */
    public Vector3f pitchAxis() {
        Vector3f result = direction.cross(yAxis);
        assert !MyVector3f.isZero(result);
        result.normalizeLocal();

        return result;
    }

    /**
     * Calculate the camera's distance from the 3D cursor.
     *
     * @return distance (in world units, &ge;0)
     */
    public float range() {
        Vector3f cursorLocation = Maud.model.cursor.copyLocation(null);
        float range = location.distance(cursorLocation);

        assert range >= 0f : range;
        return range;
    }

    /**
     * Calculate the camera's distance from the specified location.
     *
     * @param worldCoordinates (not null, unaffected)
     * @return distance (in world units, &ge;0)
     */
    public float range(Vector3f worldCoordinates) {
        Validate.nonNull(worldCoordinates, "world coordinates");
        float range = location.distance(worldCoordinates);

        assert range >= 0f : range;
        return range;
    }

    /**
     * Alter the camera direction.
     *
     * @param newDirection (not null, unaffected)
     */
    public void setDirection(Vector3f newDirection) {
        Validate.nonNull(newDirection, "direction");

        if (!MyVector3f.isZero(newDirection)) {
            direction.set(newDirection);
            direction.normalizeLocal();
        }
    }

    /**
     * Alter the camera location.
     *
     * @param newLocation (not null, unaffected)
     */
    public void setLocation(Vector3f newLocation) {
        Validate.nonNull(newLocation, "location");

        if (orbitMode) {
            /*
             * Calculate the new offset relative to the 3D cursor.
             */
            Vector3f cursorLocation = Maud.model.cursor.copyLocation(null);
            Vector3f offset = newLocation.subtract(cursorLocation);
            if (!MyVector3f.isZero(offset)) {
                /*
                 * Convert to spherical coordinates.
                 */
                float elevationAngle = MyVector3f.altitude(offset);
                float azimuthAngle = MyVector3f.azimuth(offset);
                float range = offset.length();
                setOrbit(elevationAngle, azimuthAngle, range);
            }

        } else {
            location.set(newLocation);
        }
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
            aim();
        }
    }

    /**
     * Alter the location and direction in orbit mode, based on the elevation
     * angle, azimuth, and range.
     *
     * @param elevationAngle elevation angle of camera, measured upward from the
     * 3D cursor's X-Z plane (in radians)
     * @param azimuthAngle azimuth angle of the camera, measured clockwise
     * around the 3D cursor's +Y axis (in radians)
     * @param range (in world units, &ge;0)
     */
    public void setOrbit(float elevationAngle, float azimuthAngle,
            float range) {
        Validate.nonNegative(range, "range");
        assert orbitMode;
        /*
         * Limit the range and elevation angle.
         */
        float clampedRange = FastMath.clamp(range, minRange, maxRange);
        float clampedElevation = FastMath.clamp(elevationAngle,
                minElevationAngle, maxElevationAngle);

        Vector3f dir = MyVector3f.fromAltAz(clampedElevation, azimuthAngle);
        Vector3f offset = dir.mult(clampedRange);
        Maud.model.cursor.copyLocation(location);
        location.addLocal(offset);

        dir.negateLocal();
        direction.set(dir);
    }

    /**
     * Reset camera parameters after loading a CG model.
     *
     * @param scale (&gt;0, typically=1)
     */
    public void setScale(float scale) {
        Validate.positive(scale, "scale");

        flyRate = scale / 10f;
        frustumFar = scale * 100f;
        frustumNear = scale / 100f;
        maxRange = scale * 10f;
        minRange = scale / 5f;
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
     * Create a deep copy of this object.
     *
     * @return a new object, equivalent to this one
     * @throws CloneNotSupportedException if superclass isn't cloneable
     */
    @Override
    public CameraStatus clone() throws CloneNotSupportedException {
        CameraStatus clone = (CameraStatus) super.clone();
        clone.direction = direction.clone();
        clone.location = location.clone();

        return clone;
    }
}
