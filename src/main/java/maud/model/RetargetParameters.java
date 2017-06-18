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
import com.jme3.animation.Animation;
import com.jme3.animation.Bone;
import com.jme3.animation.Skeleton;
import com.jme3.asset.AssetKey;
import com.jme3.asset.AssetLoadException;
import com.jme3.asset.AssetManager;
import com.jme3.math.Quaternion;
import com.jme3.math.Transform;
import com.jme3.scene.Spatial;
import com.jme3.scene.plugins.bvh.BVHUtils;
import com.jme3.scene.plugins.bvh.BoneMapping;
import com.jme3.scene.plugins.bvh.SkeletonMapping;
import java.util.Collections;
import java.util.List;
import java.util.logging.Logger;
import jme3utilities.Validate;
import jme3utilities.math.MyMath;
import maud.History;
import maud.Maud;

/**
 * Parameters for re-targeting animations in the Maud application.
 *
 * @author Stephen Gold sgold@sonic.net
 */
public class RetargetParameters implements Cloneable {
    // *************************************************************************
    // constants and loggers

    /**
     * message logger for this class
     */
    final private static Logger logger = Logger.getLogger(
            RetargetParameters.class.getName());
    // *************************************************************************
    // fields

    /**
     * true &rarr; invert the loaded mapping, false &rarr; don't invert it
     */
    private boolean invertMapFlag = false;
    /**
     * count of unsaved edits to the skeleton mapping (&ge;0)
     */
    private int editCount = 0;
    /**
     * the skeleton mapping
     */
    private SkeletonMapping mapping = new SkeletonMapping();
    /**
     * name of the target bone whose twist is being edited, or null for none
     */
    private String editedTwist = null;
    /**
     * asset path to the skeleton mapping, or null if none loaded
     */
    private String mappingAssetPath = null;
    /**
     * name of the target animation, or null if not set
     */
    private String targetAnimationName = null;
    // *************************************************************************
    // new methods exposed

    /**
     * Calculate the mapped transform of the indexed bone in the target CG
     * model. TODO split up
     *
     * @param targetIndex which bone to calculate
     * @param storeResult (modified if not null)
     * @return transform (either storeResult or a new instance)
     */
    public Transform boneTransform(int targetIndex, Transform storeResult) {
        if (storeResult == null) {
            storeResult = new Transform();
        }

        Skeleton targetSkeleton = Maud.model.target.bones.findSkeleton();
        Bone targetBone = targetSkeleton.getBone(targetIndex);
        String targetName = targetBone.getName();
        BoneMapping boneMapping = mapping.get(targetName);
        if (boneMapping == null) {
            storeResult.loadIdentity();
        } else {
            /*
             * Calculate the model rotation of the source bone.
             */
            Skeleton sourceSkeleton = Maud.model.source.bones.findSkeleton();
            String sourceName = boneMapping.getSourceName();
            int sourceIndex = sourceSkeleton.getBoneIndex(sourceName);
            if (sourceIndex == -1) {
                storeResult.loadIdentity();
            } else {
                Transform smt = new Transform();
                Maud.model.source.pose.modelTransform(sourceIndex, smt);
                Quaternion smr = smt.getRotation();
                /*
                 * Calculate the local rotation of the target bone.
                 */
                Quaternion tlr;
                Bone targetParent = targetBone.getParent();
                if (targetParent == null) {
                    tlr = smr;
                } else {
                    /*
                     * Factor in the orientation of the target's parent.
                     */
                    int tpIndex = targetSkeleton.getBoneIndex(targetParent);
                    Transform tpt = new Transform();
                    Maud.model.target.pose.modelTransform(tpIndex, tpt);
                    Quaternion tpimr = tpt.getRotation().inverse();
                    tlr = tpimr.mult(smr);
                }
                /*
                 * Calculate the animation/user rotation of the target bone.
                 */
                Quaternion tibr = targetBone.getBindRotation().inverse();
                Quaternion tur = tibr.mult(tlr);
                Quaternion twist = boneMapping.getTwist();
                tur.multLocal(twist);

                storeResult.getRotation().set(tur);
                storeResult.getTranslation().set(0f, 0f, 0f);
                storeResult.getScale().set(1f, 1f, 1f);
            }
        }

        return storeResult;
    }

