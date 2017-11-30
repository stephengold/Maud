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

import com.jme3.bullet.PhysicsSpace;
import com.jme3.bullet.collision.shapes.BoxCollisionShape;
import com.jme3.bullet.collision.shapes.CapsuleCollisionShape;
import com.jme3.bullet.collision.shapes.CollisionShape;
import com.jme3.bullet.collision.shapes.CompoundCollisionShape;
import com.jme3.bullet.collision.shapes.ConeCollisionShape;
import com.jme3.bullet.collision.shapes.CylinderCollisionShape;
import com.jme3.bullet.collision.shapes.SphereCollisionShape;
import com.jme3.bullet.collision.shapes.infos.ChildCollisionShape;
import com.jme3.math.Vector3f;
import java.lang.reflect.Field;
import java.util.List;
import java.util.logging.Logger;
import jme3utilities.Validate;
import jme3utilities.math.MyVector3f;

/**
 * Physics utility methods. All methods should be static.
 *
 * @author Stephen Gold sgold@sonic.net
 */
public class MyShape {
    // *************************************************************************
    // constants and loggers

    /**
     * message logger for this class
     */
    final private static Logger logger
            = Logger.getLogger(MyShape.class.getName());
    // *************************************************************************
    // constructors

    /**
     * A private constructor to inhibit instantiation of this class.
     */
    private MyShape() {
    }
    // *************************************************************************
    // new methods exposed

    /**
     * Determine the main axis of the specified shape, provided it's a capsule,
     * cone, or cylinder.
     *
     * @param shape (may be null, unaffected)
     * @return 0&rarr;X, 1&rarr;Y, 2&rarr;Z, -1&rarr;doesn't have an axis
     */
    public static int axisIndex(CollisionShape shape) {
        int result = -1;
        if (shape instanceof CapsuleCollisionShape) {
            CapsuleCollisionShape capsule = (CapsuleCollisionShape) shape;
            result = capsule.getAxis();

        } else if (shape instanceof ConeCollisionShape) {
            ConeCollisionShape cone = (ConeCollisionShape) shape;
            Field axisField;
            try {
                axisField = ConeCollisionShape.class.getDeclaredField("axis");
            } catch (NoSuchFieldException e) {
                throw new RuntimeException();
            }
            axisField.setAccessible(true);

            try {
                result = (Integer) axisField.get(cone);
            } catch (IllegalAccessException e) {
                throw new RuntimeException();
            }

        } else if (shape instanceof CylinderCollisionShape) {
            CylinderCollisionShape cylinder = (CylinderCollisionShape) shape;
            result = cylinder.getAxis();
        }

        return result;
    }

    /**
     * Test whether the specified scale can be applied to the specified shape.
     *
     * @param shape which collision shape (not null, unaffected)
     * @param scale scale factor for each local axis (not null, unaffected)
     * @return true if can be applied, otherwise false
     */
    public static boolean canScale(CollisionShape shape, Vector3f scale) {
        boolean result = true;
        if (shape instanceof CompoundCollisionShape
                || shape instanceof CapsuleCollisionShape
                || shape instanceof CylinderCollisionShape
                || shape instanceof SphereCollisionShape) {
            result = (scale.x == scale.y && scale.y == scale.z);
        }

        return result;
    }

