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

import com.jme3.animation.AnimControl;
import com.jme3.animation.Animation;
import com.jme3.animation.Bone;
import com.jme3.animation.BoneTrack;
import com.jme3.animation.Skeleton;
import com.jme3.animation.SkeletonControl;
import com.jme3.animation.Track;
import com.jme3.asset.AssetLoadException;
import com.jme3.asset.AssetManager;
import com.jme3.asset.AssetNotFoundException;
import com.jme3.asset.ModelKey;
import com.jme3.bounding.BoundingBox;
import com.jme3.bounding.BoundingSphere;
import com.jme3.bounding.BoundingVolume;
import com.jme3.bullet.PhysicsSpace;
import com.jme3.bullet.collision.shapes.CapsuleCollisionShape;
import com.jme3.bullet.collision.shapes.CollisionShape;
import com.jme3.bullet.collision.shapes.CompoundCollisionShape;
import com.jme3.bullet.collision.shapes.CylinderCollisionShape;
import com.jme3.bullet.collision.shapes.SphereCollisionShape;
import com.jme3.bullet.collision.shapes.infos.ChildCollisionShape;
import com.jme3.bullet.control.PhysicsControl;
import com.jme3.bullet.joints.PhysicsJoint;
import com.jme3.bullet.objects.PhysicsCharacter;
import com.jme3.bullet.objects.PhysicsGhostObject;
import com.jme3.bullet.objects.PhysicsRigidBody;
import com.jme3.bullet.objects.PhysicsVehicle;
import com.jme3.math.FastMath;
import com.jme3.math.Quaternion;
import com.jme3.math.Transform;
import com.jme3.math.Vector3f;
import com.jme3.scene.Geometry;
import com.jme3.scene.Mesh;
import com.jme3.scene.Node;
import com.jme3.scene.Spatial;
import com.jme3.scene.control.Control;
import com.jme3.scene.plugins.blender.meshes.Face;
import com.jme3.scene.plugins.bvh.BVHAnimData;
import com.jme3.scene.plugins.bvh.BoneMapping;
import com.jme3.scene.plugins.bvh.SkeletonMapping;
import com.jme3.scene.plugins.ogre.MaterialLoader;
import com.jme3.scene.plugins.ogre.MeshLoader;
import java.io.PrintStream;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
import jme3utilities.MyAnimation;
import jme3utilities.MyControl;
import jme3utilities.MySkeleton;
import jme3utilities.MyString;
import jme3utilities.Validate;
import jme3utilities.math.MyMath;

/**
 * Utility methods for the Maud application. All methods should be static.
 *
 * @author Stephen Gold sgold@sonic.net
 */
public class Util {
    // *************************************************************************
    // constants and loggers

    /**
     * message logger for this class
     */
    final private static Logger logger = Logger.getLogger(Util.class.getName());
    /**
     * local copy of {@link com.jme3.math.Vector3f#UNIT_XYZ}
     */
    final private static Vector3f scaleIdentity = new Vector3f(1f, 1f, 1f);
    // *************************************************************************
    // fields

    /**
     * stream to use for dumping
     */
    static PrintStream stream;
    // *************************************************************************
    // constructors

    /**
     * A private constructor to inhibit instantiation of this class.
     */
    private Util() {
    }
    // *************************************************************************
    // new methods exposed

    /**
     * Copy a bone track, deleting everything before the specified time, and
     * making it the start of the animation.
     *
     * @param oldTrack (not null, unaffected)
     * @param neckTime cutoff time (&gt;0)
     * @return a new instance
     */
    public static BoneTrack behead(BoneTrack oldTrack, float neckTime) {
        Validate.positive(neckTime, "neck time");

        float[] oldTimes = oldTrack.getKeyFrameTimes();
        Vector3f[] oldTranslations = oldTrack.getTranslations();
        Quaternion[] oldRotations = oldTrack.getRotations();
        Vector3f[] oldScales = oldTrack.getScales();
        int oldCount = oldTimes.length;

        int neckIndex = findPreviousKeyframeIndex(oldTrack, neckTime);
        int newCount = oldCount - neckIndex;
        Vector3f[] translations = new Vector3f[newCount];
        Quaternion[] rotations = new Quaternion[newCount];
        Vector3f[] scales = null;
        if (oldScales != null) {
            scales = new Vector3f[newCount];
        }
        float[] times = new float[newCount];

        Transform user = interpolate(neckTime, oldTimes, oldTranslations,
                oldRotations, oldScales, null);
        translations[0] = user.getTranslation();
        rotations[0] = user.getRotation();
        if (scales != null) {
            scales[0] = user.getScale();
        }
        times[0] = 0f;
        for (int newIndex = 1; newIndex < newCount; newIndex++) {
            int oldIndex = newIndex + neckIndex;
            translations[newIndex] = oldTranslations[oldIndex].clone();
            rotations[newIndex] = oldRotations[oldIndex].clone();
            if (scales != null) {
                scales[newIndex] = oldScales[oldIndex].clone();
            }
            times[newIndex] = oldTimes[oldIndex] - neckTime;
        }

        int boneIndex = oldTrack.getTargetBoneIndex();
        BoneTrack result = new BoneTrack(boneIndex, times, translations,
                rotations, scales);

        return result;
    }

