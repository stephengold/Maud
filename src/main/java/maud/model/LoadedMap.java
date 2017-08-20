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
import com.jme3.animation.Skeleton;
import com.jme3.asset.AssetKey;
import com.jme3.asset.AssetManager;
import com.jme3.math.Quaternion;
import com.jme3.math.Transform;
import com.jme3.scene.plugins.bvh.BoneMapping;
import com.jme3.scene.plugins.bvh.SkeletonMapping;
import java.util.Collections;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import jme3utilities.MyString;
import jme3utilities.Validate;
import jme3utilities.math.MyMath;
import jme3utilities.ui.Locators;
import maud.Maud;
import maud.Pose;
import maud.Util;

/**
 * The loaded skeleton map in the Maud application, without editing features.
 * TODO split off selected bone mapping?
 *
 * @author Stephen Gold sgold@sonic.net
 */
public class LoadedMap implements Cloneable {
    // *************************************************************************
    // constants and loggers

    /**
     * message logger for this class
     */
    final private static Logger logger = Logger.getLogger(
            LoadedMap.class.getName());
    // *************************************************************************
    // fields

    /**
     * true &rarr; invert the loaded map, false &rarr; don't invert it
     */
    private boolean invertMapFlag = false;
    /**
     * the map itself
     */
    protected SkeletonMapping map = new SkeletonMapping();
    /**
     * absolute filesystem path to asset location, or "" if unknown
     */
    protected String assetFolder = "";
    /**
     * asset path, or "" if unknown
     */
    protected String assetPath = "";
    // *************************************************************************
    // new methods exposed

    /**
     * Calculate the mapped transform of the indexed bone in the target CG
     * model.
     *
     * @param boneIndex which target bone to calculate (&ge;0)
     * @param storeResult (modified if not null)
     * @return transform (either storeResult or a new instance)
     */
    public Transform boneTransform(int boneIndex, Transform storeResult) {
        Validate.nonNegative(boneIndex, "bone index");
        if (storeResult == null) {
            storeResult = new Transform();
        }
        storeResult.loadIdentity();

        LoadedCgm source = Maud.getModel().getSource();
        LoadedCgm target = Maud.getModel().getTarget();
        Skeleton targetSkeleton = target.bones.findSkeleton();
        Bone targetBone = targetSkeleton.getBone(boneIndex);
        String targetName = targetBone.getName();
        BoneMapping boneMapping = effectiveMapping(targetName);
        Skeleton sourceSkeleton = source.bones.findSkeleton();
        if (boneMapping != null && sourceSkeleton != null) {
            String sourceName = boneMapping.getSourceName();
            int sourceIndex = sourceSkeleton.getBoneIndex(sourceName);
            if (sourceIndex != -1) {
                /*
                 * Calculate the model orientation of the source bone.
                 */
                Pose sourcePose = source.pose.getPose();
                Quaternion mo = sourcePose.modelOrientation(sourceIndex, null);

                Pose targetPose = target.pose.getPose();
                Quaternion userRotation = targetPose.userForModel(boneIndex,
                        mo, null);
                Quaternion twist = boneMapping.getTwist();
                userRotation.mult(twist, storeResult.getRotation());
            }
        }

        return storeResult;
    }

    /**
     * Copy the effective twist of the selected bone mapping.
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
        if (isInvertingMap()) {
            storeResult.inverseLocal();
        }

        return storeResult;
    }

    /**
     * Count bone mappings.
     *
     * @return count (&ge;0)
     */
    public int countMappings() {
        int result = map.countMappings();
        return result;
    }

    /**
     * Find the index of the selected bone mapping.
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
            String targetBoneName = Maud.getModel().getTarget().bone.getName();
            index = nameList.indexOf(targetBoneName);
        }

        return index;
    }

    /**
     * Read the asset folder of the loaded map.
     *
     * @return filesystem path, or "" if unknown (not null)
     */
    public String getAssetFolder() {
        assert assetFolder != null;
        return assetFolder;
    }

    /**
     * Read the asset path to the loaded map.
     *
     * @return path, or "" if unknown (not null)
     */
    public String getAssetPath() {
        assert assetPath != null;
        return assetPath;
    }

