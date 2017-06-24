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
package maud;

import com.jme3.animation.AnimControl;
import com.jme3.animation.Bone;
import com.jme3.animation.Skeleton;
import com.jme3.animation.SkeletonControl;
import com.jme3.asset.AssetManager;
import com.jme3.math.Quaternion;
import com.jme3.math.Transform;
import com.jme3.math.Vector3f;
import com.jme3.renderer.queue.RenderQueue;
import com.jme3.scene.Node;
import com.jme3.scene.Spatial;
import com.jme3.util.clone.Cloner;
import com.jme3.util.clone.JmeCloneable;
import java.util.logging.Logger;
import jme3utilities.MySkeleton;
import jme3utilities.MySpatial;
import jme3utilities.Validate;
import jme3utilities.debug.SkeletonDebugControl;
import jme3utilities.math.MyMath;
import maud.model.LoadedCGModel;

/**
 * Visualization of a loaded CG model, component of a DddModel.
 *
 * @author Stephen Gold sgold@sonic.net
 */
public class CgmView implements JmeCloneable {
    // *************************************************************************
    // constants and loggers

    /**
     * message logger for this class
     */
    final private static Logger logger = Logger.getLogger(
            CgmView.class.getName());
    // *************************************************************************
    // fields

    /**
     * the animation control with the selected skeleton
     */
    private AnimControl animControl;
    /**
     * the MVC model that owns this view
     */
    private LoadedCGModel model;
    /**
     * attachment point in the scene graph (used for transforms)
     */
    final private Node parent;
    /**
     * the selected skeleton in this view's copy of its CG model
     */
    private Skeleton skeleton;
    /**
     * the skeleton control with the selected skeleton
     */
    private SkeletonControl skeletonControl;
    /**
     * the skeleton debug control with the selected skeleton
     */
    private SkeletonDebugControl skeletonDebugControl;
    /**
     * the root spatial in this view's copy of its CG model
     */
    private Spatial cgmRoot;
    // *************************************************************************
    // constructors

    /**
     * Instantiate a new visualization.
     *
     * @param loadedModel MVC model that will own this view (not null)
     * @param parentNode attachment point in the scene graph (not null)
     */
    public CgmView(LoadedCGModel loadedModel, Node parentNode) {
        Validate.nonNull(loadedModel, "loaded model");
        Validate.nonNull(parentNode, "parent");

        model = loadedModel;
        parent = parentNode;
    }
    // *************************************************************************
    // new methods exposed

    /**
     * Calculate the location of an indexed bone, for selection.
     *
     * @param boneIndex which bone to locate (&ge;0)
     * @return a new vector (in world coordinates)
     */
    public Vector3f boneLocation(int boneIndex) {
        Validate.nonNegative(boneIndex, "bone index");

        Bone bone = skeleton.getBone(boneIndex);
        Vector3f modelLocation = bone.getModelSpacePosition();
        Transform worldTransform = copyWorldTransform();
        Vector3f location = worldTransform.transformVector(modelLocation, null);

        return location;
    }

    /**
     * Copy the world transform of the CG model, based on an animated geometry
     * if possible.
     *
     * @return a new instance
     */
    public Transform copyWorldTransform() {
        Spatial basedOn = MySpatial.findAnimatedGeometry(cgmRoot);
        if (basedOn == null) {
            basedOn = cgmRoot;
        }
        Transform transform = basedOn.getWorldTransform();

        return transform.clone();
    }

    /**
     * Access the root spatial of the CG model.
     *
     * @return the pre-existing instance (not null)
     */
    public Spatial getCgmRoot() {
        assert cgmRoot != null;
        return cgmRoot;
    }

    /**
     * Access the skeleton debug control for the CG model.
     *
     * @return the pre-existing instance, or null if none
     */
    public SkeletonDebugControl getSkeletonDebugControl() {
        return skeletonDebugControl;
    }

    /**
     * Replace the CG model with a newly loaded one.
     *
     * @param loadedRoot (not null)
     */
    public void loadModel(Spatial loadedRoot) {
        Validate.nonNull(loadedRoot, "loaded root");
        /*
         * Detach the old spatial (if any) from the scene.
         */
        if (cgmRoot != null) {
            parent.detachChild(cgmRoot);
        }
        setCgmRoot(loadedRoot);

        prepareForViewing();
    }

