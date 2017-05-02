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
import com.jme3.math.Quaternion;
import com.jme3.math.Transform;
import com.jme3.math.Vector3f;
import com.jme3.renderer.queue.RenderQueue;
import com.jme3.scene.Node;
import com.jme3.scene.Spatial;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import jme3utilities.MySkeleton;
import jme3utilities.MySpatial;
import jme3utilities.MyString;
import jme3utilities.SimpleAppState;
import jme3utilities.Validate;
import jme3utilities.debug.AxesControl;
import jme3utilities.debug.SkeletonDebugControl;

/**
 * A simple app state to manage the MVC view of the loaded CG model in Maud's
 * "3D View" screen.
 *
 * @author Stephen Gold sgold@sonic.net
 */
public class ViewState extends SimpleAppState {
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
    private Spatial cgModelRoot = null;
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
     * Pose the skeleton under user control.
     *
     * @param userTransforms a user transform for each bone (not null,
     * unaffected)
     */
    public void poseSkeleton(List<Transform> userTransforms) {
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
     * Replace the loaded CG model with a new one.
     *
     * @param newModelRoot (not null)
     */
    public void setModel(Spatial newModelRoot) {
        /*
         * Detach the old spatial (if any) from the scene.
         */
        if (cgModelRoot != null) {
            rootNode.detachChild(cgModelRoot);
        }
        cgModelRoot = newModelRoot;
        prepareForEditing();
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
                if (!Util.isIdentity(t)) {
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
        /*
         * Temporary hack to test shadows.
         */
        //cgModelRoot.setShadowMode(RenderQueue.ShadowMode.CastAndReceive);
    }
}
