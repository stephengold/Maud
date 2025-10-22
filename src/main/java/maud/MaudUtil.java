/*
 Copyright (c) 2017-2025 Stephen Gold
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

import com.jme3.anim.AnimClip;
import com.jme3.anim.Armature;
import com.jme3.anim.Joint;
import com.jme3.anim.MorphTrack;
import com.jme3.anim.TransformTrack;
import com.jme3.anim.util.HasLocalTransform;
import com.jme3.animation.Animation;
import com.jme3.animation.Bone;
import com.jme3.animation.BoneTrack;
import com.jme3.animation.Skeleton;
import com.jme3.animation.SpatialTrack;
import com.jme3.animation.Track;
import com.jme3.app.state.AppStateManager;
import com.jme3.collision.CollisionResult;
import com.jme3.collision.CollisionResults;
import com.jme3.environment.EnvironmentCamera;
import com.jme3.light.LightList;
import com.jme3.material.MatParamOverride;
import com.jme3.material.Material;
import com.jme3.math.ColorRGBA;
import com.jme3.math.Matrix3f;
import com.jme3.math.Matrix4f;
import com.jme3.math.Quaternion;
import com.jme3.math.Ray;
import com.jme3.math.Transform;
import com.jme3.math.Vector2f;
import com.jme3.math.Vector3f;
import com.jme3.math.Vector4f;
import com.jme3.renderer.queue.RenderQueue;
import com.jme3.scene.Geometry;
import com.jme3.scene.Mesh;
import com.jme3.scene.Node;
import com.jme3.scene.Spatial;
import com.jme3.scene.control.Control;
import com.jme3.shader.VarType;
import com.jme3.texture.Image;
import com.jme3.texture.Texture;
import com.jme3.texture.Texture2D;
import com.jme3.texture.Texture3D;
import com.jme3.texture.TextureCubeMap;
import com.jme3.texture.image.ColorSpace;
import com.jme3.util.BufferUtils;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.Writer;
import java.lang.reflect.Field;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import jme3utilities.Heart;
import jme3utilities.MyAnimation;
import jme3utilities.MyMesh;
import jme3utilities.MySkeleton;
import jme3utilities.MySpatial;
import jme3utilities.Validate;
import jme3utilities.math.MyArray;
import jme3utilities.math.MyMath;
import jme3utilities.math.MyQuaternion;
import jme3utilities.math.MyVector3f;
import jme3utilities.wes.TrackEdit;
import maud.model.cgm.LoadedAnimation;
import maud.model.option.RotationDisplayMode;

/**
 * Utility methods for the Maud application. All methods should be static.
 *
 * @author Stephen Gold sgold@sonic.net
 */
final public class MaudUtil {
    // *************************************************************************
    // constants and loggers

    /**
     * number of coordinate axes
     */
    final private static int numAxes = 3;
    /**
     * message logger for this class
     */
    final private static Logger logger
            = Logger.getLogger(MaudUtil.class.getName());
    /**
     * local copy of {@link com.jme3.math.Transform#IDENTITY}
     */
    final private static Transform transformIdentity = new Transform();
    // *************************************************************************
    // constructors

    /**
     * A private constructor to inhibit instantiation of this class.
     */
    private MaudUtil() {
    }
    // *************************************************************************
    // new methods exposed

    /**
     * Test whether there are any meshes in the specified subtree. Note:
     * recursive!
     *
     * @param subtree subtree to traverse (may be null, unaffected)
     * @return true if any found, otherwise false
     */
    public static boolean containsMeshes(Spatial subtree) {
        boolean result = false;

        if (subtree instanceof Geometry) {
            Mesh mesh = ((Geometry) subtree).getMesh();
            if (mesh != null) {
                result = true;
            }

        } else if (subtree instanceof Node) {
            result = false;
            Node node = (Node) subtree;
            List<Spatial> children = node.getChildren();
            for (Spatial child : children) {
                result = containsMeshes(child);
                if (result) {
                    break;
                }
            }
        }

        return result;
    }

    /**
     * Copy the specified Geometry, split the copy into subparts and attach the
     * parts to the parent of the original Geometry. (The original Geometry is
     * unaffected.)
     *
     * @param geometry the original Geometry (not null, not orphan, bare,
     * unaffected)
     * @param submeshes meshes created by partitioning the original Mesh (not
     * null, at least 2 meshes, aliases created)
     */
    public static void copyAndSplitGeometry(
            Geometry geometry, Mesh... submeshes) {
        Node parent = geometry.getParent();
        Validate.nonNull(parent, "geometry's parent");
        Validate.require(isBare(geometry), "geometry must be bare");
        int numSubmeshes = submeshes.length;
        Validate.inRange(
                numSubmeshes, "number of submeshes", 2, Integer.MAX_VALUE);

        String name = geometry.getName();
        for (int submeshI = 0; submeshI < numSubmeshes; ++submeshI) {
            boolean cloneMaterial = false;
            Geometry newGeometry = geometry.clone(cloneMaterial);
            parent.attachChild(newGeometry);

            Mesh submesh = submeshes[submeshI];
            newGeometry.setMesh(submesh);

            String newName = String.format("%s.split%03d", name, submeshI);
            newGeometry.setName(newName);
            assert isBare(newGeometry);
        }
    }