    /**
     * Calculate the bone transform for the specified track and time, using the
     * default techniques.
     *
     * @param track (not null, unaffected)
     * @param time animation time input
     * @param storeResult (modified if not null)
     * @return transform (either storeResult or a new instance)
     */
    public static Transform boneTransform(BoneTrack track, float time,
            Transform storeResult) {
        if (storeResult == null) {
            storeResult = new Transform();
        }
        float[] times = track.getTimes();
        int lastFrame = times.length - 1;
        assert lastFrame >= 0 : lastFrame;

        Vector3f[] translations = track.getTranslations();
        Quaternion[] rotations = track.getRotations();
        Vector3f[] scales = track.getScales();

        if (time <= 0f || lastFrame == 0) {
            /*
             * Copy the transform of the first frame.
             */
            storeResult.setTranslation(translations[0]);
            storeResult.setRotation(rotations[0]);
            if (scales == null) {
                storeResult.setScale(scaleIdentity);
            } else {
                storeResult.setScale(scales[0]);
            }

        } else if (time >= times[lastFrame]) {
            /*
             * Copy the transform of the last frame.
             */
            storeResult.setTranslation(translations[lastFrame]);
            storeResult.setRotation(rotations[lastFrame]);
            if (scales == null) {
                storeResult.setScale(scaleIdentity);
            } else {
                storeResult.setScale(scales[lastFrame]);
            }

        } else {
            /*
             * Interpolate between frames.
             */
            interpolate(time, times, translations, rotations, scales,
                    storeResult);
        }

        return storeResult;
    }

    /**
     * Find a cardinal quaternion similar to the specified input. A cardinal
     * quaternion is one for which the rotations angles on all three axes are
     * integer multiples of Pi/2 radians.
     *
     * @param input (not null, modified)
     */
    public static void cardinalizeLocal(Quaternion input) {
        Validate.nonNull(input, "input");

        MyMath.snapLocal(input, 0);
        MyMath.snapLocal(input, 1);
        MyMath.snapLocal(input, 2);
    }

    /**
     * Count the number of distinct vectors in an array, without distinguishing
     * 0 from -0.
     *
     * @param array input (not null, unaffected)
     * @return count (&ge;0)
     */
    public static int countNe(Vector3f[] array) {
        int length = array.length;
        Set<Vector3f> distinct = new HashSet<>(length);
        for (Vector3f vector : array) {
            Vector3f standard = Util.standardize(vector, null);
            distinct.add(standard);
        }
        int count = distinct.size();

        return count;
    }

    /**
     * Generate a textual description of a collision shape.
     *
     * @param shape (not null)
     * @return description (not null)
     */
    public static String describe(CollisionShape shape) {
        Validate.nonNull(shape, "shape");

        String name = shape.getClass().getSimpleName();
        if (name.endsWith("CollisionShape")) {
            name = MyString.removeSuffix(name, "CollisionShape");
        }

        String result = name;
        if (shape instanceof CapsuleCollisionShape) {
            CapsuleCollisionShape capsule = (CapsuleCollisionShape) shape;
            int axis = capsule.getAxis();
            result += describeAxis(axis);
            float height = capsule.getHeight();
            float radius = capsule.getRadius();
            result += String.format("[h=%f,r=%f]", height, radius);

        } else if (shape instanceof CompoundCollisionShape) {
            CompoundCollisionShape compound = (CompoundCollisionShape) shape;
            String desc = describeChildShapes(compound);
            result += String.format("[%s]", desc);

        } else if (shape instanceof CylinderCollisionShape) {
            CylinderCollisionShape cylinder = (CylinderCollisionShape) shape;
            int axis = cylinder.getAxis();
            result += describeAxis(axis);
            Vector3f halfExtents = cylinder.getHalfExtents();
            result += String.format("[hx=%f,hy=%f,hz=%f]",
                    halfExtents.x, halfExtents.y, halfExtents.z);

        } else if (shape instanceof SphereCollisionShape) {
            SphereCollisionShape sphere = (SphereCollisionShape) shape;
            float radius = sphere.getRadius();
            result += String.format("[r=%f]", radius);
        }

        return result;
    }

    /**
     * Generate a textual description of a Bullet axis.
     *
     * @param axis (0&rarr;X, 1&rarr;Y, 2&rarr;Z)
     * @return description (not null)
     */
    public static String describeAxis(int axis) {
        Validate.inRange(axis, "axis", 0, 2);

        String result;
        switch (axis) {
            case PhysicsSpace.AXIS_X:
                result = "X";
                break;
            case PhysicsSpace.AXIS_Y:
                result = "Y";
                break;
            case PhysicsSpace.AXIS_Z:
                result = "Z";
                break;
            default:
                throw new IllegalArgumentException();
        }

        return result;
    }

    /**
     * Generate a textual description of a compound shape's children.
     *
     * @param compound shape being described (not null)
     * @return description (not null)
     */
    public static String describeChildShapes(CompoundCollisionShape compound) {
        StringBuilder result = new StringBuilder(20);
        boolean addSeparators = false;
        List<ChildCollisionShape> children = compound.getChildren();
        int count = children.size();
        for (int i = 0; i < count; i++) {
            ChildCollisionShape child = children.get(i);
            if (addSeparators) {
                result.append("  ");
            } else {
                addSeparators = true;
            }
            String desc = describe(child.shape);
            result.append(desc);

            Vector3f location = child.location;
            desc = String.format("@[%.3f, %.3f, %.3f]",
                    location.x, location.y, location.z);
            result.append(desc);

            Quaternion rotation = new Quaternion();
            rotation.fromRotationMatrix(child.rotation);
            if (!rotation.isIdentity()) {
                result.append("rot");
                desc = rotation.toString();
                result.append(desc);
            }
        }

        return result.toString();
    }

