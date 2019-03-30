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
import com.jme3.bullet.collision.shapes.HullCollisionShape;
import com.jme3.bullet.collision.shapes.MultiSphere;
import com.jme3.bullet.collision.shapes.SimplexCollisionShape;
import com.jme3.bullet.collision.shapes.SphereCollisionShape;
import com.jme3.bullet.collision.shapes.infos.ChildCollisionShape;
import com.jme3.bullet.control.PhysicsControl;
import com.jme3.bullet.objects.PhysicsCharacter;
import com.jme3.bullet.objects.PhysicsGhostObject;
import com.jme3.bullet.objects.PhysicsRigidBody;
import com.jme3.bullet.util.CollisionShapeFactory;
import com.jme3.math.Quaternion;
import com.jme3.math.Transform;
import com.jme3.math.Vector3f;
import com.jme3.scene.Spatial;
import com.jme3.scene.control.Control;
import java.nio.FloatBuffer;
import java.util.logging.Logger;
import jme3utilities.MyMesh;
import jme3utilities.MySpatial;
import jme3utilities.Validate;
import jme3utilities.math.MyVector3f;
import jme3utilities.math.RectangularSolid;
import jme3utilities.math.VectorSet;

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
    /**
     * local copy of {@link com.jme3.math.Transform#IDENTITY}
     */
    final private static Transform transformIdentity = new Transform();
    /**
     * local copy of {@link com.jme3.math.Vector3f#ZERO}
     */
    final private static Vector3f translateIdentity = new Vector3f(0f, 0f, 0f);
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
     * Create a shape of the specified type for the selected spatial.
     *
     * @param subtree (not null)
     * @param shapeType (not null)
     * @return a new shape (not null)
     */
    public static CollisionShape makeShape(ShapeType shapeType,
            Spatial subtree) {
        CollisionShape shape;
        switch (shapeType) {
            case Hull:
                shape = PhysicsUtil.makeShapeHull(subtree);
                break;

            case CompoundOfBoxes:
                shape = CollisionShapeFactory.createBoxShape(subtree);
                break;

            case CompoundOfHulls:
                shape = CollisionShapeFactory.createDynamicMeshShape(subtree);
                break;

            case CompoundOfMeshes:
                shape = CollisionShapeFactory.createMeshShape(subtree);
                break;

            case MsSphere:
            case Sphere:
                shape = PhysicsUtil.makeShapeSphere(shapeType, subtree);
                break;

            case Box:
            case Capsule:
            case ConeX:
            case ConeY:
            case ConeZ:
            case CylinderX:
            case CylinderY:
            case CylinderZ:
            case MsBox:
            case MsCapsule:
            case Simplex:
                Vector3f halfExtents = MaudUtil.halfExtents(subtree);
                shape = PhysicsUtil.makeShape(shapeType, halfExtents);
                break;

            case TransBox:
            case TransCapsule:
            case TransConeX:
            case TransConeY:
            case TransConeZ:
            case TransCylinderX:
            case TransCylinderY:
            case TransCylinderZ:
            case TransSimplex:
                shape = PhysicsUtil.makeShapeTranslated(shapeType, subtree);
                break;

            default:
                throw new IllegalArgumentException(shapeType.toString());
        }
        return shape;
    }

    /**
     * Determine the orientation of the specified collision object.
     *
     * @param pco (not null, unaffected)
     * @param storeResult (modified if not null)
     * @return orientation (in physics-space coordinates, either storeResult or
     * a new instance)
     */
    public static Quaternion orientation(PhysicsCollisionObject pco,
            Quaternion storeResult) {
        Validate.nonNull(pco, "object");
        Quaternion result
                = (storeResult == null) ? new Quaternion() : storeResult;

        if (pco instanceof PhysicsRigidBody) {
            PhysicsRigidBody prb = (PhysicsRigidBody) pco;
            prb.getPhysicsRotation(result);
        } else if (pco instanceof PhysicsGhostObject) {
            PhysicsGhostObject ghost = (PhysicsGhostObject) pco;
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
     * @param pco (not null, modified)
     * @param newLocation (in physics-space coordinates, not null, unaffected)
     */
    public static void setLocation(PhysicsCollisionObject pco,
            Vector3f newLocation) {
        Validate.nonNull(pco, "object");
        Validate.nonNull(newLocation, "new location");

        if (pco instanceof PhysicsRigidBody) {
            PhysicsRigidBody body = (PhysicsRigidBody) pco;
            body.setPhysicsLocation(newLocation);
        } else if (pco instanceof PhysicsGhostObject) {
            PhysicsGhostObject ghost = (PhysicsGhostObject) pco;
            ghost.setPhysicsLocation(newLocation);
        } else if (pco instanceof PhysicsCharacter) {
            PhysicsCharacter character = (PhysicsCharacter) pco;
            character.setPhysicsLocation(newLocation);
        } else {
            throw new IllegalStateException();
        }
    }

    /**
     * Reorient (rotate) the specified collision object.
     *
     * @param pco (not null, modified)
     * @param newOrientation (in physics-space coordinates, not null,
     * unaffected)
     */
    public static void setOrientation(PhysicsCollisionObject pco,
            Quaternion newOrientation) {
        Validate.nonNull(pco, "object");
        Validate.nonNull(newOrientation, "new orientation");

        if (pco instanceof PhysicsRigidBody) {
            PhysicsRigidBody body = (PhysicsRigidBody) pco;
            body.setPhysicsRotation(newOrientation);
        } else if (pco instanceof PhysicsGhostObject) {
            PhysicsGhostObject ghost = (PhysicsGhostObject) pco;
            ghost.setPhysicsRotation(newOrientation);
        } else {
            throw new IllegalStateException();
        }
    }

    /**
     * Calculate the transform of the specified collision object.
     *
     * @param pco (not null, unaffected)
     * @param storeResult (modified if not null)
     * @return world transform (either storeResult or a new instance)
     */
    public static Transform transform(PhysicsCollisionObject pco,
            Transform storeResult) {
        Validate.nonNull(pco, "object");
        Transform result
                = (storeResult == null) ? new Transform() : storeResult;

        Vector3f storeLocation = result.getTranslation();
        pco.getPhysicsLocation(storeLocation);

        Quaternion storeOrientation = result.getRotation();
        orientation(pco, storeOrientation);

        CollisionShape shape = pco.getCollisionShape();
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
    // *************************************************************************
    // private methods

    /**
     * Create a symmetrical collision shape of the specified type with the
     * specified half extents and margin.
     *
     * @param shapeType type of shape (not null)
     * @param halfExtents desired half extents relative to the center, not
     * including margin (not null, all non-negative, unaffected)
     * @return a new instance
     */
    private static CollisionShape makeShape(ShapeType shapeType,
            Vector3f halfExtents) {
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
                break;

            case ConeY:
                radius = Math.max(halfExtents.x, halfExtents.z);
                height = 2f * halfExtents.y;
                result = new ConeCollisionShape(radius, height,
                        PhysicsSpace.AXIS_Y);
                break;

            case ConeZ:
                radius = Math.max(halfExtents.x, halfExtents.y);
                height = 2f * halfExtents.z;
                result = new ConeCollisionShape(radius, height,
                        PhysicsSpace.AXIS_Z);
                break;

            case CylinderX:
                result = new CylinderCollisionShape(halfExtents,
                        PhysicsSpace.AXIS_X);
                break;

            case CylinderY:
                result = new CylinderCollisionShape(halfExtents,
                        PhysicsSpace.AXIS_Y);
                break;

            case CylinderZ:
                result = new CylinderCollisionShape(halfExtents,
                        PhysicsSpace.AXIS_Z);
                break;

            case MsBox:
                RectangularSolid solid = new RectangularSolid(halfExtents);
                result = new MultiSphere(solid);
                break;

            case MsCapsule:
                result = new MultiSphere(radius, height, axis);
                break;

            case Simplex:
                Vector3f v1 = halfExtents.clone().multLocal(-1f, -1f, -1f);
                Vector3f v2 = halfExtents.clone().multLocal(+1f, -1f, +1f);
                Vector3f v3 = halfExtents.clone().multLocal(-1f, +1f, +1f);
                Vector3f v4 = halfExtents.clone().multLocal(+1f, +1f, -1f);
                result = new SimplexCollisionShape(v1, v2, v3, v4);
                break;

            default:
                throw new IllegalArgumentException(shapeType.toString());
        }

        return result;
    }

    /**
     * Create a hull shape for the specified subtree.
     *
     * @param subtree (not null)
     * @return a new HullCollisionShape (not null)
     */
    private static CollisionShape makeShapeHull(Spatial subtree) {
        VectorSet vertexLocations
                = MyMesh.listVertexLocations(subtree, null);
        int numVectors = vertexLocations.numVectors();
        assert numVectors > 0 : numVectors;

        Transform localToWorld = subtree.getWorldTransform();
        Transform worldToLocal = localToWorld.invert();
        Vector3f tempLocation = new Vector3f();
        FloatBuffer buffer = vertexLocations.toBuffer();
        buffer.rewind();
        while (buffer.hasRemaining()) {
            buffer.mark();
            tempLocation.x = buffer.get();
            tempLocation.y = buffer.get();
            tempLocation.z = buffer.get();
            /*
             * Transform vertex coordinates to de-scaled shape coordinates.
             */
            worldToLocal.transformVector(tempLocation, tempLocation);

            buffer.reset();
            buffer.put(tempLocation.x);
            buffer.put(tempLocation.y);
            buffer.put(tempLocation.z);
        }

        CollisionShape shape = new HullCollisionShape(buffer);

        assert shape instanceof HullCollisionShape :
                shape.getClass().getSimpleName();
        return shape;
    }

    /**
     * Create a centered sphere shape for the specified subtree.
     *
     * @param shapeType (not null)
     * @param subtree (not null)
     * @return a new CollisionShape (not null)
     */
    private static CollisionShape makeShapeSphere(ShapeType shapeType,
            Spatial subtree) {
        VectorSet vertexLocations
                = MyMesh.listVertexLocations(subtree, null);
        int numVectors = vertexLocations.numVectors();
        assert numVectors > 0 : numVectors;

        Transform localToWorld = subtree.getWorldTransform();
        Transform worldToLocal = localToWorld.invert();
        Vector3f tempLocation = new Vector3f();
        double radiusSquared = 0f;
        FloatBuffer buffer = vertexLocations.toBuffer();
        buffer.rewind();
        while (buffer.hasRemaining()) {
            tempLocation.x = buffer.get();
            tempLocation.y = buffer.get();
            tempLocation.z = buffer.get();

            worldToLocal.transformVector(tempLocation, tempLocation);
            double r2 = MyVector3f.lengthSquared(tempLocation);
            if (r2 > radiusSquared) {
                radiusSquared = r2;
            }
        }
        float radius = (float) Math.sqrt(radiusSquared);

        CollisionShape shape;
        switch (shapeType) {
            case MsSphere:
                shape = new MultiSphere(radius);
                break;

            case Sphere:
                shape = new SphereCollisionShape(radius);
                break;

            default:
                throw new IllegalArgumentException(shapeType.toString());
        }

        return shape;
    }

    /**
     * Create a translated shape of the specified type for the selected spatial.
     *
     * @param shapeType (not null)
     * @param subtree (not null)
     * @return a new shape (not null)
     */
    private static CollisionShape makeShapeTranslated(ShapeType shapeType,
            Spatial subtree) {
        Spatial clone = subtree.clone(false);
        clone.setLocalTransform(transformIdentity);
        Vector3f[] minMax = MySpatial.findMinMaxCoords(clone);
        Vector3f translation = MyVector3f.midpoint(minMax[0], minMax[1], null);
        Vector3f halfExtents = minMax[1].subtract(translation);

        CollisionShape child;
        switch (shapeType) {
            case TransBox:
                child = PhysicsUtil.makeShape(ShapeType.Box, halfExtents);
                break;
            case TransCapsule:
                child = PhysicsUtil.makeShape(ShapeType.Capsule, halfExtents);
                break;
            case TransConeX:
                child = PhysicsUtil.makeShape(ShapeType.ConeX, halfExtents);
                break;
            case TransConeY:
                child = PhysicsUtil.makeShape(ShapeType.ConeY, halfExtents);
                break;
            case TransConeZ:
                child = PhysicsUtil.makeShape(ShapeType.ConeZ, halfExtents);
                break;
            case TransCylinderX:
                child = PhysicsUtil.makeShape(ShapeType.CylinderX, halfExtents);
                break;
            case TransCylinderY:
                child = PhysicsUtil.makeShape(ShapeType.CylinderY, halfExtents);
                break;
            case TransCylinderZ:
                child = PhysicsUtil.makeShape(ShapeType.CylinderZ, halfExtents);
                break;
            case TransSimplex:
                child = PhysicsUtil.makeShape(ShapeType.Simplex, halfExtents);
                break;
            default:
                throw new IllegalArgumentException(shapeType.toString());
        }

        CompoundCollisionShape compound = new CompoundCollisionShape();
        compound.addChildShape(child, translation);

        return compound;
    }
}
