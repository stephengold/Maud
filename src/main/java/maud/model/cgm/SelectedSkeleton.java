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

import com.jme3.anim.AnimComposer;
import com.jme3.anim.Armature;
import com.jme3.anim.Joint;
import com.jme3.anim.SkinningControl;
import com.jme3.animation.AnimControl;
import com.jme3.animation.Bone;
import com.jme3.animation.Skeleton;
import com.jme3.animation.SkeletonControl;
import com.jme3.scene.Geometry;
import com.jme3.scene.Node;
import com.jme3.scene.Spatial;
import com.jme3.scene.control.AbstractControl;
import com.jme3.scene.control.Control;
import com.jme3.util.clone.Cloner;
import com.jme3.util.clone.JmeCloneable;
import java.util.ArrayList;
import java.util.BitSet;
import java.util.Collections;
import java.util.List;
import java.util.logging.Logger;
import jme3utilities.InfluenceUtil;
import jme3utilities.MySpatial;
import jme3utilities.MyString;
import jme3utilities.Validate;
import maud.Maud;
import maud.MaudUtil;
import maud.model.EditorModel;
import maud.model.LoadedMap;
import maud.model.option.ShowBones;
import maud.view.scene.SceneView;

/**
 * The MVC model of a selected Armature or Skeleton in the Maud application.
 *
 * If the selected S-G control is a AnimControl, SkeletonControl, or
 * SkinningControl, use that control's skeleton. If the control is an
 * AnimComposer, use the Armature of the first SkinningControl in its controlled
 * spatial. Otherwise, use the skeleton of the first AnimControl,
 * SkeletonControl, or SkinningControl in the C-G model's root spatial.
 *
 * @author Stephen Gold sgold@sonic.net
 */
public class SelectedSkeleton implements JmeCloneable {
    // *************************************************************************
    // constants and loggers

    /**
     * dummy bone index, used to indicate that no bone is selected
     */
    final public static int noBoneIndex = -1;
    /**
     * message logger for this class
     */
    final private static Logger logger
            = Logger.getLogger(SelectedSkeleton.class.getName());
    /**
     * dummy bone name, used to indicate that no bone is selected
     */
    final public static String noBone = "( no bone )";
    // *************************************************************************
    // fields

    /**
     * C-G model containing the skeleton (set by {@link #setCgm(Cgm)})
     */
    private Cgm cgm = null;
    /**
     * most recent selection
     */
    private Object last = null;
    // *************************************************************************
    // new methods exposed

    /**
     * Calculate the tree position of the attachments node, if any, for the
     * indexed bone.
     *
     * @param boneIndex which Bone/Joint (&ge;0)
     * @return tree position, or null if none
     */
    public List<Integer> attachmentsPosition(int boneIndex) {
        Validate.nonNegative(boneIndex, "bone index");

        Object bone = getBone(boneIndex);
        Node attachmentsNode = MaudUtil.getBoneAttachments(bone);

        List<Integer> result = null;
        if (attachmentsNode != null) {
            result = cgm.findSpatial(attachmentsNode);
        }

        return result;
    }

    /**
     * Calculate the index of the named bone in the selected skeleton.
     *
     * @param boneName name of the bone (not null, not empty)
     * @return the bone index (&ge;0) or -1 if not found
     */
    public int boneIndex(String boneName) {
        Validate.nonEmpty(boneName, "bone name");

        int result;
        Object selected = find();
        if (selected instanceof Armature) {
            result = ((Armature) selected).getJointIndex(boneName);
        } else {
            result = ((Skeleton) selected).getBoneIndex(boneName);
        }

        return result;
    }

    /**
     * Count the bones in the selected Armature or Skeleton.
     *
     * @return the count (&ge;0)
     */
    public int countBones() {
        int result = 0;
        Object selected = find();
        if (selected instanceof Armature) {
            result = ((Armature) selected).getJointCount();
        } else if (selected instanceof Skeleton) {
            result = ((Skeleton) selected).getBoneCount();
        }

        assert result >= 0 : result;
        return result;
    }

