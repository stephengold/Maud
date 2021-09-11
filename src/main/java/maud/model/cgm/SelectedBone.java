/*
 Copyright (c) 2017-2021, Stephen Gold
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
import com.jme3.anim.Joint;
import com.jme3.anim.SkinningControl;
import com.jme3.animation.Bone;
import com.jme3.animation.Skeleton;
import com.jme3.animation.SkeletonControl;
import com.jme3.bullet.animation.BoneLink;
import com.jme3.math.Quaternion;
import com.jme3.math.Transform;
import com.jme3.math.Vector3f;
import com.jme3.scene.Node;
import com.jme3.scene.Spatial;
import com.jme3.scene.control.AbstractControl;
import java.util.ArrayList;
import java.util.BitSet;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import jme3utilities.InfluenceUtil;
import jme3utilities.MyString;
import jme3utilities.Validate;
import jme3utilities.math.MyQuaternion;
import jme3utilities.math.MyVector3f;
import jme3utilities.wes.Pose;
import maud.Maud;
import maud.MaudUtil;
import maud.tool.EditorTools;

/**
 * The MVC model of the selected bone in a loaded C-G model.
 *
 * @author Stephen Gold sgold@sonic.net
 */
public class SelectedBone implements Cloneable {
    // *************************************************************************
    // constants and loggers

    /**
     * message logger for this class
     */
    final private static Logger logger
            = Logger.getLogger(SelectedBone.class.getName());
    // *************************************************************************
    // fields

    /**
     * C-G model containing the bone (set by {@link #setCgm(Cgm)})
     */
    private Cgm cgm = null;
    /**
     * editable C-G model, if any, containing the bone (set by
     * {@link #setCgm(Cgm)})
     */
    private EditableCgm editableCgm = null;
    /**
     * index of the selected bone in the selected skeleton, or noBoneIndex for
     * none selected
     */
    private int selectedIndex = SelectedSkeleton.noBoneIndex;
    // *************************************************************************
    // new methods exposed

    /**
     * Read the name of an indexed child of the selected bone.
     *
     * @param childIndex which child (&ge;0)
     * @return name, or null if none
     */
    public String childName(int childIndex) {
        Validate.nonNegative(childIndex, "child index");

        Object bone = get();
        Object child = null;
        if (bone instanceof Bone) {
            List<Bone> children = ((Bone) bone).getChildren();
            child = children.get(childIndex);

        } else if (bone instanceof Joint) {
            List<Joint> children = ((Joint) bone).getChildren();
            child = children.get(childIndex);
        }

        String result = null;
        if (child != null) {
            result = getBoneName(child);
        }

        return result;
    }

    /**
     * Create an attachments node for the selected bone.
     *
     * @return the new instance
     */
    Node createAttachments() {
        assert !hasAttachmentsNode();

        AbstractControl sc = cgm.getSkeleton().getSkeletonControl();
        String boneName = name();
        Node result;
        if (sc instanceof SkeletonControl) {
            result = ((SkeletonControl) sc).getAttachmentsNode(boneName);
        } else {
            result = ((SkinningControl) sc).getAttachmentsNode(boneName);
        }

        assert hasAttachmentsNode();
        return result;
    }

    /**
     * Count how many children the selected bone has.
     *
     * @return count (&ge;0)
     */
    public int countChildren() {
        int result = 0;

        Object bone = get();
        if (bone != null) {
            result = MaudUtil.countBoneChildren(bone);
        }

        assert result >= 0 : result;
        return result;
    }

    /**
     * Deselect the selected bone, if any.
     */
    public void deselect() {
        selectedIndex = SelectedSkeleton.noBoneIndex;
    }

    /**
     * Access the selected Bone or Joint.
     *
     * @return the pre-existing instance, or null if none selected
     */
    Object get() {
        Object result;
        if (selectedIndex == SelectedSkeleton.noBoneIndex) {
            result = null;
        } else {
            result = cgm.getSkeleton().getBone(selectedIndex);
        }

        return result;
    }

