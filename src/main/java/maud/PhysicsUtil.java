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
import com.jme3.math.Vector3f;
import com.jme3.scene.Spatial;
import com.jme3.scene.control.Control;
import java.lang.reflect.Field;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.logging.Logger;
import jme3utilities.Validate;
import jme3utilities.math.MyMath;

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
     * Test whether a shape can be scaled.
     *
     * @param shape which collision shape (not null, unaffected)
     * @return true if the shape is scalable, otherwise false
     */
    public static boolean canScale(CollisionShape shape) {
        boolean result = true;
        if (shape instanceof CapsuleCollisionShape
                || shape instanceof CompoundCollisionShape
                || shape instanceof CylinderCollisionShape
                || shape instanceof SphereCollisionShape) {
            result = false;
        }

        return result;
    }

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
     * Read the axis index of the specified shape.
     *
     * @param shape (not null, unaffected)
     * @return 0&rarr;X, 1&rarr;Y, 2&rarr;Z, -1&rarr;none
     */
    public static int getAxisIndex(CollisionShape shape) {
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
     * Create a collision shape of the specified type suitable for the specified
     * spatial.
     *
     * @param spatial where the physics control will be attached (not null)
     * @param shapeType type of shape (not null)
     * @return a new instance
     */
    public static CollisionShape makeShape(Spatial spatial,
            ShapeType shapeType) {
        CollisionShape result;
        float height, radius;
        int axis;
        Vector3f halfExtents;

        switch (shapeType) {
            case Box:
                halfExtents = MaudUtil.halfExtents(spatial);
                result = new BoxCollisionShape(halfExtents);
                break;

            case Capsule:
                halfExtents = MaudUtil.halfExtents(spatial);
                if (halfExtents.x >= halfExtents.y
                        && halfExtents.x >= halfExtents.z) {
                    radius = Math.max(halfExtents.y, halfExtents.z);
                    height = halfExtents.x;
                    axis = PhysicsSpace.AXIS_X;
                } else if (halfExtents.y >= halfExtents.z) {
                    radius = Math.max(halfExtents.x, halfExtents.z);
                    height = halfExtents.y;
                    axis = PhysicsSpace.AXIS_Y;
                } else {
                    radius = Math.max(halfExtents.x, halfExtents.y);
                    height = halfExtents.z;
                    axis = PhysicsSpace.AXIS_Z;
                }
                height = 2f * (height - radius);
                assert height >= 0f : height;
                result = new CapsuleCollisionShape(radius, height, axis);
                break;

            case ConeX:
                halfExtents = MaudUtil.halfExtents(spatial);
                radius = Math.max(halfExtents.y, halfExtents.z);
                height = 2f * halfExtents.x;
                result = new ConeCollisionShape(radius, height,
                        PhysicsSpace.AXIS_X);
                break;

            case ConeY:
                halfExtents = MaudUtil.halfExtents(spatial);
                radius = Math.max(halfExtents.x, halfExtents.z);
                height = 2f * halfExtents.y;
                result = new ConeCollisionShape(radius, height,
                        PhysicsSpace.AXIS_Y);
                break;

            case ConeZ:
                halfExtents = MaudUtil.halfExtents(spatial);
                radius = Math.max(halfExtents.x, halfExtents.y);
                height = 2f * halfExtents.z;
                result = new ConeCollisionShape(radius, height,
                        PhysicsSpace.AXIS_Z);
                break;

            case CylinderX:
                halfExtents = MaudUtil.halfExtents(spatial);
                result = new CylinderCollisionShape(halfExtents,
                        PhysicsSpace.AXIS_X);
                break;

            case CylinderY:
                halfExtents = MaudUtil.halfExtents(spatial);
                result = new CylinderCollisionShape(halfExtents,
                        PhysicsSpace.AXIS_Y);
                break;

            case CylinderZ:
                halfExtents = MaudUtil.halfExtents(spatial);
                result = new CylinderCollisionShape(halfExtents,
                        PhysicsSpace.AXIS_Z);
                break;

            case Sphere:
                halfExtents = MaudUtil.halfExtents(spatial);
                radius = MyMath.max(halfExtents.x, halfExtents.y,
                        halfExtents.z);
                assert radius > 0f : radius;
                result = new SphereCollisionShape(radius);
                break;

            default:
                throw new IllegalArgumentException();
        }

        return result;
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
