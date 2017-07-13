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
import com.jme3.bounding.BoundingBox;
import com.jme3.bounding.BoundingVolume;
import com.jme3.light.AmbientLight;
import com.jme3.light.DirectionalLight;
import com.jme3.math.Quaternion;
import com.jme3.math.Transform;
import com.jme3.math.Vector3f;
import com.jme3.renderer.Camera;
import com.jme3.renderer.ViewPort;
import com.jme3.renderer.queue.RenderQueue;
import com.jme3.scene.Geometry;
import com.jme3.scene.Node;
import com.jme3.scene.Spatial;
import com.jme3.util.clone.Cloner;
import com.jme3.util.clone.JmeCloneable;
import java.util.List;
import java.util.logging.Logger;
import jme3utilities.MySkeleton;
import jme3utilities.MySpatial;
import jme3utilities.Validate;
import jme3utilities.debug.AxesVisualizer;
import jme3utilities.debug.BoundsVisualizer;
import jme3utilities.debug.SkeletonVisualizer;
import jme3utilities.math.MyMath;
import jme3utilities.sky.SkyControl;
import jme3utilities.sky.Updater;
import maud.model.LoadedCgm;

/**
 * A rendered 3D visualization of a loaded CG model in Maud's "scene" mode.
 *
 * @author Stephen Gold sgold@sonic.net
 */
public class SceneView implements JmeCloneable {
    // *************************************************************************
    // constants and loggers

    /**
     * message logger for this class
     */
    final private static Logger logger = Logger.getLogger(
            SceneView.class.getName());
    /**
     * local copy of {@link com.jme3.math.Transform#IDENTITY}
     */
    final private static Transform transformIdentity = new Transform();
    // *************************************************************************
    // fields

    /*
     * ambient light added to the scene (not null)
     */
    private AmbientLight ambientLight = new AmbientLight();
    /**
     * animation control with the selected skeleton
     */
    private AnimControl animControl;
    /*
     * visualizer for axes added to the scene
     */
    private AxesVisualizer axesVisualizer;
    /*
     * visualizer for bounding boxes added to the scene
     */
    private BoundsVisualizer boundsVisualizer;
    /*
     * directional added to the scene (not null)
     */
    private DirectionalLight mainLight = new DirectionalLight();
    /**
     * indicator for the 3D cursor, or null if none
     */
    private Geometry cursor;
    /**
     * CG model that owns this view (not null)
     */
    private LoadedCgm cgm;
    /**
     * attachment point for CG models (applies shadowMode and transforms)
     */
    final private Node parent;
    /**
     * selected skeleton in this view's copy of its CG model
     */
    private Skeleton skeleton;
    /**
     * skeleton control with the selected skeleton
     */
    private SkeletonControl skeletonControl;
    /**
     * skeleton visualizer with the selected skeleton
     */
    private SkeletonVisualizer skeletonVisualizer;
    /**
     * sky simulation added to the scene
     */
    private SkyControl skyControl;
    /**
     * root spatial in this view's copy of the CG model
     */
    private Spatial cgmRoot;
    /**
     * horizontal platform added to the scene, or null if none
     */
    private Spatial platform = null;
    /**
     * view port used when the screen is not split, or null for none
     */
    private ViewPort viewPort1 = null;
    /**
     * view port used when the screen is split (not null)
     */
    final private ViewPort viewPort2;
    // *************************************************************************
    // constructors

    /**
     * Instantiate a new visualization.
     *
     * @param loadedCgm loaded CG model that will own this view (not null, alias
     * created)
     * @param parentNode attachment point in the scene graph (not null, alias
     * created)
     * @param port1 initial view port, or null for none (alias created)
     * @param port2 view port to use after the screen is split (not null, alias
     * created)
     */
    SceneView(LoadedCgm loadedCgm, Node parentNode, ViewPort port1,
            ViewPort port2) {
        Validate.nonNull(loadedCgm, "loaded model");
        Validate.nonNull(parentNode, "parent node");
        Validate.nonNull(port2, "view port2");

        cgm = loadedCgm;
        parent = parentNode;
        viewPort1 = port1;
        viewPort2 = port2;
    }
    // *************************************************************************
    // new methods exposed

    /**
     * Calculate the location of an indexed bone, for selection. TODO replace
     * with a method to calculate dSquared
     *
     * @param boneIndex which bone to locate (&ge;0)
     * @return a new vector (in world coordinates)
     */
    public Vector3f boneLocation(int boneIndex) {
        Validate.nonNegative(boneIndex, "bone index");

        Bone bone = skeleton.getBone(boneIndex);
        Vector3f modelLocation = bone.getModelSpacePosition();
        Transform worldTransform = worldTransform();
        Vector3f location = worldTransform.transformVector(modelLocation, null);

        return location;
    }

