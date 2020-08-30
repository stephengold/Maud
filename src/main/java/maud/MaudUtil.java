/*
 Copyright (c) 2017-2020, Stephen Gold
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
import com.jme3.anim.AnimComposer;
import com.jme3.anim.AnimTrack;
import com.jme3.anim.Armature;
import com.jme3.anim.Joint;
import com.jme3.anim.MorphTrack;
import com.jme3.anim.SkinningControl;
import com.jme3.anim.TransformTrack;
import com.jme3.anim.util.HasLocalTransform;
import com.jme3.animation.Animation;
import com.jme3.animation.AudioTrack;
import com.jme3.animation.Bone;
import com.jme3.animation.BoneTrack;
import com.jme3.animation.EffectTrack;
import com.jme3.animation.Skeleton;
import com.jme3.animation.SpatialTrack;
import com.jme3.animation.Track;
import com.jme3.collision.CollisionResult;
import com.jme3.collision.CollisionResults;
import com.jme3.material.Material;
import com.jme3.math.Matrix3f;
import com.jme3.math.Matrix4f;
import com.jme3.math.Quaternion;
import com.jme3.math.Ray;
import com.jme3.math.Transform;
import com.jme3.math.Vector2f;
import com.jme3.math.Vector3f;
import com.jme3.math.Vector4f;
import com.jme3.scene.Geometry;
import com.jme3.scene.Mesh;
import com.jme3.scene.Node;
import com.jme3.scene.Spatial;
import com.jme3.scene.VertexBuffer;
import com.jme3.scene.control.Control;
import com.jme3.shader.VarType;
import com.jme3.texture.Image;
import com.jme3.texture.Texture;
import com.jme3.texture.Texture2D;
import com.jme3.texture.Texture3D;
import com.jme3.texture.TextureCubeMap;
import com.jme3.texture.image.ColorSpace;
import com.jme3.util.BufferUtils;
import com.jme3.util.clone.Cloner;
import java.io.IOException;
import java.io.Writer;
import java.nio.ByteBuffer;
import java.nio.FloatBuffer;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import jme3utilities.MyAnimation;
import jme3utilities.MyMesh;
import jme3utilities.MySkeleton;
import jme3utilities.MySpatial;
import jme3utilities.MyString;
import jme3utilities.Validate;
import jme3utilities.math.MyArray;
import jme3utilities.math.MyMath;
import jme3utilities.math.MyQuaternion;
import jme3utilities.math.MyVector3f;
import jme3utilities.wes.SmoothRotations;
import jme3utilities.wes.SmoothVectors;
import maud.model.cgm.LoadedAnimation;
import maud.model.option.RotationDisplayMode;

/**
 * Utility methods for the Maud application. All methods should be static.
 *
 * @author Stephen Gold sgold@sonic.net
 */
public class MaudUtil {
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
     * Clone the specified track without cloning its target. TODO move to
     * TrackEdit
     *
     * @param track an AnimTrack or Track (not null)
     * @return a new AnimTrack or Track
     */
    public static Object cloneTrack(Object track) {
        Object result;

        if (track instanceof MorphTrack) {
            Cloner cloner = new Cloner();
            Geometry target = ((MorphTrack) track).getTarget();
            if (target != null) {
                cloner.setClonedValue(target, target);
            }
            result = cloner.clone(track);

        } else if (track instanceof Track) {
            result = ((Track) track).clone();

        } else if (track instanceof TransformTrack) {
            Cloner cloner = new Cloner();
            HasLocalTransform target = ((TransformTrack) track).getTarget();
            if (target != null) {
                cloner.setClonedValue(target, target);
            }
            result = cloner.clone(track);

        } else {
            String className = track.getClass().getSimpleName();
            throw new IllegalArgumentException(className);
        }

        return result;
    }

