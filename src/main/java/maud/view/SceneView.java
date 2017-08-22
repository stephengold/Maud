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
package maud.view;

import com.jme3.animation.AnimControl;
import com.jme3.animation.Bone;
import com.jme3.animation.Skeleton;
import com.jme3.animation.SkeletonControl;
import com.jme3.app.state.AppStateManager;
import com.jme3.asset.AssetManager;
import com.jme3.bounding.BoundingBox;
import com.jme3.bounding.BoundingVolume;
import com.jme3.bullet.BulletAppState;
import com.jme3.bullet.PhysicsSpace;
import com.jme3.bullet.control.PhysicsControl;
import com.jme3.bullet.objects.PhysicsRigidBody;
import com.jme3.collision.CollisionResult;
import com.jme3.collision.CollisionResults;
import com.jme3.input.InputManager;
import com.jme3.light.AmbientLight;
import com.jme3.light.DirectionalLight;
import com.jme3.math.Quaternion;
import com.jme3.math.Ray;
import com.jme3.math.Transform;
import com.jme3.math.Vector2f;
import com.jme3.math.Vector3f;
import com.jme3.renderer.Camera;
import com.jme3.renderer.ViewPort;
import com.jme3.renderer.queue.RenderQueue;
import com.jme3.scene.Geometry;
import com.jme3.scene.Node;
import com.jme3.scene.Spatial;
import com.jme3.util.clone.Cloner;
import com.jme3.util.clone.JmeCloneable;
import java.util.BitSet;
import java.util.List;
import java.util.logging.Logger;
import jme3utilities.MyCamera;
import jme3utilities.MySkeleton;
import jme3utilities.MySpatial;
import jme3utilities.Validate;
import jme3utilities.debug.AxesVisualizer;
import jme3utilities.debug.BoundsVisualizer;
import jme3utilities.debug.SkeletonVisualizer;
import jme3utilities.math.MyMath;
import jme3utilities.sky.SkyControl;
import jme3utilities.sky.Updater;
import maud.Maud;
import maud.Pose;
import maud.Selection;
import maud.Util;
import maud.model.LoadedCgm;
import maud.model.SceneBones;
import maud.model.SkeletonStatus;
import maud.model.ViewMode;

/**
 * A 3D visualization of a loaded CG model in a scene-mode viewport.
 *
 * @author Stephen Gold sgold@sonic.net
 */
