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

import com.jme3.animation.Animation;
import com.jme3.animation.Bone;
import com.jme3.animation.BoneTrack;
import com.jme3.animation.Skeleton;
import com.jme3.math.Quaternion;
import com.jme3.math.Transform;
import com.jme3.math.Vector3f;
import com.jme3.scene.plugins.bvh.BoneMapping;
import com.jme3.scene.plugins.bvh.SkeletonMapping;
import com.jme3.util.clone.Cloner;
import com.jme3.util.clone.JmeCloneable;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;
import jme3utilities.Misc;
import jme3utilities.MyAnimation;
import jme3utilities.MySkeleton;
import jme3utilities.Validate;

/**
 * Encapsulate a pose for a CG model.
 *
 * @author Stephen Gold sgold@sonic.net
 */
public class Pose implements JmeCloneable {
    // *************************************************************************
    // constants and loggers

    /**
     * message logger for this class
     */
    final private static Logger logger = Logger.getLogger(Pose.class.getName());
    // *************************************************************************
    // fields

    /**
     * user/animation transforms that describe this pose, one for each bone
     */
    private List<Transform> transforms;
    /**
     * the skeleton for which this pose was generated, or null for none
     * <p>
     * This skeleton provides the name, index, parent, children, and bind
     * transform of each bone. All other bone information is disregarded. In
     * particular, the bones' {local/model}{Pos/Rot/Scale} and userControl
     * fields are ignored.
     */
    private Skeleton skeleton;
    // *************************************************************************
    // constructors

    /**
     * Instantiate a pose for the specified skeleton.
     *
     * @param skeleton (may be null)
     */
    public Pose(Skeleton skeleton) {
        this.skeleton = skeleton;

        int boneCount;
        if (skeleton == null) {
            boneCount = 0;
        } else {
            boneCount = skeleton.getBoneCount();
        }

        transforms = new ArrayList<>(boneCount);
        for (int boneIndex = 0; boneIndex < boneCount; boneIndex++) {
            Transform transform = new Transform();
            transforms.add(transform);
        }
    }
    // *************************************************************************
    // new methods exposed

    /**
     * Convert this pose to an animation. The resulting animation will have zero
     * duration, a single keyframe at t=0, and all its tracks will be
     * BoneTracks.
     *
     * @param animationName name for the new animation (not null)
     * @return a new instance
     */
    public Animation capture(String animationName) {
        Validate.nonNull(animationName, "animation name");
        /*
         * Start with an empty animation.
         */
        float duration = 0f;
        Animation result = new Animation(animationName, duration);
        /*
         * Add a BoneTrack for each bone that's not in bind pose.
         */
        int numBones = countBones();
        Transform transform = new Transform();
        for (int boneIndex = 0; boneIndex < numBones; boneIndex++) {
            copyTransform(boneIndex, transform);
            if (!Misc.isIdentity(transform)) {
                Vector3f translation = transform.getTranslation();
                Quaternion rotation = transform.getRotation();
                Vector3f scale = transform.getScale();
                BoneTrack track = MyAnimation.createTrack(boneIndex,
                        translation, rotation, scale);
                result.addTrack(track);
            }
        }

        return result;
    }

    /**
     * Copy the user/animation transform of the indexed bone. TODO rename?
     *
     * @param boneIndex which bone to use (&ge;0)
     * @param storeResult (modified if not null)
     * @return transform (either storeResult or a new instance)
     */
    public Transform copyTransform(int boneIndex, Transform storeResult) {
        Validate.nonNegative(boneIndex, "bone index");
        if (storeResult == null) {
            storeResult = new Transform();
        }

        Transform transform = transforms.get(boneIndex);
        storeResult.set(transform);

        return storeResult;
    }

    /**
     * Count the bones in this pose.
     *
     * @return count (&ge;0)
     */
    public int countBones() {
        int count = transforms.size();
        assert count >= 0 : count;
        return count;
    }

    /**
     * Find the index of the named bone in the skeleton of this pose.
     *
     * @param boneName which bone (not null)
     * @return bone index (&ge;0) or -1 if not found
     */
    public int findBone(String boneName) {
        int result = skeleton.getBoneIndex(boneName);
        return result;
    }

    /**
     * Calculate the local rotation of the indexed bone.
     *
     * @param boneIndex which bone to use (&ge;0)
     * @param storeResult (modified if not null)
     * @return local rotation (either storeResult or a new instance)
     */
    public Quaternion localRotation(int boneIndex, Quaternion storeResult) {
        Validate.nonNegative(boneIndex, "bone index");
        /*
         * Start with the bone's bind transform.
         */
        Bone bone = skeleton.getBone(boneIndex);
        Quaternion bindRotation = bone.getBindRotation();
        /*
         * Apply its user/animation rotation.
         */
        Transform transform = transforms.get(boneIndex);
        Quaternion userRotation = transform.getRotation();
        storeResult = bindRotation.mult(userRotation, storeResult);

        return storeResult;
    }

