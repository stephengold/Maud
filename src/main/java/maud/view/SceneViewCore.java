/*
 Copyright (c) 2017-2018, Stephen Gold
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
import com.jme3.bullet.BulletAppState;
import com.jme3.bullet.PhysicsSpace;
import com.jme3.collision.CollisionResult;
import com.jme3.input.InputManager;
import com.jme3.light.AmbientLight;
import com.jme3.light.DirectionalLight;
import com.jme3.material.MatParamOverride;
import com.jme3.material.Material;
import com.jme3.material.RenderState;
import com.jme3.math.FastMath;
import com.jme3.math.Matrix4f;
import com.jme3.math.Quaternion;
import com.jme3.math.Ray;
import com.jme3.math.Transform;
import com.jme3.math.Vector2f;
import com.jme3.math.Vector3f;
import com.jme3.renderer.Camera;
import com.jme3.renderer.RenderManager;
import com.jme3.renderer.ViewPort;
import com.jme3.renderer.queue.RenderQueue;
import com.jme3.renderer.queue.RenderQueue.Bucket;
import com.jme3.scene.Geometry;
import com.jme3.scene.Mesh;
import com.jme3.scene.Node;
import com.jme3.scene.Spatial;
import com.jme3.texture.Texture;
import com.jme3.util.clone.Cloner;
import com.jme3.util.clone.JmeCloneable;
import java.util.ArrayList;
import java.util.BitSet;
import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.logging.Logger;
import jme3utilities.MyAsset;
import jme3utilities.MyCamera;
import jme3utilities.MyMesh;
import jme3utilities.MySkeleton;
import jme3utilities.MySpatial;
import jme3utilities.Validate;
import jme3utilities.debug.AxesVisualizer;
import jme3utilities.debug.BoundsVisualizer;
import jme3utilities.debug.SkeletonVisualizer;
import jme3utilities.math.MyMath;
import jme3utilities.math.MyVector3f;
import jme3utilities.sky.SkyControl;
import jme3utilities.sky.Updater;
import jme3utilities.ui.Locators;
import jme3utilities.wes.Pose;
import maud.Maud;
import maud.MaudUtil;
import maud.mesh.PointMesh;
import maud.model.cgm.Cgm;
import maud.model.cgm.DisplayedPose;
import maud.model.cgm.ScenePov;
import maud.model.cgm.SelectedSkeleton;
import maud.model.option.MiscOptions;
import maud.model.option.ShowBones;
import maud.model.option.ViewMode;
import maud.model.option.scene.SkeletonOptions;

/**
 * The essential fields and methods of the SceneView class.
 *
 * @author Stephen Gold sgold@sonic.net
 */