    /**
     * Count the children of the specified Bone or Joint.
     *
     * @param bone the subject Bone or Joint (not null, unaffected)
     * @return the number found (&ge;0)
     */
    public static int countBoneChildren(Object bone) {
        int result;
        if (bone instanceof Bone) {
            result = ((Bone) bone).getChildren().size();
        } else {
            result = ((Joint) bone).getChildren().size();
        }

        return result;
    }

    /**
     * Count the spatials of the specified type with the specified enum value in
     * the specified subtree of a scene graph. Note: recursive!
     *
     * @param <T> subclass of Spatial
     * @param subtree the root of the subtree to analyze (may be null,
     * unaffected)
     * @param spatialType the subclass of Spatial to search for (not null)
     * @param enumValue the enum value (batch hint, queue bucket, cull hint, or
     * shadow mode) to search for (may be null)
     * @return the number found (&ge;0)
     */
    public static <T extends Spatial> int countSpatials(
            Spatial subtree, Class<T> spatialType, Enum<?> enumValue) {
        int result = 0;

        if (subtree != null
                && spatialType.isAssignableFrom(subtree.getClass())) {

            if (enumValue instanceof Spatial.BatchHint
                    && subtree.getLocalBatchHint() == enumValue) {
                ++result;
            } else if (enumValue instanceof RenderQueue.Bucket
                    && subtree.getLocalQueueBucket() == enumValue) {
                ++result;
            } else if (enumValue instanceof Spatial.CullHint
                    && subtree.getLocalCullHint() == enumValue) {
                ++result;
            } else if (enumValue instanceof RenderQueue.ShadowMode
                    && subtree.getLocalShadowMode() == enumValue) {
                ++result;
            }
        }

        if (subtree instanceof Node) {
            Node node = (Node) subtree;
            List<Spatial> children = node.getChildren();
            for (Spatial child : children) {
                result += countSpatials(child, spatialType, enumValue);
            }
        }

        assert result >= 0 : result;
        return result;
    }

    /**
     * Count the spatials of the specified type with non-identity local
     * transforms. Note: recursive!
     *
     * @param <T> subclass of Spatial
     * @param subtree the root of the subtree to analyze (may be null,
     * unaffected)
     * @param spatialType the subclass of Spatial to search for (not null)
     * @return the number found (&ge;0)
     */
    public static <T extends Spatial> int countTransformSpatials(
            Spatial subtree, Class<T> spatialType) {
        int result = 0;

        if (subtree != null
                && spatialType.isAssignableFrom(subtree.getClass())) {

            Transform transform = subtree.getLocalTransform(); // alias
            if (!MyMath.isIdentity(transform)) {
                result = 1;
            }
        }

        if (subtree instanceof Node) {
            Node node = (Node) subtree;
            List<Spatial> children = node.getChildren();
            for (Spatial child : children) {
                result += countTransformSpatials(child, spatialType);
            }
        }

        assert result >= 0 : result;
        return result;
    }

    /**
     * Merge the specified geometries into a new Geometry.
     *
     * @param name the desired name for the resulting Geometry
     * @param geometries the geometries to merge (all non-null)
     * @return a new, orphan Geometry with the specified name
     */
    public static Geometry createMergedGeometry(
            String name, Geometry... geometries) {
        Validate.nonNullArray(geometries, "geometries");
        int numGeometries = geometries.length;
        Validate.require(numGeometries > 1, "at least 2 geometries");

        Geometry baseGeometry = geometries[0];
        assert isBare(baseGeometry);

        Mesh mesh = baseGeometry.getMesh();
        int maxNumWeights = mesh.getMaxNumWeights();
        for (int gIndex = 1; gIndex < numGeometries; ++gIndex) {
            Geometry addGeometry = geometries[gIndex];
            assert isBare(addGeometry);

            Mesh addMesh = addGeometry.getMesh();
            mesh = MyMesh.merge(mesh, addMesh);

            int addNumWeights = addMesh.getMaxNumWeights();
            maxNumWeights = Math.max(maxNumWeights, addNumWeights);
        }

        Geometry result = Heart.deepCopy(baseGeometry);
        result.setMesh(mesh);
        result.setName(name);

        return result;
    }

