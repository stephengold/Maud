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

import com.jme3.animation.Skeleton;
import com.jme3.math.Quaternion;
import com.jme3.math.Transform;
import com.jme3.math.Vector3f;
import com.jme3.util.clone.Cloner;
import com.jme3.util.clone.JmeCloneable;
import java.util.logging.Logger;
import jme3utilities.Validate;
import maud.Pose;

/**
 * The displayed pose of a particular CG model in the Maud application.
 *
 * @author Stephen Gold sgold@sonic.net
 */
public class DisplayedPose implements JmeCloneable {
    // *************************************************************************
    // constants and loggers

    /**
     * message logger for this class
     */
    final private static Logger logger = Logger.getLogger(
            DisplayedPose.class.getName());
    // *************************************************************************
    // fields

    /**
     * the pose, including user transforms and skeleton
     */
    private Pose pose = new Pose(null);
    /**
     * loaded CG model that is in the pose (set by
     * {@link #setCgm(LoadedCGModel)})
     */
    private LoadedCGModel loadedCgm = null;
    // *************************************************************************
    // new methods exposed

    /**
     * Copy the user transform of the indexed bone.
     *
     * @param boneIndex which bone to use (&ge;0)
     * @param storeResult (modified if not null)
     * @return user transform (either storeResult or a new instance)
     */
    public Transform copyTransform(int boneIndex, Transform storeResult) {
        Validate.nonNegative(boneIndex, "bone index");
        Transform result = pose.copyTransform(boneIndex, storeResult);
        return result;
    }

    /**
     * Access the pose.
     *
     * @return the pre-existing instance (not null)
     */
    public Pose getPose() {
        assert pose != null;
        return pose;
    }

    /**
     * Calculate the model transform of the indexed bone.
     *
     * @param boneIndex which bone to use (&ge;0)
     * @param storeResult (modified if not null)
     * @return transform in model coordinates (either storeResult or a new
     * instance)
     */
    public Transform modelTransform(int boneIndex, Transform storeResult) {
        Validate.nonNegative(boneIndex, "bone index");
        Transform result = pose.modelTransform(boneIndex, storeResult);
        return result;
    }

    /**
     * Reset the pose to bind pose.
     *
     * @param skeleton (may be null, alias created)
     */
    void resetToBind(Skeleton skeleton) {
        pose = new Pose(skeleton);
    }

    /**
     * Alter which CG model is in the pose. (Invoked only during initialization
     * and cloning.)
     *
     * @param newLoaded (not null)
     */
    void setCgm(LoadedCGModel newLoaded) {
        assert newLoaded != null;
        loadedCgm = newLoaded;
    }

    /**
     * Alter the rotation of the indexed bone to match the loaded animation.
     *
     * @param boneIndex which bone to rotate (&ge;0)
     */
    void setRotationToAnimation(int boneIndex) {
        assert boneIndex >= 0 : boneIndex;

        Transform animT = loadedCgm.animation.boneTransform(boneIndex, null);
        Quaternion animQ = animT.getRotation();
        pose.setRotation(boneIndex, animQ);
    }

    /**
     * Alter the scale of the indexed bone to match the loaded animation.
     *
     * @param boneIndex which bone to scale (&ge;0)
     */
    void setScaleToAnimation(int boneIndex) {
        Validate.nonNegative(boneIndex, "bone index");

        Transform animT = loadedCgm.animation.boneTransform(boneIndex, null);
        Vector3f animV = animT.getScale();
        pose.setScale(boneIndex, animV);
    }

    /**
     * Alter the pose to match the loaded animation.
     */
    public void setToAnimation() {
        int boneCount = pose.countBones();

        // TODO for each root bone ...
        for (int boneIndex = 0; boneIndex < boneCount; boneIndex++) {
            Transform transform;
            transform = loadedCgm.animation.boneTransform(boneIndex, null);
            pose.set(boneIndex, transform);
        }
    }

    /**
     * Alter the translation of the indexed bone to match the loaded animation.
     *
     * @param boneIndex which bone to translate (&ge;0)
     */
    void setTranslationToAnimation(int boneIndex) {
        Validate.nonNegative(boneIndex, "bone index");

        Transform animT = loadedCgm.animation.boneTransform(boneIndex, null);
        Vector3f animV = animT.getTranslation();
        pose.setTranslation(boneIndex, animV);
    }
    // *************************************************************************
    // JmeCloner methods

    /**
     * Convert this shallow-cloned instance into a deep-cloned one, using the
     * specified cloner and original to resolve copied fields.
     *
     * @param cloner the cloner currently cloning this control
     * @param original the control from which this control was shallow-cloned
     */
    @Override
    public void cloneFields(Cloner cloner, Object original) {
        pose = cloner.clone(pose);
    }

    /**
     * Create a shallow clone for the JME cloner.
     *
     * @return a new instance
     */
    @Override
    public DisplayedPose jmeClone() {
        try {
            DisplayedPose clone = (DisplayedPose) super.clone();
            return clone;
        } catch (CloneNotSupportedException exception) {
            throw new RuntimeException(exception);
        }
    }
}