    /**
     * Test whether any of the bone mappings are invalid.
     *
     * @return true if invalid mappings found, otherwise false
     */
    public boolean hasInvalidMappings() {
        if (isEmpty()) {
            return false;
        }

        EditorModel model = Maud.getModel();
        SelectedSkeleton sSkeleton = model.getSource().bones;
        if (sSkeleton.isSelected()) {
            float matchesSource = matchesSource();
            if (matchesSource < 0.9995f) {
                return true;
            }
        }
        SelectedSkeleton tSkeleton = model.getTarget().bones;
        if (tSkeleton.isSelected()) {
            float matchesTarget = matchesTarget();
            if (matchesTarget < 0.9995f) {
                return true;
            }
        }

        return false;
    }

    /**
     * Test whether the named bone in the target CG model is mapped.
     *
     * @param targetBoneName name of bone to find (not null)
     * @return true if mapped, otherwise false
     */
    public boolean isBoneMapped(String targetBoneName) {
        Validate.nonNull(targetBoneName, "bone name");

        BoneMapping boneMapping;
        if (isInvertingMap()) {
            boneMapping = map.getForSource(targetBoneName);
        } else {
            boneMapping = map.get(targetBoneName);
        }
        if (boneMapping == null) {
            return false;
        } else {
            return true;
        }
    }

    /**
     * Test whether a bone mapping is selected.
     *
     * @return true if selected, otherwise false
     */
    public boolean isBoneMappingSelected() {
        BoneMapping boneMapping = selectedMapping();
        if (boneMapping == null) {
            return false;
        } else {
            return true;
        }
    }

    /**
     * Test whether there are any bone mappings.
     *
     * @return true if no bone mappings, otherwise false
     */
    public boolean isEmpty() {
        int count = map.countMappings();
        if (count == 0) {
            return true;
        } else {
            return false;
        }
    }

    /**
     * Test whether to invert the map before applying it.
     *
     * @return true if inverting the map, otherwise false
     */
    public boolean isInvertingMap() {
        return invertMapFlag;
    }

    /**
     * Test whether the indexed bone in the source CG model is mapped.
     *
     * @param boneIndex which bone (&ge;0)
     * @return true if the mapped, otherwise false
     */
    public boolean isSourceBoneMapped(int boneIndex) {
        Validate.nonNegative(boneIndex, "bone index");

        String boneName;
        boneName = Maud.getModel().getSource().bones.getBoneName(boneIndex);
        BoneMapping boneMapping;
        if (isInvertingMap()) {
            boneMapping = map.get(boneName);
        } else {
            boneMapping = map.getForSource(boneName);
        }
        if (boneMapping == null) {
            return false;
        } else {
            return true;
        }
    }

    /**
     * Test whether the indexed bone in the target CG model is mapped.
     *
     * @param boneIndex which bone (&ge;0)
     * @return true if the mapped, otherwise false
     */
    public boolean isTargetBoneMapped(int boneIndex) {
        Validate.nonNegative(boneIndex, "bone index");

        LoadedCgm target = Maud.getModel().getTarget();
        String boneName = target.bones.getBoneName(boneIndex);
        boolean result = isBoneMapped(boneName);

        return result;
    }

    /**
     * Unload the current map and load from the specified asset.
     *
     * @param location file path to the asset root (not null, not empty)
     * @param path path to the asset to load (not null, not empty)
     * @return true if successful, otherwise false
     */
    public boolean loadAsset(String location, String path) {
        Validate.nonEmpty(location, "location");
        Validate.nonEmpty(path, "path");

        Locators.save();
        Locators.useFilesystem(location);
        SkeletonMapping loaded = loadFromAsset(path, false);
        Locators.restore();

        boolean success;
        if (loaded == null) {
            success = false;
        } else {
            success = true;
            map = loaded;
            assetFolder = location;
            assetPath = path;
        }

        return success;
    }

    /**
     * Unload the current map and load the named one from the classpath.
     *
     * @param mapName which map to load (not null, not empty)
     * @return true if successful, otherwise false
     */
    public boolean loadNamed(String mapName) {
        Validate.nonEmpty(mapName, "map name");

        String path = String.format("SkeletonMaps/%s.j3o", mapName);

        Locators.save();
        Locators.useDefault();
        SkeletonMapping loaded = loadFromAsset(path, false);
        Locators.restore();

        boolean success;
        if (loaded == null) {
            success = false;
        } else {
            success = true;
            map = loaded;
            assetFolder = "";
            assetPath = path;
        }

        return success;
    }