    /**
     * Generate an arbitrary non-null value for a material parameter.
     *
     * @param varType (not null)
     * @param parameterName name of the parameter (not null, not empty)
     * @return a new instance (not null)
     */
    public static Object defaultValue(VarType varType, String parameterName) {
        Validate.nonEmpty(parameterName, "parameter name");

        Object result;
        Image image;
        Texture texture;
        switch (varType) {
            case Boolean:
                result = false;
                break;
            case Float:
                result = 0f;
                break;
            case FloatArray:
                result = new float[]{0f};
                break;
            case Int:
                result = 1; // PreShadow.vert crashes if NumberOfBones < 1
                break;
            case IntArray:
                result = new int[]{0};
                break;
            case Matrix3:
                result = new Matrix3f();
                break;
            case Matrix3Array:
                result = new Matrix3f[]{new Matrix3f()};
                break;
            case Matrix4:
                result = new Matrix4f();
                break;
            case Matrix4Array:
                result = new Matrix4f[]{new Matrix4f()};
                break;
            case Texture2D:
                image = new Image();
                image.setFormat(Image.Format.BGRA8);
                texture = new Texture2D();
                texture.setImage(image);
                result = texture;
                break;
            case Texture3D:
                result = new Texture3D();
                break;
            case TextureCubeMap:
                ArrayList<ByteBuffer> data = new ArrayList<>(6);
                for (int i = 0; i < 6; i++) {
                    ByteBuffer buffer = BufferUtils.createByteBuffer(32);
                    data.add(buffer);
                }
                image = new Image(
                        Image.Format.BGRA8, 1, 1, 1, data, ColorSpace.Linear);
                texture = new TextureCubeMap();
                texture.setImage(image);
                result = texture;
                break;
            case Vector2:
                result = new Vector2f();
                break;
            case Vector2Array:
                result = new Vector2f[]{new Vector2f()};
                break;
            case Vector3:
                result = new Vector3f();
                break;
            case Vector3Array:
                result = new Vector3f[]{new Vector3f()};
                break;
            case Vector4:
                result = new Vector4f();
                break;
            case Vector4Array:
                result = new Vector4f[]{new Vector4f()};
                break;
            default:// TODO handle TextureArray, TextureBuffer
                logger.log(Level.SEVERE, "varType={0}", varType);
                throw new IllegalArgumentException(varType.toString());
        }

        return result;
    }

    /**
     * Test whether the indexed Bone or Joint descends from the indexed ancestor
     * in the specified Armature/Skeleton.
     *
     * @param boneIndex the index of the subject Bone or Joint (&ge;0)
     * @param ancestorIndex the index of the ancestor Bone or Joint (&ge;0)
     * @param sk the Armature or Skeleton to analyze (not null, unaffected)
     * @return true if the subject descends from the ancestor, otherwise false
     */
    public static boolean descendsFrom(
            int boneIndex, int ancestorIndex, Object sk) {
        Validate.nonNegative(boneIndex, "bone index");
        Validate.nonNegative(ancestorIndex, "ancestor index");

        if (sk instanceof Armature) {
            Joint joint = ((Armature) sk).getJoint(boneIndex);
            Joint ancestor = ((Armature) sk).getJoint(ancestorIndex);
            while (joint != null) {
                joint = joint.getParent();
                if (joint == ancestor) {
                    return true;
                }
            }

        } else {
            Bone joint = ((Skeleton) sk).getBone(boneIndex);
            Bone ancestor = ((Skeleton) sk).getBone(ancestorIndex);
            while (joint != null) {
                joint = joint.getParent();
                if (joint == ancestor) {
                    return true;
                }
            }
        }

        return false;
    }

    /**
     * Calculate the slider positions and status values to display the specified
     * rotation in the specified display mode.
     *
     * @param rotation (not null, possibly modified)
     * @param mode (not null)
     * @param storeValues (not null, length=3, modified)
     * @param storePositions (not null, length=3, modified)
     * @return unit suffix (not null)
     */
    public static String displayRotation(
            Quaternion rotation, RotationDisplayMode mode, float[] storeValues,
            float[] storePositions) {
        Validate.nonNull(rotation, "rotation");
        Validate.nonNull(storePositions, "slider positions");
        int numSliders = storePositions.length;
        Validate.inRange(numSliders, "number of sliders", numAxes, numAxes);
        Validate.nonNull(storeValues, "status values");
        int numStatuses = storeValues.length;
        Validate.inRange(numStatuses, "number of statuses", numAxes, numAxes);

        String unitSuffix;
        switch (mode) {
            case Degrees:
                rotation.toAngles(storePositions);
                for (int iAxis = 0; iAxis < numAxes; iAxis++) {
                    storeValues[iAxis]
                            = MyMath.toDegrees(storePositions[iAxis]);
                }
                unitSuffix = " deg";
                break;

            case QuatCoeff:
                if (rotation.getW() < 0f) {
                    rotation.negate();
                }
                storeValues[0] = rotation.getX();
                storeValues[1] = rotation.getY();
                storeValues[2] = rotation.getZ();
                storePositions[0] = 3.142f * storeValues[0];
                storePositions[1] = 3.142f * storeValues[1];
                storePositions[2] = 1.571f * storeValues[2];
                unitSuffix = "";
                break;

            case Radians:
                rotation.toAngles(storePositions);
                System.arraycopy(storePositions, 0, storeValues, 0, numAxes);
                unitSuffix = " rad";
                break;

            default:
                logger.log(Level.SEVERE, "mode={0}", mode);
                throw new IllegalArgumentException(
                        "invalid rotation display mode");
        }

        return unitSuffix;
    }