    /**
     * Access the axes visualizer added to the scene.
     *
     * @return the pre-existing instance (not null)
     */
    public AxesVisualizer getAxesVisualizer() {
        assert axesVisualizer != null;
        return axesVisualizer;
    }

    /**
     * Access the bounds visualizer added to the scene.
     *
     * @return the pre-existing instance (not null)
     */
    public BoundsVisualizer getBoundsVisualizer() {
        assert boundsVisualizer != null;
        return boundsVisualizer;
    }

    /**
     * Access the camera used to render the scene view.
     *
     * @return a pre-existing instance, or null if not rendered
     */
    public Camera getCamera() {
        Camera result = null;

        ViewPort viewPort = getViewPort();
        if (viewPort != null && viewPort.isEnabled()) {
            result = viewPort.getCamera();
        }

        return result;
    }

    /**
     * Access the CG model's root spatial.
     *
     * @return the pre-existing instance (not null)
     */
    public Spatial getCgmRoot() {
        assert cgmRoot != null;
        return cgmRoot;
    }

    /**
     * Access the indicator for the 3D cursor.
     *
     * @return the pre-existing instance, or null if none
     */
    public Geometry getCursor() {
        return cursor;
    }

    /**
     * Access the scene's main (directional) light.
     *
     * @return the pre-existing instance (not null)
     */
    public DirectionalLight getMainLight() {
        assert mainLight != null;
        return mainLight;
    }

    /**
     * Access the spatial for the platform added to the scene.
     *
     * @return the pre-existing instance, or null if none
     */
    public Spatial getPlatform() {
        return platform;
    }

    /**
     * Access the skeleton visualizer.
     *
     * @return the pre-existing instance, or null if none
     */
    public SkeletonVisualizer getSkeletonVisualizer() {
        return skeletonVisualizer;
    }

    /**
     * Access the sky control added to the scene.
     *
     * @return the pre-existing instance (not null)
     */
    public SkyControl getSkyControl() {
        assert skyControl != null;
        return skyControl;
    }

    /**
     * Access the view port being used to render the scene.
     *
     * @return a pre-existing view port, or null if none
     */
    public ViewPort getViewPort() {
        ViewPort result;
        String viewMode = Maud.model.misc.getViewMode();
        if (Maud.model.source.isLoaded() || viewMode.equals("hybrid")) {
            result = viewPort2; // split-screen view port
        } else {
            result = viewPort1; // not split
        }

        return result;
    }

    /**
     * Replace the CG model with a newly loaded one.
     *
     * @param cgmRoot (not null)
     */
    public void loadCgm(Spatial cgmRoot) {
        Validate.nonNull(cgmRoot, "model root");
        /*
         * Detach the old spatial (if any) from the scene.
         */
        parent.detachAllChildren();
        setCgmRoot(cgmRoot);

        prepareForViewing();
    }