    /**
     * Count the children of the specified Bone or Joint.
     *
     * @param bone the subject Bone or Joint (not null, unaffected)
     * @return the count (&ge;0)
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
                image = new Image(Image.Format.BGRA8, 1, 1, 1, data,
                        ColorSpace.Linear);
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

        assert result != null;
        return result;
    }

    /**
     * Copy a TransformTrack, deleting the indexed range of keyframes (which
     * mustn't include the first keyframe). TODO move to TrackEdit
     *
     * @param oldTrack input TransformTrack (not null, unaffected)
     * @param startIndex the index of the first keyframe to delete (&gt;0,
     * &le;lastIndex)
     * @param deleteCount number of keyframes to delete (&gt;0, &lt;lastIndex)
     * @return a new TransformTrack
     */
    public static TransformTrack deleteRange(TransformTrack oldTrack,
            int startIndex, int deleteCount) {
        float[] oldTimes = getTrackTimes(oldTrack);
        int oldCount = oldTimes.length;
        assert oldCount > 0 : oldCount;
        int lastIndex = oldCount - 1;
        Validate.inRange(startIndex, "start index", 1, lastIndex);
        Validate.inRange(deleteCount, "delete count", 1, lastIndex);
        float endIndex = startIndex + deleteCount - 1;
        Validate.inRange(endIndex, "end index", 1, lastIndex);

        Vector3f[] oldTranslations = oldTrack.getTranslations();
        Quaternion[] oldRotations = oldTrack.getRotations();
        Vector3f[] oldScales = oldTrack.getScales();

        int newCount = oldCount - deleteCount;
        /*
         * Allocate new arrays.
         */
        float[] newTimes = new float[newCount];
        Vector3f[] newTranslations = null;
        if (oldTranslations != null) {
            newTranslations = new Vector3f[newCount];
        }
        Quaternion[] newRotations = null;
        if (oldRotations != null) {
            newRotations = new Quaternion[newCount];
        }
        Vector3f[] newScales = null;
        if (oldScales != null) {
            newScales = new Vector3f[newCount];
        }

        for (int newIndex = 0; newIndex < newCount; ++newIndex) {
            int oldIndex;
            if (newIndex < startIndex) {
                oldIndex = newIndex;
            } else {
                oldIndex = newIndex + deleteCount;
            }

            newTimes[newIndex] = oldTimes[oldIndex];
            if (newTranslations != null) {
                newTranslations[newIndex] = oldTranslations[oldIndex].clone();
            }
            if (newRotations != null) {
                newRotations[newIndex] = oldRotations[oldIndex].clone();
            }
            if (newScales != null) {
                newScales[newIndex] = oldScales[oldIndex].clone();
            }
        }

        HasLocalTransform target = oldTrack.getTarget();
        TransformTrack result = new TransformTrack(target, newTimes,
                newTranslations, newRotations, newScales);

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
    public static boolean descendsFrom(int boneIndex, int ancestorIndex,
            Object sk) {
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
     * Describe an AnimClip. TODO move to MyAnimation
     *
     * @param clip the AnimClip to describe (not null, unaffected)
     * @param composer the Control that contains the clip (not null, unaffected)
     * @return textual description (not null, not empty)
     */
    public static String describe(AnimClip clip, AnimComposer composer) {
        Validate.nonNull(composer, "composer");

        String name = clip.getName();
        AnimTrack[] tracks = clip.getTracks();

        String result;
        int numTracks = tracks.length;
        if (numTracks > 2) {
            result = String.format("%s[%d]", MyString.quote(name), numTracks);
        } else {
            String[] trackDescriptions = new String[numTracks];
            for (int trackIndex = 0; trackIndex < numTracks; ++trackIndex) {
                AnimTrack track = tracks[trackIndex];
                trackDescriptions[trackIndex] = describe(track, composer);
            }
            String joined = MyString.join(trackDescriptions);
            result = String.format("%s(%s)", name, joined);
        }

        return result;
    }

    /**
     * Describe an animation track in the context of its AnimClip. TODO move to
     * MyAnimation
     *
     * @param track the AnimTrack to describe (not null, unaffected)
     * @param composer an AnimComposer that contains the AnimTrack (not null,
     * unaffected)
     * @return a textual description (not null, not empty)
     */
    public static String describe(AnimTrack track, AnimComposer composer) {
        Validate.nonNull(track, "track");
        Validate.nonNull(composer, "composer");

        StringBuilder builder = new StringBuilder(20);

        char typeChar = describeTrackType(track);
        builder.append(typeChar);

        if (track instanceof MorphTrack) {
            Geometry target = ((MorphTrack) track).getTarget();
            builder.append(target.getClass().getSimpleName());
            String targetName = target.getName();
            builder.append(MyString.quote(targetName));

        } else if (track instanceof TransformTrack) {
            TransformTrack transformTrack = (TransformTrack) track;
            HasLocalTransform target = transformTrack.getTarget();
            builder.append(target.getClass().getSimpleName());

            String targetName;
            if (target instanceof Joint) {
                targetName = ((Joint) target).getName();
                builder.append(MyString.quote(targetName));
            } else if (target instanceof Spatial) {
                targetName = ((Spatial) target).getName();
                builder.append(MyString.quote(targetName));
            }

            builder.append("T");
            builder.append("R");
            if (transformTrack.getScales() != null) {
                builder.append("S");
            }
        }

        String result = builder.toString();
        return result;
    }

    /**
     * Describe a track's type with a single character. TODO merge to
     * MyAnimation
     *
     * @param track the track to describe (may be null, unaffected)
     * @return a mnemonic character
     */
    public static char describeTrackType(Object track) {
        if (track instanceof AudioTrack) {
            return 'a';
        } else if (track instanceof BoneTrack) {
            return 'b';
        } else if (track instanceof EffectTrack) {
            return 'e';
        } else if (track instanceof MorphTrack) {
            return 'm';
        } else if (track instanceof SpatialTrack) {
            return 's';
        } else if (track instanceof TransformTrack) {
            return 't';
        }
        return '?';
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
    public static String displayRotation(Quaternion rotation,
            RotationDisplayMode mode, float[] storeValues,
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
     * triangle facing the camera.
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
            /*
             * Calculate the offset from the camera to the point of contact.
             */
            CollisionResult result = results.getCollision(resultIndex);
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
     * Find a TransformTrack in a specified AnimClip that targets the indexed
     * Joint.
     *
     * @param clip which AnimClip (not null, unaffected)
     * @param jointIndex which Joint (&ge;0)
     * @return the pre-existing instance, or null if none found
     */
    public static TransformTrack findJointTrack(AnimClip clip, int jointIndex) {
        Validate.nonNegative(jointIndex, "joint index");

        AnimTrack[] tracks = clip.getTracks();
        for (AnimTrack track : tracks) {
            if (track instanceof TransformTrack) {
                TransformTrack transformTrack = (TransformTrack) track;
                HasLocalTransform target = transformTrack.getTarget();
                if (target instanceof Joint) {
                    int trackJointIndex = ((Joint) target).getId();
                    if (jointIndex == trackJointIndex) {
                        return transformTrack;
                    }
                }
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
                    /*
                     * Climb to the subtree's root, adding indices to the list.
                     */
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
     * If the tree position is not needed, use
     * {@link jme3utilities.MySpatial#findNamed(com.jme3.scene.Spatial, java.lang.String)}
     * instead.
     *
     * @param name what name to search for (not null, not empty)
     * @param subtree which subtree to search (may be null, unaffected)
     * @param storePosition tree position of the spatial (modified if found and
     * not null)
     * @return the pre-existing spatial, or null if not found
     */
    public static Spatial findSpatialNamed(String name, Spatial subtree,
            List<Integer> storePosition) {
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
    public static SpatialTrack findSpatialTrack(Animation animation,
            int spatialTrackIndex) {
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
     * Access the keyframe rotations for the specified track.
     *
     * @param track the input track (a BoneTrack, SpatialTrack, or
     * TransformTrack)
     * @return the pre-existing instance or null
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
     * Access the keyframe scales for the specified track.
     *
     * @param track the input track (a BoneTrack, SpatialTrack, or
     * TransformTrack)
     * @return the pre-existing instance or null
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
            result = ((MorphTrack) object).getTimes();
        } else if (object instanceof Track) {
            result = ((Track) object).getKeyFrameTimes();
        } else if (object instanceof TransformTrack) {
            result = ((TransformTrack) object).getTimes();
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
    public static boolean hasExtraSpatials(Spatial subtree,
            Collection<Spatial> attachmentsNodes) {
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

        TransformTrack track = findJointTrack(clip, jointIndex);
        if (track == null) {
            return false;
        } else {
            return true;
        }
    }

    /**
     * Copy a TransformTrack, inserting a keyframe at the specified time (which
     * mustn't already have a keyframe). TODO move to TrackEdit
     *
     * @param oldTrack input TransformTrack (not null, unaffected)
     * @param frameTime when to insert (&gt;0)
     * @param transform transform to insert (not null, unaffected)
     * @return a new TransformTrack
     */
    public static TransformTrack insertKeyframe(TransformTrack oldTrack,
            float frameTime, Transform transform) {
        Validate.positive(frameTime, "keyframe time");
        assert findKeyframeIndex(oldTrack, frameTime) == -1;

        float[] oldTimes = getTrackTimes(oldTrack);
        Vector3f[] oldTranslations = oldTrack.getTranslations();
        Quaternion[] oldRotations = oldTrack.getRotations();
        Vector3f[] oldScales = oldTrack.getScales();

        int oldCount = oldTimes.length;
        assert oldCount > 0 : oldCount;
        int newCount = oldCount + 1;
        /*
         * Allocate new arrays.
         */
        float[] newTimes = new float[newCount];
        Vector3f[] newTranslations = new Vector3f[newCount];
        Quaternion[] newRotations = new Quaternion[newCount];
        Vector3f[] newScales = new Vector3f[newCount];

        boolean added = false;
        for (int oldIndex = 0; oldIndex < oldCount; ++oldIndex) {
            float time = oldTimes[oldIndex];
            int newIndex = oldIndex;
            if (time > frameTime) {
                if (!added) {
                    newTimes[newIndex] = frameTime;
                    newTranslations[newIndex]
                            = transform.getTranslation().clone();
                    newRotations[newIndex] = transform.getRotation().clone();
                    newScales[newIndex] = transform.getScale().clone();
                    added = true;
                }
                ++newIndex;
            }

            newTimes[newIndex] = oldTimes[oldIndex];
            if (oldTranslations == null) {
                newTranslations[newIndex] = new Vector3f();
            } else {
                newTranslations[newIndex] = oldTranslations[oldIndex].clone();
            }
            if (oldRotations == null) {
                newRotations[newIndex] = new Quaternion();
            } else {
                newRotations[newIndex] = oldRotations[oldIndex].clone();
            }
            if (oldScales == null) {
                newScales[newIndex] = new Vector3f(1f, 1f, 1f);
            } else {
                newScales[newIndex] = oldScales[oldIndex].clone();
            }
        }
        if (!added) {
            newTimes[oldCount] = frameTime;
            newTranslations[oldCount] = transform.getTranslation().clone();
            newRotations[oldCount] = transform.getRotation().clone();
            newScales[oldCount] = transform.getScale().clone();
        }

        HasLocalTransform target = oldTrack.getTarget();
        TransformTrack result = new TransformTrack(target, newTimes,
                newTranslations, newRotations, newScales);

        return result;
    }

    /**
     * Test whether the specified Spatial is an "extra" spatial.
     *
     * A spatial is "extra" iff neither it nor any of its descendents:
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
    public static boolean isExtra(Spatial spatial,
            Collection<Spatial> attachmentsNodes) {
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
     * Enumerate all Armature instances in the specified subtree of a scene
     * graph. Note: recursive! TODO move to MySkeleton
     *
     * @param subtree (not null, aliases created)
     * @param addResult storage for results (added to if not null)
     * @return an expanded list (either storeResult or a new instance)
     */
    public static List<Armature> listArmatures(Spatial subtree,
            List<Armature> addResult) {
        Validate.nonNull(subtree, "subtree");
        if (addResult == null) {
            addResult = new ArrayList<>(4);
        }

        int numSgcs = subtree.getNumControls();
        for (int sgcIndex = 0; sgcIndex < numSgcs; ++sgcIndex) {
            Control sgc = subtree.getControl(sgcIndex);
            if (sgc instanceof SkinningControl) {
                Armature armature = ((SkinningControl) sgc).getArmature();
                if (armature != null && !addResult.contains(armature)) {
                    addResult.add(armature);
                }
            }
        }

        if (subtree instanceof Node) {
            Node node = (Node) subtree;
            List<Spatial> children = node.getChildren();
            for (Spatial child : children) {
                listArmatures(child, addResult);
            }
        }

        return addResult;
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
    public static Object newTrack(Object oldTrack, float[] times,
            Vector3f[] translations, Quaternion[] rotations,
            Vector3f[] scales) {
        int numKeyframes = times.length;
        assert numKeyframes > 0 : numKeyframes;
        assert translations == null || translations.length == numKeyframes;
        assert rotations == null || rotations.length == numKeyframes;
        assert scales == null || scales.length == numKeyframes;

        Object result;
        if (oldTrack instanceof BoneTrack) {
            int boneIndex = ((BoneTrack) oldTrack).getTargetBoneIndex();
            result = MyAnimation.newBoneTrack(boneIndex, times, translations,
                    rotations, scales);

        } else if (oldTrack instanceof SpatialTrack) {
            Spatial spatial = ((SpatialTrack) oldTrack).getTrackSpatial();
            SpatialTrack newSpatialTrack
                    = new SpatialTrack(times, translations, rotations, scales);
            newSpatialTrack.setTrackSpatial(spatial);
            result = newSpatialTrack;

        } else if (oldTrack instanceof TransformTrack) {
            TransformTrack transformTrack = (TransformTrack) oldTrack;
            HasLocalTransform target = transformTrack.getTarget();
            result = new TransformTrack(target, times, translations, rotations,
                    scales);

        } else {
            throw new IllegalArgumentException();
        }

        return result;
    }

    /**
     * Copy a TransformTrack, uniformly reducing the number of keyframes by the
     * specified factor. TODO move to TrackEdit
     *
     * @param oldTrack input TransformTrack (not null, unaffected)
     * @param factor reduction factor (&ge;2)
     * @return a new TransformTrack
     */
    public static TransformTrack reduce(TransformTrack oldTrack, int factor) {
        Validate.inRange(factor, "factor", 2, Integer.MAX_VALUE);

        float[] oldTimes = getTrackTimes(oldTrack);
        Vector3f[] oldTranslations = oldTrack.getTranslations();
        Quaternion[] oldRotations = oldTrack.getRotations();
        Vector3f[] oldScales = oldTrack.getScales();

        int oldCount = oldTimes.length;
        assert oldCount > 0 : oldCount;
        int newCount = 1 + (oldCount - 1) / factor;
        /*
         * Allocate new arrays.
         */
        float[] newTimes = new float[newCount];
        Vector3f[] newTranslations = null;
        if (oldTranslations != null) {
            newTranslations = new Vector3f[newCount];
        }
        Quaternion[] newRotations = null;
        if (oldRotations != null) {
            newRotations = new Quaternion[newCount];
        }
        Vector3f[] newScales = null;
        if (oldScales != null) {
            newScales = new Vector3f[newCount];
        }

        for (int newIndex = 0; newIndex < newCount; ++newIndex) {
            int oldIndex = newIndex * factor;

            newTimes[newIndex] = oldTimes[oldIndex];
            if (newTranslations != null) {
                newTranslations[newIndex] = oldTranslations[oldIndex].clone();
            }
            if (newRotations != null) {
                newRotations[newIndex] = oldRotations[oldIndex].clone();
            }
            if (newScales != null) {
                newScales[newIndex] = oldScales[oldIndex].clone();
            }
        }

        HasLocalTransform target = oldTrack.getTarget();
        TransformTrack result = new TransformTrack(target, newTimes,
                newTranslations, newRotations, newScales);

        return result;
    }

    /**
     * Remove repetitious keyframes from an AnimClip. TODO move to AnimationEdit
     *
     * @param clip (not null, modified)
     * @return the number of tracks edited (&ge;0)
     */
    public static int removeRepeats(AnimClip clip) {
        int numTracksEdited = 0;
        AnimTrack[] tracks = clip.getTracks();
        for (AnimTrack track : tracks) {
            if (track instanceof TransformTrack) {
                boolean removed = removeRepeats((TransformTrack) track);
                if (removed) {
                    ++numTracksEdited;
                }
            } // TODO other track types
        }

        return numTracksEdited;
    }

    /**
     * Remove all repetitious keyframes from a TransformTrack. TODO move to
     * TrackEdit
     *
     * @param track the input track (not null, modified)
     * @return true if 1 or more keyframes were removed, otherwise false
     */
    public static boolean removeRepeats(TransformTrack track) {
        float[] oldTimes = track.getTimes();
        /*
         * Count distinct keyframes.
         */
        float prevTime = Float.NEGATIVE_INFINITY;
        int newCount = 0;
        for (float time : oldTimes) {
            if (time != prevTime) {
                ++newCount;
            }
            prevTime = time;
        }

        int oldCount = oldTimes.length;
        assert oldCount > 0 : oldCount;
        if (newCount == oldCount) {
            return false;
        }

        Vector3f[] oldTranslations = track.getTranslations();
        Quaternion[] oldRotations = track.getRotations();
        Vector3f[] oldScales = track.getScales();
        /*
         * Allocate new arrays.
         */
        float[] newTimes = new float[newCount];
        Vector3f[] newTranslations = null;
        if (oldTranslations != null) {
            newTranslations = new Vector3f[newCount];
        }
        Quaternion[] newRotations = null;
        if (oldRotations != null) {
            newRotations = new Quaternion[newCount];
        }
        Vector3f[] newScales = null;
        if (oldScales != null) {
            newScales = new Vector3f[newCount];
        }
        /*
         * Copy all non-repeated keyframes.
         */
        prevTime = Float.NEGATIVE_INFINITY;
        int newIndex = 0;
        for (int oldIndex = 0; oldIndex < oldCount; ++oldIndex) {
            float time = oldTimes[oldIndex];
            if (time != prevTime) {
                newTimes[newIndex] = oldTimes[oldIndex];
                if (newTranslations != null) {
                    newTranslations[newIndex] = oldTranslations[oldIndex];
                }
                if (newRotations != null) {
                    newRotations[newIndex] = oldRotations[oldIndex];
                }
                if (newScales != null) {
                    newScales[newIndex] = oldScales[oldIndex];
                }
                ++newIndex;
            }
            prevTime = time;
        }

        track.setKeyframes(newTimes, newTranslations, newRotations, newScales);
        return true;
    }

    /**
     * Copy a TransformTrack, setting the indexed keyframe to the specified
     * transform. TODO move to TrackEdit
     *
     * @param oldTrack the input track (not null, unaffected)
     * @param frameIndex which keyframe (&ge;0, &lt;numFrames)
     * @param transform transform to apply (not null, unaffected)
     * @return a new TransformTrack
     */
    public static TransformTrack replaceKeyframe(TransformTrack oldTrack,
            int frameIndex, Transform transform) {
        float[] oldTimes = getTrackTimes(oldTrack);
        int frameCount = oldTimes.length;
        Validate.inRange(frameIndex, "keyframe index", 0, frameCount - 1);
        Validate.nonNull(transform, "transform");

        Vector3f[] oldTranslations = oldTrack.getTranslations();
        Quaternion[] oldRotations = oldTrack.getRotations();
        Vector3f[] oldScales = oldTrack.getScales();
        /*
         * Allocate new arrays.
         */
        float[] newTimes = new float[frameCount];
        Vector3f[] newTranslations = new Vector3f[frameCount];
        Quaternion[] newRotations = new Quaternion[frameCount];
        Vector3f[] newScales = new Vector3f[frameCount];

        for (int frameI = 0; frameI < frameCount; frameI++) {
            newTimes[frameI] = oldTimes[frameI];
            if (frameI == frameIndex) {
                newTranslations[frameI] = transform.getTranslation().clone();
                newRotations[frameI] = transform.getRotation().clone();
                newScales[frameI] = transform.getScale().clone();
            } else {
                if (oldTranslations == null) {
                    newTranslations[frameI] = new Vector3f();
                } else {
                    newTranslations[frameI] = oldTranslations[frameI].clone();
                }
                if (oldRotations == null) {
                    newRotations[frameI] = new Quaternion();
                } else {
                    newRotations[frameI] = oldRotations[frameI].clone();
                }
                if (oldScales == null) {
                    newScales[frameI] = new Vector3f(1f, 1f, 1f);
                } else {
                    newScales[frameI] = oldScales[frameI].clone();
                }
            }
        }

        HasLocalTransform target = oldTrack.getTarget();
        TransformTrack result = new TransformTrack(target, newTimes,
                newTranslations, newRotations, newScales);

        return result;
    }

    /**
     * Copy a TransformTrack, resampling at the specified times. TODO move to
     * TrackEdit
     *
     * @param oldTrack the input track (not null, unaffected)
     * @param newTimes sample times (not null, alias created)
     * @return a new instance
     */
    public static TransformTrack resample(TransformTrack oldTrack,
            float[] newTimes) {
        int numSamples = newTimes.length;
        Vector3f[] oldTranslations = oldTrack.getTranslations();
        Quaternion[] oldRotations = oldTrack.getRotations();
        Vector3f[] oldScales = oldTrack.getScales();
        /*
         * Allocate new arrays.
         */
        Vector3f[] newTranslations = null;
        if (oldTranslations != null) {
            newTranslations = new Vector3f[numSamples];
        }
        Quaternion[] newRotations = null;
        if (oldRotations != null) {
            newRotations = new Quaternion[numSamples];
        }
        Vector3f[] newScales = null;
        if (oldScales != null) {
            newScales = new Vector3f[numSamples];
        }

        for (int frameIndex = 0; frameIndex < numSamples; frameIndex++) {
            float time = newTimes[frameIndex];
            Transform transform = new Transform();
            oldTrack.getDataAtTime(time, transform);

            if (newTranslations != null) {
                newTranslations[frameIndex] = transform.getTranslation();
            }
            if (newRotations != null) {
                newRotations[frameIndex] = transform.getRotation();
            }
            if (newScales != null) {
                newScales[frameIndex] = transform.getScale();
            }
        }

        HasLocalTransform target = oldTrack.getTarget();
        TransformTrack result = new TransformTrack(target, newTimes,
                newTranslations, newRotations, newScales);

        return result;
    }

    /**
     * Copy a TransformTrack, resampling it at the specified rate. TODO move to
     * TrackEdit
     *
     * @param oldTrack the input track (not null, unaffected)
     * @param sampleRate the desired sample rate (in frames per second, &gt;0)
     * @param duration the duration of the AnimClip (in seconds, &ge;0)
     * @return a new instance
     */
    public static TransformTrack resampleAtRate(TransformTrack oldTrack,
            float sampleRate, float duration) {
        Validate.positive(sampleRate, "sample rate");
        Validate.nonNegative(duration, "duration");

        int numSamples = 1 + (int) Math.floor(duration * sampleRate);
        float[] newTimes = new float[numSamples];
        for (int frameIndex = 0; frameIndex < numSamples; frameIndex++) {
            float time = frameIndex / sampleRate;
            if (time > duration) {
                time = duration;
            }
            newTimes[frameIndex] = time;
        }
        TransformTrack result = resample(oldTrack, newTimes);

        return result;
    }

    /**
     * Copy a TransformTrack, resampling to the specified number of samples.
     * TODO move to TrackEdit
     *
     * @param oldTrack the input track (not null, unaffected)
     * @param numSamples the desired number of samples (&ge;2)
     * @param duration the duration of the AnimClip (in seconds, &ge;0)
     * @return a new instance
     */
    public static TransformTrack resampleToNumber(TransformTrack oldTrack,
            int numSamples, float duration) {
        Validate.inRange(numSamples, "number of samples", 2, Integer.MAX_VALUE);
        Validate.positive(duration, "duration");

        float[] newTimes = new float[numSamples];
        for (int frameIndex = 0; frameIndex < numSamples; frameIndex++) {
            float time;
            if (frameIndex == numSamples - 1) {
                time = duration;
            } else {
                time = (frameIndex * duration) / (numSamples - 1);
            }
            newTimes[frameIndex] = time;
        }
        TransformTrack result = resample(oldTrack, newTimes);

        return result;
    }

    /**
     * Apply the specified rotation to all data in the specified VertexBuffer.
     *
     * @param mesh the subject mesh (not null)
     * @param bufferType which buffer to read (not null)
     * @param rotation the rotation to apply (not null, unaffected)
     */
    public static void rotateBuffer(Mesh mesh, VertexBuffer.Type bufferType,
            Quaternion rotation) {
        Validate.nonNull(bufferType, "buffer type");
        Validate.nonNull(rotation, "rotation");

        VertexBuffer vertexBuffer = mesh.getBuffer(bufferType);
        if (vertexBuffer != null) {
            int count = mesh.getVertexCount();
            FloatBuffer floatBuffer = (FloatBuffer) vertexBuffer.getData();
            Vector3f tmpVector = new Vector3f();
            for (int vertexIndex = 0; vertexIndex < count; ++vertexIndex) {
                MyMesh.vertexVector3f(mesh, bufferType, vertexIndex, tmpVector);
                rotation.mult(tmpVector, tmpVector);

                int floatIndex = MyVector3f.numAxes * vertexIndex;
                floatBuffer.put(floatIndex, tmpVector.x);
                floatBuffer.put(floatIndex + 1, tmpVector.y);
                floatBuffer.put(floatIndex + 2, tmpVector.z);
            }

            vertexBuffer.setUpdateNeeded();
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
    public static Object setFrameTime(Object track, int frameIndex,
            float newTime) {
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

        Object result = cloneTrack(track);
        float[] newTimes = getTrackTimes(result); // an alias
        newTimes[frameIndex] = newTime;

        return result;
    }

    /**
     * Calculate the rotation specified by a bank of sliders when the rotation
     * display mode is QuatCoeff.
     *
     * @param sliderPositions (not null, length=3)
     * @param storeResult (modified if not null)
     * @return rotation (either storeResult or a new quaternion)
     */
    public static Quaternion setFromSliders(float[] sliderPositions,
            Quaternion storeResult) {
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
            result.normalizeLocal();
        } else {
            double w = Math.sqrt(1.0 - ssq);
            result.set((float) x, (float) y, (float) z, (float) w);
        }

        return result;
    }

    /**
     * Copy a TransformTrack, deleting the scale component if it consists
     * entirely of identities. TODO move to TrackEdit
     *
     * @param oldTrack the input track (not null, unaffected)
     * @return a new TransformTrack with the same target
     */
    public static TransformTrack simplify(TransformTrack oldTrack) {
        boolean keepScales = false;

        float[] oldTimes = oldTrack.getTimes();
        int numFrames = oldTimes.length;
        assert numFrames > 0 : numFrames;
        Vector3f[] oldScales = oldTrack.getScales();
        for (int index = 0; index < numFrames; ++index) {
            if (oldScales != null) {
                Vector3f scale = oldScales[index];
                if (!MyVector3f.isScaleIdentity(scale)) {
                    keepScales = true;
                }
            }
        }

        Vector3f[] oldTranslations = oldTrack.getTranslations();
        Quaternion[] oldRotations = oldTrack.getRotations();

        float[] newTimes = new float[numFrames];
        Vector3f[] newTranslations = new Vector3f[numFrames];
        Quaternion[] newRotations = new Quaternion[numFrames];
        Vector3f[] newScales = keepScales ? new Vector3f[numFrames] : null;

        for (int index = 0; index < numFrames; ++index) {
            newTimes[index] = oldTimes[index];
            newTranslations[index] = oldTranslations[index].clone();
            newRotations[index] = oldRotations[index].clone();
            if (keepScales) {
                newScales[index] = oldScales[index].clone();
            }
        }

        HasLocalTransform target = oldTrack.getTarget();
        TransformTrack result = new TransformTrack(target, newTimes,
                newTranslations, newRotations, newScales);

        return result;
    }

    /**
     * Copy a TransformTrack, smoothing it using the specified techniques. TODO
     * move to TrackEdit
     *
     * @param oldTrack input bone/spatial track (not null, unaffected)
     * @param width width of time window (&ge;0, &le;duration)
     * @param smoothTranslations technique for translations (not null)
     * @param smoothRotations technique for translations (not null)
     * @param smoothScales technique for scales (not null)
     * @param duration animation duration (in seconds, &ge;0)
     * @return a new track of the same type as oldTrack
     */
    public static TransformTrack smooth(TransformTrack oldTrack, float width,
            SmoothVectors smoothTranslations, SmoothRotations smoothRotations,
            SmoothVectors smoothScales, float duration) {
        Validate.inRange(width, "width", 0f, duration);
        Validate.nonNegative(duration, "duration");

        float[] oldTimes = getTrackTimes(oldTrack);
        Vector3f[] oldTranslations = oldTrack.getTranslations();
        Quaternion[] oldRotations = oldTrack.getRotations();
        Vector3f[] oldScales = oldTrack.getScales();
        /*
         * Allocate new arrays.
         */
        int numFrames = oldTimes.length;
        assert numFrames > 0 : numFrames;
        float[] newTimes = new float[numFrames];
        System.arraycopy(oldTimes, 0, newTimes, 0, numFrames);

        Vector3f[] newTranslations = null;
        if (oldTranslations != null) {
            newTranslations = smoothTranslations.smooth(oldTimes, duration,
                    oldTranslations, width, null);
        }

        Quaternion[] newRotations = null;
        if (oldRotations != null) {
            newRotations = smoothRotations.smooth(oldTimes, duration,
                    oldRotations, width, null);
        }

        Vector3f[] newScales = null;
        if (oldScales != null) {
            newScales = smoothScales.smooth(oldTimes, duration, oldScales,
                    width, null);
        }

        HasLocalTransform target = oldTrack.getTarget();
        TransformTrack result = new TransformTrack(target, newTimes,
                newTranslations, newRotations, newScales);

        return result;
    }

    /**
     * Apply the specified coordinate transform to all data in the specified
     * VertexBuffer.
     *
     * @param mesh the subject mesh (not null)
     * @param bufferType which buffer to read (not null)
     * @param transform the Transform to apply (not null, unaffected)
     */
    public static void transformBuffer(Mesh mesh, VertexBuffer.Type bufferType,
            Transform transform) {
        Validate.nonNull(bufferType, "buffer type");
        Validate.nonNull(transform, "transform");

        VertexBuffer vertexBuffer = mesh.getBuffer(bufferType);
        if (vertexBuffer != null) {
            int count = mesh.getVertexCount();
            FloatBuffer floatBuffer = (FloatBuffer) vertexBuffer.getData();

            Vector3f tmpVector = new Vector3f();
            for (int vertexIndex = 0; vertexIndex < count; ++vertexIndex) {
                MyMesh.vertexVector3f(mesh, bufferType, vertexIndex, tmpVector);
                transform.transformVector(tmpVector, tmpVector);

                int floatIndex = MyVector3f.numAxes * vertexIndex;
                floatBuffer.put(floatIndex, tmpVector.x);
                floatBuffer.put(floatIndex + 1, tmpVector.y);
                floatBuffer.put(floatIndex + 2, tmpVector.z);
            }

            vertexBuffer.setUpdateNeeded();
        }
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

    /**
     * Copy a TransformTrack, altering the track's first keyframe and end-time
     * keyframe so that they precisely match. If the track doesn't end with a
     * keyframe, append one. TODO move to TrackEdit
     *
     * @param oldTrack the input TransformTrack (not null, unaffected)
     * @param duration duration of the animation (in seconds, &gt;0)
     * @param endWeight how much weight to give to the pre-existing end-time
     * keyframe, if one exists (&ge;0, &le;1)
     * @return a new TransformTrack
     */
    public static TransformTrack wrap(TransformTrack oldTrack, float duration,
            float endWeight) {
        Validate.positive(duration, "duration");
        Validate.fraction(endWeight, "end weight");

        float[] oldTimes = getTrackTimes(oldTrack);
        Vector3f[] oldTranslations = oldTrack.getTranslations();
        Quaternion[] oldRotations = oldTrack.getRotations();
        Vector3f[] oldScales = oldTrack.getScales();

        int oldCount = oldTimes.length;
        assert oldCount > 0 : oldCount;
        int newCount;
        Vector3f wrapTranslation = new Vector3f();
        Quaternion wrapRotation = new Quaternion();
        Vector3f wrapScale = new Vector3f();
        int endIndex = findKeyframeIndex(oldTrack, duration);
        if (endIndex == -1) { // doesn't end with a keyframe, ignore endWeight
            endIndex = oldCount;
            newCount = oldCount + 1;
            if (oldTranslations != null) {
                wrapTranslation.set(oldTranslations[0]);
            }
            if (oldRotations != null) {
                wrapRotation.set(oldRotations[0]);
            }
            if (oldScales != null) {
                wrapScale.set(oldScales[0]);
            }
        } else {
            newCount = oldCount;
            if (oldTranslations != null) {
                MyVector3f.lerp(endWeight, oldTranslations[0],
                        oldTranslations[endIndex], wrapTranslation);
            }
            if (oldRotations != null) {
                MyQuaternion.slerp(endWeight, oldRotations[0],
                        oldRotations[endIndex], wrapRotation);
            }
            if (oldScales != null) {
                MyVector3f.lerp(endWeight, oldScales[0], oldScales[endIndex],
                        wrapScale);
            }
        }
        assert endIndex == newCount - 1;
        /*
         * Allocate new arrays.
         */
        float[] newTimes = new float[newCount];
        newTimes[0] = 0f;
        newTimes[endIndex] = duration;
        Vector3f[] newTranslations = null;
        if (oldTranslations != null) {
            newTranslations = new Vector3f[newCount];
            newTranslations[0] = wrapTranslation.clone();
            newTranslations[endIndex] = wrapTranslation.clone();
        }
        Quaternion[] newRotations = null;
        if (oldRotations != null) {
            newRotations = new Quaternion[newCount];
            newRotations[0] = wrapRotation.clone();
            newRotations[endIndex] = wrapRotation.clone();
        }
        Vector3f[] newScales = null;
        if (oldScales != null) {
            newScales = new Vector3f[newCount];
            newScales[0] = wrapScale.clone();
            newScales[endIndex] = wrapScale.clone();
        }

        for (int frameIndex = 1; frameIndex < endIndex; ++frameIndex) {
            newTimes[frameIndex] = oldTimes[frameIndex];
            if (newTranslations != null) {
                newTranslations[frameIndex]
                        = oldTranslations[frameIndex].clone();
            }
            if (newRotations != null) {
                newRotations[frameIndex] = oldRotations[frameIndex].clone();
            }
            if (newScales != null) {
                newScales[frameIndex] = oldScales[frameIndex].clone();
            }
        }

        HasLocalTransform target = oldTrack.getTarget();
        TransformTrack result = new TransformTrack(target, newTimes,
                newTranslations, newRotations, newScales);

        return result;
    }

    /**
     * Repair all tracks in which the first keyframe isn't at time=0. TODO move
     * to AnimationEdit
     *
     * @param clip the AnimClip to repair (not null)
     * @return the number of tracks edited (&ge;0)
     */
    public static int zeroFirst(AnimClip clip) {
        int numTracksEdited = 0;
        AnimTrack[] tracks = clip.getTracks();
        for (AnimTrack track : tracks) {
            float[] times = getTrackTimes(track); // an alias
            if (times[0] != 0f) {
                times[0] = 0f;
                ++numTracksEdited;
            }
        }

        return numTracksEdited;
    }
}