    /**
     * For the specified camera ray, find the nearest collision involving a
     * triangle facing the camera. Geometries with {@code CullHint.Always} are
     * ignored.
     *
     * @param subtree collidable subtree of the scene graph (not null,
     * unaffected)
     * @param ray camera ray (not null, unaffected)
     * @return collision result, or null of no collision with a triangle facing
     * the camera
     */
    public static CollisionResult findCollision(Spatial subtree, Ray ray) {
        Validate.nonNull(subtree, "subtree");
        Validate.nonNull(ray, "ray");

        MySpatial.prepareForCollide(subtree);
        CollisionResults results = new CollisionResults();
        subtree.collideWith(ray, results);
        /*
         * Collision results are sorted by increasing distance from the camera,
         * so the first result is also the nearest one.
         */
        for (int resultIndex = 0; resultIndex < results.size(); resultIndex++) {
            CollisionResult result = results.getCollision(resultIndex);
            Geometry geometry = result.getGeometry();
            Spatial.CullHint cull = geometry.getCullHint();
            if (cull == Spatial.CullHint.Always) {
                continue;
            }

            // Calculate the offset from the camera to the point of contact.
            Vector3f contactPoint = result.getContactPoint();
            Vector3f offset = contactPoint.subtract(ray.origin);
            /*
             * If the dot product of the normal with the offset is negative,
             * then the triangle faces the camera.
             */
            Vector3f normal = result.getContactNormal();
            float dotProduct = offset.dot(normal);
            if (dotProduct < 0f) {
                return result;
            }
        }

        return null;
    }

    /**
     * Find the index of the keyframe (if any) at the specified time in the
     * specified AnimTrack or Track.
     *
     * @param track the AnimTrack or Track to search (not null, unaffected)
     * @param time the track time (in seconds, &ge;0)
     * @return the keyframe's index (&ge;0) or -1 if no keyframe at that time
     */
    public static int findKeyframeIndex(Object track, float time) {
        Validate.nonNegative(time, "time");

        float[] times = getTrackTimes(track);
        int result = MyArray.findPreviousIndex(time, times);
        if (result >= 0 && times[result] != time) {
            result = -1;
        }

        return result;
    }

    /**
     * Find a named Material in the specified subtree of the scene graph. Note:
     * recursive!
     *
     * @param subtree where to search (not null, unaffected)
     * @param name (not null)
     * @return a pre-existing instance, or null if none
     */
    public static Material findMaterialNamed(Spatial subtree, String name) {
        Validate.nonNull(name, "name");

        Material result = null;

        if (subtree instanceof Geometry) {
            Geometry geometry = (Geometry) subtree;
            Material material = geometry.getMaterial();
            String matName = material.getName();
            if (name.equals(matName)) {
                result = material;
            }

        } else if (subtree instanceof Node) {
            Node node = (Node) subtree;
            List<Spatial> children = node.getChildren();
            for (Spatial child : children) {
                result = findMaterialNamed(child, name);
                if (result != null) {
                    break;
                }
            }
        }

        return result;
    }

    /**
     * Find the specified Spatial in the specified subtree and optionally store
     * its tree position.
     *
     * @param spatial spatial to find (not null, unaffected)
     * @param subtree which subtree to search (may be null, unaffected)
     * @param storePosition tree position of the spatial (modified if found and
     * not null)
     * @return true if found, otherwise false
     */
    public static boolean findPosition(Spatial spatial, Spatial subtree,
            List<Integer> storePosition) {
        Validate.nonNull(spatial, "spatial");

        boolean success = false;
        if (spatial == subtree) {
            success = true;
            if (storePosition != null) {
                storePosition.clear();
            }

        } else if (subtree instanceof Node) {
            Node subtreeNode = (Node) subtree;
            if (spatial.hasAncestor(subtreeNode)) {
                success = true;
                if (storePosition != null) {
                    storePosition.clear();

                    // Climb to the subtree's root, adding indices to the list.
                    Spatial climber = spatial;
                    while (climber != subtree) {
                        Node parent = climber.getParent();
                        int index = parent.getChildIndex(climber);
                        assert index >= 0 : index;
                        storePosition.add(index);
                        climber = parent;
                    }
                    Collections.reverse(storePosition);
                }
            }
        }

        return success;
    }

    /**
     * Find the index of the last keyframe at or before the specified time in
     * the specified track.
     *
     * @param track the input track (a MorphTrack, TransformTrack, or Track,
     * unaffected)
     * @param time the track time (in seconds, &ge;0)
     * @return the keyframe's index (&ge;0)
     */
    public static int findPreviousKeyframeIndex(Object track, float time) {
        Validate.nonNegative(time, "time");

        float[] times = getTrackTimes(track);
        int result = MyArray.findPreviousIndex(time, times);

        assert result >= 0 : result;
        return result;
    }

