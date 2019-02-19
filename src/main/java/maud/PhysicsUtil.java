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
package maud;

import com.jme3.bullet.PhysicsSpace;
import com.jme3.bullet.collision.PhysicsCollisionObject;
import com.jme3.bullet.collision.shapes.BoxCollisionShape;
import com.jme3.bullet.collision.shapes.CapsuleCollisionShape;
import com.jme3.bullet.collision.shapes.CollisionShape;
import com.jme3.bullet.collision.shapes.CompoundCollisionShape;
import com.jme3.bullet.collision.shapes.ConeCollisionShape;
import com.jme3.bullet.collision.shapes.CylinderCollisionShape;
import com.jme3.bullet.collision.shapes.MultiSphere;
import com.jme3.bullet.collision.shapes.SimplexCollisionShape;
import com.jme3.bullet.collision.shapes.SphereCollisionShape;
import com.jme3.bullet.collision.shapes.infos.ChildCollisionShape;
import com.jme3.bullet.control.PhysicsControl;
import com.jme3.bullet.objects.PhysicsCharacter;
import com.jme3.bullet.objects.PhysicsGhostObject;
import com.jme3.bullet.objects.PhysicsRigidBody;
import com.jme3.math.Quaternion;
import com.jme3.math.Transform;
import com.jme3.math.Vector3f;
import com.jme3.scene.Spatial;
import com.jme3.scene.control.Control;
import java.util.logging.Logger;
import jme3utilities.Validate;
import jme3utilities.math.MyMath;
import jme3utilities.math.RectangularSolid;

/**
 * Physics utility methods. All methods should be static.
 *
 * @author Stephen Gold sgold@sonic.net
 */
public class PhysicsUtil {
    // *************************************************************************
    // constants and loggers

    /**
     * message logger for this class
     */
    final private static Logger logger
            = Logger.getLogger(PhysicsUtil.class.getName());
    // *************************************************************************
    // constructors

    /**
     * A private constructor to inhibit instantiation of this class.
     */
    private PhysicsUtil() {
    }
    // *************************************************************************
    // new methods exposed