    /**
     * Count the root bones in the selected skeleton.
     *
     * @return the count (&ge;0)
     */
    public int countRootBones() {
        Object[] roots = listRoots();
        int result = roots.length;

        return result;
    }

    /**
     * Find the selected Armature or Skeleton.
     *
     * @param storeSelectedSgcFlag if not null, set the first element to true if
     * the skeleton came from the selected S-G control or its controlled
     * spatial, false if it came from the C-G model root
     * @return a pre-existing Armature or Skeleton, or null if none selected
     */
    Object find(boolean[] storeSelectedSgcFlag) {
        boolean selectedSgcFlag;
        Object skeleton = null;
        /*
         * If the selected S-G control is an AnimControl, SkeletonControl,
         * or SkinningControl, use its skeleton, if it has one.
         */
        Control selectedSgc = cgm.getSgc().get();
        if (selectedSgc instanceof AnimControl) {
            skeleton = ((AnimControl) selectedSgc).getSkeleton();
        }
        if (skeleton == null && selectedSgc instanceof SkeletonControl) {
            skeleton = ((SkeletonControl) selectedSgc).getSkeleton();
        }
        if (skeleton == null && selectedSgc instanceof SkinningControl) {
            skeleton = ((SkinningControl) selectedSgc).getArmature();
        }
        /*
         * If the selected S-G control is an AnimComposer, use the Armature
         * of the first SkinningControl in its controlled spatial.
         *
         * This makes a skeleton available while editing animations.
         */
        if (skeleton == null && selectedSgc instanceof AnimComposer) {
            Spatial controlled = ((AnimComposer) selectedSgc).getSpatial();
            SkinningControl sc = controlled.getControl(SkinningControl.class);
            if (sc != null) {
                skeleton = sc.getArmature();
            }
        }
        if (skeleton != null) {
            selectedSgcFlag = true;
        } else {
            selectedSgcFlag = false;
        }
        /*
         * Otherwise, use the skeleton from the first AnimControl,
         * SkeletonControl, or SkinningControl in the C-G model's root spatial.
         */
        if (skeleton == null && cgm.isLoaded()) {
            Spatial cgmRoot = cgm.getRootSpatial();
            AnimControl animControl = cgmRoot.getControl(AnimControl.class);
            if (animControl != null) {
                skeleton = animControl.getSkeleton();
            }
            if (skeleton == null) {
                SkeletonControl skeletonControl
                        = cgmRoot.getControl(SkeletonControl.class);
                if (skeletonControl != null) {
                    skeleton = skeletonControl.getSkeleton();
                }
            }
            if (skeleton == null) {
                SkinningControl skinningControl
                        = cgmRoot.getControl(SkinningControl.class);
                if (skinningControl != null) {
                    skeleton = skinningControl.getArmature();
                }
            }
        }

        if (storeSelectedSgcFlag != null) {
            storeSelectedSgcFlag[0] = selectedSgcFlag; // side effect
        }
        return skeleton;
    }

    /**
     * Find the selected Armature or Skeleton.
     *
     * @return the pre-existing instance, or null if none
     */
    Object find() {
        Object result = find(null);
        return result;
    }

    /**
     * Find a Geometry that is animated by the selected skeleton control.
     *
     * @return the tree position, or null if none found
     */
    public List<Integer> findAnimatedGeometry() {
        List<Integer> result = null;
        Spatial spatial = findSpatial();
        Geometry geometry = MySpatial.findAnimatedGeometry(spatial);
        if (geometry != null) {
            result = cgm.findSpatial(geometry);
        }

        return result;
    }

    /**
     * Find the index of the selected skeleton, if any.
     *
     * @return index, or -1 if no skeleton is selected
     */
    public int findIndex() {
        int result;
        Object selected = find();
        if (selected == null) {
            result = -1;
        } else {
            List<Object> list = cgm.listSkeletons();
            result = list.indexOf(selected);
            assert result != -1;
        }

        return result;
    }