    /**
     * Find a spatial with the specified name in the specified subtree. Note:
     * recursive!
     * <p>
     * If the tree position is not needed, use {@link
     * jme3utilities.MySpatial#findNamed(com.jme3.scene.Spatial,
     * java.lang.String)} instead.
     *
     * @param name the name to search for (not null, not empty)
     * @param subtree which subtree to search (may be null, unaffected)
     * @param storePosition tree position of the spatial (modified if found and
     * not null)
     * @return the pre-existing spatial, or null if not found
     */
    public static Spatial findSpatialNamed(
            String name, Spatial subtree, List<Integer> storePosition) {
        Spatial result = null;
        if (subtree != null) {
            String spatialName = subtree.getName();
            if (spatialName != null && spatialName.equals(name)) {
                result = subtree;
                if (storePosition != null) {
                    storePosition.clear();
                }

            } else if (subtree instanceof Node) {
                Node node = (Node) subtree;
                List<Spatial> children = node.getChildren();
                int numChildren = children.size();
                for (int childI = 0; childI < numChildren; childI++) {
                    Spatial child = children.get(childI);
                    result = findSpatialNamed(name, child, storePosition);
                    if (result != null) {
                        if (storePosition != null) {
                            storePosition.add(0, childI);
                        }
                        break;
                    }
                }
            }
        }

        return result;
    }

    /**
     * Access the indexed SpatialTrack in the specified Animation.
     *
     * @param animation which animation to search (not null, alias created)
     * @param spatialTrackIndex which spatial track (&ge;0)
     * @return the pre-existing instance, or null if not found
     */
    public static SpatialTrack findSpatialTrack(
            Animation animation, int spatialTrackIndex) {
        Validate.nonNegative(spatialTrackIndex, "spatial track index");

        Track[] tracks = animation.getTracks();
        int spatialTracksSeen = 0;
        for (Track track : tracks) {
            if (track instanceof SpatialTrack) {
                SpatialTrack spatialTrack = (SpatialTrack) track;
                if (spatialTracksSeen == spatialTrackIndex) {
                    return spatialTrack;
                }
                ++spatialTracksSeen;
            }
        }

        return null;
    }

    /**
     * Access the attachments node (if any) for the indexed Bone or Joint.
     *
     * @param sk the Armature or Skeleton to analyze (not null, unaffected)
     * @param index the index of the subject Bone or Joint (&ge;0)
     * @return the pre-existing instance or null
     */
    public static Node getAttachments(Object sk, int index) {
        Object bone;
        if (sk instanceof Armature) {
            bone = ((Armature) sk).getJoint(index);
        } else {
            bone = ((Skeleton) sk).getBone(index);
        }
        Node result = getBoneAttachments(bone);

        return result;
    }

    /**
     * Access the attachments node (if any) for the specified Bone or Joint.
     *
     * @param bone the subject Bone or Joint (not null, unaffected)
     * @return the pre-existing instance or null
     */
    public static Node getBoneAttachments(Object bone) {
        Node result;
        if (bone instanceof Bone) {
            result = MySkeleton.getAttachments((Bone) bone);
        } else {
            result = MySkeleton.getAttachments((Joint) bone);
        }

        return result;
    }

    /**
     * Copy the keyframe rotations for the specified track.
     *
     * @param track the input track (a BoneTrack, SpatialTrack, or
     * TransformTrack)
     * @return a new array or null
     */
    public static Quaternion[] getTrackRotations(Object track) {
        Quaternion[] result;
        if (track instanceof BoneTrack) {
            result = ((BoneTrack) track).getRotations();
        } else if (track instanceof SpatialTrack) {
            result = ((SpatialTrack) track).getRotations();
        } else if (track instanceof TransformTrack) {
            result = ((TransformTrack) track).getRotations();
        } else {
            String className = track.getClass().getSimpleName();
            throw new IllegalArgumentException(className);
        }

        return result;
    }

    /**
     * Copy the keyframe scales for the specified track.
     *
     * @param track the input track (a BoneTrack, SpatialTrack, or
     * TransformTrack)
     * @return a new array or null
     */
    public static Vector3f[] getTrackScales(Object track) {
        Vector3f[] result;
        if (track instanceof BoneTrack) {
            result = ((BoneTrack) track).getScales();
        } else if (track instanceof SpatialTrack) {
            result = ((SpatialTrack) track).getScales();
        } else if (track instanceof TransformTrack) {
            result = ((TransformTrack) track).getScales();
        } else {
            String className = track.getClass().getSimpleName();
            throw new IllegalArgumentException(className);
        }

        return result;
    }

    /**
     * Access the time array of the specified track.
     *
     * @param object the input track (a MorphTrack, TransformTrack, or Track)
     * @return the pre-existing array (not null, length&gt;0)
     */
    public static float[] getTrackTimes(Object object) {
        float[] result;
        if (object instanceof MorphTrack) {
            result = ((MorphTrack) object).getTimes(); // alias
        } else if (object instanceof Track) {
            result = ((Track) object).getKeyFrameTimes(); // alias
        } else if (object instanceof TransformTrack) {
            result = ((TransformTrack) object).getTimes(); // alias
        } else {
            String className = object.getClass().getSimpleName();
            throw new IllegalArgumentException(className);
        }

        assert result != null;
        assert result.length > 0 : result.length;
        return result;
    }