public class SceneViewCore
        implements EditorView, JmeCloneable {
    // *************************************************************************
    // constants and loggers

    /**
     * linear size of the marker for the selected vertex (in pixels)
     */
    final private static float vertexSize = 12f;
    /**
     * message logger for this class
     */
    final private static Logger logger
            = Logger.getLogger(SceneViewCore.class.getName());
    /**
     * local copy of {@link com.jme3.math.Quaternion#IDENTITY}
     */
    final private static Quaternion rotationIdentity = new Quaternion();
    /**
     * local copy of {@link com.jme3.math.Transform#IDENTITY}
     */
    final private static Transform transformIdentity = new Transform();
    /**
     * location of the camera after loading a new C-G model
     */
    final private static Vector3f cameraStartLocation
            = new Vector3f(-2.4f, 1f, 1.6f);
    /**
     * location of the 3-D cursor after loading a new C-G model
     */
    final private static Vector3f cursorStartLocation
            = new Vector3f(0f, 0f, 0f);
    /**
     * local copy of {@link com.jme3.math.Vector3f#UNIT_XYZ}
     */
    final private static Vector3f scaleIdentity = new Vector3f(1f, 1f, 1f);
    /**
     * local copy of {@link com.jme3.math.Vector3f#ZERO}
     */
    final private static Vector3f translateIdentity = new Vector3f(0f, 0f, 0f);
    // *************************************************************************
    // fields

    /*
     * ambient light added to the scene (not null)
     */
    final private AmbientLight ambientLight = new AmbientLight();
    /**
     * animation control with the selected skeleton - apparently needed for
     * software skinning, though it's unclear why
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
     * C-G model that appears in this view (not null)
     */
    private Cgm cgm;
    /**
     * world transform for the C-G model rendered in this view
     */
    private CgmTransform cgmTransform = new CgmTransform();
    /*
     * directional added to the scene (not null)
     */
    final private DirectionalLight mainLight = new DirectionalLight();
    /**
     * indicator for the 3-D cursor, or null if none
     */
    private Geometry cursor;
    /**
     * attachment point for this view's copy of the C-G model (applies
     * shadowMode and transforms)
     */
    final private Node parent;
    /**
     * supporting platform
     */
    final private Platform platform = new Platform(this);
    /**
     * selected skeleton in this view's copy of the C-G model
     */
    private Skeleton skeleton;
    /**
     * skeleton control with the selected skeleton
     */
    private SkeletonControl skeletonControl;
    /**
     * skeleton visualizer with the selected skeleton, or null if none
     */
    private SkeletonVisualizer skeletonVisualizer;
    /**
     * sky simulation added to the scene
     */
    private SkyControl skyControl;
    /**
     * root spatial in this view's copy of the C-G model
     */
    private Spatial cgmRoot;
    /**
     * test projectile
     */
    final private Projectile projectile = new Projectile(this);
    /**
     * spatial to visualize the selected vertex
     */
    private Spatial vertexSpatial;
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
     * Instantiate a new view.
     *
     * @param ownerCgm C-G model that will own this view (not null, aliases
     * created)
     * @param parentNode attachment point in the scene graph (not null, alias
     * created)
     * @param port1 initial view port, or null for none (alias created)
     * @param port2 view port to use after the screen is split (not null, alias
     * created)
     */
    protected SceneViewCore(Cgm ownerCgm, Node parentNode, ViewPort port1,
            ViewPort port2) {
        Validate.nonNull(ownerCgm, "loaded model");
        Validate.nonNull(parentNode, "parent node");
        Validate.nonNull(port2, "port2");

        cgm = ownerCgm;
        parent = parentNode;
        viewPort1 = port1;
        viewPort2 = port2;
        bulletAppState = makeBullet(port1, port2);
    }
    // *************************************************************************
    // new methods exposed

    /**
     * Attach an orphan spatial to the scene's root node.
     *
     * @param orphan spatial to clone (not null)
     */
    void attachToSceneRoot(Spatial orphan) {
        assert MySpatial.isOrphan(orphan);

        Node scene = getSceneRoot();
        scene.attachChild(orphan);
    }

    /**
     * Find the the tree position of the specified spatial in this view's copy
     * of the C-G model.
     *
     * @param input spatial to search for (not null, unaffected)
     * @return a new tree-position instance, or null if not found
     */
    List<Integer> findPosition(Spatial input) {
        assert input != null;

        List<Integer> treePosition = new ArrayList<>(4);
        boolean success = MaudUtil.findPosition(input, cgmRoot, treePosition);
        if (!success) {
            treePosition = null;
        }

        return treePosition;
    }

    /**
     * Find a geometry that is animated by the selected skeleton control.
     *
     * @return a pre-existing instance, or cgmRoot if none found
     */
    Spatial findTransformSpatial() {
        Spatial result = cgmRoot;
        List<Integer> treePosition = cgm.getSkeleton().findAnimatedGeometry();
        if (treePosition != null) {
            for (int childPosition : treePosition) {
                Node node = (Node) result;
                result = node.getChild(childPosition);
            }
        }

        return result;
    }

    /**
     * Access the axes visualizer added to the scene.
     *
     * @return the pre-existing instance (not null)
     */
    AxesVisualizer getAxesVisualizer() {
        assert axesVisualizer != null;
        return axesVisualizer;
    }

    /**
     * Access the bounds visualizer added to the scene.
     *
     * @return the pre-existing instance (not null)
     */
    BoundsVisualizer getBoundsVisualizer() {
        assert boundsVisualizer != null;
        return boundsVisualizer;
    }

    /**
     * Access the Bullet app state for the scene.
     *
     * @return the pre-existing instance (not null)
     */
    BulletAppState getBulletAppState() {
        assert bulletAppState != null;
        return bulletAppState;
    }

    /**
     * Access the indicator for the 3-D cursor.
     *
     * @return the pre-existing instance, or null if none
     */
    Geometry getCursor() {
        return cursor;
    }

    /**
     * Access the main (directional) light added to the scene.
     *
     * @return the pre-existing instance (not null)
     */
    DirectionalLight getMainLight() {
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
     * Access the supporting platform for the scene.
     *
     * @return the pre-existing instance (not null)
     */
    Platform getPlatform() {
        assert platform != null;
        return platform;
    }

    /**
     * Access the point-of-view.
     *
     * @return the pre-existing instance (not null)
     */
    ScenePov getPov() {
        ScenePov pov = cgm.getScenePov();
        assert pov != null;
        return pov;
    }

    /**
     * Access the test projectile for the scene.
     *
     * @return the pre-existing instance (not null)
     */
    public Projectile getProjectile() {
        assert projectile != null;
        return projectile;
    }

    /**
     * Access the skeleton visualizer added to the scene.
     *
     * @return the pre-existing instance, or null if none
     */
    SkeletonVisualizer getSkeletonVisualizer() {
        return skeletonVisualizer;
    }

    /**
     * Access the sky control added to the scene.
     *
     * @return the pre-existing instance (not null)
     */
    SkyControl getSkyControl() {
        assert skyControl != null;
        return skyControl;
    }

    /**
     * Access the world transform for the C-G model.
     *
     * @return the pre-existing instance (not null)
     */
    public CgmTransform getTransform() {
        assert cgmTransform != null;
        return cgmTransform;
    }

    /**
     * Access the spatial that visualizes the selected vertex.
     *
     * @return the pre-existing instance (not null)
     */
    Spatial getVertexSpatial() {
        assert vertexSpatial != null;
        return vertexSpatial;
    }

    /**
     * Replace the C-G model with a newly loaded one.
     *
     * @param loadedCgmRoot root spatial (not null)
     */
    public void loadCgm(Spatial loadedCgmRoot) {
        Validate.nonNull(loadedCgmRoot, "loaded model root");

        if (cgmRoot != null) {
            MySpatial.disablePhysicsControls(cgmRoot);
        }
        parent.detachAllChildren();
        setCgmRoot(loadedCgmRoot);

        prepareForViewing();
    }

    /**
     * Re-install the C-G model in the scene graph. Invoked after restoring a
     * checkpoint.
     */
    public void postMakeLive() {
        /*
         * Detach any old visualization from the scene graph.
         */
        parent.detachAllChildren();
        /*
         * Attach this visualization.
         */
        if (cgmRoot != null) {
            parent.attachChild(cgmRoot);
            PhysicsSpace space = getPhysicsSpace();
            MySpatial.enablePhysicsControls(cgmRoot, space);
        }
    }

    /**
     * Un-install the C-G model from the scene graph. Invoked before restoring a
     * checkpoint.
     */
    public void preMakeLive() {
        if (cgmRoot != null) {
            MySpatial.disablePhysicsControls(cgmRoot);
        }
    }

    /**
     * Access the selected spatial in this view's copy of its C-G model.
     *
     * @return the pre-existing spatial (not null)
     */
    public Spatial selectedSpatial() {
        Spatial result = cgm.getSpatial().underRoot(cgmRoot);
        assert result != null;
        return result;
    }

    /**
     * Alter which loaded C-G model corresponds with this view. Invoked after
     * cloning.
     *
     * @param newCgm (not null, alias created)
     */
    public void setCgm(Cgm newCgm) {
        Validate.nonNull(newCgm, "new model");
        assert this == newCgm.getSceneView();

        cgm = newCgm;
    }

    /**
     * Alter which cursor indicator is attached to the scene graph.
     *
     * @param cursorSpatial (may be null)
     */
    void setCursor(Geometry cursorSpatial) {
        if (cursor != null) {
            cursor.removeFromParent();
        }
        if (cursorSpatial != null) {
            attachToSceneRoot(cursorSpatial);
        }
        cursor = cursorSpatial;
    }

    /**
     * Visualize using a different skeleton, or none.
     *
     * @param selectedSkeleton (may be null, unaffected)
     * @param selectedSpatialFlag where to add controls: false&rarr;C-G model
     * root, true&rarr;selected spatial
     */
    public void setSkeleton(Skeleton selectedSkeleton,
            boolean selectedSpatialFlag) {
        clearSkeleton();

        if (selectedSkeleton != null) {
            Spatial controlled;
            if (selectedSpatialFlag) {
                controlled = selectedSpatial();
            } else {
                controlled = cgmRoot;
            }

            skeleton = Cloner.deepClone(selectedSkeleton);
            MySkeleton.setUserControl(skeleton, true);

            animControl = new AnimControl(skeleton);
            controlled.addControl(animControl);

            skeletonControl = new SkeletonControl(skeleton);
            controlled.addControl(skeletonControl);
            skeletonControl.setHardwareSkinningPreferred(false);

            AssetManager assetManager = Locators.getAssetManager();
            skeletonVisualizer = new SkeletonVisualizer(assetManager);
            controlled.addControl(skeletonVisualizer);
            skeletonVisualizer.setSkeleton(skeleton);
            Spatial ts = MySpatial.findAnimatedGeometry(controlled);
            if (ts == null) {
                ts = controlled;
            }
            skeletonVisualizer.setTransformSpatial(ts);
            skeletonVisualizer.setEnabled(true);
            /*
             * Make the visualizer add its geometries to the scene graph.
             * This is vital when loading BVH files.
             */
            skeletonVisualizer.update(0f);
        }
    }

    /**
     * Update the view when unloading the C-G model.
     */
    public void unloadCgm() {
        /*
         * Detach the old spatial (if any) from the scene graph.
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
    // *************************************************************************
    // EditorView methods

    /**
     * Consider selecting each axis tip in this view.
     *
     * @param selection best selection found so far (not null, modified)
     */
    @Override
    public void considerAxes(Selection selection) {
        Validate.nonNull(selection, "selection");

        Camera camera = getCamera();
        for (int axisIndex = MyVector3f.firstAxis;
                axisIndex < MyVector3f.numAxes; axisIndex++) {
            Vector3f tipWorld = axesVisualizer.tipLocation(axisIndex);
            if (tipWorld != null) {
                Vector3f tipScreen = camera.getScreenCoordinates(tipWorld);
                Vector2f tipXY = new Vector2f(tipScreen.x, tipScreen.y);
                selection.considerAxis(cgm, axisIndex, false, tipXY);
            }
        }
    }

    /**
     * Consider selecting each visualized bone in this view. The selected bone
     * is excluded from consideration.
     *
     * @param selection best selection found so far (not null, modified)
     */
    @Override
    public void considerBones(Selection selection) {
        Validate.nonNull(selection, "selection");
        /*
         * Determine which bones to consider.
         */
        DisplayedPose displayedPose = cgm.getPose();
        int numBones = displayedPose.get().countBones();
        SkeletonOptions options = Maud.getModel().getScene().getSkeleton();
        ShowBones showBones = options.getShowBones();
        BitSet boneIndexSet = cgm.getSkeleton().listShown(showBones, null);
        int selectedBone = cgm.getBone().getIndex();
        if (selectedBone != -1) {
            boneIndexSet.clear(selectedBone);
        }

        Camera camera = getCamera();
        Vector2f inputXY = selection.copyInputXY();
        for (int boneIndex = 0; boneIndex < numBones; boneIndex++) {
            if (boneIndexSet.get(boneIndex)) {
                Vector3f world = displayedPose.worldLocation(boneIndex, null);
                Vector3f boneScreen = camera.getScreenCoordinates(world);
                Vector2f boneXY = new Vector2f(boneScreen.x, boneScreen.y);
                float dSquared = boneXY.distanceSquared(inputXY);
                selection.considerBone(cgm, boneIndex, dSquared);
            }
        }
    }

    /**
     * Consider selecting the boundary of this view.
     *
     * @param selection best selection found so far (not null, modified)
     */
    @Override
    public void considerBoundaries(Selection selection) {
        Validate.nonNull(selection, "selection");

        Camera camera = getCamera();
        if (!MaudUtil.isFullWidth(camera)) {
            MiscOptions misc = Maud.getModel().getMisc();
            int width = camera.getWidth();
            float boundaryX = misc.getXBoundary() * width;
            Vector2f inputXY = selection.copyInputXY();
            float dSquared = FastMath.sqr(inputXY.x - boundaryX);
            selection.considerBoundary(dSquared);
        }
    }

    /**
     * Consider selecting each gnomon in this view.
     *
     * @param selection best selection found so far
     */
    @Override
    public void considerGnomons(Selection selection) {
        // no gnomons in scene views
    }

    /**
     * Consider selecting each keyframe in this view.
     *
     * @param selection best selection found so far
     */
    @Override
    public void considerKeyframes(Selection selection) {
        // no keyframes in scene views
    }

    /**
     * Consider selecting each mesh vertex in this view.
     *
     * @param selection best selection found so far (not null, modified)
     */
    @Override
    public void considerVertices(Selection selection) {
        Validate.nonNull(selection, "selection");

        Camera camera = getCamera();
        InputManager inputManager = Maud.getApplication().getInputManager();
        Ray ray = MyCamera.mouseRay(camera, inputManager);
        /*
         * Trace the ray to the C-G model's visualization.
         */
        CollisionResult collision = MaudUtil.findCollision(cgmRoot, ray);
        if (collision != null) {
            Geometry geometry = collision.getGeometry();
            Mesh mesh = geometry.getMesh();
            int triangleIndex = collision.getTriangleIndex();
            int[] vertexIndices = new int[3];
            mesh.getTriangle(triangleIndex, vertexIndices);

            Pose pose = cgm.getPose().get();
            Matrix4f[] matrices = pose.skin(null);
            Vector3f worldPosition = new Vector3f();

            for (int vertexIndex : vertexIndices) {
                MyMesh.vertexWorldLocation(geometry, vertexIndex, matrices,
                        worldPosition);
                Vector3f screen = camera.getScreenCoordinates(worldPosition);
                Vector2f screenXY = new Vector2f(screen.x, screen.y);
                selection.considerVertex(cgm, geometry, vertexIndex, screenXY);
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
    public void update(Cgm ignored) {
        if (skyControl == null) {  // TODO add an init method
            /*
             * Initialize the scene graph on first update.
             */
            createAxes();
            createBounds();
            createLights();
            createSky();
            createVertex();
        }

        Camera camera = getCamera();
        if (camera != null) {
            List<Integer> modelRootPosition = new ArrayList<>(0);
            updateLocalTransforms(cgmRoot, modelRootPosition);

            updateParentShadowMode();
            updateParentTransform();
            updatePose();
            SceneUpdater.update(cgm);
            skyControl.setCamera(camera);
        }
    }

    /**
     * Attempt to warp a cursor to the screen coordinates of the mouse pointer.
     */
    @Override
    public void warpCursor() {
        Camera camera = getCamera();
        InputManager inputManager = Maud.getApplication().getInputManager();
        Ray ray = MyCamera.mouseRay(camera, inputManager);
        /*
         * Trace the ray to the C-G model.
         */
        CollisionResult collision = MaudUtil.findCollision(cgmRoot, ray);
        if (collision != null) {
            Vector3f contactPoint = collision.getContactPoint();
            getPov().setCursorLocation(contactPoint);
        } else {
            /*
             * The ray missed the C-G model; try to trace it to the platform.
             */
            platform.warpCursor(ray);
        }
    }
    // *************************************************************************
    // JmeCloneable methods

    /**
     * Don't use this method; use a {@link com.jme3.util.clone.Cloner} instead.
     *
     * @return never
     * @throws CloneNotSupportedException always
     */
    @Override
    public SceneViewCore clone() throws CloneNotSupportedException {
        super.clone();
        throw new CloneNotSupportedException("use a cloner");
    }

    /**
     * Callback from {@link com.jme3.util.clone.Cloner} to convert this
     * shallow-cloned instance into a deep-cloned one, using the specified
     * cloner and original to resolve copied fields.
     *
     * @param cloner the cloner currently cloning this control (not null)
     * @param original the view from which this view was shallow-cloned (unused)
     */
    @Override
    public void cloneFields(Cloner cloner, Object original) {
        // ambientLight not cloned: shared
        animControl = cloner.clone(animControl);
        // axesVisualizer not cloned: shared
        // boundsVisualizer not cloned: shared
        // bulletAppState not cloned: shared
        // cgm not cloned: set later
        cgmRoot = cloner.clone(cgmRoot);
        cgmTransform = cloner.clone(cgmTransform);
        // cursor not cloned: shared
        // mainLight not cloned: shared
        // parent not cloned: shared
        // platform not cloned: shared
        // projectile not cloned: shared
        skeleton = cloner.clone(skeleton);
        skeletonControl = cloner.clone(skeletonControl);
        skeletonVisualizer = cloner.clone(skeletonVisualizer);
        // skyControl not cloned: shared
        // vertexSpatial not cloned: shared
        // viewPort1, viewPort2 not cloned: shared
    }

    /**
     * Create a shallow clone for the JME cloner.
     *
     * @return a new instance
     */
    @Override
    public SceneViewCore jmeClone() {
        try {
            SceneViewCore clone = (SceneViewCore) super.clone();
            return clone;
        } catch (CloneNotSupportedException exception) {
            throw new RuntimeException(exception);
        }
    }
    // *************************************************************************
    // protected methods

    /**
     * Find the selected material-parameter override in this view's copy of its
     * C-G model.
     *
     * @return the pre-existing instance (not null)
     */
    protected MatParamOverride findSelectedMpo() {
        MatParamOverride result = null;
        Spatial spatial = selectedSpatial();
        String parameterName = cgm.getOverride().getName();
        Collection<MatParamOverride> mpos = spatial.getLocalMatParamOverrides();
        for (MatParamOverride mpo : mpos) {
            if (mpo.getName().equals(parameterName)) {
                result = mpo;
            }
        }

        assert result != null;
        return result;
    }

    /**
     * Find a spatial specified by positional indices.
     *
     * @param treePosition tree position of the spatial (not null, unaffected)
     * @return the pre-existing spatial
     */
    protected Spatial findSpatial(List<Integer> treePosition) {
        assert treePosition != null;

        Spatial spatial = cgmRoot;
        for (int childPosition : treePosition) {
            Node node = (Node) spatial;
            spatial = node.getChild(childPosition);
        }

        return spatial;
    }

    /**
     * Access the root spatial in this view's copy of the C-G model.
     *
     * @return the pre-existing instance (not null)
     */
    protected Spatial getCgmRoot() {
        assert cgmRoot != null;
        return cgmRoot;
    }

    /**
     * Access the C-G model that appears in this view.
     *
     * @return the pre-existing instance (not null)
     */
    protected Cgm getCgm() {
        assert cgm != null;
        return cgm;
    }

    /**
     * Add all physics ids used by this view to the specified set.
     *
     * @param addResult (added to if not null)
     * @return an expanded list (either addResult or a new instance)
     */
    protected Set<Long> listIds(Set<Long> addResult) {
        addResult = projectile.listIds(addResult);
        platform.listIds(addResult);

        return addResult;
    }
    // *************************************************************************
    // private methods

    /**
     * Visualize in bind pose, without a skeleton.
     */
    private void clearSkeleton() {
        if (skeleton != null) {
            int numBones = skeleton.getBoneCount();
            for (int boneIndex = 0; boneIndex < numBones; boneIndex++) {
                Bone bone = skeleton.getBone(boneIndex);
                bone.setUserTransforms(translateIdentity, rotationIdentity,
                        scaleIdentity);
            }
            skeleton.updateWorldVectors();
            skeletonControl.update(0f);
            RenderManager rm = Maud.getApplication().getRenderManager();
            skeletonControl.render(rm, null);
            skeleton = null;
        }
        /*
         * Remove any skeleton-dependent S-G controls from the scene graph.
         */
        if (animControl != null) {
            Spatial controlled = animControl.getSpatial();
            boolean success = controlled.removeControl(animControl);
            assert success;
            animControl = null;
        }
        if (skeletonControl != null) {
            Spatial controlled = skeletonControl.getSpatial();
            boolean success = controlled.removeControl(skeletonControl);
            assert success;
            skeletonControl = null;
        }
        if (skeletonVisualizer != null) {
            Spatial controlled = skeletonVisualizer.getSpatial();
            boolean success = controlled.removeControl(skeletonVisualizer);
            assert success;
            skeletonVisualizer = null;
        }
    }

    /**
     * Add an axes visualizer to the scene graph.
     */
    private void createAxes() {
        AssetManager assetManager = Locators.getAssetManager();
        axesVisualizer = new AxesVisualizer(assetManager, 1f, 1f);

        Node axesNode = new Node("axes node");
        axesNode.addControl(axesVisualizer);
        attachToSceneRoot(axesNode);
    }

    /**
     * Create a bounds visualizer and add it to the scene graph.
     */
    private void createBounds() {
        assert boundsVisualizer == null;

        AssetManager assetManager = Locators.getAssetManager();
        boundsVisualizer = new BoundsVisualizer(assetManager);
        Node scene = getSceneRoot();
        scene.addControl(boundsVisualizer);
    }

    /**
     * Name 2 lights and add them to the scene graph.
     */
    private void createLights() {
        Node scene = getSceneRoot();
        int numLights = scene.getLocalLightList().size();
        assert numLights == 0 : numLights;
        /*
         * Name the lights.
         */
        ambientLight.setName("ambient light");
        mainLight.setName("main light");
        /*
         * Light the scene.
         */
        scene.addLight(ambientLight);
        scene.addLight(mainLight);
    }

    /**
     * Create a sky simulation and add it to the scene graph.
     */
    private void createSky() {
        assert skyControl == null;

        AssetManager assetManager = Locators.getAssetManager();
        Camera camera = viewPort2.getCamera();
        skyControl = new SkyControl(assetManager, camera, 0.9f, false, true);
        Node scene = getSceneRoot();
        scene.addControl(skyControl);

        Updater updater = skyControl.getUpdater();
        updater.setAmbientLight(ambientLight);
        updater.setMainLight(mainLight);
        //updater.addShadowFilter(dlsf);
    }

    /**
     * Create a selected-vertex visualization and attach it to the scene graph.
     */
    private void createVertex() {
        assert vertexSpatial == null;

        AssetManager assetManager = Locators.getAssetManager();
        Material material = new Material(assetManager,
                "MatDefs/wireframe/multicolor2.j3md");
        material.setFloat("PointSize", vertexSize);
        Texture poseShape = MyAsset.loadTexture(assetManager,
                "Textures/shapes/saltire.png");
        material.setTexture("PointShape", poseShape);
        RenderState rs = material.getAdditionalRenderState();
        rs.setBlendMode(RenderState.BlendMode.Alpha);
        rs.setDepthTest(false);

        Mesh mesh = new PointMesh();
        vertexSpatial = new Geometry("vertex", mesh);
        vertexSpatial.setMaterial(material);
        vertexSpatial.setQueueBucket(Bucket.Transparent);

        attachToSceneRoot(vertexSpatial);
    }

    /**
     * Access the root node of the main scene graph.
     *
     * @return the pre-existing instance (not null)
     */
    private Node getSceneRoot() {
        List<Spatial> scenes = viewPort2.getScenes();
        int numScenes = scenes.size();
        if (bulletAppState.isEnabled()) {
            assert numScenes == 2 : numScenes;
        } else {
            assert numScenes == 1 : numScenes;
        }

        Spatial spatial = scenes.get(0);
        Node node = (Node) spatial;

        assert node != null;
        return node;
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

        float radius = 100f;
        Vector3f worldMin = new Vector3f(-radius, -radius, -radius);
        Vector3f worldMax = new Vector3f(radius, radius, radius);
        BulletAppState result = new BulletAppState(worldMin, worldMax,
                PhysicsSpace.BroadphaseType.AXIS_SWEEP_3);

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
     * Alter a newly loaded C-G model to prepare it for visualization.
     */
    private void prepareForViewing() {
        /*
         * Attach the C-G model to the view's scene graph.
         */
        parent.attachChild(cgmRoot);
        /*
         * Use the skeleton from the 1st AnimControl or
         * SkeletonControl in the C-G model's root spatial.
         */
        Skeleton selectedSkeleton = MySkeleton.findSkeleton(cgmRoot);
        /*
         * Remove all scene-graph controls except those concerned with physics.
         * Enable those S-G controls and configure their physics spaces so that
         * the BulletDebugAppState will render their collision shapes.
         */
        MySpatial.removeNonPhysicsControls(cgmRoot);
        PhysicsSpace space = getPhysicsSpace();
        MySpatial.enablePhysicsControls(cgmRoot, space);
        /*
         * Create and add scene-graph controls for the skeleton.
         */
        setSkeleton(selectedSkeleton, false);
        /*
         * Configure the CG-model transform based on the ranges of vertex
         * coordinates in the C-G model.
         */
        parent.setLocalTransform(transformIdentity);
        Vector3f[] minMax = MySpatial.findMinMaxCoords(cgmRoot);
        Vector3f extent = minMax[1].subtract(minMax[0]);
        Vector3f center = MyVector3f.midpoint(minMax[0], minMax[1]);
        boolean zUp = Maud.getModel().getMisc().getLoadZup();
        float baseElevation = zUp ? minMax[0].z : minMax[0].y;
        float maxExtent = MyMath.max(extent.x, extent.y, extent.z);
        cgmTransform.loadCgm(center, baseElevation, maxExtent, zUp);
        /*
         * Reset the camera position and 3-D cursor location.
         */
        ScenePov pov = getPov();
        pov.setCursorLocation(cursorStartLocation);
        pov.setCameraLocation(cameraStartLocation);

        projectile.delete();
    }

    /**
     * Visualize a different C-G model, or none.
     *
     * @param newCgmRoot C-G model's root spatial, or null if none (unaffected)
     */
    private void setCgmRoot(Spatial newCgmRoot) {
        if (newCgmRoot == null) {
            cgmRoot = null;
        } else {
            cgmRoot = newCgmRoot.clone();
        }
    }

    /**
     * Copy the local transform of each spatial in the specified subtree from
     * the MVC model. Note: recursive!
     *
     * @param spatial subtree of the scene graph (not null)
     * @param position tree position of subtree (not null, unaffected)
     */
    private void updateLocalTransforms(Spatial spatial,
            List<Integer> position) {
        assert position != null;
        assert spatial != null;
        /*
         * Copy local transform from the MVC model.
         */
        Transform transform = cgm.getLocalTransform(position);
        spatial.setLocalTransform(transform);

        int numChildren = cgm.countChildren(position);
        if (numChildren > 0) {
            Node node = (Node) spatial;
            int depth = position.size();
            List<Integer> childPosition = new ArrayList<>(depth + 1);
            childPosition.addAll(position);
            childPosition.add(-1);
            for (int childIndex = 0; childIndex < numChildren; childIndex++) {
                Spatial childSpatial = node.getChild(childIndex);
                childPosition.set(depth, childIndex);
                updateLocalTransforms(childSpatial, childPosition);
            }
        }
    }

    /**
     * Update the shadow mode of the C-G model's parent from the MVC model.
     */
    private void updateParentShadowMode() {
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
     * Update the transform of the C-G model's parent from the MVC model.
     */
    private void updateParentTransform() {
        Transform transform = cgmTransform.worldTransform();
        parent.setLocalTransform(transform);
    }

    /**
     * Update bone transforms based on the displayed pose in the MVC model.
     */
    private void updatePose() {
        SelectedSkeleton ss = cgm.getSkeleton();
        int boneCount = ss.countBones();
        Pose pose = cgm.getPose().get();
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

            List<Integer> nodePosition = ss.attachmentsPosition(boneIndex);
            if (nodePosition != null) {
                Node attachNode = (Node) cgmRoot;
                for (int childIndex : nodePosition) {
                    Spatial parentSpatial = attachNode.getChild(childIndex);
                    attachNode = (Node) parentSpatial;
                }

                pose.modelTransform(boneIndex, transform);
                Spatial transformSpatial = findTransformSpatial();
                if (!MySpatial.isIgnoringTransforms(transformSpatial)) {
                    Transform worldTransform = transformSpatial.getWorldTransform();
                    transform.combineWithParent(worldTransform);
                }
                MySpatial.setWorldTransform(attachNode, transform);
            }
        }
    }
}
