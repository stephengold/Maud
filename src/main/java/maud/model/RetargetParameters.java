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
import com.jme3.animation.Skeleton;
import com.jme3.animation.SkeletonControl;
import com.jme3.asset.AssetKey;
import com.jme3.asset.AssetLoadException;
import com.jme3.asset.AssetManager;
import com.jme3.asset.AssetNotFoundException;
import com.jme3.asset.ModelKey;
import com.jme3.scene.Spatial;
import com.jme3.scene.plugins.bvh.BVHUtils;
import com.jme3.scene.plugins.bvh.SkeletonMapping;
import com.jme3.scene.plugins.ogre.MeshLoader;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import jme3utilities.MyAnimation;
import jme3utilities.MySkeleton;
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
     * asset manager (set by constructor}
     */
    private AssetManager assetManager = null;
    /**
     * asset path to the skeleton mapping, or null if none selected
     */
    private String mappingAssetPath = "SkeletonMappings/SinbadToJaime.j3o";
    /**
     * name of the source animation, or null if none selected
     */
    private String sourceAnimationName = null;
    /**
     * asset path to the source CG model, or null if none selected
     */
    private String sourceCgmAssetPath = "Models/Sinbad/Sinbad.mesh.xml";
    /**
     * name of the target animation, or null if not set
     */
    private String targetAnimationName = null;
    // *************************************************************************
    // constructors

    /**
     * Instantiate with the specified asset manager.
     *
     * @param assetManager (not null)
     */
    public RetargetParameters(AssetManager assetManager) {
        Validate.nonNull(assetManager, "asset manager");
        this.assetManager = assetManager;
    }
    // *************************************************************************
    // new methods exposed

    /**
     * Read the asset path to the selected skeleton mapping.
     *
     * @return path (or null if none selected)
     */
    public String getMappingAssetPath() {
        return mappingAssetPath;
    }

    /**
     * Read the name of the selected source animation.
     *
     * @return name (or null if none selected)
     */
    public String getSourceAnimationName() {
        return sourceAnimationName;
    }

    /**
     * Read the asset path to the selected source CG model.
     *
     * @return path (or null if none selected)
     */
    public String getSourceCgmAssetPath() {
        return sourceCgmAssetPath;
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
     * Test whether the selected mapping is valid for the loaded CG model and
     * the selected source CG model.
     *
     * @return true if valid, otherwise false
     */
    public boolean isValidMapping() {
        boolean isValid = true;
        if (mappingAssetPath == null || !isValidSourceCgm()) {
            isValid = false;
        } else {
            SkeletonMapping map = skeletonMapping();
            /*
             * Are all target bones present in the loaded CG model?
             */
            List<String> targetBones = map.listTargetBones();
            for (String name : targetBones) {
                if (!Maud.model.cgm.hasBone(name)) {
                    isValid = false;
                    break;
                }
            }
        }
        if (isValid) {
            SkeletonMapping map = skeletonMapping();
            Spatial sourceCgm = sourceCgm();
            /*
             * Are all source bones present in the source CG model?
             */
            List<String> sourceBones = map.listSourceBones();
            for (String name : sourceBones) {
                if (MySkeleton.findBoneIndex(sourceCgm, name) == -1) {
                    isValid = false;
                    break;
                }
            }
        }

        return isValid;
    }

    /**
     * Test whether the selected source CG model is valid.
     *
     * @return true if valid, otherwise false
     */
    public boolean isValidSourceCgm() {
        boolean result = true;
        Spatial sourceCgm = sourceCgm();
        if (sourceCgm == null) {
            result = false;
        } else {
            SkeletonControl control = sourceCgm.getControl(
                    SkeletonControl.class);
            if (control == null) {
                result = false;
            }
        }

        return result;
    }

    /**
     * Enumerate all animations in the selected source CG model.
     *
     * @return a new list of names
     */
    public List<String> listAnimationNames() {
        List<String> names = new ArrayList<>();
        if (sourceCgmAssetPath != null) {
            Spatial sourceCgm = sourceCgm();
            if (sourceCgm != null) {
                Collection<String> co = MyAnimation.listAnimations(sourceCgm);
                names.addAll(co);
            }
        }

        return names;
    }

    /**
     * Add a re-targeted animation to the loaded CG model.
     */
    public void retargetAndAdd() {
        Spatial sourceCgm = sourceCgm();
        Spatial targetCgm = Maud.model.cgm.getRootSpatial();
        Animation sourceAnimation = sourceAnimation();
        Skeleton sourceSkeleton = MySkeleton.getSkeleton(sourceCgm);
        SkeletonMapping mapping = skeletonMapping();
        Animation retargeted = BVHUtils.reTarget(sourceCgm, targetCgm,
                sourceAnimation, sourceSkeleton, mapping, false,
                targetAnimationName);

        float duration = retargeted.getLength();
        assert duration >= 0f : duration;

        Maud.model.cgm.addAnimation(retargeted);
    }

    /**
     * Alter the skeleton mapping asset.
     *
     * @param path (or null to deselect)
     */
    public void setMappingAssetPath(String path) {
        mappingAssetPath = path;
    }

    /**
     * Alter the source animation.
     *
     * @param name (or null to deselect)
     */
    public void setSourceAnimationName(String name) {
        sourceAnimationName = name;
        if (name == null || !Maud.model.cgm.hasAnimation(name)) {
            setTargetAnimationName(name);
        }
    }

    /**
     * Alter the asset path for the source CG model.
     *
     * @param path (or null to deselect)
     */
    public void setSourceCgmAssetPath(String path) {
        sourceCgmAssetPath = path;
        setSourceAnimationName(null);
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
     * Access the selected skeleton mapping.
     *
     * @return a new instance, or null if the asset was not found
     */
    SkeletonMapping skeletonMapping() {
        AssetKey<SkeletonMapping> key = new AssetKey<>(mappingAssetPath);
        SkeletonMapping loaded;
        try {
            loaded = assetManager.loadAsset(key);
        } catch (AssetLoadException e) {
            loaded = null;
        }

        return loaded;
    }

    /**
     * Access the selected source animation.
     *
     * @return an orphaned spatial, or null if the asset was not found
     */
    Animation sourceAnimation() {
        Animation result = null;
        if (sourceAnimationName != null) {
            Spatial cgm = sourceCgm();
            if (cgm != null) {
                AnimControl control = cgm.getControl(AnimControl.class);
                if (control != null) {
                    result = control.getAnim(sourceAnimationName);
                }
            }
        }

        return result;
    }

    /**
     * Access the root spatial of the selected source CG model.
     *
     * @return a new orphaned spatial, or null if CG model not found
     */
    Spatial sourceCgm() {
        ModelKey key = new ModelKey(sourceCgmAssetPath);
        /*
         * Temporarily hush loader warnings about vertices with >4 weights.
         */
        Logger mlLogger = Logger.getLogger(MeshLoader.class.getName());
        Level oldLevel = mlLogger.getLevel();
        mlLogger.setLevel(Level.SEVERE);
        /*
         * Load the model.
         */
        Spatial loaded;
        try {
            loaded = assetManager.loadModel(key);
        } catch (AssetNotFoundException e) {
            loaded = null;
        }
        /*
         * Restore logging levels.
         */
        mlLogger.setLevel(oldLevel);

        return loaded;
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
    public Object clone() throws CloneNotSupportedException {
        RetargetParameters clone = (RetargetParameters) super.clone();
        return clone;
    }
}