    /**
     * Access the keyframe translations for the specified track.
     *
     * @param track the input track (a BoneTrack, SpatialTrack, or
     * TransformTrack)
     * @return the pre-existing instance or null
     */
    public static Vector3f[] getTrackTranslations(Object track) {
        Vector3f[] result;
        if (track instanceof BoneTrack) {
            result = ((BoneTrack) track).getTranslations();
        } else if (track instanceof SpatialTrack) {
            result = ((SpatialTrack) track).getTranslations();
        } else if (track instanceof TransformTrack) {
            result = ((TransformTrack) track).getTranslations();
        } else {
            String className = track.getClass().getSimpleName();
            throw new IllegalArgumentException(className);
        }

        return result;
    }

    /**
     * Calculate half extents for a symmetrical bounding box aligned with the
     * local axes of the specified scene-graph subtree.
     *
     * @param subtree (not null, unaffected)
     * @return a new vector
     */
    public static Vector3f halfExtents(Spatial subtree) {
        Spatial clone = subtree.clone(false);
        clone.setLocalTransform(transformIdentity);
        Vector3f[] minMax = MySpatial.findMinMaxCoords(clone);
        Vector3f max = minMax[1];
        Vector3f min = minMax[0];
        float heX = Math.max(Math.abs(max.x), Math.abs(min.x));
        float heY = Math.max(Math.abs(max.y), Math.abs(min.y));
        float heZ = Math.max(Math.abs(max.z), Math.abs(min.z));
        Vector3f result = new Vector3f(heX, heY, heZ);

        assert MyVector3f.isAllNonNegative(result);
        return result;
    }

    /**
     * Test whether there are any "extra" spatials in the specified subtree.
     * Note: recursive!
     *
     * @param subtree subtree to traverse (may be null, unaffected)
     * @param attachmentsNodes collection of attachments nodes (not null,
     * unaffected)
     * @return true if any found, otherwise false
     */
    public static boolean hasExtraSpatials(
            Spatial subtree, Collection<Spatial> attachmentsNodes) {
        Validate.nonNull(attachmentsNodes, "attachments nodes");

        boolean result;
        if (subtree != null && isExtra(subtree, attachmentsNodes)) {
            result = true;

        } else if (subtree instanceof Node) {
            result = false;
            Node node = (Node) subtree;
            List<Spatial> children = node.getChildren();
            for (Spatial child : children) {
                result = hasExtraSpatials(child, attachmentsNodes);
                if (result) {
                    break;
                }
            }

        } else {
            result = false;
        }

        return result;
    }

    /**
     * Test whether the specified AnimClip includes a TransformTrack for the
     * indexed Joint.
     *
     * @param clip the AnimClip to test (not null, unaffected)
     * @param jointIndex which Joint (&ge;0)
     * @return true if a track exists, otherwise false
     */
    public static boolean hasTrackForJoint(AnimClip clip, int jointIndex) {
        Validate.nonNegative(jointIndex, "joint index");

        TransformTrack track = MyAnimation.findJointTrack(clip, jointIndex);
        if (track == null) {
            return false;
        } else {
            return true;
        }
    }

    /**
     * Determine whether the specified Spatial is ready for splitting: free of
     * S-G controls, user data, local lights, local M-P overrides, and LODs.
     *
     * @param spatial the Spatial to test (not null, unaffected)
     * @return true if ready, otherwise false
     */
    public static boolean isBare(Spatial spatial) {
        int numSgcs = spatial.getNumControls();
        if (numSgcs != 0) {
            return false;
        }

        Collection<String> userDataKeys = spatial.getUserDataKeys();
        if (!userDataKeys.isEmpty()) {
            return false;
        }

        LightList lights = spatial.getLocalLightList();
        if (lights.size() != 0) {
            return false;
        }

        List<MatParamOverride> mpos = spatial.getLocalMatParamOverrides();
        if (!mpos.isEmpty()) {
            return false;
        }

        if (spatial instanceof Geometry) {
            Mesh mesh = ((Geometry) spatial).getMesh();
            int numLevels = mesh.getNumLodLevels();
            if (numLevels > 0) {
                return false;
            }
        }

        return true;
    }

    /**
     * Test whether the specified Spatial is an "extra" spatial.
     *
     * A spatial is "extra" iff neither it nor any of its descendants:
     * <ul>
     * <li> is an attachments node
     * <li> has a scene-graph control
     * <li> has user data, or
     * <li> has mesh vertices.
     * </ul>
     *
     * @param spatial spatial to test (not null, unaffected)
     * @param attachmentsNodes collection of attachments nodes (not null,
     * unaffected)
     * @return true if extra, otherwise false
     */
    public static boolean isExtra(
            Spatial spatial, Collection<Spatial> attachmentsNodes) {
        Validate.nonNull(spatial, "spatial");
        Validate.nonNull(attachmentsNodes, "attachments nodes");

        boolean hasAttachments
                = MySpatial.subtreeContainsAny(spatial, attachmentsNodes);
        int numSgcs = MySpatial.countControls(spatial, Control.class);
        int numUserData = MySpatial.countUserData(spatial);
        int numVertices = MySpatial.countVertices(spatial);

        boolean result;
        if (!hasAttachments && numSgcs == 0 && numUserData == 0
                && numVertices == 0) {
            result = true;
        } else {
            result = false;
        }

        return result;
    }