    /**
     * Create a symmetrical collision shape of the specified type with the
     * specified half extents and margin.
     *
     * @param shapeType type of shape (not null, not Hull)
     * @param halfExtents desired half extents relative to the center, not
     * including margin (not null, all non-negative, unaffected)
     * @param margin desired margin (&ge;0, ignored for sphere and capsule)
     * @return a new instance
     */
    public static CollisionShape makeShape(ShapeType shapeType,
            Vector3f halfExtents, float margin) {
        Validate.nonNull(shapeType, "shape type");
        Validate.nonNegative(halfExtents, "half extents");
        Validate.nonNegative(margin, "margin");

        CollisionShape result;
        float axisHalfExtent, height, radius;
        int axis;
        if (halfExtents.x >= halfExtents.y && halfExtents.x >= halfExtents.z) {
            radius = Math.max(halfExtents.y, halfExtents.z);
            axisHalfExtent = halfExtents.x;
            axis = PhysicsSpace.AXIS_X;
        } else if (halfExtents.y >= halfExtents.z) {
            radius = Math.max(halfExtents.x, halfExtents.z);
            axisHalfExtent = halfExtents.y;
            axis = PhysicsSpace.AXIS_Y;
        } else {
            radius = Math.max(halfExtents.x, halfExtents.y);
            axisHalfExtent = halfExtents.z;
            axis = PhysicsSpace.AXIS_Z;
        }
        height = 2f * (axisHalfExtent - radius);
        assert height >= 0f : height;

        switch (shapeType) {
            case Box:
                result = new BoxCollisionShape(halfExtents);
                result.setMargin(margin);
                break;

            case Capsule:
                result = new CapsuleCollisionShape(radius, height, axis);
                // no margin
                break;

            case ConeX:
                radius = Math.max(halfExtents.y, halfExtents.z);
                height = 2f * halfExtents.x;
                result = new ConeCollisionShape(radius, height,
                        PhysicsSpace.AXIS_X);
                result.setMargin(margin);
                break;

            case ConeY:
                radius = Math.max(halfExtents.x, halfExtents.z);
                height = 2f * halfExtents.y;
                result = new ConeCollisionShape(radius, height,
                        PhysicsSpace.AXIS_Y);
                result.setMargin(margin);
                break;

            case ConeZ:
                radius = Math.max(halfExtents.x, halfExtents.y);
                height = 2f * halfExtents.z;
                result = new ConeCollisionShape(radius, height,
                        PhysicsSpace.AXIS_Z);
                result.setMargin(margin);
                break;

            case CylinderX:
                result = new CylinderCollisionShape(halfExtents,
                        PhysicsSpace.AXIS_X);
                result.setMargin(margin);
                break;

            case CylinderY:
                result = new CylinderCollisionShape(halfExtents,
                        PhysicsSpace.AXIS_Y);
                result.setMargin(margin);
                break;

            case CylinderZ:
                result = new CylinderCollisionShape(halfExtents,
                        PhysicsSpace.AXIS_Z);
                result.setMargin(margin);
                break;

            case MsBox:
                RectangularSolid solid = new RectangularSolid(halfExtents);
                result = new MultiSphere(solid);
                result.setMargin(margin);
                break;

            case MsCapsule:
                result = new MultiSphere(radius, height, axis);
                result.setMargin(margin);
                break;

            case MsSphere:
                radius = MyMath.max(halfExtents.x, halfExtents.y,
                        halfExtents.z); // TODO average?
                assert radius >= 0f : radius;
                result = new MultiSphere(radius);
                result.setMargin(margin);
                break;

            case Simplex:
                Vector3f v1 = halfExtents.clone().multLocal(-1f, -1f, -1f);
                Vector3f v2 = halfExtents.clone().multLocal(+1f, -1f, +1f);
                Vector3f v3 = halfExtents.clone().multLocal(-1f, +1f, +1f);
                Vector3f v4 = halfExtents.clone().multLocal(+1f, +1f, -1f);
                result = new SimplexCollisionShape(v1, v2, v3, v4);
                result.setMargin(margin);
                break;

            case Sphere:
                radius = MyMath.max(halfExtents.x, halfExtents.y,
                        halfExtents.z); // TODO average?
                assert radius >= 0f : radius;
                result = new SphereCollisionShape(radius);
                // no margin
                break;

            default:
                throw new IllegalArgumentException(shapeType.toString());
        }

        return result;
    }

    /**
     * Determine the orientation of the specified collision object.
     *
     * @param object (not null, unaffected)
     * @param storeResult (modified if not null)
     * @return orientation (in physics-space coordinates, either storeResult or
     * a new instance)
     */
    public static Quaternion orientation(PhysicsCollisionObject object,
            Quaternion storeResult) {
        Validate.nonNull(object, "object");
        Quaternion result
                = (storeResult == null) ? new Quaternion() : storeResult;

        if (object instanceof PhysicsRigidBody) {
            PhysicsRigidBody prb = (PhysicsRigidBody) object;
            prb.getPhysicsRotation(result);
        } else if (object instanceof PhysicsGhostObject) {
            PhysicsGhostObject ghost = (PhysicsGhostObject) object;
            ghost.getPhysicsRotation(result);
        } else {
            throw new IllegalArgumentException();
        }

        return result;
    }

    /**
     * Find the S-G control in the specified position among physics controls in
     * the specified spatial.
     *
     * @param spatial which spatial to scan (not null)
     * @param position position index (&ge;0)
     * @return the pre-existing physics control instance (not null)
     */
    public static PhysicsControl pcFromPosition(Spatial spatial, int position) {
        Validate.nonNegative(position, "position");

        int numSgcs = spatial.getNumControls();
        int pcCount = 0;
        for (int controlIndex = 0; controlIndex < numSgcs; controlIndex++) {
            Control control = spatial.getControl(controlIndex);
            if (control instanceof PhysicsControl) {
                if (pcCount == position) {
                    return (PhysicsControl) control;
                }
                ++pcCount;
            }
        }
        throw new IllegalArgumentException();
    }

