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
package maud.model;

import com.jme3.animation.Animation;
import com.jme3.animation.Bone;
import com.jme3.animation.BoneTrack;
import com.jme3.animation.Skeleton;
import com.jme3.math.Quaternion;
import com.jme3.math.Transform;
import com.jme3.math.Vector3f;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;
import jme3utilities.Misc;
import jme3utilities.MyAnimation;
import jme3utilities.MySkeleton;
import jme3utilities.Validate;
import maud.Maud;

/**
 * A displayed pose in the Maud application.
 *
 * @author Stephen Gold sgold@sonic.net
 */
public class Pose implements Cloneable {
    // *************************************************************************
    // constants and loggers

    /**
     * message logger for this class
     */
    final private static Logger logger = Logger.getLogger(Pose.class.getName());
    // *************************************************************************
    // fields

    /**
     * user transforms that describe the pose, one for each bone
     */
    private List<Transform> transforms = new ArrayList<>(108);
    // *************************************************************************
    // new methods exposed

    /**
     * Capture the pose as an animation. The new animation has a zero duration,
     * a single keyframe at t=0, and all its tracks are BoneTracks.
     *
     * @parm animationName name for the new animation (not null)
     * @return a new instance
     */
    Animation capture(String animationName) {
        assert animationName != null;
        /*
         * Start with an empty animation.
         */
        float duration = 0f;
        Animation result = new Animation(animationName, duration);
        /*
         * Add a BoneTrack for each bone that's not in bind pose.
         */
        int numBones = Maud.model.cgm.bones.countBones();
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
     * Copy the user transform of the indexed bone in this pose.
     *
     * @param boneIndex which bone to use
     * @param storeResult (modified if not null)
     * @return user transform (either storeResult or a new instance)
     */
    public Transform copyTransform(int boneIndex, Transform storeResult) {
        if (storeResult == null) {
            storeResult = new Transform();
        }

        Transform transform = transforms.get(boneIndex);
        storeResult.set(transform);

        return storeResult;
    }

    /**
     * Count the bone transforms in this pose.
     *
     * @return count (&ge;0)
     */
    public int countTransforms() {
        int count = transforms.size();
        assert count >= 0 : count;
        return count;
    }

    /**
     * Calculate the model transform of the indexed bone in this pose.
     *
     * @param boneIndex which bone to use
     * @param storeResult (modified if not null)
     * @return transform in model coordinates (either storeResult or a new
     * instance)
     */
    public Transform modelTransform(int boneIndex, Transform storeResult) {
        if (storeResult == null) {
            storeResult = new Transform();
        }
        /*
         * Start with the bone's bind transform.
         */
        Skeleton skeleton = Maud.model.cgm.bones.getSkeleton();
        Bone bone = skeleton.getBone(boneIndex);
        Transform local = MySkeleton.copyBindTransform(bone, null);
        /*
         * Apply the user transform in a simple (yet peculiar) way
         * to obtain the bone's local transform.
         */
        Transform user = copyTransform(boneIndex, null);
        local.getTranslation().addLocal(user.getTranslation());
        local.getRotation().multLocal(user.getRotation());
        local.getScale().multLocal(user.getScale());

        Bone parentBone = bone.getParent();
        if (parentBone == null) {
            /*
             * For a root bone, the bone's model transform is simply
             * its local transform.
             */
            storeResult.set(local);

        } else {
            int parentIndex = skeleton.getBoneIndex(parentBone);
            Transform parent = modelTransform(parentIndex, null);
            /*
             * Apply the parent's model transform in a different (and even more
             * peculiar) way to obtain the bone's model transform.
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
     * @param boneIndex which bone
     */
    public void resetRotation(int boneIndex) {
        Transform transform = transforms.get(boneIndex);
        Quaternion rotation = transform.getRotation();
        rotation.loadIdentity();
    }

    /**
     * Reset the scale of the indexed bone to identity.
     *
     * @param boneIndex which bone
     */
    public void resetScale(int boneIndex) {
        Transform transform = transforms.get(boneIndex);
        Vector3f scale = transform.getScale();
        scale.set(1f, 1f, 1f);
    }

    /**
     * Reset this pose to bind pose.
     */
    public void resetToBind() {
        int boneCount = Maud.model.cgm.bones.countBones();
        transforms.clear();
        for (int boneIndex = 0; boneIndex < boneCount; boneIndex++) {
            Transform transform = new Transform();
            transforms.add(transform);
        }
    }

    /**
     * Reset the translation of the indexed bone to zero.
     *
     * @param boneIndex which bone
     */
    public void resetTranslation(int boneIndex) {
        Transform transform = transforms.get(boneIndex);
        Vector3f translation = transform.getTranslation();
        translation.zero();
    }

    /**
     * Alter the rotation of the indexed bone.
     *
     * @param boneIndex which bone to rotate
     * @param rotation (not null, unaffected)
     */
    public void setRotation(int boneIndex, Quaternion rotation) {
        Validate.nonNull(rotation, "rotation");

        Transform boneTransform = transforms.get(boneIndex);
        boneTransform.setRotation(rotation);
    }

    /**
     * Alter the rotation of the indexed bone to match the loaded animation.
     *
     * @param boneIndex which bone to scale
     */
    public void setRotationToAnimation(int boneIndex) {
        Transform poseT = transforms.get(boneIndex);
        Transform animT = Maud.model.cgm.animation.boneTransform(boneIndex,
                null);
        Quaternion animQ = animT.getRotation();
        poseT.setRotation(animQ);
    }

    /**
     * Alter the scale of the indexed bone.
     *
     * @param boneIndex which bone to scale
     * @param scale (not null, unaffected)
     */
    public void setScale(int boneIndex, Vector3f scale) {
        Validate.nonNull(scale, "scale");
        Validate.positive(scale.x, "x scale");
        Validate.positive(scale.y, "y scale");
        Validate.positive(scale.z, "z scale");

        Transform boneTransform = transforms.get(boneIndex);
        boneTransform.setScale(scale);
    }

    /**
     * Alter the scale of the indexed bone to match the loaded animation.
     *
     * @param boneIndex which bone to scale
     */
    public void setScaleToAnimation(int boneIndex) {
        Transform poseT = transforms.get(boneIndex);
        Transform animT = Maud.model.cgm.animation.boneTransform(boneIndex,
                null);
        Vector3f animV = animT.getScale();
        poseT.setScale(animV);
    }

    /**
     * Alter the transforms to match the loaded animation.
     */
    public void setToAnimation() {
        int boneCount = Maud.model.cgm.bones.countBones();
        int numTransforms = countTransforms();
        assert numTransforms == boneCount : numTransforms;

        for (int boneIndex = 0; boneIndex < boneCount; boneIndex++) {
            Transform transform = transforms.get(boneIndex);
            Maud.model.cgm.animation.boneTransform(boneIndex, transform);
        }
    }

    /**
     * Alter the translation of the indexed bone.
     *
     * @param boneIndex which bone to translate
     * @param translation (not null, unaffected)
     */
    public void setTranslation(int boneIndex, Vector3f translation) {
        Validate.nonNull(translation, "translation");

        Transform boneTransform = transforms.get(boneIndex);
        boneTransform.setTranslation(translation);
    }

    /**
     * Alter the translation of the indexed bone to match the loaded animation.
     *
     * @param boneIndex which bone to scale
     */
    public void setTranslationToAnimation(int boneIndex) {
        Transform poseT = transforms.get(boneIndex);
        Transform animT = Maud.model.cgm.animation.boneTransform(boneIndex,
                null);
        Vector3f animV = animT.getTranslation();
        poseT.setTranslation(animV);
    }
    // *************************************************************************
    // Object methods

    /**
     * Create a deep copy of this object.
     *
     * @return a new object, equivalent to this one
     * @throws CloneNotSupportedException if superclass isn't cloneable
     */
    @Override
    public Object clone() throws CloneNotSupportedException {
        Pose clone = (Pose) super.clone();

        int numTransforms = transforms.size();
        clone.transforms = new ArrayList<>(numTransforms);
        for (Transform t : transforms) {
            Transform tClone = t.clone();
            clone.transforms.add(tClone);
        }

        return clone;
    }
}