public class SceneView
        implements EditorView, JmeCloneable {
    // *************************************************************************
    // constants and loggers

    /**
     * number of coordinate axes
     */
    final private static int numAxes = 3;
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
    final private AmbientLight ambientLight = new AmbientLight();
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
    /**
     * app state for Bullet physics
     */
    final private BulletAppState bulletAppState;
    /**
     * world transform for the CG model visualization
     */
    private CgmTransform cgmTransform = new CgmTransform();
    /*
     * directional added to the scene (not null)
     */
    final private DirectionalLight mainLight = new DirectionalLight();
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
    public SceneView(LoadedCgm loadedCgm, Node parentNode, ViewPort port1,
            ViewPort port2) {
        Validate.nonNull(loadedCgm, "loaded model");
        Validate.nonNull(parentNode, "parent node");
        Validate.nonNull(port2, "port2");

        cgm = loadedCgm;
        parent = parentNode;
        viewPort1 = port1;
        viewPort2 = port2;
        bulletAppState = makeBullet(port1, port2);
    }
    // *************************************************************************
    // new methods exposed

    /**
     * Add a copy of the specified physics control to the selected spatial and
     * the physics space.
     *
     * @param physicsControl (not null, unaffected)
     */
    public void addPhysicsControl(PhysicsControl physicsControl) {
        Validate.nonNull(physicsControl, "physics control");

        PhysicsControl copy = Cloner.deepClone(physicsControl);
        if (copy instanceof PhysicsRigidBody) {
            PhysicsRigidBody body = (PhysicsRigidBody) copy;
            body.setKinematic(true);
        }
        Spatial spatial = selectedSpatial();
        spatial.addControl(copy);

        PhysicsSpace space = getPhysicsSpace();
        space.add(copy);
    }

    /**
     * Delete the selected spatial and its children, if any.
     */
    public void deleteSubtree() {
        Spatial spatial = selectedSpatial();

        MySpatial.disablePhysicsControls(spatial);
        spatial.removeFromParent();
    }

    /**
     * Delete the specified spatial and its children, if any.
     *
     * @param treePosition tree position of spatial to delete (not null)
     */
    public void deleteSubtree(List<Integer> treePosition) {
        Validate.nonNull(treePosition, "tree position");

        Spatial spatial = cgmRoot;
        for (int childPosition : treePosition) {
            Node node = (Node) spatial;
            spatial = node.getChild(childPosition);
        }

        MySpatial.disablePhysicsControls(spatial);
        spatial.removeFromParent();
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
     * Access the Bullet app state for the scene.
     *
     * @return the pre-existing instance (not null)
     */
    public BulletAppState getBulletAppState() {
        assert bulletAppState != null;
        return bulletAppState;
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
     * Access the main (directional) light added to the scene.
     *
     * @return the pre-existing instance (not null)
     */
    public DirectionalLight getMainLight() {
        assert mainLight != null;
        return mainLight;
    }

    /**
     * Access the physics space for the scene.
     *
     * @return the pre-existing instance (not null)
     */
    public PhysicsSpace getPhysicsSpace() {
        PhysicsSpace space = bulletAppState.getPhysicsSpace();
        assert space != null;
        return space;
    }

    /**
     * Access the spatial for the scene's platform.
     *
     * @return the pre-existing instance, or null if none
     */
    public Spatial getPlatform() {
        return platform;
    }

    /**
     * Access the skeleton visualizer added to the scene.
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
     * Replace the CG model with a newly loaded one.
     *
     * @param cgmRoot (not null)
     */
    public void loadCgm(Spatial cgmRoot) {
        Validate.nonNull(cgmRoot, "model root");

        if (this.cgmRoot != null) {
            MySpatial.disablePhysicsControls(this.cgmRoot);
        }
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
     * Remove an existing physics control from the selected spatial and the
     * physics space.
     *
     * @param position position among the physics controls added to the selected
     * spatial (ge;0)
     */
    public void removePhysicsControl(int position) {
        Validate.nonNegative(position, "position");

        Spatial spatial = selectedSpatial();
        PhysicsControl pc = Util.pcFromPosition(spatial, position);
        pc.setEnabled(false);
        spatial.removeControl(pc);
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
     * Alter the cull hint of the selected spatial.
     *
     * @param newHint new value for cull hint (not null)
     */
    public void setCullHint(Spatial.CullHint newHint) {
        Validate.nonNull(newHint, "cull hint");

        Spatial spatial = selectedSpatial();
        spatial.setCullHint(newHint);
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
     * Alter the queue bucket of the selected spatial.
     *
     * @param newBucket new value for queue bucket (not null)
     */
    public void setQueueBucket(RenderQueue.Bucket newBucket) {
        Validate.nonNull(newBucket, "queue bucket");

        Spatial spatial = selectedSpatial();
        spatial.setQueueBucket(newBucket);
    }

    /**
     * Visualize a different skeleton, or none.
     *
     * @param newSkeleton (may be null, unaffected)
     * @param selectedSpatialFlag where to add controls: false&rarr;CG model
     * root, true&rarr;selected spatial
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
     * Update the scene when unloading the CG model.
     */
    public void unloadCgm() {
        /*
         * Detach the old spatial (if any) from the scene.
         */
        if (cgmRoot != null) {
            MySpatial.disablePhysicsControls(cgmRoot);
            parent.detachChild(cgmRoot);
        }
        setCgmRoot(null);
        animControl = null;
        skeleton = null;
        skeletonControl = null;
        skeletonVisualizer = null;
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
    // EditorView methods

    /**
     * Consider selecting each axis tip and bone in this view.
     *
     * @param selection best selection found so far (not null, modified)
     */
    @Override
    public void considerAll(Selection selection) {
        Camera camera = getCamera();

        for (int axisIndex = 0; axisIndex < numAxes; axisIndex++) {
            Vector3f tipWorld = Maud.gui.tools.axes.tipLocation(cgm, axisIndex);
            if (tipWorld != null) {
                Vector3f tipScreen = camera.getScreenCoordinates(tipWorld);
                Vector2f tipXY = new Vector2f(tipScreen.x, tipScreen.y);
                selection.considerAxis(cgm, axisIndex, false, tipXY);
            }
        }
        /*
         * Determine which bones should be considered.
         */
        Pose pose = cgm.getPose().getPose();
        int numBones = pose.countBones();
        BitSet boneIndexSet = new BitSet(numBones);
        SkeletonStatus options = Maud.getModel().getScene().getSkeleton();
        SceneBones sceneBones = options.bones();
        switch (sceneBones) {
            case All:
                boneIndexSet.set(0, numBones - 1);
                break;
            case InfluencersOnly:
                cgm.bones.listInfluencers(boneIndexSet);
                break;
            case None:
                boneIndexSet.clear(0, numBones - 1);
                break;
            default:
                throw new IllegalStateException();
        }
        int selectedBone = cgm.bone.getIndex();
        if (selectedBone != -1) {
            boneIndexSet.clear(selectedBone);
        }

        Vector2f inputXY = selection.copyInputXY();
        for (int boneIndex = 0; boneIndex < numBones; boneIndex++) {
            if (boneIndexSet.get(boneIndex)) {
                Vector3f modelLocation;
                modelLocation = pose.modelLocation(boneIndex, null);
                Transform worldTransform = worldTransform();
                Vector3f boneWorld;
                boneWorld = worldTransform.transformVector(modelLocation, null);
                Vector3f boneScreen;
                boneScreen = camera.getScreenCoordinates(boneWorld);
                Vector2f boneXY = new Vector2f(boneScreen.x, boneScreen.y);
                float dSquared = boneXY.distanceSquared(inputXY);
                selection.considerBone(cgm, boneIndex, dSquared);
            }
        }
    }

    /**
     * Access the camera used to render this view.
     *
     * @return a pre-existing instance, or null if not rendered
     */
    @Override
    public Camera getCamera() {
        Camera result = null;
        ViewPort viewPort = getViewPort();
        if (viewPort != null && viewPort.isEnabled()) {
            result = viewPort.getCamera();
        }

        return result;
    }

    /**
     * Access the world transform for the CG model in this visualization.
     *
     * @return the pre-existing instance (not null)
     */
    public CgmTransform getTransform() {
        assert cgmTransform != null;
        return cgmTransform;
    }

    /**
     * Read what type of view this is.
     *
     * @return Scene
     */
    @Override
    public ViewType getType() {
        return ViewType.Scene;
    }

    /**
     * Access the view port used to render this view.
     *
     * @return the pre-existing view port
     */
    @Override
    public ViewPort getViewPort() {
        ViewPort result;
        ViewMode viewMode = Maud.getModel().getMisc().getViewMode();
        if (Maud.getModel().getSource().isLoaded()
                || viewMode.equals(ViewMode.Hybrid)) {
            result = viewPort2; // split-screen view port
        } else {
            result = viewPort1; // not split
        }

        return result;
    }

    /**
     * Update this view prior to rendering. (Invoked once per render pass on
     * each instance.)
     *
     * @param ignored not used
     */
    @Override
    public void update(LoadedCgm ignored) {
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
     * Attempt to warp a cursor to the screen coordinates of the mouse pointer.
     */
    @Override
    public void warpCursor() {
        Maud application = Maud.getApplication();
        InputManager inputManager = application.getInputManager();
        Camera camera = getCamera();
        Ray ray = MyCamera.mouseRay(camera, inputManager);
        /*
         * Trace the ray to the CG model's visualization.
         */
        Spatial root = getCgmRoot();
        Vector3f targetContactPoint = findContact(root, ray);

        if (targetContactPoint != null) {
            cgm.scenePov.setCursorLocation(targetContactPoint);
        } else {
            /*
             * The ray missed the CG model; try to trace it to the platform.
             */
            Spatial plat = getPlatform();
            if (plat != null) {
                Vector3f platformContactPoint = findContact(plat, ray);
                if (platformContactPoint != null) {
                    cgm.scenePov.setCursorLocation(platformContactPoint);
                }
            }
        }
    }
    // *************************************************************************
    // JmeCloneable methods

    /**
     * Convert this shallow-cloned view into a deep-cloned one, using the
     * specified cloner and original to resolve copied fields.
     *
     * @param cloner the cloner currently cloning this control (not null)
     * @param original the view from which this view was shallow-cloned (unused)
     */
    @Override
    public void cloneFields(Cloner cloner, Object original) {
        // ambientLight not cloned: shared
        animControl = cloner.clone(animControl);
        // axesVisualizernot cloned: shared
        // boundsVisualizer not cloned: shared
        // bulletAppState not cloned: shared
        // cgm not cloned: set later
        cgmRoot = cloner.clone(cgmRoot);
        cgmTransform = cloner.clone(cgmTransform);
        // cursor not cloned: shared
        // mainLight not cloned: shared
        // parent not cloned: shared
        // platform not cloned: shared
        skeleton = cloner.clone(skeleton);
        skeletonControl = cloner.clone(skeletonControl);
        skeletonVisualizer = cloner.clone(skeletonVisualizer);
        // skyControl not cloned: shared
        // viewPort1, viewPort2 not cloned: shared
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
     * For the specified camera ray, find the 1st point of contact on a triangle
     * facing the camera.
     *
     * @param spatial (not null, unaffected)
     * @param ray (not null, unaffected)
     * @return a new vector in world coordinates, or null if none found
     */
    private Vector3f findContact(Spatial spatial, Ray ray) {
        CollisionResults results = new CollisionResults();
        spatial.collideWith(ray, results);
        /*
         * Collision results are sorted by increaing distance from the camera,
         * so the first result is also the nearest one.
         */
        Camera cam = getCamera();
        Vector3f cameraLocation = cam.getLocation();
        for (int resultIndex = 0; resultIndex < results.size(); resultIndex++) {
            /*
             * Calculate the offset from the camera to the point of contact.
             */
            CollisionResult result = results.getCollision(resultIndex);
            Vector3f contactPoint = result.getContactPoint();
            Vector3f offset = contactPoint.subtract(cameraLocation);
            /*
             * If the dot product of the normal with the offset is negative,
             * then the triangle faces the camera.  Return the point of contact.
             */
            Vector3f normal = result.getContactNormal();
            float dotProduct = offset.dot(normal);
            if (dotProduct < 0f) {
                return contactPoint;
            }
        }

        return null;
    }

    /**
     * Access the scene (root node) of this visualization.
     *
     * @return the pre-existing instance (not null)
     */
    private Spatial getScene() {
        List<Spatial> scenes = viewPort2.getScenes();
        assert scenes.size() == 2 : scenes.size();
        Spatial result = scenes.get(0);

        assert result != null;
        return result;
    }

    /**
     * Create and configure an app state to manage the Bullet physics for this
     * view.
     *
     * @param viewPort1 (may be null)
     * @param viewPort2 (not null)
     * @return a new instance
     */
    private BulletAppState makeBullet(ViewPort viewPort1, ViewPort viewPort2) {
        assert viewPort2 != null;

        float radius = 1000f;
        Vector3f worldMin = new Vector3f(-radius, -radius, -radius);
        Vector3f worldMax = new Vector3f(radius, radius, radius);
        PhysicsSpace.BroadphaseType sweep3;
        sweep3 = PhysicsSpace.BroadphaseType.AXIS_SWEEP_3;
        BulletAppState result = new BulletAppState(worldMin, worldMax, sweep3);

        ViewPort[] viewPorts;
        if (viewPort1 == null) {
            viewPorts = new ViewPort[1];
            viewPorts[0] = viewPort2;
        } else {
            viewPorts = new ViewPort[2];
            viewPorts[0] = viewPort1;
            viewPorts[1] = viewPort2;
        }
        result.setDebugViewPorts(viewPorts);

        result.setDebugEnabled(true);
        result.setSpeed(0f);
        result.setThreadingType(BulletAppState.ThreadingType.PARALLEL);

        Maud application = Maud.getApplication();
        AppStateManager stateManager = application.getStateManager();
        stateManager.attach(result);

        return result;
    }

    /**
     * Alter a newly loaded CG model to prepare it for visualization. Assumes
     * the CG model's root node will be the selected spatial.
     */
    private void prepareForViewing() {
        /*
         * Attach the CG model to the view's scene graph.
         */
        parent.attachChild(cgmRoot);
        /*
         * Use the skeleton from the 1st AnimControl or
         * SkeletonControl in the CG model's root spatial.
         */
        skeleton = MySkeleton.findSkeleton(cgmRoot);
        /*
         * Remove all scene-graph controls except those concerned with physics.
         * Enable those SGCs and configure their physics spaces so that the
         * BulletDebugAppState can render their collision shapes.
         */
        MySpatial.removeNonPhysicsControls(cgmRoot);
        PhysicsSpace space = getPhysicsSpace();
        MySpatial.enablePhysicsControls(cgmRoot, space);
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
        cgmTransform.loadCgm(center, minY, maxExtent);
        /*
         * reset the camera, cursor, and platform
         */
        Vector3f baseLocation = new Vector3f(0f, 0f, 0f);
        cgm.scenePov.setCursorLocation(baseLocation);
        Maud.getModel().getScene().setPlatformDiameter(2f);

        Vector3f cameraLocation = new Vector3f(-2.4f, 1f, 1.6f);
        cgm.scenePov.setCameraLocation(cameraLocation);
    }

    /**
     * Update the pose based on the MVC model.
     */
    private void updatePose() {
        int boneCount = cgm.bones.countBones();
        Pose pose = cgm.getPose().getPose();
        int numTransforms = pose.countBones();
        assert numTransforms == boneCount : numTransforms;
        assert skeleton == null
                || skeleton.getBoneCount() == boneCount : boneCount;

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
        boolean enableShadows = Maud.getModel().getScene().areShadowsRendered();
        if (enableShadows) {
            mode = RenderQueue.ShadowMode.CastAndReceive;
        } else {
            mode = RenderQueue.ShadowMode.Off;
        }
        parent.setShadowMode(mode);
    }

    /**
     * Update the transform of the CG model's parent.
     */
    private void updateTransform() {
        Transform transform = cgmTransform.worldTransform();
        parent.setLocalTransform(transform);
    }
}
