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
import com.jme3.bullet.collision.PhysicsCollisionObject;
import com.jme3.bullet.collision.shapes.BoxCollisionShape;
import com.jme3.bullet.collision.shapes.CapsuleCollisionShape;
import com.jme3.bullet.collision.shapes.CollisionShape;
import com.jme3.bullet.collision.shapes.CompoundCollisionShape;
import com.jme3.bullet.collision.shapes.ConeCollisionShape;
import com.jme3.bullet.collision.shapes.CylinderCollisionShape;
import com.jme3.bullet.collision.shapes.SphereCollisionShape;
import com.jme3.bullet.collision.shapes.infos.ChildCollisionShape;
import com.jme3.bullet.control.PhysicsControl;
import com.jme3.bullet.joints.PhysicsJoint;
import com.jme3.bullet.objects.PhysicsCharacter;
import com.jme3.bullet.objects.PhysicsGhostObject;
import com.jme3.bullet.objects.PhysicsRigidBody;
import com.jme3.bullet.objects.PhysicsVehicle;
import com.jme3.math.Quaternion;
import com.jme3.math.Transform;
import com.jme3.math.Vector3f;
import com.jme3.scene.Spatial;
import com.jme3.scene.control.Control;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.logging.Logger;
import jme3utilities.Validate;
import jme3utilities.math.MyMath;
import jme3utilities.math.MyVector3f;

/**
 * Physics utility methods. All methods should be static.
 *
 * @author Stephen Gold sgold@sonic.net
 */
public class PhysicsUtil {
    // *************************************************************************
    // enums

    /**
     * Enumerate the types of collision shapes that makeShape() knows how to
     * make.
     */
    public enum ShapeType {
        /**
         * BoxCollisionShape
         */
        Box,
        /**
         * CapsuleCollisionShape
         */
        Capsule,
        /**
         * ConeCollisionShape on X axis
         */
        ConeX,
        /**
         * ConeCollisionShape on Y axis
         */
        ConeY,
        /**
         * ConeCollisionShape on Z axis
         */
        ConeZ,
        /**
         * CylinderCollisionShape on X axis
         */
        CylinderX,
        /**
         * CylinderCollisionShape on Y axis
         */
        CylinderY,
        /**
         * CylinderCollisionShape on Z axis
         */
        CylinderZ,
        /**
         * SphereCollisionShape
         */
        Sphere;
    }
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
     * Count the joints in the specified physics space.
     *
     * @param space which physics space (not null, unaffected)
     * @return count (&ge;0)
     */
    public static int countJoints(PhysicsSpace space) {
        Collection<PhysicsJoint> joints = space.getJointList();
        int count = joints.size();

        assert count >= 0 : count;
        return count;
    }

    /**
     * Count the collision objects in the specified physics space.
     *
     * @param space which physics space (not null, unaffected)
     * @return count (&ge;0)
     */
    public static int countObjects(PhysicsSpace space) {
        Collection<PhysicsCharacter> charas = space.getCharacterList();
        Collection<PhysicsGhostObject> ghosts = space.getGhostObjectList();
        Collection<PhysicsRigidBody> rigids = space.getRigidBodyList();
        Collection<PhysicsVehicle> vehics = space.getVehicleList();

        int count
                = charas.size() + ghosts.size() + rigids.size() + vehics.size();

        assert count >= 0 : count;
        return count;
    }

    /**
     * Find the named collision object in the specified physics space.
     *
     * @param name generated name (not null)
     * @param space which physics space (not null, unaffected)
     * @return the pre-existing object, or null if not found
     */
    public static PhysicsCollisionObject findObject(String name,
            PhysicsSpace space) {
        Validate.nonNull(name, "name");
        Validate.nonNull(space, "space");

        if (name.length() < 6) {
            return null;
        }
        String idString = name.substring(5);
        long findId = Long.parseLong(idString);

        String typePrefix = name.substring(0, 5);
        switch (typePrefix) {
            case "chara":
                Collection<PhysicsCharacter> charas = space.getCharacterList();
                for (PhysicsCharacter chara : charas) {
                    long id = chara.getObjectId();
                    if (id == findId) {
                        return chara;
                    }
                }
                break;

            case "ghost":
                Collection<PhysicsGhostObject> gs = space.getGhostObjectList();
                for (PhysicsGhostObject ghost : gs) {
                    long id = ghost.getObjectId();
                    if (id == findId) {
                        return ghost;
                    }
                }
                break;

            case "rigid":
                Collection<PhysicsRigidBody> rigids = space.getRigidBodyList();
                for (PhysicsRigidBody rigid : rigids) {
                    long id = rigid.getObjectId();
                    if (id == findId) {
                        return rigid;
                    }
                }
                break;

            case "vehic":
                Collection<PhysicsVehicle> vehics = space.getVehicleList();
                for (PhysicsVehicle vehic : vehics) {
                    long id = vehic.getObjectId();
                    if (id == findId) {
                        return vehic;
                    }
                }
        }

        return null;
    }