    /**
     * Find the Spatial associated with the selected skeleton.
     *
     * @return the pre-existing instance, or null if none selected
     */
    Spatial findSpatial() {
        Spatial result;

        boolean[] selectedSgcFlag = {false};
        Object skeleton = SelectedSkeleton.this.find(selectedSgcFlag);
        if (skeleton == null) {
            result = null;
        } else if (selectedSgcFlag[0]) {
            result = cgm.getSgc().getControlled();
        } else {
            result = cgm.getRootSpatial();
        }

        return result;
    }

    /**
     * Find the tree position of the spatial associated with the selected
     * skeleton.
     *
     * @return the pre-existing instance, or null if none selected
     */
    public List<Integer> findSpatialPosition() {
        Spatial spatial = findSpatial();
        List<Integer> result;
        if (spatial == null) {
            result = null;
        } else {
            result = cgm.findSpatial(spatial);
        }

        return result;
    }

    /**
     * Access the indexed bone in the selected skeleton.
     *
     * @param boneIndex which bone (&ge;0)
     * @return the pre-existing Bone or Joint (not null)
     */
    Object getBone(int boneIndex) {
        Validate.nonNegative(boneIndex, "bone index");

        Object result;
        Object selected = find();
        if (selected instanceof Armature) {
            result = ((Armature) selected).getJoint(boneIndex);
        } else {
            result = ((Skeleton) selected).getBone(boneIndex);
        }

        return result;
    }

    /**
     * Access the named Bone or Joint.
     *
     * @param name the name of a Bone or Joint (not null, not empty)
     * @return the pre-existing Bone or Joint (not null)
     */
    Object getBone(String name) {
        Validate.nonEmpty(name, "name");

        Object result;
        Object selected = find();
        if (selected instanceof Armature) {
            result = ((Armature) selected).getJoint(name);
        } else {
            result = ((Skeleton) selected).getBone(name);
        }

        return result;
    }

    /**
     * Read the name of the indexed Bone or Joint.
     *
     * @param index the index of a Bone or Joint (&ge;0)
     * @return the bone's name (may be null)
     */
    public String getBoneName(int index) {
        Validate.nonNegative(index, "bone index");

        String result;
        Object bone = getBone(index);
        if (bone instanceof Bone) {
            result = ((Bone) bone).getName();
        } else {
            result = ((Joint) bone).getName();
        }

        return result;
    }

    /**
     * Read the index of the indexed bone's parent in the selected skeleton.
     *
     * @param boneIndex which bone (&ge;0)
     * @return bone index (&ge;0) or noBoneIndex for a root bone
     */
    public int getParentIndex(int boneIndex) {
        Validate.nonNegative(boneIndex, "bone index");

        int result;
        Object selected = find();
        if (selected instanceof Armature) {
            Armature armature = (Armature) selected;
            Joint joint = armature.getJoint(boneIndex);
            Joint parent = joint.getParent();
            result = armature.getJointIndex(parent);

        } else {
            Skeleton skeleton = (Skeleton) selected;
            Bone bone = skeleton.getBone(boneIndex);
            Bone parent = bone.getParent();
            result = skeleton.getBoneIndex(parent);
        }

        return result;
    }

    /**
     * Access a SkeletonControl or SkinningControl for the selected skeleton.
     *
     * @return the relevant control, or null if not found
     */
    AbstractControl getSkeletonControl() {
        Object selected = find();

        AbstractControl result = null;
        if (selected instanceof Armature) {
            List<SkinningControl> list = cgm.listSgcs(SkinningControl.class);
            for (SkinningControl sgc : list) {
                Armature sgcArmature = sgc.getArmature();
                if (sgcArmature == selected) {
                    result = sgc;
                }
            }

        } else {
            List<SkeletonControl> list = cgm.listSgcs(SkeletonControl.class);
            for (SkeletonControl sgc : list) {
                Skeleton sgcSkeleton = sgc.getSkeleton();
                if (sgcSkeleton == selected) {
                    result = sgc;
                    break;
                }
            }
        }

        return result;
    }

    /**
     * Test whether the selected skeleton contains the named Bone or Joint.
     *
     * @param name the name of the subject Bone or Joint (not null)
     * @return true if found or noBone, otherwise false
     */
    public boolean hasBone(String name) {
        if (name.equals(noBone)) {
            return true;

        } else {
            Object bone = getBone(name);
            if (bone == null) {
                return false;
            } else {
                return true;
            }
        }
    }