    /**
     * Calculate how well the effective map matches the source skeleton.
     *
     * @return fraction of the effective source bones that are found in the
     * source skeleton (&ge;0, &le;1)
     */
    public float matchesSource() {
        List<String> boneNames;
        if (isInvertingMap()) {
            boneNames = map.listTargetBones();
        } else {
            boneNames = map.listSourceBones();
        }
        SelectedSkeleton sourceSkeleton = Maud.getModel().getSource().bones;

        int numMatches = 0;
        for (String name : boneNames) {
            if (sourceSkeleton.hasBone(name)) {
                ++numMatches;
            }
        }
        float result = ((float) numMatches) / boneNames.size();

        return result;
    }

    /**
     * Calculate how well the effective map matches the target skeleton.
     *
     * @return fraction of the effective target bones that are found in the
     * target skeleton (&ge;0, &le;1)
     */
    public float matchesTarget() {
        List<String> boneNames;
        if (isInvertingMap()) {
            boneNames = map.listSourceBones();
        } else {
            boneNames = map.listTargetBones();
        }
        SelectedSkeleton targetSkeleton = Maud.getModel().getTarget().bones;

        int numMatches = 0;
        for (String name : boneNames) {
            if (targetSkeleton.hasBone(name)) {
                ++numMatches;
            }
        }
        float result = ((float) numMatches) / boneNames.size();

        return result;
    }

    /**
     * Retarget the source animation to the target CG model and load the
     * resulting animation.
     *
     * @param newName name for the new animation (not null, not empty)
     */
    public void retargetAndLoad(String newName) {
        Validate.nonEmpty(newName, "new name");

        retargetAndAdd(newName);
        Maud.getModel().getTarget().animation.load(newName);
    }

    /**
     * Select the bone mapping of the selected source bone.
     */
    public void selectFromSource() {
        String sourceBoneName = Maud.getModel().getSource().bone.getName();
        String targetBoneName = targetBoneName(sourceBoneName);
        Maud.getModel().getTarget().bone.select(targetBoneName);
    }

    /**
     * Select the bone mapping of the selected target bone.
     */
    public void selectFromTarget() {
        String targetBoneName = Maud.getModel().getTarget().bone.getName();
        selectFromTarget(targetBoneName);
    }

    /**
     * Select the next bone mapping in name-sorted order.
     */
    public void selectNext() {
        if (isBoneMappingSelected()) {
            List<String> nameList = listSorted();
            String targetBoneName = Maud.getModel().getTarget().bone.getName();
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
            String targetBoneName = Maud.getModel().getTarget().bone.getName();
            int index = nameList.indexOf(targetBoneName);
            int numMappings = nameList.size();
            int previousIndex = MyMath.modulo(index - 1, numMappings);
            targetBoneName = nameList.get(previousIndex);
            selectFromTarget(targetBoneName);
        }
    }

    /**
     * Alter whether to invert the loaded map before applying it.
     *
     * @param newSetting true &rarr; invert it, false &rarr; don't invert it
     */
    public void setInvertMap(boolean newSetting) {
        invertMapFlag = newSetting;
    }