    /**
     * Calculate the position of the specified S-G control among the physics
     * controls in the specified Spatial.
     *
     * @param spatial which spatial to scan (not null)
     * @param pc (a control added to that spatial)
     * @return position index (&ge;0)
     */
    public static int pcToPosition(Spatial spatial, PhysicsControl pc) {
        int numSgcs = spatial.getNumControls();
        int result = 0;
        for (int controlIndex = 0; controlIndex < numSgcs; controlIndex++) {
            Control control = spatial.getControl(controlIndex);
            if (control instanceof PhysicsControl) {
                if (control == pc) {
                    return result;
                }
                ++result;
            }
        }
        throw new IllegalArgumentException(Integer.toString(result));
    }

    /**
     * Relocate (translate) the specified collision object.
     *
     * @param object (not null, modified)
     * @param newLocation (in physics-space coordinates, not null, unaffected)
     */
    public static void setLocation(PhysicsCollisionObject object,
            Vector3f newLocation) {
        Validate.nonNull(object, "object");
        Validate.nonNull(newLocation, "new location");

        if (object instanceof PhysicsRigidBody) {
            PhysicsRigidBody body = (PhysicsRigidBody) object;
            body.setPhysicsLocation(newLocation);
        } else if (object instanceof PhysicsGhostObject) {
            PhysicsGhostObject ghost = (PhysicsGhostObject) object;
            ghost.setPhysicsLocation(newLocation);
        } else if (object instanceof PhysicsCharacter) {
            PhysicsCharacter character = (PhysicsCharacter) object;
            character.setPhysicsLocation(newLocation);
        } else {
            throw new IllegalStateException();
        }
    }

    /**
     * Reorient (rotate) the specified collision object.
     *
     * @param object (not null, modified)
     * @param newOrientation (in physics-space coordinates, not null,
     * unaffected)
     */
    public static void setOrientation(PhysicsCollisionObject object,
            Quaternion newOrientation) {
        Validate.nonNull(object, "object");
        Validate.nonNull(newOrientation, "new orientation");

        if (object instanceof PhysicsRigidBody) {
            PhysicsRigidBody body = (PhysicsRigidBody) object;
            body.setPhysicsRotation(newOrientation);
        } else if (object instanceof PhysicsGhostObject) {
            PhysicsGhostObject ghost = (PhysicsGhostObject) object;
            ghost.setPhysicsRotation(newOrientation);
        } else {
            throw new IllegalStateException();
        }
    }

    /**
     * Calculate the transform of the specified collision object.
     *
     * @param object (not null, unaffected)
     * @param storeResult (modified if not null)
     * @return world transform (either storeResult or a new instance)
     */
    public static Transform transform(PhysicsCollisionObject object,
            Transform storeResult) {
        Validate.nonNull(object, "object");
        Transform result
                = (storeResult == null) ? new Transform() : storeResult;

        Vector3f storeLocation = result.getTranslation();
        object.getPhysicsLocation(storeLocation);

        Quaternion storeOrientation = result.getRotation();
        orientation(object, storeOrientation);

        CollisionShape shape = object.getCollisionShape();
        Vector3f storeScale = result.getScale();
        shape.getScale(storeScale);

        return result;
    }

    /**
     * Test whether the specified shape uses (or identifies as) the identified
     * shape. TODO redesign to use CompoundCollisionShape.findIndex()
     *
     * @param user (not null, unaffected)
     * @param shapeId the ID of the shape to find
     * @return true if used/identical, otherwise false
     */
    public static boolean usesShape(CollisionShape user, long shapeId) {
        long id = user.getObjectId();
        boolean result = false;
        if (id == shapeId) {
            result = true;
        } else if (user instanceof CompoundCollisionShape) {
            CompoundCollisionShape compound = (CompoundCollisionShape) user;
            ChildCollisionShape[] children = compound.listChildren();
            for (ChildCollisionShape child : children) {
                id = child.getShape().getObjectId();
                if (id == shapeId) {
                    result = true;
                    break;
                }
            }
        }

        return result;
    }
}