    /**
     * Test whether the named Bone or Joint is a leaf, with no children.
     *
     * @param boneName the name of the Bone or Joint to test (not null)
     * @return true for a leaf bone, otherwise false
     */
    public boolean isLeafBone(String boneName) {
        boolean result = false;
        if (!boneName.equals(noBone)) {
            Object bone = getBone(boneName);
            if (bone != null) {
                int count = MaudUtil.countBoneChildren(bone);
                if (count == 0) {
                    result = true;
                }
            }
        }

        return result;
    }

    /**
     * Test whether an Armature or Skeleton is selected.
     *
     * @return true if selected, otherwise false
     */
    public boolean isSelected() {
        boolean result = false;
        if (cgm.isLoaded()) {
            Object selected = find();
            if (selected != null) {
                result = true;
            }
        }

        return result;
    }

    /**
     * Enumerate bones with attachment nodes.
     *
     * @return a new list of names, in arbitrary order
     */
    public List<String> listAttachedBones() {
        List<String> result = new ArrayList<>(5);
        int numBones = countBones();
        for (int boneIndex = 0; boneIndex < numBones; ++boneIndex) {
            Object bone = getBone(boneIndex);
            Node attachmentsNode = MaudUtil.getBoneAttachments(bone);
            if (attachmentsNode != null) {
                String name = getBoneName(boneIndex);
                result.add(name);
            }
        }

        return result;
    }

    /**
     * Enumerate all named bones in lexicographic order, plus noBone.
     *
     * @return a new list of names
     */
    public List<String> listBoneNames() {
        List<String> names = listBoneNamesRaw();
        Collections.sort(names);
        names.add(noBone);

        return names;
    }

    /**
     * Enumerate named bones whose names start with the specified prefix.
     *
     * @param namePrefix the input prefix (not null)
     * @return a new list of names, sorted, which may include noBone
     */
    public List<String> listBoneNames(String namePrefix) {
        Validate.nonNull(namePrefix, "name prefix");

        List<String> boneNames = listBoneNames();
        for (String name : MyString.toArray(boneNames)) {
            if (!name.startsWith(namePrefix)) {
                boneNames.remove(name);
            }
        }

        return boneNames;
    }

    /**
     * Enumerate all named bones, in numeric order.
     *
     * @return a new list of names, not including noBone
     */
    public List<String> listBoneNamesRaw() {
        int numBones = countBones();
        int size = 1 + numBones; // allocate an extra item for the invoker
        List<String> result = new ArrayList<>(size);

        if (numBones > 0) {
            for (int boneIndex = 0; boneIndex < numBones; ++boneIndex) {
                String name = getBoneName(boneIndex);
                if (name != null && !name.isEmpty()) {
                    result.add(name);
                }
            }
        }

        return result;
    }

    /**
     * Enumerate all children of the named bone in the selected skeleton.
     *
     * @param parentName name of the parent bone
     * @return a new list of bone names
     */
    public List<String> listChildBoneNames(String parentName) {
        List<String> result;
        Object parent = getBone(parentName);
        if (parent instanceof Bone) {
            List<Bone> children = ((Bone) parent).getChildren();
            result = new ArrayList<>(children.size());
            for (Bone bone : children) {
                String name = bone.getName();
                result.add(name);
            }

        } else if (parent instanceof Joint) {
            List<Joint> children = ((Joint) parent).getChildren();
            result = new ArrayList<>(children.size());
            for (Joint joint : children) {
                String name = joint.getName();
                result.add(name);
            }

        } else {
            result = new ArrayList<>(0);
        }
        result.remove("");

        return result;
    }