    /**
     * Copy the twist of the selected bone mapping.
     *
     * @param storeResult (modified if not null)
     * @return twist rotation (either storeResult or a new instance)
     */
    public Quaternion copyTwist(Quaternion storeResult) {
        if (storeResult == null) {
            storeResult = new Quaternion();
        }

        BoneMapping boneMapping = selectedMapping();
        Quaternion twist = boneMapping.getTwist();
        storeResult.set(twist);

        return storeResult;
    }

    /**
     * Count bone mappings.
     *
     * @return count (&ge;0)
     */
    public int countMappings() {
        int result = mapping.countMappings();
        return result;
    }

    /**
     * Count unsaved edits.
     *
     * @return count (&ge;0)
     */
    public int countUnsavedEdits() {
        return editCount;
    }

    /**
     * Delete the selected bone mapping.
     */
    public void deleteMapping() {
        BoneMapping boneMapping = selectedMapping();
        if (boneMapping != null) {
            mapping.removeMapping(boneMapping);
            setEdited("delete bone mapping");
        }
    }

    /**
     * Find the index of the selected mapping.
     *
     * @return index, or -1 if none selected
     */
    public int findIndex() {
        int index;
        BoneMapping selected = selectedMapping();
        if (selected == null) {
            index = -1;
        } else {
            List<String> nameList = listSorted();
            String targetBoneName = Maud.model.target.bone.getName();
            index = nameList.indexOf(targetBoneName);
        }

        return index;
    }

    /**
     * Read the asset path to the loaded skeleton mapping.
     *
     * @return path (or null if none selected)
     */
    public String getMappingAssetPath() {
        return mappingAssetPath;
    }

    /**
     * Read the selected name for the target animation.
     *
     * @return name (or null if not set)
     */
    public String getTargetAnimationName() {
        return targetAnimationName;
    }

    /**
     * Test whether a bone mapping is selected.
     *
     * @return true if selected, otherwise false
     */
    public boolean isBoneMappingSelected() {
        BoneMapping mapping = selectedMapping();
        if (mapping == null) {
            return false;
        } else {
            return true;
        }
    }

    /**
     * Test whether to invert the mapping before applying it.
     *
     * @return true if inverting the mapping, otherwise false
     */
    public boolean isInvertingMap() {
        return invertMapFlag;
    }

    /**
     * Unload the current mapping and load the specified asset.
     *
     * @param assetPath path to the mapping asset to load (not null)
     * @return true if successful, otherwise false
     */
    public boolean loadMappingAsset(String assetPath) {
        Validate.nonNull(assetPath, "asset path");

        Maud application = Maud.getApplication();
        AssetManager assetManager = application.getAssetManager();

        AssetKey<SkeletonMapping> key = new AssetKey<>(assetPath);
        boolean result;
        try {
            mapping = assetManager.loadAsset(key);
            mappingAssetPath = assetPath;
            result = true;
        } catch (AssetLoadException e) {
            result = false;
        }

        String eventDescription = "load mapping " + assetPath;
        setPristine(eventDescription);

        return result;
    }

    /**
     * Add a bone mapping for the selected bones.
     */
    public void mapBones() {
        if (!isBoneMappingSelected() && Maud.model.source.bone.isSelected()
                && Maud.model.target.bone.isSelected()) {
            String sourceBoneName = Maud.model.source.bone.getName();
            BoneMapping boneMapping = mapping.getForSource(sourceBoneName);
            if (boneMapping != null) {
                mapping.removeMapping(boneMapping);
            }

            String targetBoneName = Maud.model.target.bone.getName();
            boneMapping = mapping.get(targetBoneName);
            if (boneMapping != null) {
                mapping.removeMapping(boneMapping);
            }

            mapping.map(targetBoneName, sourceBoneName);
            String event = String.format("map bone %s", targetBoneName);
            setEdited(event);
        }
    }