    /**
     * Count how many vertices in the specified subtree of the scene graph are
     * directly influenced by the indexed bone. Note: recursive!
     *
     * @param subtree subtree to traverse (may be null)
     * @param boneIndex which bone (&ge;0)
     * @return count of vertices (&ge;0)
     */
    public static int directInfluence(Spatial subtree, int boneIndex) {
        Validate.nonNegative(boneIndex, "bone index");

        int result = 0;
        if (subtree instanceof Geometry) {
            Geometry geometry = (Geometry) subtree;
            Mesh mesh = geometry.getMesh();
            if (mesh.isAnimated()) {
                result = MySkeleton.numInfluenced(mesh, boneIndex);
            }

        } else if (subtree instanceof Node) {
            Node node = (Node) subtree;
            List<Spatial> children = node.getChildren();
            for (Spatial child : children) {
                result += directInfluence(child, boneIndex);
            }
        }

        return result;
    }

    /**
     * Disable all physics controls added to the specified subtree of the scene
     * graph. Disabling the contols removes any collision objects they may have
     * added to physics space. Note: recursive!
     *
     * @param subtree (not null)
     */
    public static void disablePhysicsControls(Spatial subtree) {
        int numControls = subtree.getNumControls();
        for (int controlI = 0; controlI < numControls; controlI++) {
            Control control = subtree.getControl(controlI);
            if (control instanceof PhysicsControl) {
                MyControl.setEnabled(control, false);
            }
        }
        if (subtree instanceof Node) {
            Node node = (Node) subtree;
            List<Spatial> children = node.getChildren();
            for (Spatial child : children) {
                disablePhysicsControls(child);
            }
        }
    }

    /**
     * Test whether the first N elements of the specified vector contain &gt;1
     * distinct values, without distinguishing 0 from -0.
     *
     * @param vector input (not null, unaffected)
     * @param n number of elements to consider
     * @return true if multiple values found, otherwise false
     */
    public static boolean distinct(float[] vector, int n) {
        Validate.nonNull(vector, "vector");
        Validate.inRange(n, "length", 0, vector.length);

        boolean result = false;
        if (n > 1) {
            float firstValue = vector[0];
            for (int i = 1; i < n; i++) {
                float value = vector[i];
                if (value != firstValue) {
                    result = true;
                    break;
                }
            }
        }

        return result;
    }

    /**
     * Find the root bone in the specified skeleton that has the greatest
     * influence on the meshes in the specified subtree.
     *
     * @param subtree subtree to traverse (may be null)
     * @param skeleton skeleton (not null)
     * @return the pre-existing instance, or null if no root bone found
     */
    public static Bone dominantRootBone(Spatial subtree, Skeleton skeleton) {
        Bone result = null;
        Bone[] roots = skeleton.getRoots();
        if (roots.length == 1) {
            result = roots[0];

        } else if (subtree != null) {
            int maxInfluenced = -1;
            for (Bone rootBone : roots) {
                int boneIndex = skeleton.getBoneIndex(rootBone);
                int numInfluenced;
                numInfluenced = Util.influence(subtree, skeleton, boneIndex);
                if (numInfluenced > maxInfluenced) {
                    maxInfluenced = numInfluenced;
                    result = rootBone;
                }
            }
        }

        return result;
    }

    /**
     * Dump the specified character.
     *
     * @param character the character to dump (not null)
     */
    public static void dump(PhysicsCharacter character) {
        long objectId = character.getObjectId();
        stream.printf("  character #%s ", Long.toHexString(objectId));

        Vector3f location = character.getPhysicsLocation();
        stream.printf("loc=[%.3f, %.3f, %.3f]",
                location.x, location.y, location.z);

        stream.println();

    }

    /**
     * Dump the specified ghost object.
     *
     * @param ghost the ghost object to dump (not null)
     */
    public static void dump(PhysicsGhostObject ghost) {
        long objectId = ghost.getObjectId();
        stream.printf("  ghost #%s ", Long.toHexString(objectId));

        Vector3f location = ghost.getPhysicsLocation();
        stream.printf("loc=[%.3f, %.3f, %.3f]",
                location.x, location.y, location.z);

        stream.println();
    }

    /**
     * Dump the specified joint.
     *
     * @param joint the joint to dump (not null)
     */
    public static void dump(PhysicsJoint joint) {
        long objectId = joint.getObjectId();
        long aId = joint.getBodyA().getObjectId();
        long bId = joint.getBodyB().getObjectId();
        stream.printf("  joint #%s a=%s,b=%s", Long.toHexString(objectId),
                Long.toHexString(aId), Long.toHexString(bId));

        stream.println();
    }

    /**
     * Dump the specified rigid body.
     *
     * @param body the rigid body to dump (not null)
     */
    public static void dump(PhysicsRigidBody body) {
        long objectId = body.getObjectId();
        float mass = body.getMass();
        stream.printf("  rigid body #%s mass=%f",
                Long.toHexString(objectId), mass);

        Vector3f location = body.getPhysicsLocation();
        stream.printf(" loc=[%.3f, %.3f, %.3f]",
                location.x, location.y, location.z);

        CollisionShape shape = body.getCollisionShape();
        String desc = describe(shape);
        stream.printf(" shape=%s", desc);

        Vector3f scale = shape.getScale();
        if (scale.x != 1f || scale.y != 1f || scale.z != 1f) {
            stream.printf(" sca=[%.3f, %.3f, %.3f]", scale.x, scale.y, scale.z);
        }

        stream.println();
    }

