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
import com.jme3.scene.Spatial;
import com.jme3.scene.control.Control;
import java.util.ArrayList;
import java.util.BitSet;
import java.util.Collections;
import java.util.List;
import java.util.logging.Logger;
import jme3utilities.MyString;
import jme3utilities.Validate;
import maud.Util;

/**
 * The MVC model of a selected skeleton in the Maud application.
 *
 * If the selected SG control is a SkeletonControl or AnimControl, use that
 * control's skeleton, otherwise use the skeleton of the 1st SkeletonControl or
 * AnimControl in the CG model's root spatial.
 *
 * @author Stephen Gold sgold@sonic.net
 */
public class SelectedSkeleton implements Cloneable {
    // *************************************************************************
    // constants and loggers

    /**
     * message logger for this class
     */
    final private static Logger logger = Logger.getLogger(
            SelectedSkeleton.class.getName());
    /**
     * dummy bone name used to indicate that no bone is selected
     */
    final public static String noBone = "( no bone )";
    // *************************************************************************
    // fields

    /**
     * CG-model load slot containing the skeleton (set by
     * {@link #setCgm(LoadedCGModel)})
     */
    private LoadedCgm cgm = null;
    // *************************************************************************
    // new methods exposed

    /**
     * Count the bones in the selected skeleton.
     *
     * @return count (&ge;0)
     */
    public int countBones() {
        Skeleton skeleton = findSkeleton();
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
        Skeleton skeleton = findSkeleton();
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
     * came from the selected spatial, false if it came from the CG model root
     * @return the pre-existing instance, or null if none
     */
    Skeleton findSkeleton(Boolean storeSelectedSpatialFlag) {
        AnimControl animControl;
        boolean selectedSpatialFlag;
        SkeletonControl skeletonControl;
        Skeleton skeleton = null;
        /*
         * If the selected SG control is an AnimControl or SkeletonControl,
         * use its skeleton, if it has one.
         */
        Control selectedSgc = cgm.sgc.findSgc();
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
         * SkeletonControl in the CG model's root spatial.
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
    Skeleton findSkeleton() {
        Skeleton result = findSkeleton(null);
        return result;
    }

    /**
     * Access the indexed bone in the selected skeleton.
     *
     * @param boneIndex which bone (&ge;0)
     * @return the pre-existing instance
     */
    public Bone getBone(int boneIndex) {
        Validate.nonNegative(boneIndex, "bone index");

        Skeleton skeleton = findSkeleton();
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
            Skeleton skeleton = findSkeleton();
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
            Skeleton skeleton = findSkeleton();
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
            Skeleton skeleton = findSkeleton();
            if (skeleton != null) {
                result = true;
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
        Skeleton skeleton = findSkeleton();
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
        Skeleton skeleton = findSkeleton();
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
     * Enumerate which bones influence mesh vertices.
     *
     * @param storeResult (modified if not null)
     * @return set of bones with influence (either storeResult or a new
     * instance)
     */
    public BitSet listInfluencers(BitSet storeResult) {
        Boolean selectedSpatialFlag = false;
        Skeleton skeleton = findSkeleton(selectedSpatialFlag);
        assert skeleton != null;

        Spatial subtree;
        if (selectedSpatialFlag) {
            subtree = cgm.getSpatial().modelSpatial();
        } else {
            subtree = cgm.getRootSpatial();
        }
        storeResult = Util.addAllInfluencers(subtree, skeleton, storeResult);

        return storeResult;
    }

    /**
     * Enumerate the root bones in the selected skeleton.
     *
     * @return a new list of bone names (each non-empty)
     */
    public List<String> listRootBoneNames() {
        List<String> boneNames = new ArrayList<>(5);
        Skeleton skeleton = findSkeleton();
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
        Skeleton skeleton = findSkeleton();
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
     * Alter the selected skeleton.
     *
     * @param newSkeleton (may be null, unaffected)
     * @param selectedSpatialFlag where to add controls: false&rarr;CG model
     * root, true&rarr;selected spatial
     */
    void set(Skeleton newSkeleton, boolean selectedSpatialFlag) {
        cgm.getSceneView().setSkeleton(newSkeleton, selectedSpatialFlag);
        cgm.getBone().deselect();
        cgm.getPose().resetToBind(newSkeleton);
    }

    /**
     * Alter which CG model contains the skeleton.
     *
     * @param newLoaded (not null)
     */
    void setCgm(LoadedCgm newLoaded) {
        assert newLoaded != null;
        cgm = newLoaded;
    }
    // *************************************************************************
    // Object methods

    /**
     * Create a copy of this object.
     *
     * @return a new object, equivalent to this one
     * @throws CloneNotSupportedException if superclass isn't cloneable
     */
    @Override
    public SelectedSkeleton clone() throws CloneNotSupportedException {
        SelectedSkeleton clone = (SelectedSkeleton) super.clone();
        return clone;
    }
}
