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
import com.jme3.renderer.Camera;
import java.util.logging.Logger;
import jme3utilities.MyCamera;
import jme3utilities.Validate;
import jme3utilities.math.MyVector3f;
import maud.Maud;
import maud.view.SceneView;

/**
 * The positions of a scene camera and 3D cursor in Maud's edit screen.
 *
 * @author Stephen Gold sgold@sonic.net
 */
public class ScenePov implements Cloneable, Pov {
    // *************************************************************************
    // constants and loggers

    /**
     * rate to dolly in/out (orbit mode only, percentage points per wheel notch)
     */
    final private static float dollyInOutRate = 15f;
    /**
     * message logger for this class
     */
    final private static Logger logger = Logger.getLogger(
            ScenePov.class.getName());
    /**
     * local copy of {@link com.jme3.math.Vector3f#UNIT_Y}
     */
    final private static Vector3f yAxis = new Vector3f(0f, 1f, 0f);
    // *************************************************************************
    // fields

    /**
     * loaded CG model containing this POV (set by
     * {@link #setCgm(LoadedCGModel)})
     */
    private LoadedCgm loadedCgm = null;
    /**
     * direction the scene camera points (unit vector in world coordinates)
     */
    private Vector3f cameraDirection = new Vector3f(1f, 0f, 0f);
    /**
     * location of the scene camera (in world coordinates)
     */
    private Vector3f cameraLocation = new Vector3f();
    /**
     * the location of the 3D cursor (in world coordinates)
     */
    private Vector3f cursorLocation = new Vector3f();
    // *************************************************************************
    // new methods exposed

    /**
     * Aim the camera at the 3D cursor without changing the location of either.
     * (Orbit mode only)
     */
    public void aim() {
        assert Maud.model.camera.isOrbitMode();
        setCameraLocation(cameraLocation.clone());
    }

    /**
     * Copy the location of the camera.
     *
     * @param storeResult (modified if not null)
     * @return world coordinates (either storeResult or a new vector)
     */
    public Vector3f cameraLocation(Vector3f storeResult) {
        if (storeResult == null) {
            storeResult = new Vector3f();
        }
        storeResult.set(cameraLocation);

        return storeResult;
    }

    /**
     * Copy the location of the cursor.
     *
     * @param storeResult (modified if not null)
     * @return world coordinates (either storeResult or a new vector)
     */
    public Vector3f cursorLocation(Vector3f storeResult) {
        if (storeResult == null) {
            storeResult = new Vector3f();
        }
        storeResult.set(cursorLocation);

        return storeResult;
    }

    /**
     * Move/turn the camera to a horizontal orientation.
     */
    public void goHorizontal() {
        if (Maud.model.camera.isOrbitMode()) {
            float azimuthAngle = azimuthAngle();
            float range = range();
            setOrbitMode(0f, azimuthAngle, range);

        } else {
            if (cameraDirection.x != 0f || cameraDirection.z != 0f) {
                cameraDirection.y = 0f;
                cameraDirection.normalizeLocal();
            }
        }
    }

    /**
     * Calculate the camera's distance from the 3D cursor.
     *
     * @return distance (in world units, &ge;0)
     */
    public float range() {
        float range = cameraLocation.distance(cursorLocation);
        assert range >= 0f : range;
        return range;
    }

    /**
     * Alter the camera location.
     *
     * @param newLocation (not null, unaffected)
     */
    public void setCameraLocation(Vector3f newLocation) {
        Validate.nonNull(newLocation, "location");

        if (Maud.model.camera.isOrbitMode()) {
            /*
             * Calculate the new offset relative to the 3D cursor.
             */
            Vector3f offset = newLocation.subtract(cursorLocation);
            if (!MyVector3f.isZero(offset)) {
                /*
                 * Convert to spherical coordinates.
                 */
                float elevationAngle = MyVector3f.altitude(offset);
                float azimuthAngle = MyVector3f.azimuth(offset);
                float range = offset.length();
                setOrbitMode(elevationAngle, azimuthAngle, range);
            }

        } else {
            cameraLocation.set(newLocation);
        }
    }

    /**
     * Alter the location of the cursor.
     *
     * @param newLocation (in world coordinates, not null, unaffected)
     */
    public void setCursorLocation(Vector3f newLocation) {
        Validate.nonNull(newLocation, "location");
        cursorLocation.set(newLocation);
    }