    /**
     * Calculate the un-scaled half extents of the specified shape, which must
     * be a box, capsule, cone, cylinder, or sphere.
     *
     * @param shape (not null, unaffected)
     * @param storeResult (modified if not null)
     * @return a vector with all components non-negative (either storeResult or
     * a new instance)
     */
    public static Vector3f halfExtents(CollisionShape shape,
            Vector3f storeResult) {
        Validate.nonNull(shape, "shape");
        if (storeResult == null) {
            storeResult = new Vector3f();
        }

        if (shape instanceof BoxCollisionShape) {
            BoxCollisionShape box = (BoxCollisionShape) shape;
            Vector3f halfExtents = box.getHalfExtents();
            storeResult.set(halfExtents);

        } else if (shape instanceof CapsuleCollisionShape) {
            CapsuleCollisionShape capsule = (CapsuleCollisionShape) shape;
            float height = capsule.getHeight();
            float radius = capsule.getRadius();
            float axisHalfExtent = height / 2f + radius;
            int axisIndex = axisIndex(shape);
            switch (axisIndex) {
                case PhysicsSpace.AXIS_X:
                    storeResult.set(axisHalfExtent, radius, radius);
                    break;
                case PhysicsSpace.AXIS_Y:
                    storeResult.set(radius, axisHalfExtent, radius);
                    break;
                case PhysicsSpace.AXIS_Z:
                    storeResult.set(radius, radius, axisHalfExtent);
                    break;
                default:
                    throw new IllegalStateException();
            }

        } else if (shape instanceof ConeCollisionShape) {
            ConeCollisionShape cone = (ConeCollisionShape) shape;
            float height = cone.getHeight();
            float radius = cone.getRadius();
            float axisHalfExtent = height / 2f;
            int axisIndex = axisIndex(shape);
            switch (axisIndex) {
                case PhysicsSpace.AXIS_X:
                    storeResult.set(axisHalfExtent, radius, radius);
                    break;
                case PhysicsSpace.AXIS_Y:
                    storeResult.set(radius, axisHalfExtent, radius);
                    break;
                case PhysicsSpace.AXIS_Z:
                    storeResult.set(radius, radius, axisHalfExtent);
                    break;
                default:
                    throw new IllegalStateException();
            }

        } else if (shape instanceof CylinderCollisionShape) {
            CylinderCollisionShape cylinder = (CylinderCollisionShape) shape;
            Vector3f halfExtents = cylinder.getHalfExtents();
            storeResult.set(halfExtents);

        } else if (shape instanceof SphereCollisionShape) {
            SphereCollisionShape sphere = (SphereCollisionShape) shape;
            float radius = sphere.getRadius();
            storeResult.set(radius, radius, radius);

        } else {
            throw new IllegalArgumentException();
        }

        assert MyVector3f.isAllNonNegative(storeResult) : storeResult;

        return storeResult;
    }

    /**
     * Calculate the un-scaled height of the specified shape.
     *
     * @param shape (not null, unaffected)
     * @return un-scaled height &ge;0) or NaN if the shape is not a capsule,
     * cone, cylinder, or sphere
     */
    public static float height(CollisionShape shape) {
        Validate.nonNull(shape, "shape");

        float result = Float.NaN;
        if (shape instanceof CapsuleCollisionShape) {
            CapsuleCollisionShape capsule = (CapsuleCollisionShape) shape;
            result = capsule.getHeight();

        } else if (shape instanceof ConeCollisionShape) {
            ConeCollisionShape cone = (ConeCollisionShape) shape;
            result = cone.getHeight();

        } else if (shape instanceof CylinderCollisionShape) {
            Vector3f halfExtents = halfExtents(shape, null);
            int axisIndex = axisIndex(shape);
            switch (axisIndex) {
                case PhysicsSpace.AXIS_X:
                    result = halfExtents.x;
                    break;
                case PhysicsSpace.AXIS_Y:
                    result = halfExtents.y;
                    break;
                case PhysicsSpace.AXIS_Z:
                    result = halfExtents.z;
                    break;
                default:
                    throw new IllegalArgumentException();
            }

        } else if (shape instanceof SphereCollisionShape) {
            SphereCollisionShape sphere = (SphereCollisionShape) shape;
            result = 2f * sphere.getRadius();
        }

        assert Float.isNaN(result) || result >= 0f : result;
        return result;
    }