    /**
     * Calculate the local transform of the indexed bone. When applied as a left
     * factor, the local transform converts from the bone's coordinate system to
     * the parent bone's coordinate system.
     *
     * @param boneIndex which bone to use (&ge;0)
     * @param storeResult (modified if not null)
     * @return transform in local coordinates (either storeResult or a new
     * instance)
     */
    public Transform localTransform(int boneIndex, Transform storeResult) {
        Validate.nonNegative(boneIndex, "bone index");
        /*
         * Start with the bone's bind transform.
         */
        Bone bone = skeleton.getBone(boneIndex);
        storeResult = MySkeleton.copyBindTransform(bone, storeResult);
        /*
         * Apply the user/animation transform in a simple (yet peculiar) way
         * to obtain the bone's local transform.
         */
        Transform user = copyTransform(boneIndex, null);
        storeResult.getTranslation().addLocal(user.getTranslation());
        storeResult.getRotation().multLocal(user.getRotation());
        storeResult.getScale().multLocal(user.getScale());

        return storeResult;
    }

    /**
     * Calculate the orientation of the indexed bone in the coordinate system of
     * an animated spatial.
     *
     * @param boneIndex which bone to use (&ge;0)
     * @param storeResult (modified if not null)
     * @return orientation in model space (either storeResult or a new instance)
     */
    public Quaternion modelOrientation(int boneIndex, Quaternion storeResult) {
        Validate.nonNegative(boneIndex, "bone index");

        Bone bone = skeleton.getBone(boneIndex);
        Bone parentBone = bone.getParent();
        if (parentBone == null) {
            /*
             * For a root bone, use the local rotation.
             */
            storeResult = localRotation(boneIndex, storeResult);
        } else {
            int parentIndex = skeleton.getBoneIndex(parentBone);
            /*
             * For a non-root bone, use the parent's model orientation
             * times the local rotation.
             */
            storeResult = modelOrientation(parentIndex, storeResult);
            Quaternion localRotation = localRotation(boneIndex, null);
            storeResult.multLocal(localRotation);
        }

        return storeResult;
    }

    /**
     * Calculate the model transform of the indexed bone. When applied as a left
     * factor, the model transform converts from the bone's coordinate system to
     * the coordinate system of an animated spatial.
     *
     * @param boneIndex which bone to use (&ge;0)
     * @param storeResult (modified if not null)
     * @return transform (either storeResult or a new instance)
     */
    public Transform modelTransform(int boneIndex, Transform storeResult) {
        Validate.nonNegative(boneIndex, "bone index");
        /*
         * Start with the bone's local transform.
         */
        storeResult = localTransform(boneIndex, storeResult);

        Bone bone = skeleton.getBone(boneIndex);
        Bone parentBone = bone.getParent();
        if (parentBone != null) {
            Transform local = storeResult.clone();

            int parentIndex = skeleton.getBoneIndex(parentBone);
            Transform parent = modelTransform(parentIndex, null);
            /*
             * Apply the parent's model transform in a very peculiar way
             * to obtain the bone's model transform.
             */
            Vector3f mTranslation = storeResult.getTranslation();
            Quaternion mRotation = storeResult.getRotation();
            Vector3f mScale = storeResult.getScale();
            parent.getRotation().mult(local.getRotation(), mRotation);
            parent.getScale().mult(local.getScale(), mScale);
            parent.getRotation().mult(local.getTranslation(), mTranslation);
            mTranslation.multLocal(parent.getScale());
            mTranslation.addLocal(parent.getTranslation());
        }

        return storeResult;
    }

    /**
     * Reset the rotation of the indexed bone to identity.
     *
     * @param boneIndex which bone (&ge;0)
     */
    public void resetRotation(int boneIndex) {
        Validate.nonNegative(boneIndex, "bone index");

        Transform transform = transforms.get(boneIndex);
        Quaternion rotation = transform.getRotation();
        rotation.loadIdentity();
    }

    /**
     * Reset the scale of the indexed bone to identity.
     *
     * @param boneIndex which bone (&ge;0)
     */
    public void resetScale(int boneIndex) {
        Validate.nonNegative(boneIndex, "bone index");

        Transform transform = transforms.get(boneIndex);
        Vector3f scale = transform.getScale();
        scale.set(1f, 1f, 1f);
    }

    /**
     * Alter the skeleton and reset all bones to bind pose.
     *
     * @param skeleton (may be null)
     */
    public void resetToBind(Skeleton skeleton) {
        this.skeleton = skeleton;

        int boneCount;
        if (skeleton == null) {
            boneCount = 0;
        } else {
            boneCount = skeleton.getBoneCount();
        }
        transforms.clear();
        for (int boneIndex = 0; boneIndex < boneCount; boneIndex++) {
            Transform transform = new Transform();
            transforms.add(transform);
        }
    }

