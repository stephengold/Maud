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
package maud.view;

import com.jme3.math.FastMath;
import com.jme3.math.Quaternion;
import com.jme3.math.Transform;
import com.jme3.math.Vector3f;
import java.util.logging.Logger;
import jme3utilities.math.MyMath;

/**
 * The world transform applied to a loaded C-G model in its scene view.
 *
 * @author Stephen Gold sgold@sonic.net
 */
public class CgmTransform implements Cloneable {
    // *************************************************************************
    // constants and loggers

    /**
     * message logger for this class
     */
    final private static Logger logger
            = Logger.getLogger(CgmTransform.class.getName());
    /**
     * local copy of {@link com.jme3.math.Vector3f#UNIT_X}
     */
    final private static Vector3f xAxis = new Vector3f(1f, 0f, 0f);
    /**
     * local copy of {@link com.jme3.math.Vector3f#UNIT_Y}
     */
    final private static Vector3f yAxis = new Vector3f(0f, 1f, 0f);
    // *************************************************************************
    // fields

    /**
     * true&rarr; model Z-axis points up, false&rarr; model Y-axis points up
     */
    private boolean zUpFlag = false;
    /**
     * scale of the C-G model on all 3 axes (&gt;0, 1 &rarr; unscaled)
     */
    private float scale = 1f;
    /**
     * rotation angle of the C-G model around the world's +Y axis (in radians)
     */
    private float yAngle = 0f;
    /**
     * Y-coordinate (in world space) of the C-G model's base in bind pose
     */
    private float yOffset = 0f;
    /**
     * coordinates (in CG-model space) of the C-G model's geometric center in
     * bind pose
     */
    private Vector3f bindCenter = new Vector3f();
    // *************************************************************************
    // new methods exposed

    /**
     * Automatically configure the transform for a newly loaded C-G model.
     *
     * @param center the coordinates (in CG-model space) of the geometric center
     * (not null, unaffected)
     * @param baseElevation the elevation (in CG-model space) of the base
     * @param maxExtent the greatest extent of the C-G model over its 3
     * principal axes (in CG-model units, &gt;0)
     * @param zUp true&rarr; model Z-axis points up
     */
    void loadCgm(Vector3f center, float baseElevation, float maxExtent,
            boolean zUp) {
        assert center != null;
        assert Float.isFinite(baseElevation) : baseElevation;
        assert maxExtent > 0f : maxExtent;

        bindCenter.set(center);
        scale = 1f / maxExtent;
        yOffset = -baseElevation * scale;
        zUpFlag = zUp;
    }

    /**
     * Add to the Y-axis rotation angle.
     *
     * @param angle (in radians)
     */
    public void rotateY(float angle) {
        yAngle = MyMath.modulo(yAngle + angle, FastMath.TWO_PI);
    }

    /**
     * Enlarge the C-G model.
     *
     * @param scaleFactor scale factor (&gt;0, 1&rarr;no effect)
     */
    void scale(float scaleFactor) {
        assert scaleFactor > 0f : scaleFactor;
        scale *= scaleFactor;
    }

    /**
     * Calculate the transform to be applied to the C-G model. Note that this
     * may differ from the world transform of the C-G model's root spatial.
     *
     * @return a new instance (in world coordinates)
     */
    Transform worldTransform() {
        Transform result = new Transform();
        Quaternion rotation = result.getRotation();
        rotation.fromAngleNormalAxis(yAngle, yAxis);
        if (zUpFlag) {
            Quaternion q2 = new Quaternion();
            q2.fromAngleNormalAxis(-FastMath.HALF_PI, xAxis);
            rotation.multLocal(q2);
        }
        result.getScale().set(scale, scale, scale);
        /*
         * Translate the C-G model so that (in bind pose) its geometric center
         * is directly above the world's origin (center of platform).
         */
        Vector3f modelTranslation;
        if (zUpFlag) {
            modelTranslation = new Vector3f(-bindCenter.x, -bindCenter.y, 0f);
        } else {
            modelTranslation = new Vector3f(-bindCenter.x, 0f, -bindCenter.z);
        }
        Vector3f worldTranslation
                = result.transformVector(modelTranslation, null);
        worldTranslation.y += yOffset;
        result.setTranslation(worldTranslation);

        return result;
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
    public CgmTransform clone() throws CloneNotSupportedException {
        CgmTransform clone = (CgmTransform) super.clone();
        clone.bindCenter = bindCenter.clone();

        return clone;
    }
}