    /**
     * Calculate the scale of the cursor.
     *
     * @return world scale factor (&ge;0)
     */
    public float worldScaleForCursor() {
        float range = range();
        float worldScale = Maud.model.cursor.getSize() * range;

        assert worldScale >= 0f : worldScale;
        return worldScale;
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
    public ScenePov clone() throws CloneNotSupportedException {
        ScenePov clone = (ScenePov) super.clone();
        clone.cursorLocation = cursorLocation.clone();
        clone.cameraDirection = cameraDirection.clone();
        clone.cameraLocation = cameraLocation.clone();

        return clone;
    }
    // *************************************************************************
    // Pov methods

    /**
     * Zoom the camera and/or move it forward/backward when the scroll wheel is
     * turned.
     *
     * @param amount scroll wheel notches
     */
    @Override
    public void moveBackward(float amount) {
        if (Maud.model.camera.isOrbitMode()) {
            float rate = 1f + dollyInOutRate / 100f;
            float factor = FastMath.pow(rate, amount);
            float range = range();
            range = Maud.model.camera.clampRange(range * factor);

            float elevationAngle = elevationAngle();
            float azimuthAngle = azimuthAngle();
            setOrbitMode(elevationAngle, azimuthAngle, range);

        } else {
            float flyRate = Maud.model.camera.getFlyRate();
            Vector3f offset = cameraDirection.mult(amount * flyRate);
            cameraLocation.addLocal(offset);
        }
    }

    /**
     * Move the camera left/right when the mouse is dragged left/right.
     *
     * @param amount drag component
     */
    @Override
    public void moveLeft(float amount) {
        if (Maud.model.camera.isOrbitMode()) {
            float azimuthAngle = azimuthAngle();
            azimuthAngle += 2f * amount;

            float elevationAngle = elevationAngle();
            float range = range();
            setOrbitMode(elevationAngle, azimuthAngle, range);

        } else {
            Quaternion rotate = new Quaternion();
            rotate.fromAngleAxis(amount, yAxis);
            rotate.multLocal(cameraDirection);
        }
    }

    /**
     * Move the camera up/down when the mouse is dragged up/down.
     *
     * @param amount drag component
     */
    @Override
    public void moveUp(float amount) {
        if (Maud.model.camera.isOrbitMode()) {
            float elevationAngle = elevationAngle();
            elevationAngle += amount;
            elevationAngle = Maud.model.camera.clampElevation(elevationAngle);

            float azimuthAngle = azimuthAngle();
            float range = range();
            setOrbitMode(elevationAngle, azimuthAngle, range);

        } else {
            Vector3f pitchAxis = pitchAxis();
            Quaternion rotate = new Quaternion();
            rotate.fromAngleAxis(amount, pitchAxis);
            rotate.multLocal(cameraDirection);
        }
    }

    /**
     * Alter which loaded CG model corresponds to this POV. (Invoked only during
     * initialization and cloning.)
     *
     * @param newLoaded (not null)
     */
    @Override
    public void setCgm(LoadedCgm newLoaded) {
        assert newLoaded != null;
        loadedCgm = newLoaded;
    }

    /**
     * Update the camera for this POV.
     */
    @Override
    public void updateCamera() {
        if (Maud.model.camera.isOrbitMode()) {
            aim(); // in case the 3D cursor moved
        }

        SceneView view = loadedCgm.getSceneView();
        Camera camera = view.getCamera();
        if (camera != null) {
            camera.setLocation(cameraLocation);
            Quaternion orientation = cameraOrientation(null);
            camera.setRotation(orientation);

            float aspectRatio = MyCamera.frustumAspectRatio(camera);
            float range = range();
            float far = 10f * range;
            float near = 0.01f * range;
            boolean parallel = Maud.model.camera.isParallelProjection();
            if (parallel) {
                float halfHeight = 0.4f * range;
                float halfWidth = aspectRatio * halfHeight;
                camera.setFrustumBottom(-halfHeight);
                camera.setFrustumFar(far);
                camera.setFrustumLeft(-halfWidth);
                camera.setFrustumNear(near);
                camera.setFrustumRight(halfWidth);
                camera.setFrustumTop(halfHeight);
                camera.setParallelProjection(true);
            } else {
                float yDegrees = Maud.model.camera.getFrustumYDegrees();
                camera.setFrustumPerspective(yDegrees, aspectRatio, near, far);
            }
        }
    }
    // *************************************************************************
    // private methods

    /**
     * Calculate the camera's azimuth angle from the 3D cursor.
     *
     * @return azimuth angle of the camera, measured clockwise from +X around
     * the 3D cursor's +Y axis (in radians)
     */
    private float azimuthAngle() {
        Vector3f offset = cameraLocation.subtract(cursorLocation);
        float azimuthAngle;
        if (MyVector3f.isZero(offset)) {
            azimuthAngle = 0f;
        } else {
            azimuthAngle = MyVector3f.azimuth(offset);
        }

        return azimuthAngle;
    }

    /**
     * Calculate the orientation of the camera.
     *
     * @param storeResult (modified if not null)
     * @return rotation relative to world coordinates (either storeResult or a
     * new instance)
     */
    private Quaternion cameraOrientation(Quaternion storeResult) {
        if (storeResult == null) {
            storeResult = new Quaternion();
        }
        storeResult.lookAt(cameraDirection, yAxis);

        return storeResult;
    }

    /**
     * Calculate the camera's elevation angle from the 3D cursor.
     *
     * @return elevation angle of camera, measured upward from the 3D cursor's
     * X-Z plane (in radians)
     */
    private float elevationAngle() {
        Vector3f offset = cameraLocation.subtract(cursorLocation);
        float elevationAngle;
        if (MyVector3f.isZero(offset)) {
            elevationAngle = 0f;
        } else {
            elevationAngle = MyVector3f.altitude(offset);
        }

        return elevationAngle;
    }

    /**
     * Calculate the camera's pitch axis.
     *
     * @return a new unit vector
     */
    private Vector3f pitchAxis() {
        Vector3f result = cameraDirection.cross(yAxis);
        assert !MyVector3f.isZero(result);
        result.normalizeLocal();

        return result;
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
    private void setOrbitMode(float elevationAngle, float azimuthAngle,
            float range) {
        Validate.nonNegative(range, "range");
        assert Maud.model.camera.isOrbitMode();
        /*
         * Limit the range and elevation angle.
         */
        float clampedRange = Maud.model.camera.clampRange(range);
        float clampedElevation;
        clampedElevation = Maud.model.camera.clampElevation(elevationAngle);

        Vector3f dir = MyVector3f.fromAltAz(clampedElevation, azimuthAngle);
        Vector3f offset = dir.mult(clampedRange);
        cameraLocation.set(cursorLocation);
        cameraLocation.addLocal(offset);

        dir.negateLocal();
        cameraDirection.set(dir);
    }
}
