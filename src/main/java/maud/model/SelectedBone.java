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

import com.jme3.animation.Bone;
import com.jme3.animation.BoneTrack;
import com.jme3.animation.Skeleton;
import com.jme3.math.Quaternion;
import com.jme3.math.Transform;
import com.jme3.math.Vector3f;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import jme3utilities.MyString;
import maud.Pose;

/**
 * The MVC model of the selected bone in the Maud application.
 *
 * @author Stephen Gold sgold@sonic.net
 */
public class SelectedBone implements Cloneable {
    // *************************************************************************
    // constants and loggers

    /**
     * message logger for this class
     */
    final private static Logger logger = Logger.getLogger(
            SelectedBone.class.getName());
    // *************************************************************************
    // fields

    /**
     * editable CG model containing the bone, if any (set by
     * {@link #setCgm(LoadedCGModel)})
     */
    private EditableCgm editableCgm;
    /**
     * index of the selected bone, or -1 for none selected
     */
    private int selectedIndex = -1;
    /**
     * loaded CG model containing the bone (set by
     * {@link #setCgm(LoadedCGModel)})
     */
    private LoadedCgm loadedCgm = null;
    // *************************************************************************
    // new methods exposed

    /**
     * Count how many children the selected bone has.
     *
     * @return count (&ge;0)
     */
    public int countChildren() {
        Bone bone = getBone();
        int result;
        if (bone == null) {
            result = 0;
        } else {
            List<Bone> children = bone.getChildren();
            result = children.size();
        }

        assert result >= 0 : result;
        return result;
    }

    /**
     * Deselect the selected bone, if any.
     */
    public void deselect() {
        selectedIndex = -1;
    }

    /**
     * Access the selected bone.
     *
     * @return the pre-existing instance, or null if none selected
     */
    Bone getBone() {
        Bone bone;
        if (selectedIndex == -1) {
            bone = null;
        } else {
            bone = loadedCgm.bones.getBone(selectedIndex);
        }

        return bone;
    }

    /**
     * Read the name of an indexed child of the selected bone.
     *
     * @param childIndex which child (&ge;0)
     * @return name, or null if none
     */
    public String getChildName(int childIndex) {
        assert childIndex >= 0 : childIndex;

        Bone bone = getBone();
        String name;
        if (bone == null) {
            name = null;
        } else {
            List<Bone> children = bone.getChildren();
            Bone child = children.get(childIndex);
            if (child == null) {
                name = null;
            } else {
                name = child.getName();
            }
        }

        return name;
    }

    /**
     * Read the index of the selected bone.
     *
     * @return the bone index, or -1 if none selected
     */
    public int getIndex() {
        return selectedIndex;
    }

    /**
     * Read the name of the selected bone.
     *
     * @return the name or noBone (not null)
     */
    public String getName() {
        String name;
        if (selectedIndex == -1) {
            name = SelectedSkeleton.noBone;
        } else {
            Bone bone = getBone();
            name = bone.getName();
        }

        return name;
    }

    /**
     * Read the name of the parent of the selected bone.
     *
     * @return name, or null if none
     */
    public String getParentName() {
        Bone bone = getBone();
        String name;
        if (bone == null) {
            name = null;
        } else {
            Bone parent = bone.getParent();
            if (parent == null) {
                name = null;
            } else {
                name = parent.getName();
            }
        }

        return name;
    }

    /**
     * Test whether the selected bone has a BoneTrack in the loaded animation.
     *
     * @return true if a bone is selected and it has a track, otherwise false
     */
    public boolean hasTrack() {
        BoneTrack track = loadedCgm.track.findTrack();
        if (track == null) {
            return false;
        } else {
            return true;
        }
    }

    /**
     * Test whether the selected bone is a root bone.
     *
     * @return true if it's a root, otherwise false
     */
    public boolean isRootBone() {
        Bone bone = getBone();
        boolean result;
        if (bone == null) {
            result = false;
        } else {
            Bone parent = bone.getParent();
            result = (parent == null);
        }

        return result;
    }

