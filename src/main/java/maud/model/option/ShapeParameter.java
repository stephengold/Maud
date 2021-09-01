/*
 Copyright (c) 2017-2019, Stephen Gold
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
package maud.model.option;

import com.jme3.bullet.collision.shapes.CapsuleCollisionShape;
import com.jme3.bullet.collision.shapes.CollisionShape;
import com.jme3.bullet.collision.shapes.CompoundCollisionShape;
import com.jme3.bullet.collision.shapes.SphereCollisionShape;
import jme3utilities.Validate;
import jme3utilities.minie.MyShape;

/**
 * Enumerate some parameters of physics collision shapes.
 *
 * @author Stephen Gold sgold@sonic.net
 */
public enum ShapeParameter {
    // *************************************************************************
    // values

    /**
     * unscaled half extent on the local X axis
     */
    HalfExtentX,
    /**
     * unscaled half extent on the local Y axis
     */
    HalfExtentY,
    /**
     * unscaled half extent on the local Z axis
     */
    HalfExtentZ,
    /**
     * unscaled height
     */
    Height,
    /**
     * margin
     */
    Margin,
    /**
     * unscaled radius
     */
    Radius,
    /**
     * X-component of the scale
     */
    ScaleX,
    /**
     * Y-component of the scale
     */
    ScaleY,
    /**
     * Z-component of the scale
     */
    ScaleZ,
    /**
     * scaled volume
     */
    ScaledVolume;
    // *************************************************************************
    // new methods exposed

    /**
     * Test whether this parameter can be set to the specified value in the
     * specified shape.
     *
     * @param shape the shape to alter (not null, modified)
     * @param value the new value
     * @return true if settable, otherwise false
     */
    public boolean canSet(CollisionShape shape, float value) {
        Validate.nonNull(shape, "shape");

        boolean capsule = shape instanceof CapsuleCollisionShape;
        boolean sphere = shape instanceof SphereCollisionShape;

        boolean result;
        switch (this) {
            case HalfExtentX:
            case HalfExtentY:
            case HalfExtentZ:
            case Height:
            case Radius:
                result = false;
                break;
            case Margin:
                result = !sphere && !capsule;
                break;
            case ScaleX:
            case ScaleY:
            case ScaleZ:
            case ScaledVolume:
                result = false; // TODO canScale()
                break;

            default:
                throw new IllegalArgumentException(toString());
        }

        return result;
    }

    /**
     * Determine a maximum value for this parameter.
     *
     * @return a maximum value, or Float.MAX_VALUE if there's no maximum
     */
    public float maxValue() {
        switch (this) {
            case HalfExtentX:
            case HalfExtentY:
            case HalfExtentZ:
            case Height:
            case Margin:
            case Radius:
            case ScaleX:
            case ScaleY:
            case ScaleZ:
            case ScaledVolume:
                return Float.MAX_VALUE;

            default:
                throw new IllegalArgumentException(toString());
        }
    }

    /**
     * Determine a minimum value for this parameter.
     *
     * @return a minimum value, or -Float.MAX_VALUE if there's no minimum
     */
    public float minValue() {
        switch (this) {
            case HalfExtentX:
            case HalfExtentY:
            case HalfExtentZ:
            case Height:
            case Margin:
            case Radius:
            case ScaleX:
            case ScaleY:
            case ScaleZ:
                return Float.MIN_VALUE;

            case ScaledVolume:
                return 0f;

            default:
                throw new IllegalArgumentException(toString());
        }
    }

    /**
     * Read the parameter in the specified shape.
     *
     * @param shape the shape to read (not null, unaffected)
     * @return the parameter value (&ge;0) or NaN if not applicable
     */
    public float read(CollisionShape shape) {
        Validate.nonNull(shape, "shape");

        if (shape instanceof CompoundCollisionShape) {
            switch (this) {
                case HalfExtentX:
                case HalfExtentY:
                case HalfExtentZ:
                case Height:
                case Radius:
                    return Float.NaN;
            }
        }

        float result;
        switch (this) {
            case HalfExtentX:
                result = MyShape.halfExtents(shape, null).x;
                break;
            case HalfExtentY:
                result = MyShape.halfExtents(shape, null).y;
                break;
            case HalfExtentZ:
                result = MyShape.halfExtents(shape, null).z;
                break;
            case Height:
                result = MyShape.height(shape);
                break;
            case Margin:
                result = shape.getMargin();
                break;
            case Radius:
                result = MyShape.radius(shape);
                break;
            case ScaleX:
                result = shape.getScale(null).x;
                break;
            case ScaleY:
                result = shape.getScale(null).y;
                break;
            case ScaleZ:
                result = shape.getScale(null).z;
                break;
            case ScaledVolume:
                result = MyShape.volume(shape);
                break;
            default:
                throw new IllegalArgumentException(toString());
        }

        assert Float.isNaN(result) || result >= 0f : result;
        return result;
    }

    /**
     * Alter the parameter in the specified shape.
     *
     * @param shape the shape to alter (not null, modified)
     * @param newValue the new parameter value (&ge;0)
     */
    public void set(CollisionShape shape, float newValue) {
        Validate.nonNull(shape, "shape");
        Validate.nonNegative(newValue, "new value");
        assert canSet(shape, newValue);

        switch (this) {
            case Margin:
                shape.setMargin(newValue);
                break;

            case ScaleX:
            case ScaleY:
            case ScaleZ:
            case ScaledVolume:
            // TODO for kinematic controls, alter via the spatial

            default:
                throw new IllegalArgumentException(toString());
        }
    }
}