    /**
     * Test whether the selected bone has an attachments Node.
     *
     * @return true if the bone has a Node, otherwise false
     */
    public boolean hasAttachmentsNode() {
        boolean result = false;
        Object bone = get();
        if (bone != null) {
            Node attachmentsNode = MaudUtil.getBoneAttachments(bone);
            if (attachmentsNode != null) {
                result = true;
            }
        }

        return result;
    }

    /**
     * Test whether the selected bone has a BoneTrack in the loaded animation.
     *
     * @return true if a bone is selected and it has a track, otherwise false
     */
    public boolean hasTrack() {
        boolean result = false;
        int boneIndex = index();
        if (boneIndex >= 0) {
            Object track = cgm.getAnimation().findTrackForBone(boneIndex);
            if (track != null) {
                result = true;
            }
        }

        return result;
    }

    /**
     * Read the index of the selected bone in the selected skeleton.
     *
     * @return the bone index, or noBoneIndex if none selected
     */
    public int index() {
        assert selectedIndex >= SelectedSkeleton.noBoneIndex : selectedIndex;
        return selectedIndex;
    }

    /**
     * Test whether the selected bone influences (directly or indirectly) any
     * attachment nodes.
     *
     * @return true if a bone influences a node, otherwise false
     */
    public boolean influencesAttachmentsNode() {
        boolean result = hasAttachmentsNode();
        if (!result) {
            /*
             * Test descendent bones/joints.
             */
            Object skeleton = cgm.getSkeleton().find();
            int numBones = cgm.getSkeleton().countBones();
            for (int iBone = 0; iBone < numBones; iBone++) {
                Node attachmentNode = null;

                if (MaudUtil.descendsFrom(iBone, selectedIndex, skeleton)) {
                    attachmentNode = MaudUtil.getAttachments(skeleton, iBone);
                }

                if (attachmentNode != null) {
                    result = true;
                    break;
                }
            }
        }

        return result;
    }

    /**
     * Test whether the selected bone animates (directly or indirectly) any mesh
     * vertices.
     *
     * @return true if bone has influence, otherwise false
     */
    public boolean influencesVertices() {
        boolean result = false;
        if (isSelected()) {
            SelectedSkeleton selectedSkeleton = cgm.getSkeleton();
            Spatial subtree = selectedSkeleton.findSpatial();
            Object skeleton = selectedSkeleton.find();
            BitSet bones;
            if (skeleton instanceof Armature) {
                bones = InfluenceUtil.addAllInfluencers(subtree,
                        (Armature) skeleton);
            } else {
                bones = InfluenceUtil.addAllInfluencers(subtree,
                        (Skeleton) skeleton);
            }
            result = bones.get(selectedIndex);
        }

        return result;
    }

    /**
     * Test whether the selected bone is linked in the selected ragdoll.
     *
     * @return true if linked, otherwise false
     */
    public boolean isLinked() {
        if (selectedIndex == SelectedSkeleton.noBoneIndex) {
            return false;
        } else {
            SelectedRagdoll ragdoll = cgm.getRagdoll();
            if (ragdoll.isSelected()) {
                String name = name();
                BoneLink link = cgm.getRagdoll().findBoneLink(name);
                if (link == null) {
                    return false;
                } else {
                    return true;
                }
            } else {
                return false;
            }
        }
    }

    /**
     * Test whether the selected bone is a root bone.
     *
     * @return true if it's a root, otherwise false
     */
    public boolean isRootBone() {
        Object bone = get();
        Object parent = getBoneParent(bone);
        boolean result = (parent == null);

        return result;
    }