    /**
     * Test whether a bone is selected.
     *
     * @return true if selected, otherwise false
     */
    public boolean isSelected() {
        if (selectedIndex == -1) {
            return false;
        } else {
            return true;
        }
    }

    /**
     * Enumerate the names of all children of the selected bone.
     *
     * @return a new list
     */
    public List<String> listChildNames() {
        Bone bone = getBone();
        List<String> result;
        if (bone == null) {
            result = new ArrayList<>(0);
        } else {
            List<Bone> children = bone.getChildren();
            int numChildren = children.size();
            result = new ArrayList<>(numChildren);
            for (Bone child : children) {
                String name = child.getName();
                result.add(name);
            }
        }

        return result;
    }

    /**
     * Calculate the model orientation of the selected bone.
     *
     * @param storeResult (modified if not null)
     * @return orientation in model space (either storeResult or a new instance)
     */
    public Quaternion modelOrientation(Quaternion storeResult) {
        Pose pose = loadedCgm.pose.getPose();
        int boneIndex = getIndex();
        storeResult = pose.modelOrientation(boneIndex, storeResult);

        return storeResult;
    }

    /**
     * Calculate the model transform of the selected bone.
     *
     * @param storeResult (modified if not null)
     * @return transform (either storeResult or a new instance)
     */
    public Transform modelTransform(Transform storeResult) {
        Pose pose = loadedCgm.pose.getPose();
        int boneIndex = getIndex();
        storeResult = pose.modelTransform(boneIndex, storeResult);

        return storeResult;
    }

    /**
     * If bone controls are enabled, reset the bone rotation to identity.
     */
    public void resetRotation() {
        if (shouldEnableControls()) {
            int boneIndex = getIndex();
            editableCgm.pose.getPose().resetRotation(boneIndex);
        }
    }

    /**
     * If bone controls are enabled, reset the bone scale to identity.
     */
    public void resetScale() {
        if (shouldEnableControls()) {
            int boneIndex = getIndex();
            editableCgm.pose.getPose().resetScale(boneIndex);
        }
    }

    /**
     * If bone controls are enabled, reset the bone translation to identity.
     */
    public void resetTranslation() {
        if (shouldEnableControls()) {
            int boneIndex = getIndex();
            editableCgm.pose.getPose().resetTranslation(boneIndex);
        }
    }

    /**
     * Select the specified bone.
     *
     * @param bone which bone to select (not null)
     */
    void select(Bone bone) {
        assert bone != null;

        Skeleton skeleton = loadedCgm.bones.findSkeleton();
        int index = skeleton.getBoneIndex(bone);
        if (index != -1) {
            select(index);
        }
    }

    /**
     * Select a bone by its index.
     *
     * @param newIndex which bone to select, or -1 to deselect
     */
    public void select(int newIndex) {
        selectedIndex = newIndex;
    }

    /**
     * Select a bone by its name.
     *
     * @param name bone name or noBone (not null)
     */
    public void select(String name) {
        if (name.equals(SelectedSkeleton.noBone)) {
            deselect();

        } else {
            Skeleton skeleton = loadedCgm.bones.findSkeleton();
            int index = skeleton.getBoneIndex(name);
            if (index == -1) {
                logger.log(Level.WARNING, "Select failed: no bone named {0}.",
                        MyString.quote(name));
            } else {
                select(index);
            }
        }
    }

    /**
     * Select the first child of the selected bone.
     */
    public void selectFirstChild() {
        Bone bone = getBone();
        if (bone != null) {
            List<Bone> children = bone.getChildren();
            Bone firstChild = children.get(0);
            if (firstChild != null) {
                select(firstChild);
            }
        }
    }

    /**
     * Select the first root bone of the loaded CG model.
     */
    public void selectFirstRoot() {
        Skeleton skeleton = loadedCgm.bones.findSkeleton();
        Bone[] roots = skeleton.getRoots();
        Bone firstRoot = roots[0];
        select(firstRoot);
    }