    /**
     * Find the identified collision object in the specified physics space.
     *
     * @param id object identifier
     * @param space which physics space (not null, unaffected)
     * @return the pre-existing object, or null if not found
     */
    public static PhysicsCollisionObject findObject(long id,
            PhysicsSpace space) {
        Validate.nonNull(space, "space");

        PhysicsCollisionObject result = null;
        if (id != -1L) {
            Map<Long, PhysicsCollisionObject> map = objectMap(space);
            result = map.get(id);
        }

        return result;
    }

    /**
     * Find the identified collision shape in the specified physics space.
     *
     * @param id shape identifier
     * @param space which physics space (not null, unaffected)
     * @return the pre-existing shape, or null if not found
     */
    public static CollisionShape findShape(long id, PhysicsSpace space) {
        Validate.nonNull(space, "space");

        CollisionShape result = null;
        if (id != -1L) {
            Map<Long, CollisionShape> map = shapeMap(space);
            result = map.get(id);
        }

        return result;
    }

    /**
     * Create a collision shape of the specified type with the specified half
     * extents and margin.
     *
     * @param shapeType type of shape (not null)
     * @param halfExtents desired half extents relative to the center (not null,
     * all non-negative, unaffected)
     * @param margin desired margin (&ge;0)
     * @return a new instance
     */
    public static CollisionShape makeShape(ShapeType shapeType,
            Vector3f halfExtents, float margin) {
        Validate.nonNull(shapeType, "shape type");
        Validate.nonNull(halfExtents, "half extents");
        assert MyVector3f.isAllNonNegative(halfExtents);
        Validate.nonNegative(margin, "margin");

        CollisionShape result;
        float axisHalfExtent, height, radius;
        int axis;

        switch (shapeType) {
            case Box:
                result = new BoxCollisionShape(halfExtents);
                break;

            case Capsule:
                if (halfExtents.x >= halfExtents.y
                        && halfExtents.x >= halfExtents.z) {
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
                result = new CapsuleCollisionShape(radius, height, axis);
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

            case Sphere:
                radius = MyMath.max(halfExtents.x, halfExtents.y,
                        halfExtents.z);
                assert radius > 0f : radius;
                result = new SphereCollisionShape(radius);
                break;

            default:
                throw new IllegalArgumentException();
        }

        result.setMargin(margin);

        return result;
    }

    /**
     * Determine the location (translation) of the specified collision object.
     *
     * @param object (not null, unaffected)
     * @param storeResult (modified if not null)
     * @return world coordinates (either storeResult or a new instance)
     */
    public static Vector3f location(PhysicsCollisionObject object,
            Vector3f storeResult) {
        Validate.nonNull(object, "object");
        if (storeResult == null) {
            storeResult = new Vector3f();
        }

        if (object instanceof PhysicsRigidBody) {
            PhysicsRigidBody prb = (PhysicsRigidBody) object;
            prb.getPhysicsLocation(storeResult);
        } else if (object instanceof PhysicsGhostObject) {
            PhysicsGhostObject ghost = (PhysicsGhostObject) object;
            ghost.getPhysicsLocation(storeResult);
        } else if (object instanceof PhysicsCharacter) {
            PhysicsCharacter character = (PhysicsCharacter) object;
            character.getPhysicsLocation(storeResult);
        } else {
            throw new IllegalArgumentException();
        }

        return storeResult;
    }

    /**
     * Enumerate all collision objects in the specified physics space.
     *
     * @param space which physics space (not null, unaffected)
     * @return a new map from ids to objects
     */
    public static Map<Long, PhysicsCollisionObject> objectMap(
            PhysicsSpace space) {
        Map<Long, PhysicsCollisionObject> result = new TreeMap<>();

        Collection<PhysicsCharacter> characters = space.getCharacterList();
        for (PhysicsCollisionObject pco : characters) {
            long id = pco.getObjectId();
            result.put(id, pco);
        }

        Collection<PhysicsGhostObject> ghosts = space.getGhostObjectList();
        for (PhysicsCollisionObject pco : ghosts) {
            long id = pco.getObjectId();
            result.put(id, pco);
        }

        Collection<PhysicsRigidBody> rigidBodies = space.getRigidBodyList();
        for (PhysicsCollisionObject pco : rigidBodies) {
            long id = pco.getObjectId();
            result.put(id, pco);
        }

        Collection<PhysicsVehicle> vehicles = space.getVehicleList();
        for (PhysicsCollisionObject pco : vehicles) {
            long id = pco.getObjectId();
            result.put(id, pco);
        }

        return result;
    }

    /**
     * Determine the orientation of the specified collision object.
     *
     * @param object (not null, unaffected)
     * @param storeResult (modified if not null)
     * @return world orientation (either storeResult or a new instance)
     */
    public static Quaternion orientation(PhysicsCollisionObject object,
            Quaternion storeResult) {
        Validate.nonNull(object, "object");
        if (storeResult == null) {
            storeResult = new Quaternion();
        }

        if (object instanceof PhysicsRigidBody) {
            PhysicsRigidBody prb = (PhysicsRigidBody) object;
            prb.getPhysicsRotation(storeResult);
        } else if (object instanceof PhysicsGhostObject) {
            PhysicsGhostObject ghost = (PhysicsGhostObject) object;
            ghost.getPhysicsRotation(storeResult);
        } else { // TODO PhysicsCharacter
            throw new IllegalArgumentException();
        }

        return storeResult;
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
     * controls in the specified spatial.
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
        throw new IllegalArgumentException();
    }

    /**
     * Replace every use of the specified shape in the specified physics space
     * with a new shape.
     *
     * @param space which physics space (not null)
     * @param oldShape shape to replace (not null)
     * @param newShape replacement shape (not null)
     */
    public static void replace(PhysicsSpace space, CollisionShape oldShape,
            CollisionShape newShape) {
        Validate.nonNull(space, "space");
        Validate.nonNull(newShape, "new shape");

        long oldShapeId = oldShape.getObjectId();

        Map<Long, PhysicsCollisionObject> objectMap = objectMap(space);
        for (PhysicsCollisionObject pco : objectMap.values()) {
            CollisionShape shape = pco.getCollisionShape();
            long shapeId = shape.getObjectId();
            if (shapeId == oldShapeId) {
                space.remove(pco);
                pco.setCollisionShape(newShape);
                space.add(pco);
            }
            // TODO check whether shape is used by a ChildCollisionShape
        }
    }

    /**
     * Relocate (translate) the specified collision object.
     *
     * @param object (not null, modified)
     * @param newLocation (not null, unaffected)
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
     * @param newOrientation in world (not null, unaffected)
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
        } else { // TODO PhysicsCharacter
            throw new IllegalStateException();
        }
    }

    /**
     * Enumerate all collision shapes in the specified physics space.
     *
     * @param space which physics space (not null, unaffected)
     * @return a new map from ids to shapes
     */
    public static Map<Long, CollisionShape> shapeMap(PhysicsSpace space) {
        Validate.nonNull(space, "space");

        Map<Long, CollisionShape> result = new TreeMap<>();
        Map<Long, PhysicsCollisionObject> objectMap = objectMap(space);
        for (PhysicsCollisionObject pco : objectMap.values()) {
            CollisionShape shape = pco.getCollisionShape();
            long id = shape.getObjectId();
            result.put(id, shape);
            if (shape instanceof CompoundCollisionShape) {
                CompoundCollisionShape ccs = (CompoundCollisionShape) shape;
                List<ChildCollisionShape> children = ccs.getChildren();
                for (ChildCollisionShape child : children) {
                    CollisionShape childShape = child.shape;
                    long childId = childShape.getObjectId();
                    result.put(childId, childShape);
                }
            }
        }

        return result;
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
        if (storeResult == null) {
            storeResult = new Transform();
        }

        Vector3f storeLocation = storeResult.getTranslation();
        location(object, storeLocation);

        Quaternion storeOrientation = storeResult.getRotation();
        orientation(object, storeOrientation);

        CollisionShape shape = object.getCollisionShape();
        Vector3f scale = shape.getScale();
        storeResult.setScale(scale);

        return storeResult;
    }

    /**
     * Enumerate all collision objects and compound shapes in the specified
     * physics space that reference the identified shape.
     *
     * @param usedShapeId id of the collision shape to find
     * @param space which physics space to search (not null, unaffected)
     * @return a new set of ids of objects/shapes
     */
    public static Set<Long> userSet(long usedShapeId, PhysicsSpace space) {
        Validate.nonNull(space, "space");

        Set<Long> result = new TreeSet<>();
        Map<Long, PhysicsCollisionObject> objectMap = objectMap(space);
        for (PhysicsCollisionObject pco : objectMap.values()) {
            CollisionShape shape = pco.getCollisionShape();
            long shapeId = shape.getObjectId();
            if (shapeId == usedShapeId) {
                long pcoId = pco.getObjectId();
                result.add(pcoId);
            }
            if (shape instanceof CompoundCollisionShape) {
                CompoundCollisionShape ccs = (CompoundCollisionShape) shape;
                List<ChildCollisionShape> children = ccs.getChildren();
                for (ChildCollisionShape child : children) {
                    CollisionShape childShape = child.shape;
                    long childId = childShape.getObjectId();
                    if (childId == usedShapeId) {
                        long parentId = shape.getObjectId();
                        result.add(parentId);
                    }
                }
            }
        }

        return result;
    }
}