    /**
     * Reset the translation of the indexed bone to zero.
     *
     * @param boneIndex which bone (&ge;0)
     */
    public void resetTranslation(int boneIndex) {
        Validate.nonNegative(boneIndex, "bone index");

        Transform transform = transforms.get(boneIndex);
        Vector3f translation = transform.getTranslation();
        translation.zero();
    }

    /**
     * Alter the transform of the indexed bone.
     *
     * @param boneIndex which bone to translate (&ge;0)
     * @param transform (not null, unaffected)
     */
    public void set(int boneIndex, Transform transform) {
        Validate.nonNegative(boneIndex, "bone index");
        Validate.nonNull(transform, "transform");

        Transform boneTransform = transforms.get(boneIndex);
        boneTransform.set(transform);
    }

    /**
     * Alter the rotation of the indexed bone.
     *
     * @param boneIndex which bone to rotate (&ge;0)
     * @param rotation (not null, unaffected)
     */
    public void setRotation(int boneIndex, Quaternion rotation) {
        Validate.nonNegative(boneIndex, "bone index");
        Validate.nonNull(rotation, "rotation");

        Transform boneTransform = transforms.get(boneIndex);
        boneTransform.setRotation(rotation);
    }

    /**
     * Alter the scale of the indexed bone.
     *
     * @param boneIndex which bone to scale (&ge;0)
     * @param scale (not null, unaffected)
     */
    public void setScale(int boneIndex, Vector3f scale) {
        Validate.nonNegative(boneIndex, "bone index");
        Validate.nonNull(scale, "scale");
        Validate.positive(scale.x, "x scale");
        Validate.positive(scale.y, "y scale");
        Validate.positive(scale.z, "z scale");

        Transform boneTransform = transforms.get(boneIndex);
        boneTransform.setScale(scale);
    }

    /**
     * Alter the translation of the indexed bone.
     *
     * @param boneIndex which bone to translate (&ge;0)
     * @param translation (not null, unaffected)
     */
    public void setTranslation(int boneIndex, Vector3f translation) {
        Validate.nonNegative(boneIndex, "bone index");
        Validate.nonNull(translation, "translation");

        Transform boneTransform = transforms.get(boneIndex);
        boneTransform.setTranslation(translation);
    }

    /**
     * Configure this pose for the specified animation at the specified time.
     *
     * @param animation which animation (not null, unaffected)
     * @param time animation time (in seconds)
     */
    public void setToAnimation(Animation animation, float time) {
        Validate.nonNull(animation, "animation");

        int numBones = transforms.size();
        for (int boneIndex = 0; boneIndex < numBones; boneIndex++) {
            Transform transform = transforms.get(boneIndex);
            BoneTrack track = MyAnimation.findTrack(animation, boneIndex);
            if (track == null) {
                transform.loadIdentity();
            } else {
                Util.boneTransform(track, time, transform);
            }
        }
    }

    /**
     * Configure this pose by re-targeting the specified source pose.
     *
     * @param sourcePose which source pose to re-target (not null, unaffected)
     * @param mapping skeleton mapping to use (not null, unaffected)
     */
    public void setToRetarget(Pose sourcePose, SkeletonMapping mapping) {
        Validate.nonNull(sourcePose, "source pose");
        Validate.nonNull(mapping, "mapping");

        Bone[] rootBones = skeleton.getRoots();
        for (Bone rootBone : rootBones) {
            retargetBones(rootBone, sourcePose, mapping);
        }
    }

    /**
     * Calculate the user/animation rotation for the indexed bone to give it the
     * specified orientation in the coordinate system of an animated spatial.
     *
     * @param boneIndex which bone (&ge;0)
     * @param modelOrientation desired orientation (not null, unaffected)
     * @param storeResult (modified if not null)
     * @return transform (either storeResult or a new instance)
     */
    public Quaternion userForModel(int boneIndex, Quaternion modelOrientation,
            Quaternion storeResult) {
        Validate.nonNegative(boneIndex, "bone index");
        Validate.nonNull(modelOrientation, "model orienation");

        Bone bone = skeleton.getBone(boneIndex);
        Quaternion bind = bone.getBindRotation();
        Quaternion inverseBind = bind.inverse();
        Quaternion local = localForModel(bone, modelOrientation, null);
        storeResult = inverseBind.mult(local, storeResult);

        return storeResult;
    }