    /**
     * Test whether the specified animation name is reserved.
     *
     * @param name which name to test (not null)
     * @return true if reserved, otherwise false
     */
    public static boolean isReservedAnimationName(String name) {
        boolean result;
        if (name.isEmpty()) {
            result = true;
        } else if (name.equals(LoadedAnimation.bindPoseName)) {
            result = true;
        } else if (name.equals(LoadedAnimation.retargetedPoseName)) {
            result = true;
        } else {
            result = false;
        }

        return result;
    }

    /**
     * Create a new track.
     *
     * @param oldTrack to identify the track type and target bone/spatial (not
     * null, unaffected)
     * @param times (not null, alias created)
     * @param translations (either null or same length as times)
     * @param rotations (either null or same length as times)
     * @param scales (either null or same length as times)
     * @return a new track of the same type as oldTrack
     */
    public static Object newTrack(
            Object oldTrack, float[] times, Vector3f[] translations,
            Quaternion[] rotations, Vector3f[] scales) {
        int numKeyframes = times.length;
        assert numKeyframes > 0 : numKeyframes;
        assert translations == null || translations.length == numKeyframes;
        assert rotations == null || rotations.length == numKeyframes;
        assert scales == null || scales.length == numKeyframes;

        Object result;
        if (oldTrack instanceof BoneTrack) {
            int boneIndex = ((BoneTrack) oldTrack).getTargetBoneIndex();
            result = MyAnimation.newBoneTrack(
                    boneIndex, times, translations, rotations, scales);

        } else if (oldTrack instanceof SpatialTrack) {
            Spatial spatial = ((SpatialTrack) oldTrack).getTrackSpatial();
            SpatialTrack newSpatialTrack
                    = new SpatialTrack(times, translations, rotations, scales);
            newSpatialTrack.setTrackSpatial(spatial);
            result = newSpatialTrack;

        } else if (oldTrack instanceof TransformTrack) {
            TransformTrack transformTrack = (TransformTrack) oldTrack;
            HasLocalTransform target = transformTrack.getTarget();
            result = new TransformTrack(
                    target, times, translations, rotations, scales);

        } else {
            throw new IllegalArgumentException();
        }

        return result;
    }

    /**
     * Convert a JME image to an AWT image.
     *
     * @param imageIn the input Image (not null, 2-D, single buffer, limited
     * subset of formats, unaffected)
     * @param flipY true&rarr;flip the Y coordinate, false&rarr;don't flip
     * @param awtType the desired output format, such as
     * BufferedImage.TYPE_4BYTE_ABGR
     * @return a new BufferedImage
     */
    public static BufferedImage render(
            Image imageIn, boolean flipY, int awtType) {
        int depth = imageIn.getDepth();
        assert depth <= 1 : depth; // 3-D images aren't handled

        int numBuffers = imageIn.getData().size();
        assert numBuffers == 1 : numBuffers; // multiple buffers aren't handled

        Image.Format format = imageIn.getFormat();
        int bpp = format.getBitsPerPixel();
        assert (bpp % 8) == 0 : bpp; // pixels must be a whole number of bytes
        int bytesPerPixel = bpp / 8;

        int height = imageIn.getHeight();
        int width = imageIn.getWidth();
        int numBytes = height * width * bytesPerPixel;
        ByteBuffer byteBuffer = imageIn.getData(0);
        int capacity = byteBuffer.capacity();
        assert capacity >= numBytes : "capacity = " + capacity
                + ", numBytes = " + numBytes;

        BufferedImage result = new BufferedImage(width, height, awtType);
        int[] pixelBytes = new int[bytesPerPixel];
        Graphics2D graphics = result.createGraphics();
        int byteOffset = 0;
        for (int yIn = 0; yIn < height; ++yIn) {
            int yOut;
            if (flipY) {
                yOut = height - 1 - yIn;
            } else {
                yOut = yIn;
            }

            for (int x = 0; x < width; ++x) {
                for (int byteI = 0; byteI < bytesPerPixel; ++byteI) {
                    int pixelByte = byteBuffer.get(byteOffset);
                    pixelBytes[byteI] = 0xFF & pixelByte;
                    ++byteOffset;
                }
                Color color = pixelColor(format, pixelBytes);
                graphics.setColor(color);
                graphics.fillRect(x, yOut, 1, 1);
            }
        }

        return result;
    }

    /**
     * Alter the background color of an EnvironmentCamera. See JME issue #1447.
     *
     * @param bgColor the desired color (not null, unaffected)
     */
    public static void setEnvironmentCameraBackground(ColorRGBA bgColor) {
        Validate.nonNull(bgColor, "color");

        Field field;
        try {
            field = EnvironmentCamera.class.getDeclaredField("backGroundColor");
        } catch (NoSuchFieldException exception) {
            throw new RuntimeException();
        }
        field.setAccessible(true);

        AppStateManager stateManager = Maud.getApplication().getStateManager();
        EnvironmentCamera environmentCamera
                = stateManager.getState(EnvironmentCamera.class);
        try {
            ColorRGBA color = (ColorRGBA) field.get(environmentCamera);
            color.set(bgColor);
        } catch (IllegalAccessException exception) {
            throw new RuntimeException();
        }
    }