    /**
     * Dump the specified vehicle.
     *
     * @param vehicle the vehicle to dump (not null)
     */
    public static void dump(PhysicsVehicle vehicle) {
        long objectId = vehicle.getObjectId();
        float mass = vehicle.getMass();
        stream.printf("  vehicle #%s mass=%f", Long.toHexString(objectId),
                mass);

        Vector3f location = vehicle.getPhysicsLocation();
        stream.printf(" loc=[%.3f, %.3f, %.3f]",
                location.x, location.y, location.z);

        stream.println();
    }

    /**
     * Dump the specified physics space.
     *
     * @param space the physics space to dump (not null)
     */
    public static void dump(PhysicsSpace space) {
        Collection<PhysicsCharacter> characters = space.getCharacterList();
        Collection<PhysicsGhostObject> ghosts = space.getGhostObjectList();
        Collection<PhysicsJoint> joints = space.getJointList();
        Collection<PhysicsRigidBody> rigidBodies = space.getRigidBodyList();
        Collection<PhysicsVehicle> vehicles = space.getVehicleList();

        int numCharacters = characters.size();
        int numGhosts = ghosts.size();
        int numJoints = joints.size();
        int numBodies = rigidBodies.size();
        int numVehicles = vehicles.size();

        stream.printf("%nphysics space with %d character%s, %d ghost%s, ",
                numCharacters, (numCharacters == 1) ? "" : "s",
                numGhosts, (numGhosts == 1) ? "" : "s");
        stream.printf("%d joint%s, %d rigid bod%s, and %d vehicle%s%n",
                numJoints, (numJoints == 1) ? "" : "s",
                numBodies, (numBodies == 1) ? "y" : "ies",
                numJoints, (numVehicles == 1) ? "" : "s");

        for (PhysicsCharacter character : characters) {
            dump(character);
        }
        for (PhysicsGhostObject ghost : ghosts) {
            dump(ghost);
        }
        for (PhysicsJoint joint : joints) {
            dump(joint);
        }
        for (PhysicsRigidBody rigid : rigidBodies) {
            dump(rigid);
        }
        for (PhysicsVehicle vehicle : vehicles) {
            dump(vehicle);
        }
    }

    /**
     * Enable all physics controls added to the specified subtree of the scene
     * graph and configure their physics spaces. Note: recursive!
     *
     * @param subtree (not null)
     * @param space physics space to use, or null for none
     */
    public static void enablePhysicsControls(Spatial subtree,
            PhysicsSpace space) {
        int numControls = subtree.getNumControls();
        for (int controlI = 0; controlI < numControls; controlI++) {
            Control control = subtree.getControl(controlI);
            if (control instanceof PhysicsControl) {
                PhysicsControl pc = (PhysicsControl) control;
                pc.setPhysicsSpace(space);
                pc.setEnabled(true);
            }
        }
        if (subtree instanceof Node) {
            Node node = (Node) subtree;
            List<Spatial> children = node.getChildren();
            for (Spatial child : children) {
                enablePhysicsControls(child, space);
            }
        }
    }

    /**
     * Find the index of the keyframe at the specified time in the specified
     * track.
     *
     * @param track which track to search (not null, unaffected)
     * @param time track time (in seconds, &ge;0)
     * @return keyframe index (&ge;0) or -1 if keyframe not found
     */
    public static int findKeyframeIndex(Track track, float time) {
        Validate.nonNegative(time, "time");

        float[] times = track.getKeyFrameTimes();
        int result = findPreviousIndex(time, times);
        if (result >= 0 && times[result] != time) {
            result = -1;
        }

        return result;
    }

    /**
     * Find the index of the last value &le; the specified one in a sorted
     * array, using binary search.
     *
     * @param value value to search for
     * @param array array to search (not null, strictly monotonic increasing
     * order, unaffected)
     * @return array index (&ge;0) or -1 if array is empty or value&le;array[0]
     */
    public static int findPreviousIndex(float value, float[] array) {
        Validate.nonNull(array, "array");

        int lowerBound = -1;
        int upperBound = array.length - 1;
        int result;
        while (true) {
            if (upperBound == lowerBound) {
                result = lowerBound;
                break;
            }
            int testIndex = (lowerBound + upperBound + 1) / 2;
            float testValue = array[testIndex];
            if (value > testValue) {
                lowerBound = testIndex;
            } else if (value < testValue) {
                upperBound = testIndex - 1;
            } else if (value == testValue) {
                result = testIndex;
                break;
            }
        }

        assert result >= -1 : result;
        return result;
    }

    /**
     * Find the index of the keyframe at or before the specified time in the
     * specified track.
     *
     * @param track which track to search (not null, unaffected)
     * @param time track time (in seconds, &ge;0)
     * @return keyframe index (&ge;0)
     */
    public static int findPreviousKeyframeIndex(Track track, float time) {
        Validate.nonNegative(time, "time");

        float[] times = track.getKeyFrameTimes();
        int result = findPreviousIndex(time, times);

        assert result >= 0 : result;
        return result;
    }