    /**
     * Enumerate which bones are referenced by the specified selection option.
     *
     * @param showBones selection option (not null)
     * @param selectedBi the index of the selected bone, or noBoneIndex if none
     * @param storeResult storage for the result (modified if not null)
     * @return a set of bone indices (either storeResult or a new instance, not
     * null)
     */
    public BitSet listShown(ShowBones showBones, int selectedBi,
            BitSet storeResult) {
        int numBones = countBones();
        BitSet result
                = (storeResult == null) ? new BitSet(numBones) : storeResult;
        assert result.size() >= numBones : result.size();

        if (numBones > 0) {
            Object selected = find();
            EditorModel model = Maud.getModel();
            LoadedMap map = model.getMap();
            int ascentBi = selectedBi;

            switch (showBones) {
                case All:
                    result.set(0, numBones);
                    break;

                case Ancestry:
                    result.clear();
                    while (ascentBi != noBoneIndex) {
                        result.set(ascentBi);
                        ascentBi = getParentIndex(ascentBi);
                    }
                    break;

                case Family:
                    result.clear();
                    if (selectedBi != noBoneIndex) {
                        if (selected instanceof Armature) {
                            Joint joint = ((Armature) selected).getJoint(
                                    selectedBi);
                            List<Joint> children = joint.getChildren();
                            for (Joint child : children) {
                                int childIndex = child.getId();
                                result.set(childIndex);
                            }
                        } else if (selected instanceof Skeleton) {
                            Bone bone = ((Skeleton) selected).getBone(
                                    selectedBi);
                            List<Bone> children = bone.getChildren();
                            for (Bone child : children) {
                                int childIndex = ((Skeleton) selected)
                                        .getBoneIndex(child);
                                result.set(childIndex);
                            }
                        }
                    }
                    while (ascentBi != noBoneIndex) {
                        result.set(ascentBi);
                        ascentBi = getParentIndex(ascentBi);
                    }
                    break;

                case Influencers:
                    Spatial subtree = findSpatial();
                    BitSet bitset;
                    if (selected instanceof Armature) {
                        bitset = InfluenceUtil.addAllInfluencers(subtree,
                                (Armature) selected);
                    } else {
                        bitset = InfluenceUtil.addAllInfluencers(subtree,
                                (Skeleton) selected);
                    }
                    result.clear();
                    result.or(bitset);
                    break;

                case Leaves:
                    for (int loopBi = 0; loopBi < numBones; ++loopBi) {
                        Object bone = getBone(loopBi);
                        int numChildren = MaudUtil.countBoneChildren(bone);
                        boolean isLeaf = (numChildren == 0);
                        result.set(loopBi, isLeaf);
                    }
                    break;

                case Mapped:
                    for (int loopBi = 0; loopBi < numBones; ++loopBi) {
                        boolean isMapped;
                        if (cgm == model.getSource()) {
                            isMapped = map.isSourceBoneMapped(loopBi);
                        } else if (cgm == model.getTarget()) {
                            isMapped = map.isTargetBoneMapped(loopBi);
                        } else {
                            throw new IllegalStateException();
                        }
                        result.set(loopBi, isMapped);
                    }
                    break;

                case None:
                    result.clear();
                    break;

                case Roots:
                    result.clear();
                    for (int loopBi : listRootIndices()) {
                        result.set(loopBi);
                    }
                    break;

                case Selected:
                    result.clear();
                    if (selectedBi != noBoneIndex) {
                        result.set(selectedBi);
                    }
                    break;

                case Subtree:
                    result.clear();
                    if (selectedBi != noBoneIndex) {
                        for (int loopBi = 0; loopBi < numBones; ++loopBi) {
                            boolean inSubtree = (loopBi == selectedBi)
                                    || MaudUtil.descendsFrom(loopBi, selectedBi,
                                            selected);
                            result.set(loopBi, inSubtree);
                        }
                    }
                    break;

                case Tracked:
                    result.clear();
                    LoadedAnimation animation = cgm.getAnimation();
                    for (int loopBi = 0; loopBi < numBones; ++loopBi) {
                        boolean tracked = animation.hasTrackForBone(loopBi);
                        result.set(loopBi, tracked);
                    }
                    break;

                case Unmapped:
                    for (int loopBi = 0; loopBi < numBones; ++loopBi) {
                        boolean isMapped;
                        if (cgm == model.getSource()) {
                            isMapped = map.isSourceBoneMapped(loopBi);
                        } else if (cgm == model.getTarget()) {
                            isMapped = map.isTargetBoneMapped(loopBi);
                        } else {
                            throw new IllegalStateException();
                        }
                        result.set(loopBi, !isMapped);
                    }
                    break;

                default:
                    throw new IllegalStateException("showBones = " + showBones);
            }
        }

        return result;
    }

