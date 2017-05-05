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
import java.util.logging.Level;
import java.util.logging.Logger;
import jme3utilities.Misc;
import jme3utilities.MySkeleton;
import jme3utilities.MySpatial;
import jme3utilities.MyString;
import jme3utilities.Validate;
import jme3utilities.debug.AxesControl;
import jme3utilities.debug.SkeletonDebugControl;

/**
 * Encapsulate the view's copy of the loaded CG model in Maud's "3D View"
 * screen. TODO rename
 *
 * @author Stephen Gold sgold@sonic.net
 */
public class ViewState {
    // *************************************************************************
    // constants and loggers

    /**
     * message logger for this class
     */
    final private static Logger logger = Logger.getLogger(
            ViewState.class.getName());
    // *************************************************************************
    // fields

    /**
     * the application's asset manager
     */
    final private AssetManager assetManager;
    /**
     * the root node of the application's scene graph
     */
    final private Node rootNode;
    /**
     * the skeleton of this view's copy of the CG model
     */
    private Skeleton skeleton = null;
    /**
     * the skeleton control in this view's copy of the CG model
     */
    private SkeletonControl skeletonControl = null;
    /**
     * the skeleton debug control in this view's copy of the CG model
     */
    private SkeletonDebugControl skeletonDebugControl = null;
    /**
     * the root spatial in this view's copy of the CG model
     */
    private Spatial cgModelRoot;
    // *************************************************************************
    // constructors

    ViewState(AssetManager assetManager, Node rootNode, Spatial cgmRoot) {
        this.assetManager = assetManager;
        this.rootNode = rootNode;
        this.cgModelRoot = cgmRoot;
    }
    // *************************************************************************
    // new methods exposed

    /**
     * Create a duplicate copy of this state for checkpointing.
     *
     * @param preparedRoot (not null)
     */
    ViewState createCopy() {
        Spatial cgmClone = cgModelRoot.clone();
        ViewState result = new ViewState(assetManager, rootNode, cgmClone);

        return result;
    }

    /**
     * Access an AxesControl for the specified bone.
     *
     * @param boneIndex which bone
     * @return a control, or null if no such bone
     */
    AxesControl getBoneAxesControl(int boneIndex) {
        Bone bone = skeleton.getBone(boneIndex);
        String boneName = bone.getName();

        Node attachmentsNode = skeletonControl.getAttachmentsNode(boneName);
        AxesControl result = getAxesControl(attachmentsNode);

        return result;
    }

    /**
     * Access an AxesControl for the CG model.
     *
     * @return a control (not null)
     */
    AxesControl getModelAxesControl() {
        AxesControl result = getAxesControl(cgModelRoot);
        return result;
    }

    /**
     * Access an AxesControl for the world.
     *
     * @return a control (not null)
     */
    AxesControl getWorldAxesControl() {
        AxesControl result = getAxesControl(rootNode);
        return result;
    }

    /**
     * Access the skeleton debug control for the CG model.
     *
     * @return the pre-existing instance (not null)
     */
    SkeletonDebugControl getSkeletonDebugControl() {
        assert skeletonDebugControl != null;
        return skeletonDebugControl;
    }

    /**
     * Access the root spatial of the CG model.
     *
     * @return the pre-existing instance (not null)
     */
    Spatial getSpatial() {
        assert cgModelRoot != null;
        return cgModelRoot;
    }

    /**
     * Replace the CG model with a newly loaded one.
     *
     * @param loadedRoot (not null)
     */
    public void loadModel(Spatial loadedRoot) {
        /*
         * Detach the old spatial (if any) from the scene.
         */
        if (cgModelRoot != null) {
            rootNode.detachChild(cgModelRoot);
        }
        cgModelRoot = loadedRoot;

        prepareForEditing();
    }

    /**
     * Copy a saved state to this one.
     *
     * @param preparedRoot (not null)
     */
    void restore(ViewState savedState) {
        assert cgModelRoot != null;
        /*
         * Detach the old spatial from the scene graph.
         */
        rootNode.detachChild(cgModelRoot);

        Spatial sp = savedState.getSpatial();
        cgModelRoot = sp.clone();
        rootNode.attachChild(cgModelRoot);

        skeletonControl = cgModelRoot.getControl(SkeletonControl.class);
        assert skeletonControl != null;

        skeleton = skeletonControl.getSkeleton();
        assert skeleton != null;

        skeletonDebugControl = cgModelRoot.getControl(
                SkeletonDebugControl.class);
        assert skeletonDebugControl != null;
    }

