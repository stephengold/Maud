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
import java.util.List;
import java.util.logging.Logger;
import jme3utilities.Validate;
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
     * the skeleton mapping
     */
    private SkeletonMapping mapping = new SkeletonMapping();
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
     * model.
     *
     * @param targetIndex which bone to calculate
     * @param storeResult (modified if not null)
     * @return transform (either storeResult or a new instance)
     */
    public Transform boneTransform(int targetIndex, Transform storeResult) {
        if (storeResult == null) {
            storeResult = new Transform();
        }

        Skeleton targetSkeleton = Maud.model.target.bones.getSkeleton();
        Bone targetBone = targetSkeleton.getBone(targetIndex);
        String targetName = targetBone.getName();
        BoneMapping boneMapping = mapping.get(targetName);
        if (boneMapping == null) {
            storeResult.loadIdentity();
        } else {
            Skeleton sourceSkeleton = Maud.model.source.bones.getSkeleton();
            String sourceName = boneMapping.getSourceName();
            int sourceIndex = sourceSkeleton.getBoneIndex(sourceName);
            Maud.model.source.pose.copyTransform(sourceIndex, storeResult);
            Quaternion twist = boneMapping.getTwist();
            storeResult.getRotation().multLocal(twist);
        }

        return storeResult;
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

        return result;
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
     * Add a re-targeted animation to the target CG model.
     */
    private void retargetAndAdd() {
        AnimControl sourceControl = Maud.model.source.getAnimControl();
        Spatial sourceSpatial = sourceControl.getSpatial();
        AnimControl targetControl = Maud.model.target.getAnimControl();
        Spatial targetSpatial = targetControl.getSpatial();
        Animation sourceAnimation;
        sourceAnimation = Maud.model.source.animation.getLoadedAnimation();
        Skeleton sourceSkeleton = Maud.model.source.bones.getSkeleton();
        SkeletonMapping map = skeletonMapping();
        Animation retargeted = BVHUtils.reTarget(sourceSpatial, targetSpatial,
                sourceAnimation, sourceSkeleton, map, false,
                targetAnimationName);

        float duration = retargeted.getLength();
        assert duration >= 0f : duration;

        Maud.model.target.addAnimation(retargeted);
    }
}
