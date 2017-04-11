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
 * Stephen Gold's name may not be used to endorse or promote products
 derived from this software without specific prior written permission.

 THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND
 ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
 WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 DISCLAIMED. IN NO EVENT SHALL STEPHEN GOLD BE LIABLE FOR ANY
 DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES
 (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
 LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND
 ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */
package maud;

import com.jme3.animation.Bone;
import com.jme3.animation.Skeleton;
import com.jme3.animation.SkeletonControl;
import com.jme3.math.Quaternion;
import com.jme3.math.Transform;
import com.jme3.math.Vector3f;
import com.jme3.scene.Node;
import com.jme3.scene.Spatial;
import java.util.List;
import java.util.logging.Logger;
import jme3utilities.MySkeleton;
import jme3utilities.MySpatial;
import jme3utilities.SimpleAppState;
import jme3utilities.debug.AxesControl;
import jme3utilities.debug.SkeletonDebugControl;

/**
 * View state for the Maud application.
 *
 * @author Stephen Gold sgold@sonic.net
 */
class ViewState extends SimpleAppState {
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
     * the selected bone, or null if none selected
     */
    private Bone selectedBone = null;
    /**
     * attachments node for the selected bone, or null if none selected
     */
    private Node attachmentsNode = null;
    /**
     * the skeleton of this view's copy of the model
     */
    private Skeleton skeleton = null;
    /**
     * the skeleton control in this view's copy of the model
     */
    private SkeletonControl skeletonControl = null;
    /**
     * the skeleton debug control in this view's copy of the model
     */
    private SkeletonDebugControl skeletonDebugControl = null;
    /**
     * the root spatial in this view's copy of the model
     */
    private Spatial modelRoot = null;
    // *************************************************************************
    // constructors

    /**
     * Instantiate an uninitialized, enabled state.
     */
    ViewState() {
        super(true);
    }
    // *************************************************************************
    // new methods exposed

    /**
     * Access the AxesControl for the selected bone.
     *
     * @return the pre-existing instance, or null if no bone selected
     */
    AxesControl getBoneAxesControl() {
        if (selectedBone == null) {
            return null;
        }
        AxesControl result = attachmentsNode.getControl(AxesControl.class);
        assert result != null;

        return result;
    }

    /**
     * Access the skeleton debug control.
     *
     * @return the pre-existing instance (not null)
     */
    SkeletonDebugControl getSkeletonDebugControl() {
        assert skeletonDebugControl != null;
        return skeletonDebugControl;
    }

    /**
     * Replace the loaded model with a new one.
     *
     * @param newModelRoot (not null)
     */
    void setModel(Spatial newModelRoot) {
        /*
         * Detach the old spatial (if any) from the scene.
         */
        if (modelRoot != null) {
            rootNode.detachChild(modelRoot);
        }
        modelRoot = newModelRoot;
        prepareForEditing();
    }

    /**
     * Pose the skeleton under user control.
     *
     * @param userTransforms a user transform for each bone (not null,
     * unaffected)
     */
    void poseSkeleton(List<Transform> userTransforms) {
        int boneCount = skeleton.getBoneCount();
        int numTransforms = userTransforms.size();
        assert numTransforms == boneCount : numTransforms;

        for (int boneIndex = 0; boneIndex < boneCount; boneIndex++) {
            Transform transform = userTransforms.get(boneIndex);
            Vector3f translation = transform.getTranslation(null);
            Quaternion rotation = transform.getRotation(null);
            Vector3f scale = transform.getScale(null);

            Bone bone = skeleton.getBone(boneIndex);
            bone.setUserTransforms(translation, rotation, scale);
        }
    }

    /**
     * Rotate the model around +Y by the specified angle.
     *
     * @param angle in radians
     */
    void rotateY(float angle) {
        modelRoot.rotate(0f, angle, 0f);
    }

    /**
     * Alter which bone is selected.
     *
     * @param index
     */
    void selectBone(int index) {
        selectedBone = skeleton.getBone(index);
        updateAttachmentsNode();
    }

    /**
     * Cancel any bone selection.
     */
    void selectNoBone() {
        selectedBone = null;
        updateAttachmentsNode();
    }
    // *************************************************************************
    // private methods

    /**
     * Alter a newly-loaded model to prepare it for viewing.
     */
    private void prepareForEditing() {
        /*
         * Attach the model to the scene and enable user control.
         */
        rootNode.attachChild(modelRoot);
        MySkeleton.setUserControl(modelRoot, true);
        /*
         * Update references to controls, skeleton, and bone.
         */
        skeletonControl = modelRoot.getControl(SkeletonControl.class);
        if (skeletonControl == null) {
            throw new IllegalArgumentException(
                    "expected the model's root to have a SkeletonControl");
        }
        skeleton = skeletonControl.getSkeleton();
        selectNoBone();
        /*
         * Apply an identity transform to every child spatial of the model.
         * This hack enables accurate bone attachments on some models with
         * locally transformed geometries (such as Jaime).
         * The attachments bug should be fixed in jMonkeyEngine 3.2.
         */
        if (modelRoot instanceof Node) {
            Node node = (Node) modelRoot;
            for (Spatial child : node.getChildren()) {
                child.setLocalTransform(new Transform());
            }
        }
        /*
         * Scale and translate the model so its bind pose is 1.0 world-unit
         * tall, with its base resting on the XZ plane.
         */
        float maxY = MySpatial.getMaxY(modelRoot);
        float minY = MySpatial.getMinY(modelRoot);
        assert maxY > minY : maxY; // no 2D models!
        float worldScale = 1f / (maxY - minY);
        MySpatial.setWorldScale(modelRoot, worldScale);
        Vector3f worldLocation = new Vector3f(0f, -minY * worldScale, 0f);
        MySpatial.setWorldLocation(modelRoot, worldLocation);
        /*
         * Add a new, enabled SkeletonDebugControl.
         */
        skeletonDebugControl = new SkeletonDebugControl(assetManager);
        modelRoot.addControl(skeletonDebugControl);
        skeletonDebugControl.setEnabled(true);
    }

    /**
     * Update the attachments node.
     */
    private void updateAttachmentsNode() {
        Node newNode;
        if (selectedBone == null) {
            newNode = null;
        } else {
            String name = selectedBone.getName();
            newNode = skeletonControl.getAttachmentsNode(name);
        }
        if (newNode != attachmentsNode) {
            if (attachmentsNode != null) {
                attachmentsNode.removeControl(AxesControl.class);
            }
            if (newNode != null) {
                AxesControl axesControl = new AxesControl(assetManager, 1f, 1f);
                newNode.addControl(axesControl);
            }
            attachmentsNode = newNode;
        }
    }
}