    /**
     * Rotate the CG model around +Y by the specified angle.
     *
     * @param angle in radians
     */
    void rotateY(float angle) {
        cgModelRoot.rotate(0f, angle, 0f);
    }

    /**
     * Alter the cull hint of the selected spatial.
     *
     * @param newHint new value for cull hint (not null)
     */
    public void setHint(Spatial.CullHint newHint) {
        Validate.nonNull(newHint, "cull hint");

        Spatial spatial = Maud.model.spatial.findSpatial(cgModelRoot);
        spatial.setCullHint(newHint);
    }

    /**
     * Alter the shadow mode of the selected spatial.
     *
     * @param newMode new value for shadow mode (not null)
     */
    public void setMode(RenderQueue.ShadowMode newMode) {
        Validate.nonNull(newMode, "shadow mode");

        Spatial spatial = Maud.model.spatial.findSpatial(cgModelRoot);
        spatial.setShadowMode(newMode);
    }

    /**
     * Update the user transforms of all bones from the MVC model.
     */
    void updatePose() {
        int boneCount = Maud.model.cgm.countBones();
        int numTransforms = Maud.model.pose.countTransforms();
        assert numTransforms == boneCount : numTransforms;

        Transform transform = new Transform();
        Vector3f translation = new Vector3f();
        Quaternion rotation = new Quaternion();
        Vector3f scale = new Vector3f();

        for (int boneIndex = 0; boneIndex < boneCount; boneIndex++) {
            Maud.model.pose.copyTransform(boneIndex, transform);
            transform.getTranslation(translation);
            transform.getRotation(rotation);
            transform.getScale(scale);

            Bone bone = skeleton.getBone(boneIndex);
            bone.setUserTransforms(translation, rotation, scale);
        }
    }
    // *************************************************************************
    // private methods

    /**
     * Access the axes control of the specified node.
     *
     * @param spatial (not null)
     * @return the pre-existing instance (not null)
     */
    private AxesControl getAxesControl(Spatial spatial) {
        AxesControl axesControl = spatial.getControl(AxesControl.class);
        if (axesControl == null) {
            axesControl = new AxesControl(assetManager, 1f, 1f);
            spatial.addControl(axesControl);
        }

        return axesControl;
    }

    /**
     * Alter a newly-loaded CG model to prepare it for viewing and editing.
     */
    private void prepareForEditing() {
        /*
         * Attach the CG model to the scene and enable user control.
         */
        rootNode.attachChild(cgModelRoot);
        MySkeleton.setUserControl(cgModelRoot, true);
        /*
         * Update references to controls, skeleton, and bone.
         */
        skeletonControl = cgModelRoot.getControl(SkeletonControl.class);
        if (skeletonControl == null) {
            throw new IllegalArgumentException(
                    "expected the model's root to have a SkeletonControl");
        }
        skeleton = skeletonControl.getSkeleton();
        /*
         * Apply an identity transform to every child spatial of the model.
         * This hack enables accurate bone attachments on some models with
         * locally transformed geometries (such as Jaime).
         * The attachments bug should be fixed in jMonkeyEngine 3.2.
         */
        if (cgModelRoot instanceof Node) {
            Node node = (Node) cgModelRoot;
            for (Spatial child : node.getChildren()) {
                Transform t = child.getLocalTransform();
                if (!Misc.isIdentity(t)) {
                    String name = child.getName();
                    logger.log(Level.WARNING,
                            "Overriding local transform on {0}",
                            MyString.quote(name));
                    child.setLocalTransform(new Transform());
                }
            }
        }
        /*
         * Scale and translate the CG model so its bind pose is 1.0 world-unit
         * tall, with its base resting on the XZ plane.
         */
        float maxY = MySpatial.getMaxY(cgModelRoot);
        float minY = MySpatial.getMinY(cgModelRoot);
        assert maxY > minY : maxY; // no 2D models!
        float worldScale = 1f / (maxY - minY);
        MySpatial.setWorldScale(cgModelRoot, worldScale);
        Vector3f worldLocation = new Vector3f(0f, -minY * worldScale, 0f);
        MySpatial.setWorldLocation(cgModelRoot, worldLocation);
        /*
         * Add a new SkeletonDebugControl.
         */
        skeletonDebugControl = new SkeletonDebugControl(assetManager);
        cgModelRoot.addControl(skeletonDebugControl);
        skeletonDebugControl.setEnabled(true);
    }
}
