/*
 Copyright (c) 2017-2018, Stephen Gold
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
package maud.model.option.scene;

import com.jme3.math.FastMath;
import java.io.IOException;
import java.io.Writer;
import java.util.logging.Logger;
import jme3utilities.Validate;
import maud.Maud;
import maud.MaudUtil;
import maud.action.ActionPrefix;

/**
 * Status of the scene cameras in Maud's editor screen. TODO rename
 * CameraOptions
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
    final private static Logger logger
            = Logger.getLogger(CameraStatus.class.getName());
    // *************************************************************************
    // fields

    /**
     * maximum distance of camera from the center (orbit mode only, in world
     * units, &gt;0)
     */
    private float maxRange = 10f;
    /**
     * minimum distance of camera from the center (orbit mode only, in world
     * units, &gt;0)
     */
    private float minRange = 0.2f;
    /**
     * movement mode (not null)
     */
    private MovementMode movementMode = MovementMode.Orbit;
    /**
     * centering option (orbit mode only, not null)
     */
    private OrbitCenter orbitCenter = OrbitCenter.DddCursor;
    /**
     * projection mode (not null)
     */
    private ProjectionMode projectionMode = ProjectionMode.Perspective;
    // *************************************************************************
    // new methods exposed

    /**
     * Clamp the elevation angle of the camera in orbit mode.
     *
     * @param angle desired elevation angle (in radians)
     * @return clamped angle (in radians)
     */
    public float clampElevation(float angle) {
        float result;
        result = FastMath.clamp(angle, minElevationAngle, maxElevationAngle);
        return result;
    }

    /**
     * Clamp the distance of the camera from the 3-D cursor in orbit mode.
     *
     * @param range desired distance (in world units)
     * @return clamped distance (in world units)
     */
    public float clampRange(float range) {
        float result = FastMath.clamp(range, minRange, maxRange);
        return result;
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
     * Read the scene camera's movement mode.
     *
     * @return an enum value (not null)
     */
    public MovementMode getMovementMode() {
        assert movementMode != null;
        return movementMode;
    }

    /**
     * Read the centering option for orbit mode.
     *
     * @return an enum value (not null)
     */
    public OrbitCenter getOrbitCenter() {
        assert orbitCenter != null;
        return orbitCenter;
    }

    /**
     * Read the scene camera's projection mode.
     *
     * @return an enum value (not null)
     */
    public ProjectionMode getProjectionMode() {
        assert projectionMode != null;
        return projectionMode;
    }

    /**
     * Test whether the camera is in orbit mode.
     *
     * @return true if in orbit mode, otherwise false
     */
    public boolean isOrbitMode() {
        boolean result = movementMode.equals(MovementMode.Orbit);
        return result;
    }

    /**
     * Test whether the camera is in ortho/parallel-projection mode.
     *
     * @return true if in parallel-projection mode, otherwise false
     */
    public boolean isParallelProjection() {
        boolean result = projectionMode.equals(ProjectionMode.Parallel);
        return result;
    }

    /**
     * Alter the camera's movement mode.
     *
     * @param newMode (not null)
     */
    public void setMode(MovementMode newMode) {
        movementMode = newMode;

        if (newMode == MovementMode.Orbit) {
            Maud.getModel().getSource().getScenePov().aim();
            Maud.getModel().getTarget().getScenePov().aim();
        }
    }

    /**
     * Alter the center for orbit mode.
     *
     * @param newCenter (not null)
     */
    public void setMode(OrbitCenter newCenter) {
        Validate.nonNull(newCenter, "new center");

        orbitCenter = newCenter;
        if (movementMode == MovementMode.Orbit) {
            Maud.getModel().getSource().getScenePov().aim();
            Maud.getModel().getTarget().getScenePov().aim();
        }
    }

    /**
     * Alter the camera's projection mode.
     *
     * @param mode (not null)
     */
    public void setMode(ProjectionMode mode) {
        Validate.nonNull(mode, "mode");
        projectionMode = mode;
    }

    /**
     * Toggle the movement mode.
     */
    public void toggleMovement() {
        if (movementMode.equals(MovementMode.Orbit)) {
            setMode(MovementMode.Fly);
        } else {
            setMode(MovementMode.Orbit);
        }
    }

    /**
     * Toggle the projection mode.
     */
    public void toggleProjection() {
        if (projectionMode.equals(ProjectionMode.Parallel)) {
            projectionMode = ProjectionMode.Perspective;
        } else {
            projectionMode = ProjectionMode.Parallel;
        }
    }

    /**
     * Write the status to a script using the specified writer.
     *
     * @param writer (not null)
     * @throws java.io.IOException if an I/O error occurs while writing
     */
    public void writeToScript(Writer writer) throws IOException {
        Validate.nonNull(writer, "writer");

        String action = ActionPrefix.selectMovement + movementMode.toString();
        MaudUtil.writePerformAction(writer, action);

        action = ActionPrefix.selectOrbitCenter + orbitCenter.toString();
        MaudUtil.writePerformAction(writer, action);

        action = ActionPrefix.selectProjection + projectionMode.toString();
        MaudUtil.writePerformAction(writer, action);
    }
    // *************************************************************************
    // Object methods

    /**
     * Create a copy of this object.
     *
     * @return a new object, equivalent to this one
     * @throws CloneNotSupportedException if the superclass isn't cloneable
     */
    @Override
    public CameraStatus clone() throws CloneNotSupportedException {
        CameraStatus clone = (CameraStatus) super.clone();
        return clone;
    }
}
