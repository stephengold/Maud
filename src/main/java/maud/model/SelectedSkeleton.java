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

import com.jme3.animation.AnimControl;
import com.jme3.animation.Bone;
import com.jme3.animation.Skeleton;
import com.jme3.animation.SkeletonControl;
import com.jme3.scene.Geometry;
import com.jme3.scene.Node;
import com.jme3.scene.Spatial;
import com.jme3.scene.control.Control;
import com.jme3.util.clone.Cloner;
import com.jme3.util.clone.JmeCloneable;
import java.util.ArrayList;
import java.util.BitSet;
import java.util.Collections;
import java.util.List;
import java.util.logging.Logger;
import jme3utilities.MySkeleton;
import jme3utilities.MySpatial;
import jme3utilities.MyString;
import jme3utilities.Validate;
import maud.Maud;
import maud.Util;

/**
 * The MVC model of a selected skeleton in the Maud application.
 *
 * If the selected S-G control is a SkeletonControl or AnimControl, use that
 * control's skeleton, otherwise use the skeleton of the 1st SkeletonControl or
 * AnimControl in the C-G model's root spatial.
 *
 * @author Stephen Gold sgold@sonic.net
 */
public class SelectedSkeleton implements JmeCloneable {
    // *************************************************************************
    // constants and loggers

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
    private Skeleton last = null;
    // *************************************************************************
    // new methods exposed

    /**
     * Calculate the tree position of the attachments node, if any, for the
     * specified bone.
     *
     * @param boneIndex which bone (&ge;0)
     * @return tree position, or null if none
     */
    public List<Integer> attachmentsPosition(int boneIndex) {
        Validate.nonNegative(boneIndex, "bone index");

        Bone bone = getBone(boneIndex);
        Node node = Util.getAttachments(bone);

        //TODO rewrite Util.findPosition() and use that
        List<Integer> result = null;
        if (node != null) {
            result = new ArrayList<>(5);
            Node parent = node.getParent();
            while (parent != null) {
                int index = parent.getChildIndex(node);
                assert index >= 0 : index;
                result.add(index);
                node = parent;
                parent = node.getParent();
            }
            Collections.reverse(result);
        }

        return result;
    }

    /**
     * Count the bones in the selected skeleton.
     *
     * @return count (&ge;0)
     */
    public int countBones() {
        Skeleton skeleton = find();
        int count;
        if (skeleton == null) {
            count = 0;
        } else {
            count = skeleton.getBoneCount();
        }

        assert count >= 0 : count;
        return count;
    }

    /**
     * Count the root bones in the selected skeleton.
     *
     * @return count (&ge;0)
     */
    public int countRootBones() {
        int count;
        Skeleton skeleton = find();
        if (skeleton == null) {
            count = 0;
        } else {
            Bone[] roots = skeleton.getRoots();
            count = roots.length;
        }

        assert count >= 0 : count;
        return count;
    }

    /**
     * Find the selected skeleton.
     *
     * @param storeSelectedSpatialFlag if not null, set to true if the skeleton
     * came from the selected spatial, false if it came from the C-G model root
     * @return the pre-existing instance, or null if none
     */
    Skeleton find(Boolean storeSelectedSpatialFlag) {
        AnimControl animControl;
        boolean selectedSpatialFlag;
        SkeletonControl skeletonControl;
        Skeleton skeleton = null;
        /*
         * If the selected S-G control is an AnimControl or SkeletonControl,
         * use its skeleton, if it has one.
         */
        Control selectedSgc = cgm.getSgc().find();
        if (selectedSgc instanceof AnimControl) {
            animControl = (AnimControl) selectedSgc;
            skeleton = animControl.getSkeleton();
        }
        if (skeleton == null && selectedSgc instanceof SkeletonControl) {
            skeletonControl = (SkeletonControl) selectedSgc;
            skeleton = skeletonControl.getSkeleton();
        }
        if (skeleton != null) {
            selectedSpatialFlag = true;
        } else {
            selectedSpatialFlag = false;
        }
        /*
         * If not, use the skeleton from the first AnimControl or
         * SkeletonControl in the C-G model's root spatial.
         */
        Spatial cgmRoot = cgm.getRootSpatial();
        if (skeleton == null) {
            animControl = cgmRoot.getControl(AnimControl.class);
            if (animControl != null) {
                skeleton = animControl.getSkeleton();
            }
        }
        if (skeleton == null) {
            skeletonControl = cgmRoot.getControl(SkeletonControl.class);
            if (skeletonControl != null) {
                skeleton = skeletonControl.getSkeleton();
            }
        }

        if (storeSelectedSpatialFlag != null) {
            storeSelectedSpatialFlag = selectedSpatialFlag;
        }
        return skeleton;
    }