    /**
     * Calculate the un-scaled radius of the specified shape.
     *
     * @param shape (not null, unaffected)
     * @return un-scaled radius (&ge;0) or NaN if the shape is not a capsule,
     * cone, or sphere
     */
    public static float radius(CollisionShape shape) {
        Validate.nonNull(shape, "shape");

        float result = Float.NaN;
        if (shape instanceof CapsuleCollisionShape) {
            CapsuleCollisionShape capsule = (CapsuleCollisionShape) shape;
            result = capsule.getRadius();

        } else if (shape instanceof ConeCollisionShape) {
            ConeCollisionShape cone = (ConeCollisionShape) shape;
            result = cone.getRadius();

        } else if (shape instanceof SphereCollisionShape) {
            SphereCollisionShape sphere = (SphereCollisionShape) shape;
            result = sphere.getRadius();
        }

        assert Float.isNaN(result) || result >= 0f : result;
        return result;
    }

    /**
     * Copy a shape, altering only its half extents.
     *
     * @param oldShape input shape (not null, unaffected)
     * @param newHalfExtents (not null, all non-negative, unaffected)
     * @return a new shape, or null if not possible
     */
    public static CollisionShape setHalfExtents(CollisionShape oldShape,
            Vector3f newHalfExtents) {
        Validate.nonNull(oldShape, "old shape");
        Validate.nonNull(newHalfExtents, "new half extents");
        assert MyVector3f.isAllNonNegative(newHalfExtents);

        CollisionShape result;
        if (oldShape instanceof BoxCollisionShape) {
            result = new BoxCollisionShape(newHalfExtents);

        } else if (oldShape instanceof CapsuleCollisionShape
                || oldShape instanceof ConeCollisionShape) {
            int axisIndex = axisIndex(oldShape);
            float axisHalfExtent, radius1, radius2;
            switch (axisIndex) {
                case PhysicsSpace.AXIS_X:
                    axisHalfExtent = newHalfExtents.x;
                    radius1 = newHalfExtents.y;
                    radius2 = newHalfExtents.z;
                    break;
                case PhysicsSpace.AXIS_Y:
                    axisHalfExtent = newHalfExtents.y;
                    radius1 = newHalfExtents.x;
                    radius2 = newHalfExtents.z;
                    break;
                case PhysicsSpace.AXIS_Z:
                    axisHalfExtent = newHalfExtents.z;
                    radius1 = newHalfExtents.x;
                    radius2 = newHalfExtents.y;
                    break;
                default:
                    throw new IllegalStateException();
            }
            if (radius1 != radius2) {
                result = null;
            } else if (oldShape instanceof CapsuleCollisionShape) {
                float height = 2f * (axisHalfExtent - radius1);
                result = new CapsuleCollisionShape(radius1, height, axisIndex);
            } else {
                assert oldShape instanceof ConeCollisionShape;
                float height = 2f * axisHalfExtent;
                result = new ConeCollisionShape(radius1, height, axisIndex);
            }

        } else if (oldShape instanceof CylinderCollisionShape) {
            int axisIndex = axisIndex(oldShape);
            result = new CylinderCollisionShape(newHalfExtents, axisIndex);

        } else if (oldShape instanceof SphereCollisionShape) {
            if (newHalfExtents.x != newHalfExtents.y
                    || newHalfExtents.y != newHalfExtents.z) {
                result = null;
            } else {
                result = new SphereCollisionShape(newHalfExtents.x);
            }

        } else {
            throw new IllegalArgumentException();
        }

        if (result != null) {
            float margin = oldShape.getMargin();
            result.setMargin(margin);
        }

        return result;
    }

