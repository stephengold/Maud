/*
 Copyright (c) 2017-2021, Stephen Gold
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
package maud.model.cgm;

import com.jme3.math.FastMath;
import com.jme3.math.Quaternion;
import com.jme3.math.Vector3f;
import com.jme3.renderer.Camera;
import java.util.logging.Logger;
import jme3utilities.MyCamera;
import jme3utilities.Validate;
import jme3utilities.math.MyMath;
import jme3utilities.math.MyVector3f;
import maud.Maud;
import maud.model.option.scene.CameraOptions;
import maud.model.option.scene.OrbitCenter;
import maud.view.scene.SceneView;

/**
 * The positions of the POV and orbit center in a scene view.
 *
 * @author Stephen Gold sgold@sonic.net
 */
public class ScenePov implements Cloneable, Pov {
    // *************************************************************************
    // constants and loggers TODO move some of these to CameraOptions

    /**
     * rate to dolly in/out (orbit mode only, percentage points per wheel notch)
     */
    final private static float dollyInOutRate = 15f;
    /**
     * maximum rotation speed of a scene POV when not directly controlled by the
     * user (in radians per second)
     */
    final private static float maxRotationRate = 1f;
    /**
     * message logger for this class
     */
    final private static Logger logger
            = Logger.getLogger(ScenePov.class.getName());
    /**
     * initial look direction for scene POVs (unit vector in world coordinates)
     */
    final private static Vector3f initialLookDirection
            = new Vector3f(0.78614616f, -0.3275609f, -0.52409756f);
    /**
     * "up" direction for scene POVs (unit vector in world coordinates)
     */
    final private static Vector3f upDirection = new Vector3f(0f, 1f, 0f);
    // *************************************************************************
    // fields

    /**
     * if true, the POV is temporarily pivoting (despite being in orbit mode)
     * because either (1) the center recently moved or (2) the POV recently
     * transitioned from fly mode
     */
    private boolean isPivoting = false;
    /**
     * C-G model using this POV (set by {@link #setCgm(Cgm)})
     */
    private Cgm cgm = null;
    /**
     * POV movement rate for fly mode (in world units per scroll wheel notch,
     * &gt;0)
     */
    private float flyRate = 0.1f;
    /**
     * maximum distance of this POV from its center in orbit mode (in world
     * units, &gt;0)
     */
    private float maxRange = 10f;
    /**
     * minimum distance of this POV from its center in orbit mode (in world
     * units, &gt;0)
     */
    private float minRange = 0.2f;
    /**
     * direction the POV will eventually look (unit vector in world coordinates)
     */
    private Vector3f directionalGoal = initialLookDirection.clone();
    /**
     * location of the center on the previous update (in world coordinates)
     */
    private Vector3f lastCenterLocation = new Vector3f();
    /**
     * direction the POV is looking (unit vector in world coordinates)
     */
    private Vector3f lookDirection = initialLookDirection.clone();
    /**
     * location of the POV (in world coordinates)
     */
    private Vector3f povLocation = new Vector3f();
    // *************************************************************************
    // new methods exposed

    /**
     * Move (or turn) the POV to a horizontal orientation.
     */
    public void goHorizontal() {
        if (Maud.getModel().getScene().getCamera().isOrbitMode()) {
            float azimuth = azimuth();
            setOrbitGoal(0f, azimuth);
        } else {
            if (lookDirection.x != 0f || lookDirection.z != 0f) {
                directionalGoal.set(lookDirection.x, 0f, lookDirection.z);
                MyVector3f.normalizeLocal(directionalGoal);
            }
        }
    }

    /**
     * Copy the location of the POV.
     *
     * @param storeResult storage for the result (modified if not null)
     * @return world coordinates (either storeResult or a new vector)
     */
    public Vector3f location(Vector3f storeResult) {
        if (storeResult == null) {
            return povLocation.clone();
        } else {
            return storeResult.set(povLocation);
        }
    }