    /**
     * Find the selected skeleton.
     *
     * @return the pre-existing instance, or null if none
     */
    Skeleton find() {
        Skeleton result = find(null);
        return result;
    }

    /**
     * Find a geometry that is animated by the selected skeleton control.
     *
     * @return the tree position, or null if none found
     */
    public List<Integer> findAnimatedGeometry() {
        List<Integer> result = null;
        Spatial spatial = findSpatial();
        if (spatial != null) {
            Geometry geometry = MySpatial.findAnimatedGeometry(spatial);
            if (geometry != null) {
                result = cgm.findSpatial(geometry);
            }
        }

        return result;
    }

    /**
     * Find the spatial associated with the selected skeleton.
     *
     * @return the pre-existing instance, or null if none
     */
    Spatial findSpatial() {
        Boolean selectedSpatialFlag = false;
        find(selectedSpatialFlag);
        Spatial spatial;
        if (selectedSpatialFlag) {
            spatial = cgm.getSpatial().find();
        } else {
            spatial = cgm.getRootSpatial();
        }

        return spatial;
    }

    /**
     * Access the indexed bone in the selected skeleton.
     *
     * @param boneIndex which bone (&ge;0)
     * @return the pre-existing instance
     */
    Bone getBone(int boneIndex) {
        Validate.nonNegative(boneIndex, "bone index");

        Skeleton skeleton = find();
        Bone result = skeleton.getBone(boneIndex);

        return result;
    }

    /**
     * Read the name of the indexed bone in the selected skeleton.
     *
     * @param boneIndex which bone (&ge;0)
     * @return the bone's name
     */
    public String getBoneName(int boneIndex) {
        Validate.nonNegative(boneIndex, "bone index");

        Bone bone = getBone(boneIndex);
        String result = bone.getName();

        return result;
    }

    /**
     * Read the index of the indexed bone's parent in the selected skeleton.
     *
     * @param boneIndex which bone (&ge;0)
     * @return bone index (&ge;0) or -1 for a root bone
     */
    public int getParentIndex(int boneIndex) {
        Validate.nonNegative(boneIndex, "bone index");

        Skeleton skeleton = find();
        Bone bone = skeleton.getBone(boneIndex);
        Bone parent = bone.getParent();
        int result = skeleton.getBoneIndex(parent);

        return result;
    }

    /**
     * Access a skeleton control for the selected skeleton.
     */
    SkeletonControl getSkeletonControl() {
        Skeleton skeleton = find();
        SkeletonControl result = null;

        List<SkeletonControl> list = cgm.listSgcs(SkeletonControl.class);
        for (SkeletonControl sgc : list) {
            Skeleton controlSkeleton = sgc.getSkeleton();
            if (controlSkeleton == skeleton) {
                result = sgc;
                break;
            }
        }

        return result;
    }

    /**
     * Test whether the selected skeleton contains the named bone.
     *
     * @param name which bone (not null)
     * @return true if found or noBone, otherwise false
     */
    public boolean hasBone(String name) {
        boolean result;
        if (name.equals(noBone)) {
            result = true;
        } else {
            Skeleton skeleton = find();
            if (skeleton == null) {
                result = false;
            } else {
                Bone bone = skeleton.getBone(name);
                if (bone == null) {
                    result = false;
                } else {
                    result = true;
                }
            }
        }

        return result;
    }

    /**
     * Test whether the named bone is a leaf bone, with no children.
     *
     * @param boneName which bone to test (not null)
     * @return true for a leaf bone, otherwise false
     */
    public boolean isLeafBone(String boneName) {
        boolean result = false;
        if (!boneName.equals(noBone)) {
            Skeleton skeleton = find();
            Bone bone = skeleton.getBone(boneName);
            if (bone != null) {
                ArrayList<Bone> children = bone.getChildren();
                result = children.isEmpty();
            }
        }

        return result;
    }

