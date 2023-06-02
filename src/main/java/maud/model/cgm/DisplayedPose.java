/*
 Copyright (c) 2017-2023, Stephen Gold
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
package maud.model.cgm;

import com.jme3.anim.Armature;
import com.jme3.animation.Skeleton;
import com.jme3.math.Quaternion;
import com.jme3.math.Transform;
import com.jme3.math.Vector3f;
import com.jme3.util.clone.Cloner;
import com.jme3.util.clone.JmeCloneable;
import java.util.logging.Logger;
import jme3utilities.Validate;
import jme3utilities.math.MyMath;
import jme3utilities.wes.Pose;

/**
 * MVC model of a displayed pose in the Editor screen.
 *
 * @author Stephen Gold sgold@sonic.net
 */
public class DisplayedPose implements JmeCloneable {
    // *************************************************************************
    // constants and loggers

    /**
     * message logger for this class
     */
    final private static Logger logger
            = Logger.getLogger(DisplayedPose.class.getName());
    // *************************************************************************
    // fields

    /**
     * false &rarr; update displayed pose when animation time changes, true
     * &rarr; don't update
     */
    private boolean frozenFlag = false;
    /**
     * C-G model holding the pose (set by {@link #setCgm(Cgm)})
     */
    private Cgm cgm = null;
    /**
     * the pose, including a skeleton and a user/animation transform for each
     * bone
     */
    private Pose pose;
    // *************************************************************************
    // new methods exposed

    /**
     * Access the pose.
     *
     * @return the pre-existing instance (not null)
     */
    public Pose get() {
        assert pose != null;
        return pose;
    }

    /**
     * Test whether the pose is frozen.
     *
     * @return false if updated when animation time changes, otherwise true
     */
    public boolean isFrozen() {
        return frozenFlag;
    }

    /**
     * Change skeletons and reset the pose to bind pose.
     *
     * @param skeleton the Armature or Skeleton (alias created) or null
     */
    void resetToBind(Object skeleton) {
        if (skeleton instanceof Armature) {
            this.pose = new Pose((Armature) skeleton);
            pose.setToBind();
        } else {
            this.pose = new Pose((Skeleton) skeleton);
        }
    }

    /**
     * Alter which C-G model displays the pose. (Invoked only during
     * initialization and cloning.)
     *
     * @param newCgm (not null, alias created)
     */
    void setCgm(Cgm newCgm) {
        assert newCgm != null;
        assert newCgm.getPose() == this;

        this.cgm = newCgm;
    }

    /**
     * Alter whether the pose is frozen. When unfreezing, set the pose using the
     * current animation.
     *
     * @param newSetting false &rarr; update pose automatically when the
     * animation time changes, true &rarr; don't update
     */
    public void setFrozen(boolean newSetting) {
        if (frozenFlag && !newSetting) {
            setToAnimation();
        }
        this.frozenFlag = newSetting;
    }

    /**
     * Alter the user/animation rotation of the indexed bone.
     *
     * @param boneIndex which bone to rotate (&ge;0)
     * @param userRotation the desired rotation (not null, unaffected)
     */
    void setRotation(int boneIndex, Quaternion userRotation) {
        assert boneIndex >= 0 : boneIndex;
        pose.setRotation(boneIndex, userRotation);
    }

    /**
     * Alter the scale of the indexed bone to match the loaded animation.
     *
     * @param boneIndex which bone to scale (&ge;0)
     */
    void setScaleToAnimation(int boneIndex) {
        assert boneIndex >= 0 : boneIndex;

        Transform animT = cgm.getAnimation().boneTransform(boneIndex, null);
        Vector3f animV = animT.getScale(); // alias
        pose.setScale(boneIndex, animV);
    }

    /**
     * Alter the pose to match the loaded animation.
     */
    public void setToAnimation() {
        Transform transform = new Transform();
        for (int boneIndex : pose.preOrderIndices()) {
            cgm.getAnimation().boneTransform(boneIndex, transform);
            pose.set(boneIndex, transform);
        }

        if (cgm.getPlay().isPinned()) {
            int[] rootBones = pose.rootBoneIndices();
            for (int boneIndex : rootBones) {
                pose.resetTranslation(boneIndex);
            }
        }
    }

    /**
     * Alter the translation of the indexed bone to match the loaded animation.
     *
     * @param boneIndex which bone to translate (&ge;0)
     */
    void setTranslationToAnimation(int boneIndex) {
        assert boneIndex >= 0 : boneIndex;

        Transform animT = cgm.getAnimation().boneTransform(boneIndex, null);
        Vector3f animV = animT.getTranslation(); // alias
        pose.setTranslation(boneIndex, animV);
    }

    /**
     * Toggle whether the pose is frozen.
     */
    public void toggleFrozen() {
        setFrozen(!frozenFlag);
    }

    /**
     * Calculate the world location of the indexed bone in the scene view.
     *
     * @param boneIndex which bone to use (&ge;0)
     * @param storeResult storage for the result (modified if not null)
     * @return a location vector (in world coordinates, either storeResult or a
     * new instance, not null)
     */
    public Vector3f worldLocation(int boneIndex, Vector3f storeResult) {
        Validate.nonNegative(boneIndex, "bone index");
        Vector3f result = (storeResult == null) ? new Vector3f() : storeResult;

        Transform transform = cgm.getSceneView().worldTransform(null);
        Vector3f modelLocation = pose.modelLocation(boneIndex, null);
        MyMath.transform(transform, modelLocation, result);

        return result;
    }
    // *************************************************************************
    // JmeCloneable methods

    /**
     * Don't use this method; use a {@link com.jme3.util.clone.Cloner} instead.
     *
     * @return never
     * @throws CloneNotSupportedException always
     */
    @Override
    public DisplayedPose clone() throws CloneNotSupportedException {
        super.clone();
        throw new CloneNotSupportedException("use a cloner");
    }

    /**
     * Callback from {@link com.jme3.util.clone.Cloner} to convert this
     * shallow-cloned instance into a deep-cloned one, using the specified
     * cloner and original to resolve copied fields.
     *
     * @param cloner the cloner currently cloning this control
     * @param original the control from which this control was shallow-cloned
     */
    @Override
    public void cloneFields(Cloner cloner, Object original) {
        this.pose = cloner.clone(pose);
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