    /**
     * Select the next bone (by index).
     */
    public void selectNext() {
        if (selectedIndex != -1) {
            ++selectedIndex;
            int numBones = loadedCgm.bones.countBones();
            if (selectedIndex >= numBones) {
                selectedIndex = 0;
            }
        }
    }

    /**
     * Select the parent of the selected bone, if any.
     */
    public void selectParent() {
        Bone bone = getBone();
        if (bone != null) {
            Bone parent = bone.getParent();
            if (parent != null) {
                select(parent);
            }
        }
    }

    /**
     * Select the previous bone (by index).
     */
    public void selectPrevious() {
        if (selectedIndex != -1) {
            --selectedIndex;
            if (selectedIndex < 0) {
                int numBones = loadedCgm.bones.countBones();
                selectedIndex = numBones - 1;
            }
        }
    }

    /**
     * Alter which CG model contains the bone.
     *
     * @param newLoaded (not null)
     */
    void setCgm(LoadedCgm newLoaded) {
        assert newLoaded != null;

        loadedCgm = newLoaded;
        if (newLoaded instanceof EditableCgm) {
            editableCgm = (EditableCgm) newLoaded;
        } else {
            editableCgm = null;
        }
    }

    /**
     * If bone controls are enabled, set the bone rotation based on the loaded
     * animation.
     */
    public void setRotationToAnimation() {
        if (shouldEnableControls()) {
            int boneIndex = getIndex();
            editableCgm.pose.setRotationToAnimation(boneIndex);
        }
    }

    /**
     * If bone controls are enabled, set the bone scale based on the loaded
     * animation.
     */
    public void setScaleToAnimation() {
        if (shouldEnableControls()) {
            int boneIndex = getIndex();
            editableCgm.pose.setScaleToAnimation(boneIndex);
        }
    }

    /**
     * If bone controls are enabled, set the bone translation based on the
     * loaded animation.
     */
    public void setTranslationToAnimation() {
        if (shouldEnableControls()) {
            int boneIndex = getIndex();
            editableCgm.pose.setTranslationToAnimation(boneIndex);
        }
    }

    /**
     * Test whether the GUI controls for the selected bone should be enabled.
     *
     * @return true if controls should be enabled, otherwise false
     */
    public boolean shouldEnableControls() {
        if (!isSelected()) {
            return false;
        } else if (loadedCgm.animation.isMoving()) {
            return false;
        } else if (loadedCgm.animation.isRetargetedPose()) {
            return false;
        } else {
            return true;
        }
    }

    /**
     * Calculate the animation/user rotation of the selected bone.
     *
     * @param storeResult (modified if not null)
     * @return user rotation (either storeResult or a new instance)
     */
    public Quaternion userRotation(Quaternion storeResult) {
        Pose pose = loadedCgm.pose.getPose();
        int boneIndex = getIndex();
        storeResult = pose.userRotation(boneIndex, storeResult);

        return storeResult;
    }

    /**
     * Calculate the animation/user scale of the selected bone.
     *
     * @param storeResult (modified if not null)
     * @return user scale (either storeResult or a new instance)
     */
    public Vector3f userScale(Vector3f storeResult) {
        Pose pose = loadedCgm.pose.getPose();
        int boneIndex = getIndex();
        storeResult = pose.userScale(boneIndex, storeResult);

        return storeResult;
    }

    /**
     * Calculate the animation/user translation of the selected bone.
     *
     * @param storeResult (modified if not null)
     * @return user translation (either storeResult or a new instance)
     */
    public Vector3f userTranslation(Vector3f storeResult) {
        Pose pose = loadedCgm.pose.getPose();
        int boneIndex = getIndex();
        storeResult = pose.userTranslation(boneIndex, storeResult);

        return storeResult;
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
    public SelectedBone clone() throws CloneNotSupportedException {
        SelectedBone clone = (SelectedBone) super.clone();
        return clone;
    }
}