    /**
     * Copy a shape, altering only its height.
     *
     * @param oldShape input shape (not null, unaffected)
     * @param newHeight un-scaled height (&ge;0)
     * @return a new shape
     */
    public static CollisionShape setHeight(CollisionShape oldShape,
            float newHeight) {
        Validate.nonNull(oldShape, "old shape");
        Validate.nonNegative(newHeight, "new height");

        CollisionShape result;
        if (oldShape instanceof BoxCollisionShape) {
            result = setRadius(oldShape, newHeight / 2f);

        } else if (oldShape instanceof CapsuleCollisionShape) {
            float radius = radius(oldShape);
            int axisIndex = axisIndex(oldShape);
            result = new CapsuleCollisionShape(radius, newHeight, axisIndex);

        } else if (oldShape instanceof ConeCollisionShape) {
            float radius = radius(oldShape);
            int axisIndex = axisIndex(oldShape);
            result = new ConeCollisionShape(radius, newHeight, axisIndex);

        } else if (oldShape instanceof CylinderCollisionShape) {
            Vector3f halfExtents = halfExtents(oldShape, null);
            int axisIndex = axisIndex(oldShape);
            switch (axisIndex) {
                case PhysicsSpace.AXIS_X:
                    halfExtents.x = newHeight;
                    break;
                case PhysicsSpace.AXIS_Y:
                    halfExtents.y = newHeight;
                    break;
                case PhysicsSpace.AXIS_Z:
                    halfExtents.z = newHeight;
                    break;
                default:
                    throw new IllegalArgumentException();
            }
            result = new CylinderCollisionShape(halfExtents, axisIndex);

        } else if (oldShape instanceof SphereCollisionShape) {
            result = setHeight(oldShape, newHeight / 2f);

        } else {
            throw new IllegalArgumentException();
        }

        float margin = oldShape.getMargin();
        result.setMargin(margin);

        return result;
    }

    /**
     * Copy a shape, altering only its radius.
     *
     * @param oldShape input shape (not null, unaffected)
     * @param newRadius un-scaled radius (&ge;0)
     * @return a new shape
     */
    public static CollisionShape setRadius(CollisionShape oldShape,
            float newRadius) {
        Validate.nonNull(oldShape, "old shape");
        Validate.nonNegative(newRadius, "new radius");

        CollisionShape result;
        if (oldShape instanceof BoxCollisionShape) {
            Vector3f halfExtents
                    = new Vector3f(newRadius, newRadius, newRadius);
            result = new BoxCollisionShape(halfExtents);

        } else if (oldShape instanceof CapsuleCollisionShape) {
            int axisIndex = axisIndex(oldShape);
            float height = height(oldShape);
            result = new CapsuleCollisionShape(newRadius, height, axisIndex);

        } else if (oldShape instanceof ConeCollisionShape) {
            int axisIndex = axisIndex(oldShape);
            float height = height(oldShape);
            result = new ConeCollisionShape(newRadius, height, axisIndex);

        } else if (oldShape instanceof CylinderCollisionShape) {
            Vector3f halfExtents = halfExtents(oldShape, null);
            int axisIndex = axisIndex(oldShape);
            switch (axisIndex) {
                case PhysicsSpace.AXIS_X:
                    halfExtents.y = newRadius;
                    halfExtents.z = newRadius;
                    break;
                case PhysicsSpace.AXIS_Y:
                    halfExtents.x = newRadius;
                    halfExtents.z = newRadius;
                    break;
                case PhysicsSpace.AXIS_Z:
                    halfExtents.x = newRadius;
                    halfExtents.y = newRadius;
                    break;
                default:
                    throw new IllegalArgumentException();
            }
            result = new CylinderCollisionShape(halfExtents, axisIndex);

        } else if (oldShape instanceof SphereCollisionShape) {
            result = new SphereCollisionShape(newRadius);

        } else {
            throw new IllegalArgumentException();
        }

        float margin = oldShape.getMargin();
        result.setMargin(margin);

        return result;
    }

    /**
     * Test whether the specified shape uses (or identifies as) the identified
     * shape.
     *
     * @param user (not null, unaffected)
     * @param shapeId id of the shape to find
     * @return true if used/identical, otherwise false
     */
    public static boolean usesShape(CollisionShape user, long shapeId) {
        long id = user.getObjectId();
        boolean result = false;
        if (id == shapeId) {
            result = true;
        } else if (user instanceof CompoundCollisionShape) {
            CompoundCollisionShape compound = (CompoundCollisionShape) user;
            List<ChildCollisionShape> children = compound.getChildren();
            for (ChildCollisionShape child : children) {
                id = child.shape.getObjectId();
                if (id == shapeId) {
                    result = true;
                    break;
                }
            }
        }

        return result;
    }
}