    /**
     * Test whether a skeleton is selected.
     *
     * @return true if selected, otherwise false
     */
    public boolean isSelected() {
        boolean result = false;
        if (cgm.isLoaded()) {
            Skeleton skeleton = find();
            if (skeleton != null) {
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
        Skeleton skeleton = find();
        int numBones = countBones();
        for (int boneIndex = 0; boneIndex < numBones; boneIndex++) {
            Bone bone = skeleton.getBone(boneIndex);
            Node attachmentsNode = Util.getAttachments(bone);
            if (attachmentsNode != null) {
                String name = bone.getName();
                result.add(name);
            }
        }

        return result;
    }

    /**
     * Enumerate bones in the selected skeleton.
     *
     * @return a new list of names, including noBone
     */
    public List<String> listBoneNames() {
        List<String> names = new ArrayList<>(80);
        Skeleton skeleton = find();
        if (skeleton != null) {
            int boneCount = skeleton.getBoneCount();

            for (int boneIndex = 0; boneIndex < boneCount; boneIndex++) {
                Bone bone = skeleton.getBone(boneIndex);
                String name = bone.getName();
                if (name != null && !name.isEmpty()) {
                    names.add(name);
                }
            }
        }

        Collections.sort(names);
        names.add(noBone);

        return names;
    }

    /**
     * Enumerate all bones in the loaded model having names that start with the
     * specified prefix.
     *
     * @param namePrefix the input prefix
     * @return a new list of names
     */
    public List<String> listBoneNames(String namePrefix) {
        List<String> boneNames = listBoneNames();
        for (String name : MyString.toArray(boneNames)) {
            if (!name.startsWith(namePrefix)) {
                boneNames.remove(name);
            }
        }

        return boneNames;
    }

    /**
     * Enumerate all children of the named bone in the selected skeleton.
     *
     * @param parentName name of the parent bone
     * @return a new list of bone names
     */
    public List<String> listChildBoneNames(String parentName) {
        Skeleton skeleton = find();
        Bone parent = skeleton.getBone(parentName);
        List<Bone> children = parent.getChildren();
        List<String> boneNames = new ArrayList<>(children.size());
        for (Bone b : children) {
            String name = b.getName();
            boneNames.add(name);
        }
        boneNames.remove("");

        return boneNames;
    }

    /**
     * Enumerate which bones are included the specified selection option.
     *
     * @param showBones selection option (not null)
     * @param storeResult (modified if not null)
     * @return set of bone indices (either storeResult or a new instance)
     */
    public BitSet listShown(ShowBones showBones, BitSet storeResult) {
        int numBones = countBones();
        if (storeResult == null) {
            storeResult = new BitSet(numBones);
        } else {
            assert storeResult.size() == numBones : storeResult.size();
        }

        if (numBones > 0) {
            Skeleton skeleton = find();
            int boneIndex = cgm.getBone().getIndex();
            EditorModel model = Maud.getModel();
            LoadedMap map = model.getMap();

            switch (showBones) {
                case All:
                    storeResult.set(0, numBones);
                    break;

                case Ancestry:
                    storeResult.clear();
                    while (boneIndex != -1) {
                        storeResult.set(boneIndex);
                        boneIndex = getParentIndex(boneIndex);
                    }
                    break;

                case Family:
                    storeResult.clear();
                    if (boneIndex != -1) {
                        Bone bone = skeleton.getBone(boneIndex);
                        List<Bone> children = bone.getChildren();
                        for (Bone child : children) {
                            int childIndex = skeleton.getBoneIndex(child);
                            storeResult.set(childIndex);
                        }
                    }
                    while (boneIndex != -1) {
                        storeResult.set(boneIndex);
                        boneIndex = getParentIndex(boneIndex);
                    }
                    break;

                case Influencers:
                    storeResult.clear();
                    Spatial subtree = findSpatial();
                    Util.addAllInfluencers(subtree, skeleton, storeResult);
                    break;

                case Leaves:
                    for (boneIndex = 0; boneIndex < numBones; boneIndex++) {
                        Bone bone = skeleton.getBone(boneIndex);
                        int numChildren = bone.getChildren().size();
                        boolean isLeaf = (numChildren == 0);
                        storeResult.set(boneIndex, isLeaf);
                    }
                    break;

                case Mapped:
                    for (boneIndex = 0; boneIndex < numBones; boneIndex++) {
                        boolean isMapped;
                        if (cgm == model.getSource()) {
                            isMapped = map.isSourceBoneMapped(boneIndex);
                        } else if (cgm == model.getTarget()) {
                            isMapped = map.isTargetBoneMapped(boneIndex);
                        } else {
                            throw new IllegalStateException();
                        }
                        storeResult.set(boneIndex, isMapped);
                    }
                    break;

                case None:
                    storeResult.clear();
                    break;

                case Roots:
                    storeResult.clear();
                    Bone[] roots = skeleton.getRoots();
                    for (Bone root : roots) {
                        boneIndex = skeleton.getBoneIndex(root);
                        storeResult.set(boneIndex);
                    }
                    break;

                case Selected:
                    storeResult.clear();
                    if (boneIndex != -1) {
                        storeResult.set(boneIndex);
                    }
                    break;

                case Subtree:
                    storeResult.clear();
                    if (boneIndex != -1) {
                        for (int boneI = 0; boneI < numBones; boneI++) {
                            boolean inSubtree = (boneI == boneIndex)
                                    || MySkeleton.descendsFrom(boneI, boneIndex,
                                            skeleton);
                            storeResult.set(boneI, inSubtree);
                        }
                    }
                    break;

                case Tracked:
                    storeResult.clear();
                    LoadedAnimation animation = cgm.getAnimation();
                    for (boneIndex = 0; boneIndex < numBones; boneIndex++) {
                        boolean tracked = animation.hasTrackForBone(boneIndex);
                        storeResult.set(boneIndex, tracked);
                    }
                    break;

                case Unmapped:
                    for (boneIndex = 0; boneIndex < numBones; boneIndex++) {
                        boolean isMapped;
                        if (cgm == model.getSource()) {
                            isMapped = map.isSourceBoneMapped(boneIndex);
                        } else if (cgm == model.getTarget()) {
                            isMapped = map.isTargetBoneMapped(boneIndex);
                        } else {
                            throw new IllegalStateException();
                        }
                        storeResult.set(boneIndex, !isMapped);
                    }
                    break;

                default:
                    throw new IllegalStateException();
            }
        }

        return storeResult;
    }

    /**
     * Enumerate the root bones in the selected skeleton.
     *
     * @return a new list of bone names (each non-empty)
     */
    public List<String> listRootBoneNames() {
        List<String> boneNames = new ArrayList<>(5);
        Skeleton skeleton = find();
        if (skeleton != null) {
            Bone[] roots = skeleton.getRoots();
            for (Bone rootBone : roots) {
                String name = rootBone.getName();
                boneNames.add(name);
            }
            boneNames.remove("");
        }

        return boneNames;
    }

    /**
     * Enumerate the root bones in the selected skeleton.
     *
     * @return a new list of bone indices
     */
    public List<Integer> listRootIndices() {
        List<Integer> result = new ArrayList<>(5);
        Skeleton skeleton = find();
        if (skeleton != null) {
            Bone[] roots = skeleton.getRoots();
            for (Bone rootBone : roots) {
                int index = skeleton.getBoneIndex(rootBone);
                result.add(index);
            }
        }

        return result;
    }

    /**
     * Update after (for instance) selecting a different spatial or S-G control.
     */
    void postSelect() {
        Boolean selectedSpatialFlag = false;
        Skeleton foundSkeleton = find(selectedSpatialFlag);
        if (foundSkeleton != last) {
            cgm.getBone().deselect();
            cgm.getPose().resetToBind(foundSkeleton);
            cgm.getSceneView().setSkeleton(foundSkeleton, selectedSpatialFlag);
            last = foundSkeleton;
        }
    }

    /**
     * Alter which C-G model contains the selected skeleton.
     *
     * @param newCgm (not null)
     */
    void setCgm(Cgm newCgm) {
        assert newCgm != null;
        assert newCgm.getSkeleton() == this;

        cgm = newCgm;
    }
    // *************************************************************************
    // JmeCloneable methods

    /**
     * Callback from {@link com.jme3.util.clone.Cloner} to convert this
     * shallow-cloned instance into a deep-cloned one, using the specified
     * cloner and original to resolve copied fields.
     *
     * @param cloner the cloner currently cloning this control (not null)
     * @param original the view from which this view was shallow-cloned (unused)
     */
    @Override
    public void cloneFields(Cloner cloner, Object original) {
        last = cloner.clone(last);
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
    // *************************************************************************
    // Object methods

    /**
     * Don't use this method; use a {@link com.jme3.util.clone.Cloner} instead.
     *
     * @return never
     * @throws CloneNotSupportedException
     */
    @Override
    public SelectedSkeleton clone() throws CloneNotSupportedException {
        super.clone();
        throw new CloneNotSupportedException("use a cloner");
    }
}