    /**
     * Count how many vertices in the specified subtree of the scene graph are
     * influenced by the indexed bone and its descendents. Note: recursive!
     *
     * @param subtree subtree to traverse (may be null)
     * @param skeleton skeleton (not null)
     * @param boneIndex which bone in the skeleton (&ge;0)
     * @return count of vertices (&ge;0)
     */
    public static int influence(Spatial subtree, Skeleton skeleton,
            int boneIndex) {
        Validate.nonNull(skeleton, "skeleton");
        Validate.nonNegative(boneIndex, "bone index");

        int result = directInfluence(subtree, boneIndex);

        Bone bone = skeleton.getBone(boneIndex);
        List<Bone> children = bone.getChildren();
        for (Bone child : children) {
            int childIndex = skeleton.getBoneIndex(child);
            result += influence(subtree, skeleton, childIndex);
        }

        return result;
    }

    /**
     * Copy a bone track, inserting a keyframe at the specified time (which
     * mustn't already have a keyframe).
     *
     * @param oldTrack (not null, unaffected)
     * @param frameTime when to insert (&gt;0)
     * @param transform user transform to insert (not null, unaffected)
     * @return a new instance
     */
    public static BoneTrack insertKeyframe(BoneTrack oldTrack, float frameTime,
            Transform transform) {
        Validate.positive(frameTime, "keyframe time");
        assert findKeyframeIndex(oldTrack, frameTime) == -1;

        float[] oldTimes = oldTrack.getKeyFrameTimes();
        Vector3f[] oldTranslations = oldTrack.getTranslations();
        Quaternion[] oldRotations = oldTrack.getRotations();
        Vector3f[] oldScales = oldTrack.getScales();
        int oldCount = oldTimes.length;

        int newCount = oldCount + 1;
        Vector3f[] translations = new Vector3f[newCount];
        Quaternion[] rotations = new Quaternion[newCount];
        Vector3f[] scales = new Vector3f[newCount];
        float[] newTimes = new float[newCount];

        boolean added = false;
        for (int oldIndex = 0; oldIndex < oldCount; oldIndex++) {
            float time = oldTimes[oldIndex];
            int newIndex = oldIndex;
            if (time > frameTime) {
                if (!added) {
                    translations[newIndex] = transform.getTranslation().clone();
                    rotations[newIndex] = transform.getRotation().clone();
                    scales[newIndex] = transform.getScale().clone();
                    newTimes[newIndex] = frameTime;
                    added = true;
                }
                ++newIndex;
            }
            translations[newIndex] = oldTranslations[oldIndex].clone();
            rotations[newIndex] = oldRotations[oldIndex].clone();
            if (oldScales != null) {
                scales[newIndex] = oldScales[oldIndex].clone();
            } else {
                scales[newIndex] = new Vector3f(1f, 1f, 1f);
            }
            newTimes[newIndex] = oldTimes[oldIndex];
        }
        if (!added) {
            translations[oldCount] = transform.getTranslation().clone();
            rotations[oldCount] = transform.getRotation().clone();
            scales[oldCount] = transform.getScale().clone();
            newTimes[oldCount] = frameTime;
        }

        int boneIndex = oldTrack.getTargetBoneIndex();
        BoneTrack result = new BoneTrack(boneIndex, newTimes, translations,
                rotations, scales);

        return result;
    }

    /**
     * Interpolate between quaternions in a time sequence.
     *
     * @param time (in seconds, &gt;times[0], &lt;times[last])
     * @param times (not null, unaffected)
     * @param quaternions (not null, unaffected, same length as times)
     * @param storeResult (modified if not null)
     * @return transform (either storeResult or a new instance)
     */
    public static Quaternion interpolate(float time, float[] times,
            Quaternion[] quaternions, Quaternion storeResult) {
        assert time >= times[0] : time;
        assert time < times[times.length - 1] : time;
        assert times.length == quaternions.length;
        if (storeResult == null) {
            storeResult = new Quaternion();
        }

        int index1 = findPreviousIndex(time, times);
        int index2 = index1 + 1;
        float interval = times[index2] - times[index1];
        assert interval > 0f : interval;
        float fraction = (time - times[index1]) / interval;
        storeResult.set(quaternions[index1]);
        if (ne(storeResult, quaternions[index2])) {
            storeResult.nlerp(quaternions[index2], fraction);
        }

        return storeResult;
    }

    /**
     * Interpolate between vectors in a time sequence.
     *
     * @param time (in seconds, &gt;times[0], &lt;times[last])
     * @param times (not null, unaffected)
     * @param vectors (not null, unaffected, same length as times)
     * @param storeResult (modified if not null)
     * @return transform (either storeResult or a new instance)
     */
    public static Vector3f interpolate(float time, float[] times,
            Vector3f[] vectors, Vector3f storeResult) {
        assert time >= times[0] : time;
        assert time < times[times.length - 1] : time;
        assert times.length == vectors.length;
        if (storeResult == null) {
            storeResult = new Vector3f();
        }

        int index1 = findPreviousIndex(time, times);
        int index2 = index1 + 1;
        float interval = times[index2] - times[index1];
        assert interval > 0f : interval;
        float f2 = (time - times[index1]) / interval;
        float f1 = 1f - f2;
        storeResult.set(vectors[index1]);
        Vector3f v2 = vectors[index2];
        if (storeResult.x != v2.x) {
            storeResult.x = f1 * storeResult.x + f2 * v2.x;
        }
        if (storeResult.y != v2.y) {
            storeResult.y = f1 * storeResult.y + f2 * v2.y;
        }
        if (storeResult.z != v2.z) {
            storeResult.z = f1 * storeResult.z + f2 * v2.z;
        }

        return storeResult;
    }