    /**
     * Calculate the distance between the POV and the center.
     *
     * @return distance (in world units, &ge;0)
     */
    public float range() {
        Vector3f centerLocation = centerLocation(null);
        float range = povLocation.distance(centerLocation);

        assert range >= 0f : range;
        return range;
    }

    /**
     * Reposition the POV after loading a C-G model.
     *
     * @param newLocation (not null, unaffected)
     */
    public void setLocation(Vector3f newLocation) {
        Validate.nonNull(newLocation, "new location");

        povLocation.set(newLocation);
        if (Maud.getModel().getScene().getCamera().isOrbitMode()) {
            setOrbitGoal();
        }
    }

    /**
     * For orbit mode, update the directional goal after transitioning from fly
     * mode and/or relocating of the center and/or POV. This typically causes
     * the POV start pivoting.
     */
    public void setOrbitGoal() {
        assert Maud.getModel().getScene().getCamera().isOrbitMode();

        Vector3f centerLocation = centerLocation(null);
        Vector3f offset = centerLocation.subtract(povLocation);
        if (!MyVector3f.isZero(offset)) {
            float elevationAngle = MyVector3f.altitude(offset);
            float azimuthAngle = MyVector3f.azimuth(offset);
            setOrbitGoal(elevationAngle, azimuthAngle);
            isPivoting = true;
        }
    }
    // *************************************************************************
    // Object methods

    /**
     * Create a deep copy of this object.
     *
     * @return a new object, equivalent to this one
     * @throws CloneNotSupportedException if the superclass isn't cloneable
     */
    @Override
    public ScenePov clone() throws CloneNotSupportedException {
        ScenePov clone = (ScenePov) super.clone();
        clone.directionalGoal = directionalGoal.clone();
        clone.lastCenterLocation = lastCenterLocation.clone();
        clone.lookDirection = lookDirection.clone();
        clone.povLocation = povLocation.clone();

        return clone;
    }
    // *************************************************************************
    // Pov methods

    /**
     * Move this POV forward/backward when the scroll wheel is turned.
     *
     * @param amount scroll-wheel notches (non-zero)
     */
    @Override
    public void moveBackward(float amount) {
        Validate.nonZero(amount, "amount");

        CameraOptions options = Maud.getModel().getScene().getCamera();
        if (options.isOrbitMode()) {
            if (!isPivoting) {
                float rate = 1f - dollyInOutRate / 100f;
                float factor = FastMath.pow(rate, amount);
                float range = range() * factor;
                float elevationAngle = elevationAngle();
                float azimuthAngle = azimuth();
                setOrbitLocation(elevationAngle, azimuthAngle, range);
            }
        } else {
            Vector3f offset = lookDirection.mult(amount * flyRate);
            povLocation.addLocal(offset);
        }
    }

    /**
     * Move this POV left/right when the mouse is dragged left/right.
     *
     * @param amount drag component (non-zero)
     */
    @Override
    public void moveLeft(float amount) {
        Validate.nonZero(amount, "amount");

        if (Maud.getModel().getScene().getCamera().isOrbitMode()) {
            float azimuthAngle = azimuth() + 2f * amount;
            float elevationAngle = elevationAngle();
            setOrbitGoal(elevationAngle, azimuthAngle);
        } else {
            Quaternion rotate = new Quaternion();
            rotate.fromAngleAxis(amount, upDirection);
            rotate.mult(lookDirection, directionalGoal);
        }
        lookDirection.set(directionalGoal);
    }

    /**
     * Move this POV up/down when the mouse is dragged up/down.
     *
     * @param amount drag component (non-zero)
     */
    @Override
    public void moveUp(float amount) {
        Validate.nonZero(amount, "amount");

        CameraOptions options = Maud.getModel().getScene().getCamera();
        if (options.isOrbitMode()) {
            float azimuthAngle = azimuth();
            float elevationAngle = elevationAngle() - amount;
            setOrbitGoal(elevationAngle, azimuthAngle);
        } else {
            Vector3f pitchAxis = pitchAxis();
            Quaternion rotate = new Quaternion();
            rotate.fromAngleAxis(amount, pitchAxis);
            rotate.mult(lookDirection, directionalGoal);
        }
        lookDirection.set(directionalGoal);
    }