    /**
     * Re-install this visualization in the scene graph.
     */
    public void reinstall() {
        /*
         * Detach any old visualization from the scene graph.
         */
        parent.detachAllChildren();
        /*
         * Attach this visualization.
         */
        if (cgmRoot != null) {
            parent.attachChild(cgmRoot);
        }
    }

    /**
     * Access the selected spatial in this view's copy of its CG model.
     *
     * @return the pre-existing spatial (not null)
     */
    public Spatial selectedSpatial() {
        Spatial result = model.spatial.underRoot(cgmRoot);
        return result;
    }

    /**
     * Visualize a different CG model, or none.
     *
     * @param newCgmRoot CG model's root spatial, or null if none (unaffected)
     */
    void setCgmRoot(Spatial newCgmRoot) {
        if (newCgmRoot == null) {
            cgmRoot = null;
        } else {
            cgmRoot = newCgmRoot.clone();
        }
    }

    /**
     * Alter the cull hint of the selected spatial.
     *
     * @param newHint new value for cull hint (not null)
     */
    public void setHint(Spatial.CullHint newHint) {
        Validate.nonNull(newHint, "cull hint");

        Spatial spatial = selectedSpatial();
        spatial.setCullHint(newHint);
    }

    /**
     * Alter the shadow mode of the selected spatial.
     *
     * @param newMode new value for shadow mode (not null)
     */
    public void setMode(RenderQueue.ShadowMode newMode) {
        Validate.nonNull(newMode, "shadow mode");

        Spatial spatial = selectedSpatial();
        spatial.setShadowMode(newMode);
    }

    /**
     * Alter which MVC model corresponds with this view. Used after cloning.
     *
     * @param loadedModel (not null)
     */
    public void setModel(LoadedCGModel loadedModel) {
        Validate.nonNull(loadedModel, "loaded model");
        model = loadedModel;
    }

    /**
     * Visualize a different skeleton, or none.
     *
     * @param newSkeleton (may be null, unaffected)
     * @param selectedSpatialFlag where to add controls: false &rarr; CG model
     * root, true &rarr; selected spatial
     */
    public void setSkeleton(Skeleton newSkeleton, boolean selectedSpatialFlag) {
        Spatial controlled;
        if (animControl != null) {
            controlled = animControl.getSpatial();
            controlled.removeControl(animControl);
        }
        if (skeletonControl != null) {
            controlled = skeletonControl.getSpatial();
            controlled.removeControl(skeletonControl);
        }
        if (skeletonDebugControl != null) {
            controlled = skeletonDebugControl.getSpatial();
            controlled.removeControl(skeletonDebugControl);
        }

        if (selectedSpatialFlag) {
            controlled = selectedSpatial();
        } else {
            controlled = cgmRoot;
        }

        if (newSkeleton == null) {
            animControl = null;
            skeleton = null;
            skeletonControl = null;
            skeletonDebugControl = null;
        } else {
            skeleton = Cloner.deepClone(newSkeleton);
            MySkeleton.setUserControl(skeleton, true);

            animControl = new AnimControl(skeleton);
            controlled.addControl(animControl);

            skeletonControl = new SkeletonControl(skeleton);
            controlled.addControl(skeletonControl);
            skeletonControl.setHardwareSkinningPreferred(false);

            Maud application = Maud.getApplication();
            AssetManager assetManager = application.getAssetManager();
            skeletonDebugControl = new SkeletonDebugControl(assetManager);
            controlled.addControl(skeletonDebugControl);
            skeletonDebugControl.setSkeleton(skeleton);
            /*
             * Update the control to initialize vertex positions.
             */
            skeletonDebugControl.setEnabled(true);
            skeletonDebugControl.update(0f);
        }
    }

    /**
     * Alter the local rotation of the selected spatial.
     *
     * @param rotation (not null, unaffected)
     */
    public void setSpatialRotation(Quaternion rotation) {
        Validate.nonNull(rotation, "rotation");

        Spatial spatial = selectedSpatial();
        spatial.setLocalRotation(rotation);
    }

    /**
     * Alter the local scale of the selected spatial.
     *
     * @param scale (not null, unaffected)
     */
    public void setSpatialScale(Vector3f scale) {
        Validate.nonNull(scale, "scale");

        Spatial spatial = selectedSpatial();
        spatial.setLocalScale(scale);
    }

    /**
     * Alter the local translation of the selected spatial.
     *
     * @param translation (not null, unaffected)
     */
    public void setSpatialTranslation(Vector3f translation) {
        Validate.nonNull(translation, "translation");

        Spatial spatial = selectedSpatial();
        spatial.setLocalTranslation(translation);
    }