    /**
     * Enumerate the root bones in the selected skeleton.
     *
     * @return a new list of bone names (each non-empty)
     */
    public List<String> listRootBoneNames() {
        Object[] roots = listRoots();
        List<String> result = new ArrayList<>(roots.length);
        Object selected = find();
        for (Object root : roots) {
            if (selected instanceof Armature) {
                String name = ((Joint) root).getName();
                result.add(name);
            } else {
                String name = ((Bone) root).getName();
                result.add(name);
            }
        }
        result.remove("");

        return result;
    }

    /**
     * Enumerate the root bones in the selected skeleton.
     *
     * @return a new list of bone indices (each &ge;0)
     */
    public List<Integer> listRootIndices() {
        Object[] roots = listRoots();
        List<Integer> result = new ArrayList<>(roots.length);
        Object selected = find();
        for (Object root : roots) {
            if (selected instanceof Armature) {
                int index = ((Joint) root).getId();
                result.add(index);
            } else {
                Bone bone = (Bone) root;
                int index = ((Skeleton) selected).getBoneIndex(bone);
                result.add(index);
            }
        }

        return result;
    }

    /**
     * Enumerate the roots in the selected Armature or Skeleton.
     *
     * @return a new array of Bones or Joints (each non-null)
     */
    Object[] listRoots() {
        Object[] result;
        Object selected = find();
        if (selected instanceof Armature) {
            result = ((Armature) selected).getRoots();

        } else if (selected instanceof Skeleton) {
            result = ((Skeleton) selected).getRoots();

        } else {
            result = new Object[0];
        }

        return result;
    }

    /**
     * Update after (for instance) selecting a different spatial or S-G control.
     */
    void postSelect() {
        boolean[] selectedSgcFlag = {false};
        Object foundSkeleton = find(selectedSgcFlag);
        if (foundSkeleton != last) {
            cgm.getBone().deselect();
            cgm.getPose().resetToBind(foundSkeleton);
            SceneView view = cgm.getSceneView();
            view.setSkeleton(foundSkeleton, selectedSgcFlag[0]);
            this.last = foundSkeleton;
        }
    }

    /**
     * Select a skeleton by name.
     *
     * @param name which skeleton to select (not null, not empty)
     */
    public void select(String name) {
        Validate.nonEmpty(name, "name");

        List<String> names = cgm.listSkeletonNames();
        int index = names.indexOf(name);
        assert index != -1;
        List<Object> list = cgm.listSkeletons();
        Object skeleton = list.get(index);
        boolean success = cgm.getSgc().selectSkeleton(skeleton);
        assert success : name;
    }

    /**
     * Alter which C-G model contains the selected skeleton. (Invoked only
     * during initialization and cloning.)
     *
     * @param newCgm (not null, alias created)
     */
    void setCgm(Cgm newCgm) {
        assert newCgm != null;
        assert newCgm.getSkeleton() == this;

        this.cgm = newCgm;
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
    public SelectedSkeleton clone() throws CloneNotSupportedException {
        super.clone();
        throw new CloneNotSupportedException("use a cloner");
    }

    /**
     * Callback from {@link com.jme3.util.clone.Cloner} to convert this
     * shallow-cloned instance into a deep-cloned one, using the specified
     * cloner and original to resolve copied fields.
     *
     * @param cloner the cloner currently cloning this control (not null)
     * @param original the instance from which this instance was shallow-cloned
     * (unused)
     */
    @Override
    public void cloneFields(Cloner cloner, Object original) {
        this.last = cloner.clone(last);
    }

    /**
     * Create a shallow clone for the JME cloner.
     *
     * @return a new instance
     */
    @Override
    public SelectedSkeleton jmeClone() {
        try {
            SelectedSkeleton clone = (SelectedSkeleton) super.clone();
            return clone;
        } catch (CloneNotSupportedException exception) {
            throw new RuntimeException(exception);
        }
    }
}