    /**
     * Interpolate between keyframes in a bone track using the default
     * techniques.
     *
     * @param time (in seconds, &gt;times[0], &lt;times[last])
     * @param times (not null, unaffected)
     * @param translations (not null, unaffected, same length as times)
     * @param rotations (not null, unaffected, same length as times)
     * @param scales (may be null, unaffected, same length as times)
     * @param storeResult (modified if not null)
     * @return transform (either storeResult or a new instance)
     */
    public static Transform interpolate(float time, float[] times,
            Vector3f[] translations, Quaternion[] rotations, Vector3f[] scales,
            Transform storeResult) {
        if (storeResult == null) {
            storeResult = new Transform();
        }

        Util.interpolate(time, times, translations,
                storeResult.getTranslation());
        Util.interpolate(time, times, rotations, storeResult.getRotation());
        if (scales == null) {
            storeResult.setScale(scaleIdentity);
        } else {
            Util.interpolate(time, times, scales, storeResult.getScale());
        }

        return storeResult;
    }

    /**
     * Load a BVH asset as a CG model without logging any warning/error
     * messages.
     *
     * @param assetManager asset manager
     * @param assetPath path to BVH asset
     * @return a new orphan spatial, or null if unsuccessful
     */
    public static Spatial loadBvhAsset(AssetManager assetManager,
            String assetPath) {
        if (assetManager == null || assetPath == null) {
            return null;
        }

        BVHAnimData loadedData;
        try {
            loadedData = (BVHAnimData) assetManager.loadAsset(assetPath);
        } catch (AssetNotFoundException | NoSuchElementException e) {
            return null;
        }

        Skeleton skeleton = loadedData.getSkeleton();
        SkeletonControl skeletonControl = new SkeletonControl(skeleton);

        AnimControl animControl = new AnimControl(skeleton);
        Animation anim = loadedData.getAnimation();
        animControl.addAnim(anim);

        Spatial result = new Node(assetPath);
        result.addControl(animControl);
        result.addControl(skeletonControl);

        return result;
    }

    /**
     * Load a CG model asset without logging any warning/error messages.
     *
     * @param assetManager asset manager
     * @param assetPath path to CG model asset
     * @return a new orphan spatial, or null if unsuccessful
     */
    public static Spatial loadCgmAsset(AssetManager assetManager,
            String assetPath) {
        if (assetManager == null || assetPath == null) {
            return null;
        }

        ModelKey key = new ModelKey(assetPath);
        /*
         * Temporarily hush warnings about failures to triangulate,
         * vertices with >4 weights, shapes that can't be scaled, and
         * unsupported pass directives.
         */
        Logger faceLogger = Logger.getLogger(Face.class.getName());
        Level faceLevel = faceLogger.getLevel();
        faceLogger.setLevel(Level.SEVERE);

        Logger meshLoaderLogger = Logger.getLogger(MeshLoader.class.getName());
        Level meshLoaderLevel = meshLoaderLogger.getLevel();
        meshLoaderLogger.setLevel(Level.SEVERE);

        Logger materialLoaderLogger = Logger.getLogger(
                MaterialLoader.class.getName());
        Level materialLoaderLevel = materialLoaderLogger.getLevel();
        materialLoaderLogger.setLevel(Level.SEVERE);

        Logger compoundCollisionShapeLogger = Logger.getLogger(
                CompoundCollisionShape.class.getName());
        Level compoundCollisionShapeLevel;
        compoundCollisionShapeLevel = compoundCollisionShapeLogger.getLevel();
        compoundCollisionShapeLogger.setLevel(Level.SEVERE);
        /*
         * Load the model.
         */
        Spatial loaded;
        try {
            loaded = assetManager.loadModel(key);
        } catch (AssetNotFoundException | AssetLoadException exception) {
            loaded = null;
        }
        /*
         * Restore logging levels.
         */
        faceLogger.setLevel(faceLevel);
        meshLoaderLogger.setLevel(meshLoaderLevel);
        materialLoaderLogger.setLevel(materialLoaderLevel);
        compoundCollisionShapeLogger.setLevel(compoundCollisionShapeLevel);

        return loaded;
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
        result.addChildShape(childShape, location);

        return result;
    }

    /**
     * Test whether two quaternions are distinct, without distinguishing 0 from
     * -0.
     *
     * @param a 1st input quaternion (not null, unaffected)
     * @param b 2nd input quaternion (not null, unaffected)
     * @return true if distinct, otherwise false
     */
    public static boolean ne(Quaternion a, Quaternion b) {
        Validate.nonNull(a, "1st input quaternion");
        Validate.nonNull(b, "2nd input quaternion");

        boolean result = a.getW() != b.getW()
                || a.getX() != b.getX()
                || a.getY() != b.getY()
                || a.getZ() != b.getZ();
        return result;
    }

    /**
     * Find the SGC in the specified position among physics controls in the
     * specified spatial.
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
     * Calculate the position of the specified SGC among the physics controls in
     * the specified spatial.
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
     * Remove all non-physics controls from the specified subtree of the scene
     * graph. Note: recursive!
     *
     * @param subtree (not null)
     */
    public static void removeNonPhysicsControls(Spatial subtree) {
        int numControls = subtree.getNumControls();
        for (int controlI = numControls - 1; controlI >= 0; controlI--) {
            Control control = subtree.getControl(controlI);
            if (!(control instanceof PhysicsControl)) {
                subtree.removeControl(control);
            }
        }
        if (subtree instanceof Node) {
            Node node = (Node) subtree;
            List<Spatial> children = node.getChildren();
            for (Spatial child : children) {
                removeNonPhysicsControls(child);
            }
        }
    }