    /**
     * Unload the CG model.
     */
    public void unloadModel() {
        /*
         * Detach the old spatial (if any) from the scene.
         */
        if (cgmRoot != null) {
            parent.detachChild(cgmRoot);
        }
        setCgmRoot(null);
        animControl = null;
        skeleton = null;
        skeletonControl = null;
        skeletonDebugControl = null;
    }

    /**
     * Update the user transforms of all bones using the MVC model.
     */
    void updatePose() {
        int boneCount = model.bones.countBones();
        int numTransforms = model.pose.getPose().countBones();
        assert numTransforms == boneCount : numTransforms;
        assert skeleton == null || skeleton.getBoneCount() == boneCount : boneCount;

        Transform transform = new Transform();
        Vector3f translation = new Vector3f();
        Quaternion rotation = new Quaternion();
        Vector3f scale = new Vector3f();

        for (int boneIndex = 0; boneIndex < boneCount; boneIndex++) {
            model.pose.copyTransform(boneIndex, transform);
            transform.getTranslation(translation);
            transform.getRotation(rotation);
            transform.getScale(scale);

            Bone bone = skeleton.getBone(boneIndex);
            bone.setUserTransforms(translation, rotation, scale);
        }
    }

    /**
     * Update the transform of the CG model.
     *
     * @param angle in radians
     */
    void updateTransform() {
        Transform transform = model.transform.worldTransform();
        parent.setLocalTransform(transform);
    }
    // *************************************************************************
    // JmeCloner methods

    /**
     * Convert this shallow-cloned instance into a deep-cloned one, using the
     * specified cloner and original to resolve copied fields.
     *
     * @param cloner the cloner currently cloning this control (not null)
     * @param original the control from which this control was shallow-cloned
     */
    @Override
    public void cloneFields(Cloner cloner, Object original) {
        animControl = cloner.clone(animControl);
        cgmRoot = cloner.clone(cgmRoot);
        skeleton = cloner.clone(skeleton);
        skeletonControl = cloner.clone(skeletonControl);
        skeletonDebugControl = cloner.clone(skeletonDebugControl);
    }

    /**
     * Create a shallow clone for the JME cloner.
     *
     * @return a new instance
     */
    @Override
    public CgmView jmeClone() {
        try {
            CgmView clone = (CgmView) super.clone();
            return clone;
        } catch (CloneNotSupportedException exception) {
            throw new RuntimeException(exception);
        }
    }
    // *************************************************************************
    // private methods

    /**
     * Alter a newly loaded CG model to prepare it for visualization. Assumes
     * the CG model's root node will be the selected spatial.
     */
    private void prepareForViewing() {
        /*
         * Attach the CG model to the scene graph.
         */
        parent.attachChild(cgmRoot);
        /*
         * Use the skeleton from the first AnimControl or
         * SkeletonControl in the CG model's root spatial.
         */
        AnimControl anControl = cgmRoot.getControl(AnimControl.class);
        if (anControl != null) {
            skeleton = anControl.getSkeleton();
        } else {
            SkeletonControl skelControl;
            skelControl = cgmRoot.getControl(SkeletonControl.class);
            if (skelControl != null) {
                skeleton = skelControl.getSkeleton();
            } else {
                skeleton = null;
            }
        }
        /*
         * Remove all SG controls.
         */
        Util.removeAllControls(cgmRoot);
        /*
         * Create and add controls for the skeleton.
         */
        setSkeleton(skeleton, false);
        /*
         * Configure the world transform based on the range
         * of mesh coordinates in the CG model.
         */
        Vector3f[] minMax = MySpatial.findMinMaxCoords(cgmRoot, false);
        Vector3f extents = minMax[1].subtract(minMax[0]);
        float maxExtent = MyMath.max(extents.x, extents.y, extents.z);
        assert maxExtent > 0f : maxExtent;
        float minY = minMax[0].y;
        model.transform.loadCgm(minY, maxExtent);
        /*
         * reset the camera, cursor, and platform
         */
        Vector3f baseLocation = new Vector3f(0f, 0f, 0f);
        Maud.model.cursor.setLocation(baseLocation);
        Maud.model.misc.setPlatformDiameter(2f);
        Maud.model.misc.setPlatformLocation(baseLocation);

        Vector3f cameraLocation = new Vector3f(-2.4f, 1f, 1.6f);
        Maud.model.camera.setLocation(cameraLocation);
        Maud.model.camera.setScale(1f);
    }
}
