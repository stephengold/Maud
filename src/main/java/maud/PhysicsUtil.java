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

import com.jme3.bounding.BoundingBox;
import com.jme3.bounding.BoundingSphere;
import com.jme3.bounding.BoundingVolume;
import com.jme3.bullet.PhysicsSpace;
import com.jme3.bullet.collision.PhysicsCollisionObject;
import com.jme3.bullet.collision.shapes.CapsuleCollisionShape;
import com.jme3.bullet.collision.shapes.CollisionShape;
import com.jme3.bullet.collision.shapes.CompoundCollisionShape;
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
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.logging.Logger;
import jme3utilities.Validate;

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

        int count;
        count = charas.size() + ghosts.size() + rigids.size() + vehics.size();

        assert count >= 0 : count;
        return count;
    }

    /**
     * Count the collision shapes in the specified physics space.
     *
     * @param space which physics space (not null, unaffected)
     * @return count (&ge;0)
     */
    public static int countShapes(PhysicsSpace space) {
        Validate.nonNull(space, "space");

        Map<Long, CollisionShape> map = shapeMap(space);
        int count = map.size();

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
     * Find the identified collision shape in the specified physics space.
     *
     * @param id identifier
     * @param space which physics space (not null, unaffected)
     * @return the pre-existing shape, or null if not found
     */
    public static CollisionShape findShape(Long id, PhysicsSpace space) {
        CollisionShape result = null;
        if (id != -1L) {
            Map<Long, CollisionShape> map = shapeMap(space);
            result = map.get(id);
        }

        return result;
    }

    /**
     * Enumerate all collision objects in the specified physics space.
     *
     * @param space which physics space (not null, unaffected)
     * @return a new set of objects
     */
    public static Set<PhysicsCollisionObject> listObjects(PhysicsSpace space) {
        Set<PhysicsCollisionObject> result = new HashSet<>(30);

        Collection<PhysicsCharacter> characters = space.getCharacterList();
        result.addAll(characters);

        Collection<PhysicsGhostObject> ghosts = space.getGhostObjectList();
        result.addAll(ghosts);

        Collection<PhysicsRigidBody> rigidBodies = space.getRigidBodyList();
        result.addAll(rigidBodies);

        Collection<PhysicsVehicle> vehicles = space.getVehicleList();
        result.addAll(vehicles);

        return result;
    }

    /**
     * Create a collision shape suitable for the specified spatial.
     *
     * @param spatial (not null)
     * @return a new instance
     */
    public static CollisionShape makeShape(Spatial spatial) {
        CollisionShape childShape;
        BoundingVolume bound = spatial.getWorldBound();
        if (bound instanceof BoundingBox) {
            BoundingBox boundingBox = (BoundingBox) bound;
            float xHalfExtent = boundingBox.getXExtent();
            float yHalfExtent = boundingBox.getYExtent();
            float zHalfExtent = boundingBox.getZExtent();
            // TODO consider other possible axes for the capsule
            float radius = Math.max(xHalfExtent, zHalfExtent);
            if (yHalfExtent > radius) {
                float height = 2 * (yHalfExtent - radius);
                childShape = new CapsuleCollisionShape(radius, height);
            } else {
                childShape = new SphereCollisionShape(yHalfExtent);
            }
        } else if (bound instanceof BoundingSphere) {
            BoundingSphere boundingSphere = (BoundingSphere) bound;
            float radius = boundingSphere.getRadius();
            childShape = new SphereCollisionShape(radius);
        } else {
            throw new IllegalStateException();
        }
        CompoundCollisionShape result = new CompoundCollisionShape();
        Vector3f location = bound.getCenter();
        Vector3f translation = spatial.getWorldTranslation();
        location.subtractLocal(translation);
        // TODO account for rotation
        result.addChildShape(childShape, location);

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
        Map<Long, CollisionShape> result = new TreeMap<>();

        Set<PhysicsCollisionObject> pcoSet = listObjects(space);
        for (PhysicsCollisionObject pco : pcoSet) {
            CollisionShape shape = pco.getCollisionShape();
            long id = shape.getObjectId();
            result.put(id, shape);
        }

        int numShapes = result.size();
        CollisionShape[] shapes = new CollisionShape[numShapes];
        result.values().toArray(shapes);
        for (CollisionShape shape : shapes) {
            if (shape instanceof CompoundCollisionShape) {
                CompoundCollisionShape ccs = (CompoundCollisionShape) shape;
                List<ChildCollisionShape> children = ccs.getChildren();
                for (ChildCollisionShape child : children) {
                    CollisionShape childShape = child.shape;
                    long id = childShape.getObjectId();
                    result.put(id, childShape);
                }
            }
        }

        return result;
    }
}