    /**
     * Re-install this visualization in the appropriate scene graph. Invoked
     * when restoring a checkpoint.
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
        Spatial result = cgm.spatial.underRoot(cgmRoot);
        assert result != null;
        return result;
    }

    /**
     * Alter which loaded CG model corresponds with this view. Invoked after
     * cloning.
     *
     * @param loadedCgm (not null)
     */
    public void setCgm(LoadedCgm loadedCgm) {
        Validate.nonNull(loadedCgm, "loaded model");
        cgm = loadedCgm;
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
     * Alter which cursor indicator is attached to the scene.
     *
     * @param cursorSpatial (may be null)
     */
    public void setCursor(Geometry cursorSpatial) {
        if (cursor != null) {
            cursor.removeFromParent();
        }
        if (cursorSpatial != null) {
            Node scene = (Node) getScene();
            scene.attachChild(cursorSpatial);
        }
        cursor = cursorSpatial;
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
     * Alter which platform spatial is attached to the scene.
     *
     * @param platformSpatial (may be null, alias created)
     */
    public void setPlatform(Spatial platformSpatial) {
        if (platform != null) {
            platform.removeFromParent();
        }
        if (platformSpatial != null) {
            Node scene = (Node) getScene();
            scene.attachChild(platformSpatial);
        }
        platform = platformSpatial;
    }

    /**
     * Visualize a different skeleton, or none.
     *
     * @param newSkeleton (may be null, unaffected)
     * @param selectedSpatialFlag where to add controls: false &rarr; CG model
     * root, true &rarr; selected spatial
     */
    public void setSkeleton(Skeleton newSkeleton, boolean selectedSpatialFlag) {
        clearSkeleton();

        if (newSkeleton != null) {
            Spatial controlled;
            if (selectedSpatialFlag) {
                controlled = selectedSpatial();
            } else {
                controlled = cgmRoot;
            }

            skeleton = Cloner.deepClone(newSkeleton);
            MySkeleton.setUserControl(skeleton, true);

            animControl = new AnimControl(skeleton);
            controlled.addControl(animControl);

            skeletonControl = new SkeletonControl(skeleton);
            controlled.addControl(skeletonControl);
            skeletonControl.setHardwareSkinningPreferred(false);

            Maud application = Maud.getApplication();
            AssetManager assetManager = application.getAssetManager();
            skeletonVisualizer = new SkeletonVisualizer(assetManager);
            controlled.addControl(skeletonVisualizer);
            skeletonVisualizer.setSkeleton(skeleton);
            /*
             * Update the control to initialize vertex positions.
             */
            skeletonVisualizer.setEnabled(true);
            skeletonVisualizer.update(0f);
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
        skeletonVisualizer = null;
    }

    /**
     * Update prior to rendering. (Invoked once per render pass on each
     * instance.)
     */
    void update() {
        if (skyControl == null) {  // TODO add an init method
            /*
             * Initialize scene on first update.
             */
            createAxes();
            createBounds();
            createLights();
            createSky();
        }

        Camera camera = getCamera();
        if (camera != null) {
            updatePose();
            updateShadowMode();
            updateTransform();
            Maud.gui.tools.updateScene(cgm);
            skyControl.setCamera(camera);
        }
    }

    /**
     * Copy the world transform of the CG model, based on an animated geometry
     * if possible.
     *
     * @return a new instance
     */
    public Transform worldTransform() {
        Spatial basedOn = MySpatial.findAnimatedGeometry(cgmRoot);
        if (basedOn == null) {
            basedOn = cgmRoot;
        }
        Transform transform = basedOn.getWorldTransform();

        return transform.clone();
    }
    // *************************************************************************
    // JmeCloner methods

    /**
     * Convert this shallow-cloned view into a deep-cloned one, using the
     * specified cloner and original to resolve copied fields.
     *
     * @param cloner the cloner currently cloning this control (not null)
     * @param original the view from which this view was shallow-cloned (unused)
     */
    @Override
    public void cloneFields(Cloner cloner, Object original) {
        ambientLight = cloner.clone(ambientLight);
        animControl = cloner.clone(animControl);
        axesVisualizer = cloner.clone(axesVisualizer);
        boundsVisualizer = cloner.clone(boundsVisualizer);
        // cgm not cloned: set later
        cgmRoot = cloner.clone(cgmRoot);
        cursor = cloner.clone(cursor);
        mainLight = cloner.clone(mainLight);
        // parent not cloned
        platform = cloner.clone(platform);
        skeleton = cloner.clone(skeleton);
        skeletonControl = cloner.clone(skeletonControl);
        skeletonVisualizer = cloner.clone(skeletonVisualizer);
        skyControl = cloner.clone(skyControl);
        // viewPort1, viewPort2 not cloned
    }

    /**
     * Create a shallow clone for the JME cloner.
     *
     * @return a new instance
     */
    @Override
    public SceneView jmeClone() {
        try {
            SceneView clone = (SceneView) super.clone();
            return clone;
        } catch (CloneNotSupportedException exception) {
            throw new RuntimeException(exception);
        }
    }
    // *************************************************************************
    // private methods

    /**
     * Visualize no skeleton.
     */
    private void clearSkeleton() {
        Spatial controlled;
        if (animControl != null) {
            controlled = animControl.getSpatial();
            controlled.removeControl(animControl);
        }
        if (skeletonControl != null) {
            controlled = skeletonControl.getSpatial();
            controlled.removeControl(skeletonControl);
        }
        if (skeletonVisualizer != null) {
            controlled = skeletonVisualizer.getSpatial();
            controlled.removeControl(skeletonVisualizer);
        }

        animControl = null;
        skeleton = null;
        skeletonControl = null;
        skeletonVisualizer = null;
    }

    /**
     * Add an axes visualizer to the root node of this view.
     */
    private void createAxes() {
        Maud application = Maud.getApplication();
        AssetManager assetManager = application.getAssetManager();
        axesVisualizer = new AxesVisualizer(assetManager, 1f, 1f);

        Node axesNode = new Node("axes node");
        axesNode.addControl(axesVisualizer);

        Node scene = (Node) getScene();
        scene.attachChild(axesNode);
    }

    /**
     * Add a bounds visualizer to root node of this view.
     */
    private void createBounds() {
        Maud application = Maud.getApplication();
        AssetManager assetManager = application.getAssetManager();
        boundsVisualizer = new BoundsVisualizer(assetManager);
        Spatial scene = getScene();
        scene.addControl(boundsVisualizer);
    }

    /**
     * Add lights and shadows to this view.
     */
    private void createLights() {
        /*
         * Name the lights.
         */
        ambientLight.setName("ambient light");
        mainLight.setName("main light");
        /*
         * Light the scene.
         */
        Spatial scene = getScene();
        scene.addLight(ambientLight);
        scene.addLight(mainLight);
    }

    /**
     * Add a sky to this view.
     */
    private void createSky() {
        Maud application = Maud.getApplication();
        AssetManager assetManager = application.getAssetManager();
        Camera camera = viewPort2.getCamera();
        skyControl = new SkyControl(assetManager, camera, 0.9f, false, true);
        Spatial scene = getScene();
        scene.addControl(skyControl);

        Updater updater = skyControl.getUpdater();
        updater.setAmbientLight(ambientLight);
        updater.setMainLight(mainLight);
        //updater.addShadowFilter(dlsf);
    }

    /**
     * Access the scene (root node) of this visualization.
     *
     * @return the pre-existing instance (not null)
     */
    private Spatial getScene() {
        List<Spatial> scenes = viewPort2.getScenes();
        assert scenes.size() == 1 : scenes.size();
        Spatial result = scenes.get(0);

        assert result != null;
        return result;
    }

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
        skeleton = MySkeleton.findSkeleton(cgmRoot);
        /*
         * Remove all scene-graph controls.
         */
        MySpatial.removeAllControls(cgmRoot);
        /*
         * Create and add scene-graph controls for the skeleton.
         */
        setSkeleton(skeleton, false);
        /*
         * Configure the world transform based on the bounding box of
         * the CG model.
         */
        parent.setLocalTransform(transformIdentity);
        BoundingVolume volume = cgmRoot.getWorldBound();
        Vector3f center = volume.getCenter();
        BoundingBox box = (BoundingBox) volume; // TODO if BoundingSphere
        Vector3f halfExtent = box.getExtent(null);
        float maxExtent;
        maxExtent = 2f * MyMath.max(halfExtent.x, halfExtent.y, halfExtent.z);
        Vector3f min = box.getMin(null);
        float minY = min.y;
        cgm.transform.loadCgm(center, minY, maxExtent);
        /*
         * reset the camera, cursor, and platform
         */
        Vector3f baseLocation = new Vector3f(0f, 0f, 0f);
        cgm.scenePov.setCursorLocation(baseLocation);
        Maud.model.misc.setPlatformDiameter(2f);

        Vector3f cameraLocation = new Vector3f(-2.4f, 1f, 1.6f);
        cgm.scenePov.setCameraLocation(cameraLocation);
    }

    /**
     * Update the user transforms of all bones based on the MVC model.
     */
    private void updatePose() {
        int boneCount = cgm.bones.countBones();
        int numTransforms = cgm.pose.getPose().countBones();
        assert numTransforms == boneCount : numTransforms;
        assert skeleton == null
                || skeleton.getBoneCount() == boneCount : boneCount;

        Pose pose = cgm.pose.getPose();
        Transform transform = new Transform();
        Vector3f translation = transform.getTranslation();
        Quaternion rotation = transform.getRotation();
        Vector3f scale = transform.getScale();

        for (int boneIndex = 0; boneIndex < boneCount; boneIndex++) {
            pose.userTransform(boneIndex, transform);
            Bone bone = skeleton.getBone(boneIndex);
            bone.setUserTransforms(translation, rotation, scale);
        }
    }

    /**
     * Update the shadow mode of the CG model's parent based on the MVC model.
     */
    private void updateShadowMode() {
        RenderQueue.ShadowMode mode;
        boolean enableShadows = Maud.model.misc.areShadowsRendered();
        if (enableShadows) {
            mode = RenderQueue.ShadowMode.CastAndReceive;
        } else {
            mode = RenderQueue.ShadowMode.Off;
        }
        parent.setShadowMode(mode);
    }

    /**
     * Update the transform of the CG model's parent based on the MVC model.
     */
    private void updateTransform() {
        Transform transform = cgm.transform.worldTransform();
        parent.setLocalTransform(transform);
    }
}