    /**
     * Re-target the specified animation from the specified source skeleton to
     * the specified target skeleton using the specified mapping.
     *
     * @param sourceAnimation which animation to re-target (not null,
     * unaffected)
     * @param sourceSkeleton (not null, unaffected)
     * @param targetSkeleton (not null, unaffected)
     * @param mapping which skeleton mapping to use (not null, unaffected)
     * @param animationName name for the resulting animation (not null)
     * @return a new animation
     */
    public static Animation retargetAnimation(Animation sourceAnimation,
            Skeleton sourceSkeleton, Skeleton targetSkeleton,
            SkeletonMapping mapping, String animationName) {
        Validate.nonNull(sourceAnimation, "source animation");
        Validate.nonNull(sourceSkeleton, "source skeleton");
        Validate.nonNull(targetSkeleton, "target skeleton");
        Validate.nonNull(mapping, "mapping");
        Validate.nonNull(animationName, "animation name");
        /*
         * Start with an empty animation.
         */
        float duration = sourceAnimation.getLength();
        Animation result = new Animation(animationName, duration);
        /*
         * Add a bone track for each target bone that's mapped.
         */
        int numTargetBones = targetSkeleton.getBoneCount();
        for (int iTarget = 0; iTarget < numTargetBones; iTarget++) {
            Bone targetBone = targetSkeleton.getBone(iTarget);
            String targetName = targetBone.getName();
            BoneMapping boneMapping = mapping.get(targetName);
            if (boneMapping != null) {
                String sourceName = boneMapping.getSourceName();
                int iSource = sourceSkeleton.getBoneIndex(sourceName);
                BoneTrack sourceTrack;
                sourceTrack = MyAnimation.findTrack(sourceAnimation, iSource);
                BoneTrack track = retargetTrack(sourceAnimation, sourceTrack,
                        sourceSkeleton, targetSkeleton, mapping, iTarget);
                result.addTrack(track);
            }
        }

        return result;
    }

    /**
     * Re-target the specified bone track from the specified source skeleton to
     * the specified target skeleton using the specified mapping.
     *
     * @param sourceAnimation the animation to re-target, or null for bind pose
     * @param sourceSkeleton (not null, unaffected)
     * @param sourceTrack input bone track (not null, unaffected)
     * @param targetSkeleton (not null, unaffected)
     * @param mapping which skeleton mapping to use (not null, unaffected)
     * @param targetBoneIndex index of the target bone (&ge;0)
     * @return a new bone track
     */
    public static BoneTrack retargetTrack(Animation sourceAnimation,
            BoneTrack sourceTrack, Skeleton sourceSkeleton,
            Skeleton targetSkeleton, SkeletonMapping mapping,
            int targetBoneIndex) {
        Validate.nonNull(sourceSkeleton, "source skeleton");
        Validate.nonNull(targetSkeleton, "target skeleton");
        Validate.nonNull(mapping, "mapping");
        Validate.nonNegative(targetBoneIndex, "target bone index");

        float[] times;
        int numKeyframes;
        if (sourceTrack == null) {
            numKeyframes = 1;
            times = new float[numKeyframes];
            times[0] = 0f;
        } else {
            times = sourceTrack.getTimes();
            numKeyframes = times.length;
        }
        Vector3f[] translations = new Vector3f[numKeyframes];
        Quaternion[] rotations = new Quaternion[numKeyframes];
        Vector3f[] scales = new Vector3f[numKeyframes];
        Pose sourcePose = new Pose(sourceSkeleton);
        Pose targetPose = new Pose(targetSkeleton);

        for (int frameIndex = 0; frameIndex < numKeyframes; frameIndex++) {
            float trackTime = times[frameIndex];
            sourcePose.setToAnimation(sourceAnimation, trackTime);
            targetPose.setToRetarget(sourcePose, mapping);

            Transform userTransform;
            userTransform = targetPose.userTransform(targetBoneIndex, null);
            translations[frameIndex] = userTransform.getTranslation();
            rotations[frameIndex] = userTransform.getRotation();
            scales[frameIndex] = userTransform.getScale();
        }

        BoneTrack result = new BoneTrack(targetBoneIndex, times, translations,
                rotations, scales);

        return result;
    }

    /**
     * Standardize a single-precision floating-point value in preparation for
     * hashing.
     *
     * @param input input value
     * @return an equivalent value that's not -0
     */
    public static float standardize(float input) {
        float result = input;
        if (Float.compare(input, -0f) == 0) {
            result = 0f;
        }

        return result;
    }

    /**
     * Standardize a quaternion in preparation for hashing.
     *
     * @param input (not null, unaffected)
     * @param storeResult (modified if not null)
     * @return an equivalent quaternion without negative zeroes (either
     * storeResult or a new instance)
     */
    public static Quaternion standardize(Quaternion input,
            Quaternion storeResult) {
        Validate.nonNull(input, "input quaternion");
        if (storeResult == null) {
            storeResult = new Quaternion();
        }

        float w = input.getW();
        float x = input.getX();
        float y = input.getY();
        float z = input.getZ();
        w = standardize(w);
        x = standardize(x);
        y = standardize(y);
        z = standardize(z);
        storeResult.set(x, y, z, w);

        return storeResult;
    }