    /**
     * Alter which C-G model uses this POV. (Invoked only during initialization
     * and cloning.)
     *
     * @param newCgm (not null, alias created)
     */
    @Override
    public void setCgm(Cgm newCgm) {
        Validate.nonNull(newCgm, "new model");
        cgm = newCgm;
    }

    /**
     * Alter the rates and limits based on the size of the C-G model.
     *
     * @param cgmSize the C-G model's maximum extent (in world units, &gt;0)
     */
    public void setCgmSize(float cgmSize) {
        Validate.positive(cgmSize, "model size");

        flyRate = 0.1f * cgmSize;
        maxRange = 10f * cgmSize;
        minRange = 0.2f * cgmSize;
    }

    /**
     * Update this POV and its camera.
     *
     * @param tpf time interval between frames (in seconds, &ge;0)
     */
    @Override
    public void update(float tpf) {
        CameraOptions options = Maud.getModel().getScene().getCamera();

        if (options.isOrbitMode()) {
            Vector3f centerLocation = centerLocation(null);
            if (!centerLocation.equals(lastCenterLocation)) {
                /*
                 * Pivot toward the new center.
                 */
                setOrbitGoal();
                lastCenterLocation.set(centerLocation);
            }
        }

        updateLookDirection(tpf);

        if (options.isOrbitMode() && !isPivoting) {
            /*
             * Update the POV's location, so that it looks toward the center.
             */
            float range = range();
            Vector3f offset = lookDirection.mult(range);
            centerLocation(povLocation);
            povLocation.subtractLocal(offset);
        }

        SceneView view = cgm.getSceneView();
        Camera camera = view.getCamera();
        if (camera != null) {
            updateCamera(camera);
        }
    }
    // *************************************************************************
    // private methods

    /**
     * Calculate the POV's azimuth.
     *
     * @return the azimuth of its look direction, measured clockwise from +X
     * around the +Y axis (in radians)
     */
    private float azimuth() {
        float azimuth = MyVector3f.azimuth(lookDirection);
        return azimuth;
    }

    /**
     * Calculate the location of the center.
     *
     * @param storeResult storage for the result (modified if not null)
     * @return world coordinates (either storeResult or a new vector)
     */
    private Vector3f centerLocation(Vector3f storeResult) {
        Vector3f result = (storeResult == null) ? new Vector3f() : storeResult;

        CameraOptions options = Maud.getModel().getScene().getCamera();
        OrbitCenter orbitCenter = options.getOrbitCenter();
        switch (orbitCenter) {
            case DddCursor:
                cgm.getSceneView().getCursor().location(result);
                break;

            case Origin:
                result.zero();
                break;

            case SelectedBone:
                SelectedBone bone = cgm.getBone();
                if (bone.isSelected()) {
                    bone.worldLocation(result);
                } else {
                    cgm.getSceneView().getCursor().location(result);
                }
                break;

            case SelectedVertex:
                SelectedVertex vertex = cgm.getVertex();
                if (vertex.isSelected()) {
                    vertex.worldLocation(result);
                } else {
                    cgm.getSceneView().getCursor().location(result);
                }
                break;

            default:
                throw new IllegalStateException();
        }

        return result;
    }

    /**
     * Clamp the distance of the POV from its center.
     *
     * @param range desired distance (in world units)
     * @return clamped distance (in world units)
     */
    private float clampRange(float range) {
        float result = FastMath.clamp(range, minRange, maxRange);
        return result;
    }

    /**
     * Calculate the POV's elevation angle.
     *
     * @return the elevation angle of its look direction, measured upward from
     * the X-Z plane (in radians)
     */
    private float elevationAngle() {
        float elevationAngle = MyVector3f.altitude(lookDirection);
        return elevationAngle;
    }