    /**
     * Read the name of the source bone mapped to the named target bone.
     *
     * @param targetBoneName which target bone (not null)
     * @return bone name, or null if none
     */
    public String sourceBoneName(String targetBoneName) {
        Validate.nonNull(targetBoneName, "bone name");

        String result = null;
        if (invertMapFlag) {
            BoneMapping boneMapping = map.getForSource(targetBoneName);
            if (boneMapping != null) {
                result = boneMapping.getTargetName();
            }
        } else {
            BoneMapping boneMapping = map.get(targetBoneName);
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
        Validate.nonNull(sourceBoneName, "bone name");

        String result = null;
        if (invertMapFlag) {
            BoneMapping boneMapping = map.get(sourceBoneName);
            if (boneMapping != null) {
                result = boneMapping.getSourceName();
            }
        } else {
            BoneMapping boneMapping = map.getForSource(sourceBoneName);
            if (boneMapping != null) {
                result = boneMapping.getTargetName();
            }
        }
        return result;
    }
    // *************************************************************************
    // protected methods

    /**
     * Access the selected bone mapping.
     *
     * @return the pre-existing instance, or null if none selected
     */
    protected BoneMapping selectedMapping() {
        BoneMapping result = null;
        if (Maud.getModel().getSource().isLoaded()) {
            String sourceBoneName = Maud.getModel().getSource().bone.getName();
            String targetBoneName = Maud.getModel().getTarget().bone.getName();
            if (invertMapFlag) {
                String swap = sourceBoneName;
                sourceBoneName = targetBoneName;
                targetBoneName = swap;
            }
            BoneMapping boneMapping = map.get(targetBoneName);
            if (boneMapping != null) {
                String name = boneMapping.getSourceName();
                if (name.equals(sourceBoneName)) {
                    result = boneMapping;
                }
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
    public LoadedMap clone() throws CloneNotSupportedException {
        LoadedMap clone = (LoadedMap) super.clone();
        clone.map = map.clone();

        return clone;
    }
    // *************************************************************************
    // private methods

    /**
     * Calculate an effective skeleton map.
     *
     * @return a new map
     */
    private SkeletonMapping effectiveMap() {
        SkeletonMapping result;
        if (invertMapFlag) {
            result = map.inverse();
        } else {
            result = map.clone();
        }

        return result;
    }

    /**
     * Calculate an effective bone mapping for the named bone in the target CG
     * model.
     *
     * @param targetBoneName name of bone to find (not null)
     * @return a bone mapping (may be pre-existing) or null if none found
     */
    private BoneMapping effectiveMapping(String targetBoneName) {
        Validate.nonNull(targetBoneName, "bone name");

        BoneMapping result = null;
        if (invertMapFlag) {
            BoneMapping inverse = map.getForSource(targetBoneName);
            if (inverse != null) {
                String sourceBoneName = inverse.getTargetName();
                Quaternion inverseTwist = inverse.getTwist();
                Quaternion twist = inverseTwist.inverse(); // TODO conjugate
                result = new BoneMapping(targetBoneName, sourceBoneName, twist);
            }
        } else {
            result = map.get(targetBoneName);
        }

        return result;
    }

    /**
     * Generate a sorted list of target-bone names.
     *
     * @return a new list
     */
    private List<String> listSorted() {
        List<String> result = map.listTargetBones();
        Collections.sort(result);

        return result;
    }

    /**
     * Quietly load a skeleton map asset from persistent storage without
     * installing it.
     *
     * @param path (not null)
     * @return a skeleton map, or null if the asset had errors
     */
    private SkeletonMapping loadFromAsset(String path, boolean useCache) {
        AssetManager assetManager = Locators.getAssetManager();
        /*
         * Load the skeleton map quietly.
         */
        AssetKey<SkeletonMapping> key = new AssetKey<>(path);
        if (!useCache) {
            /*
                 * Delete the key from the asset manager's cache in order
                 * to force a fresh load from persistent storage.
             */
            assetManager.deleteFromCache(key);
        }

        SkeletonMapping loaded = Util.loadMapAsset(assetManager, path);
        if (loaded == null) {
            logger.log(Level.SEVERE, "Failed to load map from asset {0}",
                    MyString.quote(path));
        } else {
            logger.log(Level.INFO, "Loaded map from asset {0}",
                    MyString.quote(path));
            History.autoAdd();
        }

        return loaded;
    }

    /**
     * Add a re-targeted animation to the target CG model.
     *
     * @param newAnimationName name for the resulting animation (not null)
     */
    private void retargetAndAdd(String newAnimationName) {
        assert newAnimationName != null;

        LoadedCgm source = Maud.getModel().getSource();
        EditableCgm target = Maud.getModel().getTarget();
        Animation sourceAnimation = source.animation.getAnimation();
        Skeleton sourceSkeleton = source.bones.findSkeleton();
        Skeleton targetSkeleton = target.bones.findSkeleton();
        SkeletonMapping effectiveMap = effectiveMap();
        Animation retargeted = Util.retargetAnimation(sourceAnimation,
                sourceSkeleton, targetSkeleton, effectiveMap, newAnimationName);

        float duration = retargeted.getLength();
        assert duration >= 0f : duration;

        target.addAnimation(retargeted);
    }

    /**
     * Select the bone mapping of the named target bone.
     *
     * @param targetBoneName name of bone to find (not null)
     */
    private void selectFromTarget(String targetBoneName) {
        assert targetBoneName != null;

        String sourceBoneName = sourceBoneName(targetBoneName);
        if (sourceBoneName != null) {
            Maud.getModel().getSource().bone.select(sourceBoneName);
            Maud.getModel().getTarget().bone.select(targetBoneName);
        }
    }
}