    /**
     * Standardize a vector in preparation for hashing.
     *
     * @param input (not null, unaffected)
     * @param storeResult (modified if not null)
     * @return an equivalent vector without negative zeroes (either storeResult
     * or a new instance)
     */
    public static Vector3f standardize(Vector3f input, Vector3f storeResult) {
        Validate.nonNull(input, "input vector");
        if (storeResult == null) {
            storeResult = new Vector3f();
        }

        storeResult.x = standardize(input.x);
        storeResult.y = standardize(input.y);
        storeResult.z = standardize(input.z);

        return storeResult;
    }

    /**
     * Copy a bone track, truncating it at the specified time.
     *
     * @param oldTrack (not null, unaffected)
     * @param endTime cutoff time (&ge;0)
     * @return a new instance
     */
    public static BoneTrack truncate(BoneTrack oldTrack, float endTime) {
        Validate.positive(endTime, "end time");

        float[] oldTimes = oldTrack.getKeyFrameTimes();
        Vector3f[] oldTranslations = oldTrack.getTranslations();
        Quaternion[] oldRotations = oldTrack.getRotations();
        Vector3f[] oldScales = oldTrack.getScales();

        int newCount = 1 + findPreviousKeyframeIndex(oldTrack, endTime);
        Vector3f[] translations = new Vector3f[newCount];
        Quaternion[] rotations = new Quaternion[newCount];
        Vector3f[] scales = null;
        if (oldScales != null) {
            scales = new Vector3f[newCount];
        }
        float[] times = new float[newCount];

        for (int frameIndex = 0; frameIndex < newCount; frameIndex++) {
            translations[frameIndex] = oldTranslations[frameIndex].clone();
            rotations[frameIndex] = oldRotations[frameIndex].clone();
            if (oldScales != null) {
                scales[frameIndex] = oldScales[frameIndex].clone();
            }
            times[frameIndex] = oldTimes[frameIndex];
        }

        int boneIndex = oldTrack.getTargetBoneIndex();
        BoneTrack result = MyAnimation.newBoneTrack(boneIndex, times,
                translations, rotations, scales);

        return result;
    }

    /**
     * Copy a bone track, altering its duration and adjusting all its keyframes
     * proportionately.
     *
     * @param oldTrack (not null, unaffected)
     * @param newDuration new duration (in seconds, &ge;0)
     * @return a new instance
     */
    public static BoneTrack setDuration(BoneTrack oldTrack, float newDuration) {
        Validate.nonNegative(newDuration, "duration");

        BoneTrack result = oldTrack.clone();
        float[] newTimes = result.getKeyFrameTimes();

        float oldDuration = oldTrack.getLength();
        float[] oldTimes = oldTrack.getKeyFrameTimes();
        int numFrames = oldTimes.length;
        assert numFrames == 1 || oldDuration > 0f : numFrames;

        for (int frameIndex = 0; frameIndex < numFrames; frameIndex++) {
            float oldTime = oldTimes[frameIndex];
            assert oldTime <= oldDuration : oldTime;

            float newTime;
            if (oldDuration == 0f) {
                assert frameIndex == 0 : frameIndex;
                assert oldTime == 0f : oldTime;
                newTime = 0f;
            } else {
                newTime = newDuration * oldTime / oldDuration;
                newTime = FastMath.clamp(newTime, 0f, newDuration);
            }
            newTimes[frameIndex] = newTime;
        }

        return result;
    }

    /**
     * Copy a bone track, altering its end-time keyframe to match its 1st
     * keyframe. If the track doesn't end with a keyframe, append one.
     *
     * @param oldTrack (not null, unaffected)
     * @param endTime when to insert (&gt;0)
     * @return a new instance
     */
    public static BoneTrack wrap(BoneTrack oldTrack, float endTime) {
        Validate.positive(endTime, "end time");

        float[] oldTimes = oldTrack.getKeyFrameTimes();
        Vector3f[] oldTranslations = oldTrack.getTranslations();
        Quaternion[] oldRotations = oldTrack.getRotations();
        Vector3f[] oldScales = oldTrack.getScales();
        int oldCount = oldTimes.length;

        int newCount;
        int endIndex = findKeyframeIndex(oldTrack, endTime);
        if (endIndex == -1) {
            endIndex = oldCount;
            newCount = oldCount + 1;
        } else {
            newCount = oldCount;
        }
        assert endIndex == newCount - 1;
        Vector3f[] translations = new Vector3f[newCount];
        Quaternion[] rotations = new Quaternion[newCount];
        Vector3f[] scales = null;
        if (oldScales != null) {
            scales = new Vector3f[newCount];
        }
        float[] newTimes = new float[newCount];

        for (int frameIndex = 0; frameIndex < endIndex; frameIndex++) {
            translations[frameIndex] = oldTranslations[frameIndex].clone();
            rotations[frameIndex] = oldRotations[frameIndex].clone();
            if (oldScales != null) {
                scales[frameIndex] = oldScales[frameIndex].clone();
            }
            newTimes[frameIndex] = oldTimes[frameIndex];
        }

        translations[endIndex] = oldTranslations[0].clone();
        rotations[endIndex] = oldRotations[0].clone();
        if (oldScales != null) {
            scales[endIndex] = oldScales[0].clone();
        }
        newTimes[endIndex] = endTime;

        int boneIndex = oldTrack.getTargetBoneIndex();
        BoneTrack result = MyAnimation.newBoneTrack(boneIndex, newTimes,
                translations, rotations, scales);

        return result;
    }
}