    /**
     * Calculate the POV's pitch axis.
     *
     * @return a new unit vector, orthogonal to both its look direction and "up"
     * direction
     */
    private Vector3f pitchAxis() {
        Vector3f result = lookDirection.cross(upDirection);
        assert !MyVector3f.isZero(result);
        MyVector3f.normalizeLocal(result);

        return result;
    }

    /**
     * In orbit mode, set the directional goal using the specified elevation
     * angle and azimuth, provided the POV is not pivoting.
     *
     * @param elevationAngle elevation angle of the goal, measured upward from
     * the X-Z plane (in radians)
     * @param azimuth azimuth of the goal, measured clockwise from +X around the
     * axis (in radians)
     */
    private void setOrbitGoal(float elevationAngle, float azimuth) {
        CameraOptions options = Maud.getModel().getScene().getCamera();
        assert options.isOrbitMode();

        if (!isPivoting) {
            float clampedElevation = CameraOptions.clampElevation(elevationAngle);
            Vector3f direction
                    = MyVector3f.fromAltAz(clampedElevation, azimuth);
            directionalGoal.set(direction);
        }
    }

    /**
     * In orbit mode and not pivoting, set the POV's location using the
     * specified elevation angle, azimuth, and range.
     *
     * @param elevationAngle elevation angle of the center, measured upward from
     * the POV's X-Z plane (in radians)
     * @param azimuth azimuth of the center, measured clockwise from +X around
     * the POV's +Y axis (in radians)
     * @param range distance to the center (in world units, &ge;0)
     */
    private void setOrbitLocation(float elevationAngle, float azimuth,
            float range) {
        assert range >= 0f : range;
        CameraOptions options = Maud.getModel().getScene().getCamera();
        assert options.isOrbitMode();
        assert !isPivoting;

        float clampedElevation = CameraOptions.clampElevation(elevationAngle);
        Vector3f direction = MyVector3f.fromAltAz(clampedElevation, azimuth);
        centerLocation(povLocation);
        float clampedRange = clampRange(range);
        MyVector3f.accumulateScaled(povLocation, direction, -clampedRange);
    }

    /**
     * Update the camera of this POV.
     *
     * @param camera the camera (not null)
     */
    private void updateCamera(Camera camera) {
        camera.setLocation(povLocation);
        camera.lookAtDirection(lookDirection, upDirection);
        // Use the view's aspect ratio in case the boundary is being dragged.
        float aspectRatio = MyCamera.viewAspectRatio(camera);

        float range;
        CameraOptions options = Maud.getModel().getScene().getCamera();
        if (options.isOrbitMode()) {
            range = range();
        } else {
            range = povLocation.length();
            range = clampRange(range);
        }
        float far = 10f * range;
        float near = 0.01f * range;

        boolean parallel = options.isParallelProjection();
        if (parallel) {
            float halfHeight = CameraOptions.getFrustumYHalfTangent() * range;
            float halfWidth = aspectRatio * halfHeight;
            camera.setFrustumBottom(-halfHeight);
            camera.setFrustumFar(far);
            camera.setFrustumLeft(-halfWidth);
            camera.setFrustumNear(near);
            camera.setFrustumRight(halfWidth);
            camera.setFrustumTop(halfHeight);
            camera.setParallelProjection(true);
        } else {
            float yDegrees = CameraOptions.getFrustumYDegrees();
            camera.setFrustumPerspective(yDegrees, aspectRatio, near, far);
        }
    }

    /**
     * Update the look direction, turning the POV toward its goal at a limited
     * rate.
     *
     * @param tpf time interval between frames (in seconds, &ge;0)
     */
    private void updateLookDirection(float tpf) {
        double dot = MyVector3f.dot(lookDirection, directionalGoal);
        dot = MyMath.clamp(dot, 1.0);
        float angle = (float) Math.acos(dot);
        float maxAngle = maxRotationRate * tpf;
        if (angle <= maxAngle) {
            lookDirection.set(directionalGoal);
            isPivoting = false;
        } else {
            float fraction = maxAngle / angle;
            MyVector3f.lerp(fraction, lookDirection, directionalGoal,
                    lookDirection);
        }
        lookDirection.normalizeLocal();
    }
}