    /**
     * Test whether the loaded mapping matches the source CG model.
     *
     * @return true if they match, otherwise false
     */
    public boolean matchesSource() {
        boolean matches;
        SkeletonMapping map = skeletonMapping();
        Spatial sourceCgm = Maud.model.source.getRootSpatial();
        if (sourceCgm == null) {
            matches = false;
        } else {
            /*
             * Are all source bones in the mapping present
             * in the source CG model?
             */
            matches = true;
            List<String> sourceBones = map.listSourceBones();
            for (String name : sourceBones) {
                if (!Maud.model.source.bones.hasBone(name)) {
                    matches = false;
                    break;
                }
            }
        }

        return matches;
    }

    /**
     * Test whether the mapping matches the target CG model.
     *
     * @return true if they match, otherwise false
     */
    public boolean matchesTarget() {
        SkeletonMapping map = skeletonMapping();
        /*
         * Are all target bones in the mapping present in the target CG model?
         */
        boolean matches = true;
        List<String> targetBones = map.listTargetBones();
        for (String name : targetBones) {
            if (!Maud.model.target.bones.hasBone(name)) {
                matches = false;
                break;
            }
        }

        return matches;
    }

    /**
     * Retarget the source animation to the target CG model and load the
     * resulting animation.
     *
     * @param newName name for the new animation (not null, not empty)
     */
    public void retargetAndLoad(String newName) {
        Validate.nonEmpty(newName, "new name");

        setTargetAnimationName(newName);
        retargetAndAdd();
        Maud.model.target.animation.load(newName);
    }

    /**
     * Select the bone mapping of the selected source bone.
     */
    public void selectFromSource() {
        String sourceBoneName = Maud.model.source.bone.getName();
        String targetBoneName = targetBoneName(sourceBoneName);
        Maud.model.target.bone.select(targetBoneName);
    }

    /**
     * Select the bone mapping of the selected target bone.
     */
    public void selectFromTarget() {
        String targetBoneName = Maud.model.target.bone.getName();
        selectFromTarget(targetBoneName);
    }

    /**
     * Select the next bone mapping in name-sorted order.
     */
    public void selectNext() {
        if (isBoneMappingSelected()) {
            List<String> nameList = listSorted();
            String targetBoneName = Maud.model.target.bone.getName();
            int index = nameList.indexOf(targetBoneName);
            int numMappings = nameList.size();
            int nextIndex = MyMath.modulo(index + 1, numMappings);
            targetBoneName = nameList.get(nextIndex);
            selectFromTarget(targetBoneName);
        }
    }

    /**
     * Select the previous bone mapping in name-sorted order.
     */
    public void selectPrevious() {
        if (isBoneMappingSelected()) {
            List<String> nameList = listSorted();
            String targetBoneName = Maud.model.target.bone.getName();
            int index = nameList.indexOf(targetBoneName);
            int numMappings = nameList.size();
            int previousIndex = MyMath.modulo(index - 1, numMappings);
            targetBoneName = nameList.get(previousIndex);
            selectFromTarget(targetBoneName);
        }
    }

    /**
     * Alter whether to invert the loaded mapping before applying it.
     *
     * @param newSetting true &rarr; invert it, false &rarr; don't invert it
     */
    public void setInvertMap(boolean newSetting) {
        invertMapFlag = newSetting;
    }

    /**
     * Alter the name for the target animation.
     *
     * @param name (or null if not set)
     */
    public void setTargetAnimationName(String name) {
        targetAnimationName = name;
    }

    /**
     * Alter the twist of the selected bone mapping.
     *
     * @param newTwist (not null, unaffected)
     */
    public void setTwist(Quaternion newTwist) {
        BoneMapping boneMapping = selectedMapping();
        Quaternion twist = boneMapping.getTwist();
        twist.set(newTwist);
        setEditedTwist();
    }

    /**
     * Calculate the effective skeleton mapping.
     *
     * @return a new mapping
     */
    SkeletonMapping skeletonMapping() {
        SkeletonMapping result;
        if (invertMapFlag) {
            result = mapping.inverse();
        } else {
            result = mapping.clone();
        }

        return result;
    }

    /**
     * Read the name of the source bone mapped to the named target bone.
     *
     * @param targetBoneName which target bone (not null)
     * @return bone name, or null if none
     */
    public String sourceBoneName(String targetBoneName) {
        String result = null;
        if (invertMapFlag) {
            BoneMapping boneMapping = mapping.getForSource(targetBoneName);
            if (boneMapping != null) {
                result = boneMapping.getTargetName();
            }
        } else {
            BoneMapping boneMapping = mapping.get(targetBoneName);
            if (boneMapping != null) {
                result = boneMapping.getSourceName();
            }
        }

        return result;
    }