    /**
     * Test whether a bone is selected.
     *
     * @return true if selected, otherwise false
     */
    public boolean isSelected() {
        if (selectedIndex == SelectedSkeleton.noBoneIndex) {
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
        List<String> result;
        Object bone = get();
        if (bone == null) {
            result = new ArrayList<>(0);

        } else if (bone instanceof Bone) {
            List<Bone> children = ((Bone) bone).getChildren();
            int numChildren = children.size();
            result = new ArrayList<>(numChildren);
            for (Bone child : children) {
                String name = child.getName();
                result.add(name);
            }

        } else {
            List<Joint> children = ((Joint) bone).getChildren();
            int numChildren = children.size();
            result = new ArrayList<>(numChildren);
            for (Joint child : children) {
                String name = child.getName();
                result.add(name);
            }
        }

        return result;
    }

    /**
     * Calculate the model orientation of the selected bone.
     *
     * @param storeResult storage for the result (modified if not null)
     * @return the orientation in model space (either storeResult or a new
     * instance, not null)
     */
    public Quaternion modelOrientation(Quaternion storeResult) {
        Quaternion result
                = (storeResult == null) ? new Quaternion() : storeResult;

        Pose pose = cgm.getPose().get();
        int boneIndex = index();
        pose.modelOrientation(boneIndex, result);

        return result;
    }

    /**
     * Calculate the model transform of the selected bone.
     *
     * @param storeResult storage for the result (modified if not null)
     * @return the Transform (either storeResult or a new instance, not null)
     */
    public Transform modelTransform(Transform storeResult) {
        Transform result
                = (storeResult == null) ? new Transform() : storeResult;

        Pose pose = cgm.getPose().get();
        int boneIndex = index();
        pose.modelTransform(boneIndex, result);

        return result;
    }

    /**
     * Read the name of the selected bone.
     *
     * @return the name or noBone (not null)
     */
    public String name() {
        String result;
        if (selectedIndex == SelectedSkeleton.noBoneIndex) {
            result = SelectedSkeleton.noBone;
        } else {
            Object bone = get();
            result = getBoneName(bone);
        }

        return result;
    }

    /**
     * Find the index of the parent of the selected bone.
     *
     * @return bone index, or noBoneIndex if none
     */
    public int parentIndex() {
        int result = SelectedSkeleton.noBoneIndex;
        Object bone = get();
        if (bone != null) {
            Object parent = getBoneParent(bone);
            if (parent != null) {
                Object skeleton = cgm.getSkeleton().find();
                result = findBoneIndex(skeleton, parent);
            }
        }

        return result;
    }

    /**
     * Read the name of the parent of the selected bone.
     *
     * @return name, or null if none
     */
    public String parentName() {
        String result = null;
        Object bone = get();
        if (bone != null) {
            Object parent = getBoneParent(bone);
            if (parent != null) {
                result = getBoneName(parent);
            }
        }

        return result;
    }

    /**
     * If bone controls are enabled, reset the bone rotation to identity.
     */
    public void resetRotation() {
        if (shouldEnableControls()) {
            int boneIndex = index();
            editableCgm.getPose().get().resetRotation(boneIndex);
        }
    }

    /**
     * If bone controls are enabled, reset the bone scale to identity.
     */
    public void resetScale() {
        if (shouldEnableControls()) {
            int boneIndex = index();
            editableCgm.getPose().get().resetScale(boneIndex);
        }
    }

    /**
     * If bone controls are enabled, reset the bone translation to identity.
     */
    public void resetTranslation() {
        if (shouldEnableControls()) {
            int boneIndex = index();
            editableCgm.getPose().get().resetTranslation(boneIndex);
        }
    }

    /**
     * Select the specified Bone or Joint.
     *
     * @param bone which Bone or Joint to select (not null)
     */
    void select(Object bone) {
        assert bone != null;

        Object skeleton = cgm.getSkeleton().find();
        int index = findBoneIndex(skeleton, bone);
        if (index >= 0) {
            select(index);
        }
    }

    /**
     * Select a bone by its index.
     *
     * @param newIndex which bone to select, or noBoneIndex to deselect
     */
    public void select(int newIndex) {
        Validate.inRange(newIndex, "bone index", SelectedSkeleton.noBoneIndex,
                Short.MAX_VALUE);
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
            Object skeleton = cgm.getSkeleton().find();
            int index = findBoneIndex(skeleton, name);
            if (index == SelectedSkeleton.noBoneIndex) {
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
        Object bone = get();
        if (bone instanceof Bone) {
            List<Bone> children = ((Bone) bone).getChildren();
            Bone firstChild = children.get(0);
            if (firstChild != null) {
                select(firstChild);
            }

        } else if (bone instanceof Joint) {
            List<Joint> children = ((Joint) bone).getChildren();
            Joint firstChild = children.get(0);
            if (firstChild != null) {
                select(firstChild);
            }
        }
    }

    /**
     * Select the first root bone of the loaded C-G model.
     */
    public void selectFirstRoot() {
        Object[] roots = cgm.getSkeleton().listRoots();
        if (roots.length > 0) {
            Object firstRoot = roots[0];
            select(firstRoot);
        }
    }

    /**
     * Select the next bone (by index).
     */
    public void selectNext() {
        if (selectedIndex >= 0) {
            ++selectedIndex;
            int numBones = cgm.getSkeleton().countBones();
            if (selectedIndex >= numBones) {
                selectedIndex = 0;
            }
        }
    }

    /**
     * Select the parent of the selected bone, if any.
     */
    public void selectParent() {
        Object bone = get();
        Object parent = getBoneParent(bone);
        if (parent != null) {
            select(parent);
        }
    }

    /**
     * Select the previous bone (by index).
     */
    public void selectPrevious() {
        if (selectedIndex >= 0) {
            --selectedIndex;
            if (selectedIndex < 0) {
                int numBones = cgm.getSkeleton().countBones();
                selectedIndex = numBones - 1;
            }
        }
    }

    /**
     * Select a track that has the selected Bone or Joint as its target.
     */
    public void selectTrack() {
        if (selectedIndex >= 0) {
            Object track = cgm.getAnimation().findTrackForBone(selectedIndex);
            if (track != null) {
                cgm.getTrack().select(track);
                EditorTools.select("track");
            }
        }
    }

    /**
     * Alter which C-G model contains the selected bone. (Invoked only during
     * initialization and cloning.)
     *
     * @param newCgm (not null, aliases created)
     */
    void setCgm(Cgm newCgm) {
        assert newCgm != null;
        assert newCgm.getBone() == this;

        cgm = newCgm;
        if (newCgm instanceof EditableCgm) {
            editableCgm = (EditableCgm) newCgm;
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
            int boneIndex = index();
            Transform animT = cgm.getAnimation().boneTransform(boneIndex, null);
            Quaternion animQ = animT.getRotation();
            editableCgm.getPose().setRotation(boneIndex, animQ);
        }
    }

    /**
     * Mirror the user rotation of the source bone along the link-tool axis.
     */
    public void setRotationToMirror() {
        if (shouldEnableControls()) {
            Cgm source = Maud.getModel().getSource();
            int sbIndex = source.getBone().index();
            Pose sourcePose = source.getPose().get();
            Quaternion sourceRotation = sourcePose.modelOrientation(sbIndex, null);

            int axisIndex = Maud.getModel().getMisc().linkToolAxis();
            Transform model = new Transform();
            MyQuaternion.mirrorAxis(sourceRotation, axisIndex,
                    model.getRotation());

            int tbIndex = index();
            Quaternion user = editableCgm.getPose().get().userForModel(tbIndex, model.getRotation(), null);
            editableCgm.getPose().setRotation(tbIndex, user);
        }
    }

    /**
     * If bone controls are enabled, set the bone scale based on the loaded
     * animation.
     */
    public void setScaleToAnimation() {
        if (shouldEnableControls()) {
            int boneIndex = index();
            editableCgm.getPose().setScaleToAnimation(boneIndex);
        }
    }

    /**
     * If bone controls are enabled, set the bone translation based on the
     * loaded animation.
     */
    public void setTranslationToAnimation() {
        if (shouldEnableControls()) {
            int boneIndex = index();
            editableCgm.getPose().setTranslationToAnimation(boneIndex);
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
        } else if (cgm.getAnimation().isMoving()) {
            return false;
        } else if (cgm.getAnimation().isRetargetedPose()) {
            return false;
        } else {
            return true;
        }
    }

    /**
     * Snap one axis-angle of the bone's user/animation rotation.
     *
     * @param axisIndex which axis: 0&rarr;X, 1&rarr;Y, 2&rarr;Z
     */
    public void snapRotation(int axisIndex) {
        Validate.inRange(axisIndex, "axis index", MyVector3f.firstAxis,
                MyVector3f.lastAxis);

        if (shouldEnableControls()) {
            Quaternion userRotation = userRotation(null);
            MyQuaternion.snapLocal(userRotation, axisIndex);
            int boneIndex = index();
            editableCgm.getPose().setRotation(boneIndex, userRotation);
        }
    }

    /**
     * Calculate the animation/user rotation of the selected bone.
     *
     * @param storeResult storage for the result (modified if not null)
     * @return the user rotation (either storeResult or a new instance, not
     * null)
     */
    public Quaternion userRotation(Quaternion storeResult) {
        Quaternion result
                = (storeResult == null) ? new Quaternion() : storeResult;

        Pose pose = cgm.getPose().get();
        int boneIndex = index();
        pose.userRotation(boneIndex, result);

        return result;
    }

    /**
     * Calculate the animation/user scale of the selected bone.
     *
     * @param storeResult storage for the result (modified if not null)
     * @return user scale (either storeResult or a new instance)
     */
    public Vector3f userScale(Vector3f storeResult) {
        Vector3f result = (storeResult == null) ? new Vector3f() : storeResult;

        Pose pose = cgm.getPose().get();
        int boneIndex = index();
        pose.userScale(boneIndex, result);

        return result;
    }

    /**
     * Calculate the animation/user translation of the selected bone.
     *
     * @param storeResult storage for the result (modified if not null)
     * @return user translation (either storeResult or a new instance)
     */
    public Vector3f userTranslation(Vector3f storeResult) {
        Vector3f result = (storeResult == null) ? new Vector3f() : storeResult;

        Pose pose = cgm.getPose().get();
        int boneIndex = index();
        pose.userTranslation(boneIndex, result);

        return result;
    }

    /**
     * Calculate the world location of the selected bone in the scene view.
     *
     * @param storeResult storage for the result (modified if not null)
     * @return world coordinates (either storeResult or a new instance)
     */
    public Vector3f worldLocation(Vector3f storeResult) {
        Vector3f result = (storeResult == null) ? new Vector3f() : storeResult;

        DisplayedPose displayedPose = cgm.getPose();
        int boneIndex = index();
        displayedPose.worldLocation(boneIndex, result);

        return result;
    }
    // *************************************************************************
    // Object methods

    /**
     * Create a deep copy of this object.
     *
     * @return a new object, equivalent to this one
     * @throws CloneNotSupportedException if the superclass isn't cloneable
     */
    @Override
    public SelectedBone clone() throws CloneNotSupportedException {
        SelectedBone clone = (SelectedBone) super.clone();
        return clone;
    }
    // *************************************************************************
    // private methods

    /**
     * Determine the index of the specified/named Bone or Joint.
     *
     * @param skel the Armature or Skeleton containing the subject (not null,
     * unaffected)
     * @param bone the subject Bone or Joint or its name (not null, unaffected)
     * @return the index, or -1 if not found
     */
    private static int findBoneIndex(Object skel, Object bone) {
        assert skel != null;
        assert bone != null;

        int result;
        if (bone instanceof Bone) {
            Skeleton skeleton = (Skeleton) skel;
            result = skeleton.getBoneIndex((Bone) bone);
        } else if (bone instanceof Joint) {
            result = ((Joint) bone).getId();
        } else {
            String boneName = (String) bone;
            if (skel instanceof Armature) {
                result = ((Armature) skel).getJointIndex(boneName);
            } else {
                result = ((Skeleton) skel).getBoneIndex(boneName);
            }
        }
        return result;
    }

    /**
     * Determine the name of the specified Bone or Joint.
     *
     * @param bone the subject Bone or Joint (not null, unaffected)
     * @return the name
     */
    private static String getBoneName(Object bone) {
        assert bone != null;

        String result;
        if (bone instanceof Bone) {
            result = ((Bone) bone).getName();
        } else {
            result = ((Joint) bone).getName();
        }

        return result;
    }

    /**
     * Determine the parent of the specified Bone or Joint.
     *
     * @param bone the subject Bone or Joint or null (unaffected)
     * @return the parent Bone or Joint, or null if none
     */
    private static Object getBoneParent(Object bone) {
        Object result = null;
        if (bone instanceof Bone) {
            result = ((Bone) bone).getParent();
        } else if (bone instanceof Joint) {
            result = ((Joint) bone).getParent();
        }

        return result;
    }
}