    /**
     * Copy the user/animation rotation of the indexed bone.
     *
     * @param boneIndex which bone to use (&ge;0)
     * @param storeResult (modified if not null)
     * @return user rotation (either storeResult or a new instance)
     */
    public Quaternion userRotation(int boneIndex, Quaternion storeResult) {
        Validate.nonNegative(boneIndex, "bone index");

        Transform transform = transforms.get(boneIndex);
        storeResult = transform.getRotation(storeResult);

        return storeResult;
    }

    /**
     * Copy the user/animation scale of the indexed bone.
     *
     * @param boneIndex which bone to use (&ge;0)
     * @param storeResult (modified if not null)
     * @return user scale (either storeResult or a new instance)
     */
    public Vector3f userScale(int boneIndex, Vector3f storeResult) {
        Validate.nonNegative(boneIndex, "bone index");

        Transform transform = transforms.get(boneIndex);
        storeResult = transform.getScale(storeResult);

        return storeResult;
    }

    /**
     * Copy the user/animation translation of the indexed bone.
     *
     * @param boneIndex which bone to use (&ge;0)
     * @param storeResult (modified if not null)
     * @return user translation (either storeResult or a new instance)
     */
    public Vector3f userTranslation(int boneIndex, Vector3f storeResult) {
        Validate.nonNegative(boneIndex, "bone index");

        Transform transform = transforms.get(boneIndex);
        storeResult = transform.getTranslation(storeResult);

        return storeResult;
    }
    // *************************************************************************
    // JmeCloner methods

    /**
     * Convert this shallow-cloned instance into a deep-cloned one, using the
     * specified cloner and original to resolve copied fields.
     *
     * @param cloner the cloner currently cloning this control (not null)
     * @param original the control from which this control was shallow-cloned
     * (not null)
     */
    @Override
    public void cloneFields(Cloner cloner, Object original) {
        skeleton = cloner.clone(skeleton);

        int numTransforms = transforms.size();
        List<Transform> originalTransforms = transforms;
        transforms = new ArrayList<>(numTransforms);
        for (Transform t : originalTransforms) {
            Transform tClone = t.clone();
            transforms.add(tClone);
        }
    }

    /**
     * Create a shallow clone for the JME cloner.
     *
     * @return a new instance
     */
    @Override
    public Pose jmeClone() {
        try {
            Pose clone = (Pose) super.clone();
            return clone;
        } catch (CloneNotSupportedException exception) {
            throw new RuntimeException(exception);
        }
    }
    // *************************************************************************
    // private methods

    /**
     * Calculate the local rotation for the specified bone to give it the
     * specified orientation in the coordinate system of an animated spatial.
     *
     * @param bone which bone (not null, unaffected)
     * @param modelOrientation desired orientation (not null, unaffected)
     * @param storeResult (modified if not null)
     * @return rotation (either storeResult or a new instance)
     */
    private Quaternion localForModel(Bone bone, Quaternion modelOrientation,
            Quaternion storeResult) {
        assert bone != null;
        assert modelOrientation != null;
        if (storeResult == null) {
            storeResult = new Quaternion();
        }

        Bone parent = bone.getParent();
        if (parent == null) {
            storeResult.set(modelOrientation);
        } else {
            int parentIndex = skeleton.getBoneIndex(parent);
            /*
             * Factor in the orientation of the parent bone.
             */
            Quaternion parentMo = modelOrientation(parentIndex, null);
            Quaternion parentImo = parentMo.inverse();
            parentImo.mult(modelOrientation, storeResult);
        }

        return storeResult;
    }

    /**
     * Configure the specified bone and its descendents by re-targeting the
     * specified source pose. Note: recursive!
     *
     * @param bone the bone to start with (not null, unaffected)
     * @param sourcePose which source pose to re-target (not null, unaffected)
     * @param mapping skeleton mapping to use (not null, unaffected)
     */
    private void retargetBones(Bone bone, Pose sourcePose,
            SkeletonMapping mapping) {
        assert bone != null;
        assert sourcePose != null;
        assert mapping != null;

        int targetIndex = skeleton.getBoneIndex(bone);
        Transform userTransform = transforms.get(targetIndex);
        userTransform.loadIdentity();

        String targetName = bone.getName();
        BoneMapping boneMapping = mapping.get(targetName);
        if (boneMapping != null) {
            /*
             * Calculate the orientation of the source bone in model space.
             */
            String sourceName = boneMapping.getSourceName();
            int sourceIndex = sourcePose.findBone(sourceName);
            Quaternion mo = sourcePose.modelOrientation(sourceIndex, null);

            Quaternion userRotation = userForModel(targetIndex, mo, null);
            Quaternion twist = boneMapping.getTwist();
            userRotation.mult(twist, userTransform.getRotation());
        }

        List<Bone> children = bone.getChildren();
        for (Bone childBone : children) {
            retargetBones(childBone, sourcePose, mapping);
        }
    }
}