    /**
     * Copy the specified track, adjusting the animation time of the indexed
     * frame.
     *
     * @param track the input track (a MorphTrack, TransformTrack, or Track,
     * unaffected)
     * @param frameIndex the index of the frame to adjust (&gt;0)
     * @param newTime the desired time for the adjusted frame (in seconds,
     * &gt;0)
     * @return a new track of the same type, or null if unsuccessful
     */
    public static Object setFrameTime(
            Object track, int frameIndex, float newTime) {
        float[] oldTimes = getTrackTimes(track);
        int numFrames = oldTimes.length;
        Validate.inRange(frameIndex, "frame index", 1, numFrames - 1);
        Validate.positive(newTime, "new time");

        if (newTime <= oldTimes[frameIndex - 1]) {
            return null;
        }
        if (frameIndex < numFrames - 1) {
            if (newTime >= oldTimes[frameIndex + 1]) {
                return null;
            }
        }

        Object result = TrackEdit.cloneTrack(track);
        float[] newTimes = getTrackTimes(result); // an alias
        newTimes[frameIndex] = newTime;

        return result;
    }

    /**
     * Calculate the rotation specified by a bank of sliders when the rotation
     * display mode is QuatCoeff.
     *
     * @param sliderPositions (not null, length=3)
     * @param storeResult storage for the result (modified if not null)
     * @return rotation (either storeResult or a new quaternion)
     */
    public static Quaternion setFromSliders(
            float[] sliderPositions, Quaternion storeResult) {
        int numSliders = sliderPositions.length;
        Validate.inRange(numSliders, "numSliders", 3, 3);
        Quaternion result;
        if (storeResult == null) {
            result = new Quaternion();
        } else {
            result = storeResult;
        }

        double x = sliderPositions[0] / 3.142;
        double y = sliderPositions[1] / 3.142;
        double z = sliderPositions[2] / 1.571;
        double ssq = x * x + y * y + z * z;
        if (ssq > 1.0) {
            result.set((float) x, (float) y, (float) z, 0f);
            MyQuaternion.normalizeLocal(result);
        } else {
            double w = Math.sqrt(1.0 - ssq);
            result.set((float) x, (float) y, (float) z, (float) w);
        }

        return result;
    }

    /**
     * Write an editor action to the specified writer.
     *
     * @param writer (not null)
     * @param actionString (not null)
     * @throws java.io.IOException if an I/O error occurs while writing
     */
    public static void writePerformAction(Writer writer, String actionString)
            throws IOException {
        Validate.nonNull(actionString, "action string");

        writer.write("Maud.perform('");
        writer.write(actionString);
        writer.write("');\n");
    }
    // *************************************************************************
    // private methods

    /**
     * Determine the color of a single pixel in a JME image.
     *
     * @param format the image format (not null, limited subset)
     * @param bytes the pixel's ByteBuffer data (not null, unaffected)
     * @return a new Color
     */
    private static Color pixelColor(Image.Format format, int[] bytes) {
        float a; // alpha channel
        float b; // blue channel
        float g; // green channel
        float r; // red channel
        int numBytes = bytes.length;

        switch (format) {
            case ABGR8:
                assert numBytes == 4 : numBytes;
                a = bytes[0] / 255f;
                b = bytes[1] / 255f;
                g = bytes[2] / 255f;
                r = bytes[3] / 255f;
                break;

            case ARGB8:
                assert numBytes == 4 : numBytes;
                a = bytes[0] / 255f;
                r = bytes[1] / 255f;
                g = bytes[2] / 255f;
                b = bytes[3] / 255f;
                break;

            case Alpha8:
                assert numBytes == 1 : numBytes;
                a = bytes[0] / 255f;
                b = g = r = 1f;
                break;

            case BGR8:
                assert numBytes == 3 : numBytes;
                b = bytes[0] / 255f;
                g = bytes[1] / 255f;
                r = bytes[2] / 255f;
                a = 1f;
                break;

            case BGRA8:
                assert numBytes == 4 : numBytes;
                b = bytes[0] / 255f;
                g = bytes[1] / 255f;
                r = bytes[2] / 255f;
                a = bytes[3] / 255f;
                break;

            case Luminance8:
                assert numBytes == 1 : numBytes;
                b = g = r = bytes[0] / 255f;
                a = 1f;
                break;

            case Luminance8Alpha8:
                assert numBytes == 2 : numBytes;
                b = g = r = bytes[0] / 255f;
                a = bytes[1] / 255f;
                break;

            case RGB8:
                assert numBytes == 3 : numBytes;
                r = bytes[0] / 255f;
                g = bytes[1] / 255f;
                b = bytes[2] / 255f;
                a = 1f;
                break;

            case RGBA8:
                assert numBytes == 4 : numBytes;
                r = bytes[0] / 255f;
                g = bytes[1] / 255f;
                b = bytes[2] / 255f;
                a = bytes[3] / 255f;
                break;

            // TODO handle more formats
            default:
                String message = "format = " + format;
                throw new IllegalArgumentException(message);
        }

        Color result = new Color(r, g, b, a);
        return result;
    }
}
