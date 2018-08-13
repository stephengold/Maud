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
import jme3utilities.math.MyMath;
import maud.Maud;
import maud.MaudUtil;
import maud.action.ActionPrefix;

/**
 * Options for cameras and POVs in scene views.
 *
 * @author Stephen Gold sgold@sonic.net
 */
public class CameraOptions implements Cloneable {
    // *************************************************************************
    // constants and loggers

    /**
     * vertical angle for camera frusta (in degrees of arc, &gt;0)
     */
    final private static float frustumYDegrees = 45f;
    /**
     * Disorientation occurs when POVs look straight up, so we limit their
     * elevation angles. (orbit mode only, in radians)
     */
    final private static float maxElevationAngle = 0.5f;
    /**
     * Disorientation occurs when POVs look straight down, so we limit their
     * elevation angles. (orbit mode only, in radians)
     */
    final private static float minElevationAngle = -1.5f;
    /**
     * message logger for this class
     */
    final private static Logger logger
            = Logger.getLogger(CameraOptions.class.getName());
    // *************************************************************************
    // fields

    /**
     * movement mode for all scene POVs (not null)
     */
    private MovementMode movementMode = MovementMode.Orbit;
    /**
     * centering option for all scene POVs (orbit mode only, not null)
     */
    private OrbitCenter orbitCenter = OrbitCenter.DddCursor;
    /**
     * projection mode for all scene POVs (not null)
     */
    private ProjectionMode projectionMode = ProjectionMode.Perspective;
    // *************************************************************************
    // new methods exposed

    /**
     * Clamp the elevation angle of a POV in orbit mode.
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
     * Generate a description of the options, for display in the status bar.
     *
     * @return textual description (not null, length&lt;50)
     */
    public String describe() {
        StringBuilder builder = new StringBuilder(50);
        builder.append(movementMode);
        if (isOrbitMode()) {
            builder.append(" ");
            builder.append(orbitCenter);
        }
        builder.append(" in ");
        builder.append(projectionMode);

        assert builder.length() < 50 : builder.length();
        return builder.toString();
    }

    /**
     * Read the vertical angle for camera frusta.
     *
     * @return angle (in degrees of arc, &gt;0, &lt;180)
     */
    public float getFrustumYDegrees() {
        assert frustumYDegrees > 0f : frustumYDegrees;
        assert frustumYDegrees < 180f : frustumYDegrees;
        return frustumYDegrees;
    }

    /**
     * Read the vertical half-tangent for camera frusta.
     *
     * @return tangent of 1/2 the vertical angle (&gt;0)
     */
    public float getFrustumYHalfTangent() {
        float yRadians = MyMath.toRadians(frustumYDegrees);
        float tangent = FastMath.tan(yRadians / 2f);

        assert tangent > 0f : tangent;
        return tangent;
    }

    /**
     * Read the movement mode for scene POVs.
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
     * Read the projection mode for scene POVs.
     *
     * @return an enum value (not null)
     */
    public ProjectionMode getProjectionMode() {
        assert projectionMode != null;
        return projectionMode;
    }

    /**
     * Test whether scene POVs are in orbit mode.
     *
     * @return true if in orbit mode, otherwise false
     */
    public boolean isOrbitMode() {
        boolean result = movementMode.equals(MovementMode.Orbit);
        return result;
    }

    /**
     * Test whether scene POVs are in ortho/parallel-projection mode.
     *
     * @return true if in parallel-projection mode, otherwise false
     */
    public boolean isParallelProjection() {
        boolean result = projectionMode.equals(ProjectionMode.Parallel);
        return result;
    }

    /**
     * Alter the movement mode for scene POVs.
     *
     * @param newMode (not null)
     */
    public void setMode(MovementMode newMode) {
        Validate.nonNull(newMode, "new mode");

        if (movementMode != newMode) {
            movementMode = newMode;
            if (newMode == MovementMode.Orbit) {
                Maud.getModel().getSource().getScenePov().setOrbitGoal();
                Maud.getModel().getTarget().getScenePov().setOrbitGoal();
            }
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
    }

    /**
     * Alter the projection mode for scene POVs.
     *
     * @param newMode (not null)
     */
    public void setMode(ProjectionMode newMode) {
        Validate.nonNull(newMode, "new mode");
        projectionMode = newMode;
    }

    /**
     * Toggle the movement mode for scene POVs.
     */
    public void toggleMovement() {
        if (movementMode.equals(MovementMode.Orbit)) {
            setMode(MovementMode.Fly);
        } else {
            setMode(MovementMode.Orbit);
        }
    }

    /**
     * Toggle the projection mode for scene POVs.
     */
    public void toggleProjection() {
        if (projectionMode.equals(ProjectionMode.Parallel)) {
            projectionMode = ProjectionMode.Perspective;
        } else {
            projectionMode = ProjectionMode.Parallel;
        }
    }

    /**
     * Write the options to a script using the specified writer.
     *
     * @param writer (not null)
     * @throws java.io.IOException if an I/O error occurs while writing
     */
    void writeToScript(Writer writer) throws IOException {
        assert writer != null;

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
    public CameraOptions clone() throws CloneNotSupportedException {
        CameraOptions clone = (CameraOptions) super.clone();
        return clone;
    }
}