    /**
     * Read the name of the target bone mapped from the named source bone.
     *
     * @param sourceBoneName which source bone (not null)
     * @return bone name, or null if none
     */
    public String targetBoneName(String sourceBoneName) {
        String result = null;
        if (invertMapFlag) {
            BoneMapping boneMapping = mapping.get(sourceBoneName);
            if (boneMapping != null) {
                result = boneMapping.getSourceName();
            }
        } else {
            BoneMapping boneMapping = mapping.getForSource(sourceBoneName);
            if (boneMapping != null) {
                result = boneMapping.getTargetName();
            }
        }
        return result;
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
    public RetargetParameters clone() throws CloneNotSupportedException {
        RetargetParameters clone = (RetargetParameters) super.clone();
        clone.mapping = mapping.clone();

        return clone;
    }
    // *************************************************************************
    // private methods

    /**
     * Generate a sorted list of target-bone names.
     *
     * @return a new list
     */
    private List<String> listSorted() {
        List<String> result = mapping.listTargetBones();
        Collections.sort(result);

        return result;
    }

    /**
     * Add a re-targeted animation to the target CG model.
     */
    private void retargetAndAdd() {
        AnimControl sourceControl = Maud.model.source.getAnimControl();
        Spatial sourceSpatial = sourceControl.getSpatial();
        AnimControl targetControl = Maud.model.target.getAnimControl();
        Spatial targetSpatial = targetControl.getSpatial();
        Animation sourceAnimation;
        sourceAnimation = Maud.model.source.animation.getAnimation();
        Skeleton sourceSkeleton = Maud.model.source.bones.findSkeleton();
        SkeletonMapping map = skeletonMapping();
        Animation retargeted = BVHUtils.reTarget(sourceSpatial, targetSpatial,
                sourceAnimation, sourceSkeleton, map, false,
                targetAnimationName);

        float duration = retargeted.getLength();
        assert duration >= 0f : duration;

        Maud.model.target.addAnimation(retargeted);
    }

    /**
     * Access the selected bone mapping.
     *
     * @return the pre-existing instance, or null if none selected
     */
    private BoneMapping selectedMapping() {
        BoneMapping result = null;
        if (Maud.model.source.isLoaded()) {
            String sourceBoneName = Maud.model.source.bone.getName();
            String targetBoneName = Maud.model.target.bone.getName();
            if (invertMapFlag) {
                String swap = sourceBoneName;
                sourceBoneName = targetBoneName;
                targetBoneName = swap;
            }
            BoneMapping boneMapping = mapping.get(targetBoneName);
            if (boneMapping != null) {
                String name = boneMapping.getSourceName();
                if (name.equals(sourceBoneName)) {
                    result = boneMapping;
                }
            }
        }

        return result;
    }

    /**
     * Select the bone mapping of the named target bone.
     */
    private void selectFromTarget(String targetBoneName) {
        String sourceBoneName = sourceBoneName(targetBoneName);
        Maud.model.source.bone.select(sourceBoneName);
        Maud.model.target.bone.select(targetBoneName);
    }

    /**
     * Increment the count of unsaved edits and update the edit history.
     *
     * @param eventDescription description of causative event (not null)
     */
    private void setEdited(String eventDescription) {
        ++editCount;
        editedTwist = null;
        History.addEvent(eventDescription);
    }

    /**
     * If not a continuation of the previous edit, update the edit count and the
     * edit history.
     */
    private void setEditedTwist() {
        String newName = Maud.model.target.bone.getName();
        if (!newName.equals(editedTwist)) {
            ++editCount;
            editedTwist = newName;
            String event = String.format("set twist for %s", newName);
            History.addEvent(event);
        }
    }

    /**
     * Mark the mapping as pristine.
     *
     * @param eventDescription description of causative event (not null)
     */
    private void setPristine(String eventDescription) {
        editCount = 0;
        editedTwist = null;
        History.addEvent(eventDescription);
    }
}
