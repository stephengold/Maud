/*
 Copyright (c) 2017-2019, Stephen Gold
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

import com.jme3.asset.AssetManager;
import com.jme3.renderer.Camera;
import com.jme3.renderer.RenderManager;
import com.jme3.renderer.ViewPort;
import com.jme3.renderer.queue.RenderQueue;
import com.jme3.scene.Node;
import com.jme3.shadow.DirectionalLightShadowRenderer;
import java.util.logging.Level;
import java.util.logging.Logger;
import jme3utilities.MyCamera;
import jme3utilities.Validate;
import jme3utilities.ui.Locators;
import maud.model.EditorModel;
import maud.model.option.MiscOptions;
import maud.model.option.ViewMode;
import maud.model.option.scene.RenderOptions;
import maud.view.ScoreView;
import maud.view.scene.SceneView;

/**
 * Viewports used by Maud's "editor" screen.
 *
 * @author Stephen Gold sgold@sonic.net
 */
public class EditorViewPorts {
    // *************************************************************************
    // enums

    private enum Side {
        Left, Right;
    }
    // *************************************************************************
    // constants and loggers

    /**
     * message logger for this class
     */
    final private static Logger logger
            = Logger.getLogger(EditorViewPorts.class.getName());
    // *************************************************************************
    // fields

    /**
     * base view port for the left side of the display in scene mode
     */
    private static ViewPort sourceSceneBase;
    /**
     * overlay view port for the left side of the display in scene mode
     */
    private static ViewPort sourceSceneOverlay;
    /**
     * left side of the display in score mode
     */
    private static ViewPort sourceScore;
    /**
     * base view port for the right side of the display in hybrid mode or scene
     * mode
     */
    private static ViewPort targetSceneRightBase;
    /**
     * overlay view port for the right side of the display in hybrid mode or
     * scene mode
     */
    private static ViewPort targetSceneRightOverlay;
    /**
     * base view port for the whole display in scene mode, aka "Default"
     */
    private static ViewPort targetSceneWideBase;
    /**
     * overlay view port for the whole display in scene mode
     */
    private static ViewPort targetSceneWideOverlay;
    /**
     * left side of the display in hybrid mode
     */
    private static ViewPort targetScoreLeft;
    /**
     * right side of the display in score mode
     */
    private static ViewPort targetScoreRight;
    /**
     * whole display in score mode
     */
    private static ViewPort targetScoreWide;
// *************************************************************************
    // constructors

    /**
     * A private constructor to inhibit instantiation of this class.
     */
    private EditorViewPorts() {
    }
    // *************************************************************************
    // new methods exposed

    /**
     * Add a shadow renderer to the specified view port, without specifying a
     * light.
     *
     * @param vp which view port (not null, modified)
     * @return the new, disabled instance
     */
    public static DirectionalLightShadowRenderer addShadows(ViewPort vp) {
        Validate.nonNull(vp, "view port");

        AssetManager manager = Locators.getAssetManager();
        RenderOptions options = Maud.getModel().getScene().getRender();
        int mapSize = options.shadowMapSize();
        int numSplits = options.numSplits();
        DirectionalLightShadowRenderer dlsr
                = new DirectionalLightShadowRenderer(manager, mapSize,
                        numSplits);
        vp.addProcessor(dlsr);

        return dlsr;
    }

    /**
     * Test whether the screen is split.
     *
     * @return true if split, otherwise false
     */
    static boolean isSplitScreen() {
        EditorModel editorModel = Maud.getModel();
        boolean twoModelsLoaded = editorModel.getSource().isLoaded();
        ViewMode viewMode = editorModel.getMisc().viewMode();
        boolean split = twoModelsLoaded || viewMode == ViewMode.Hybrid;

        return split;
    }

    /**
     * Initialization performed during the first invocation of
     * {@link #simpleUpdate(float)}.
     */
    static void startup1() {
        /*
         * Configure the default view port, camera, and root node
         * for use by the target scene view.
         */
        Maud application = Maud.getApplication();
        targetSceneWideBase = application.getViewPort();
        Camera camera = targetSceneWideBase.getCamera();
        camera.setName("Target Scene Wide");
        Node targetBRoot = application.getRootNode();
        targetBRoot.setName("root for target scene base");
        /*
         * Create 2 view ports for split-display scene views.
         */
        Node sourceSceneParent = createSourceSceneViewPort();
        Node targetSceneParent = createTargetSceneViewPort();
        /*
         * Create 2 root nodes for scene-view overlays.
         */
        Node sourceOverlayNode = new Node("root for source scene overlays");
        Node targetOverlayNode = new Node("root for target scene overlays");
        /*
         * Create 3 view ports for scene-view overlays.
         */
        sourceSceneOverlay = createOverlay(sourceSceneBase, sourceOverlayNode);
        targetSceneRightOverlay = createOverlay(targetSceneRightBase,
                targetOverlayNode);
        targetSceneWideOverlay = createOverlay(targetSceneWideBase,
                targetOverlayNode);
        /*
         * Create 4 view ports for score views.
         */
        createSourceScoreViewPort();
        createTargetScoreLeftViewPort();
        createTargetScoreRightViewPort();
        createTargetScoreWideViewPort();
        /*
         * Create 2 scene views, each with its own bulletAppState.
         */
        EditorModel editorModel = Maud.getModel();
        Node sourceORoot = (Node) sourceSceneOverlay.getScenes().get(0);
        SceneView sourceSceneView = new SceneView(editorModel.getSource(),
                sourceSceneParent, null, sourceSceneBase, sourceORoot);
        Node targetORoot = (Node) targetSceneWideOverlay.getScenes().get(0);
        SceneView targetSceneView = new SceneView(editorModel.getTarget(),
                targetSceneParent, targetSceneWideBase, targetSceneRightBase,
                targetORoot);
        /*
         * Create 2 score views.
         */
        ScoreView sourceScoreView = new ScoreView(null, sourceScore, null);
        ScoreView targetScoreView = new ScoreView(targetScoreWide,
                targetScoreRight, targetScoreLeft);
        /*
         * Attach views to C-G model slots.
         */
        editorModel.getSource().setViews(sourceSceneView, sourceScoreView);
        editorModel.getTarget().setViews(targetSceneView, targetScoreView);
        /*
         * Create the view port for the boundary's drag handle.
         */
        createBoundaryViewPort();
    }

    /**
     * Update the configuration of view ports to reflect the MVC model.
     */
    static void update() {
        EditorModel editorModel = Maud.getModel();
        boolean twoModelsLoaded = editorModel.getSource().isLoaded();

        MiscOptions misc = editorModel.getMisc();
        ViewMode viewMode = misc.viewMode();
        switch (viewMode) {
            case Hybrid: // score view on left, scene view on right
                sourceSceneBase.setEnabled(false);
                sourceSceneOverlay.setEnabled(false);
                sourceScore.setEnabled(false);
                targetSceneRightBase.setEnabled(true);
                targetSceneRightOverlay.setEnabled(true);
                targetSceneWideBase.setEnabled(false);
                targetSceneWideOverlay.setEnabled(false);
                targetScoreLeft.setEnabled(true);
                targetScoreRight.setEnabled(false);
                targetScoreWide.setEnabled(false);
                break;

            case Scene:
                sourceSceneBase.setEnabled(twoModelsLoaded);
                sourceSceneOverlay.setEnabled(twoModelsLoaded);
                sourceScore.setEnabled(false);
                targetSceneRightBase.setEnabled(twoModelsLoaded);
                targetSceneRightOverlay.setEnabled(twoModelsLoaded);
                targetSceneWideBase.setEnabled(!twoModelsLoaded);
                targetSceneWideOverlay.setEnabled(!twoModelsLoaded);
                targetScoreLeft.setEnabled(false);
                targetScoreRight.setEnabled(false);
                targetScoreWide.setEnabled(false);
                break;

            case Score:
                sourceSceneBase.setEnabled(false);
                sourceSceneOverlay.setEnabled(false);
                sourceScore.setEnabled(twoModelsLoaded);
                targetSceneRightBase.setEnabled(false);
                targetSceneRightOverlay.setEnabled(false);
                targetSceneWideBase.setEnabled(false);
                targetSceneWideOverlay.setEnabled(false);
                targetScoreLeft.setEnabled(false);
                targetScoreRight.setEnabled(twoModelsLoaded);
                targetScoreWide.setEnabled(!twoModelsLoaded);
                break;

            default:
                logger.log(Level.SEVERE, "view mode={0}", viewMode);
                throw new IllegalStateException("unknown view mode");
        }

        boolean split = isSplitScreen();
        if (split) {
            updateSideViewPort(sourceSceneBase, Side.Left);
            updateSideViewPort(sourceSceneOverlay, Side.Left);
            updateSideViewPort(sourceScore, Side.Left);
            updateSideViewPort(targetSceneRightBase, Side.Right);
            updateSideViewPort(targetSceneRightOverlay, Side.Right);
            updateSideViewPort(targetScoreLeft, Side.Left);
            updateSideViewPort(targetScoreRight, Side.Right);
        }
    }
    // *************************************************************************
    // private methods

    /**
     * Create a full-width view port for the boundary's drag handle.
     */
    private static void createBoundaryViewPort() {
        String name = "Boundary";
        Maud application = Maud.getApplication();
        Camera cam = application.getGuiViewPort().getCamera();
        Camera camera = cam.clone();
        camera.setName(name);
        RenderManager renderManager = application.getRenderManager();
        ViewPort viewPort = renderManager.createMainView(name, camera);
        viewPort.setClearFlags(false, false, false);
        viewPort.setEnabled(false);
        /*
         * Attach a scene to the new view port.
         */
        Node boundaryRoot = new Node("Root for " + name);
        boundaryRoot.setQueueBucket(RenderQueue.Bucket.Gui);
        viewPort.attachScene(boundaryRoot);
    }

    /**
     * Create an overlay for a pre-existing view port.
     *
     * @param base base view port (not null, unaffected)
     * @param overlayRoot root node for the new view port (not null, alias
     * created)
     * @return a new, disabled view port
     */
    private static ViewPort createOverlay(ViewPort base, Node overlayRoot) {
        String name = base.getName() + " Overlay";
        Camera camera = base.getCamera();
        RenderManager renderManager = Maud.getApplication().getRenderManager();
        ViewPort viewPort = renderManager.createMainView(name, camera);
        viewPort.attachScene(overlayRoot);
        viewPort.setClearFlags(false, false, false);
        viewPort.setEnabled(false);

        return viewPort;
    }

    /**
     * Instantiate a new camera with a partial-width view port.
     *
     * @param side which side of the boundary to put the viewport on (not null)
     * @return a new instance with perspective projection
     */
    private static Camera createSideCamera(Side side) {
        assert side != null;

        Camera cam = Maud.getApplication().getCamera();
        Camera newCamera = cam.clone();
        updateSideCamera(newCamera, side);

        assert !MyCamera.isFullWidth(newCamera);
        assert !newCamera.isParallelProjection();
        return newCamera;
    }

    /**
     * Create a left-side view port for the source scene.
     *
     * @return the attachment point (a new instance)
     */
    private static Node createSourceSceneViewPort() {
        String name = "Source Scene Left";
        Camera camera = createSideCamera(Side.Left);
        camera.setName(name);
        RenderManager renderManager = Maud.getApplication().getRenderManager();
        sourceSceneBase = renderManager.createMainView(name, camera);
        sourceSceneBase.setClearFlags(true, true, true);
        sourceSceneBase.setEnabled(false);
        /*
         * Attach a scene to the new view port.
         */
        Node scene = new Node("root for source scene base");
        sourceSceneBase.attachScene(scene);
        /*
         * Add an attachment point to the scene.
         */
        Node parent = new Node("parent for source CGM");
        scene.attachChild(parent);

        return parent;
    }

    /**
     * Create a left-side view port for the source score.
     */
    private static void createSourceScoreViewPort() {
        Camera camera = createSideCamera(Side.Left);
        camera.setName("Source Score Left");
        camera.setParallelProjection(true);
        RenderManager renderManager = Maud.getApplication().getRenderManager();
        sourceScore = renderManager.createMainView("Source Score", camera);
        sourceScore.setClearFlags(true, true, true);
        sourceScore.setEnabled(false);
        /*
         * Attach a scene to the new view port.
         */
        Node root = new Node("Root for source score");
        sourceScore.attachScene(root);
    }

    /**
     * Create a right-side view port for the target scene.
     *
     * @return the attachment point (a new instance)
     */
    private static Node createTargetSceneViewPort() {
        String name = "Target Scene Right";
        Camera camera = createSideCamera(Side.Right);
        camera.setName(name);
        Maud application = Maud.getApplication();
        RenderManager renderManager = application.getRenderManager();
        targetSceneRightBase = renderManager.createMainView(name, camera);
        targetSceneRightBase.setClearFlags(true, true, true);
        targetSceneRightBase.setEnabled(false);
        /*
         * Attach the existing scene to the new view port.
         */
        Node rootNode = application.getRootNode();
        targetSceneRightBase.attachScene(rootNode);
        /*
         * Add an attachment point to the scene.
         */
        Node parent = new Node("parent for target CGM");
        rootNode.attachChild(parent);

        return parent;
    }

    /**
     * Create a left-side view port for the target score.
     */
    private static void createTargetScoreLeftViewPort() {
        String name = "Target Score Left";
        Camera camera = createSideCamera(Side.Left);
        camera.setName(name);
        camera.setParallelProjection(true);
        RenderManager renderManager = Maud.getApplication().getRenderManager();
        targetScoreLeft = renderManager.createMainView(name, camera);
        targetScoreLeft.setClearFlags(true, true, true);
        targetScoreLeft.setEnabled(false);
        /*
         * Attach a scene to the new view port.
         */
        Node root = new Node("Root for " + name);
        targetScoreLeft.attachScene(root);
    }

    /**
     * Create a right-side view port for the target score.
     */
    private static void createTargetScoreRightViewPort() {
        String name = "Target Score Right";
        Camera camera = createSideCamera(Side.Right);
        camera.setName(name);
        camera.setParallelProjection(true);
        RenderManager renderManager = Maud.getApplication().getRenderManager();
        targetScoreRight = renderManager.createMainView(name, camera);
        targetScoreRight.setClearFlags(true, true, true);
        targetScoreRight.setEnabled(false);
        /*
         * Attach a scene to the new view port.
         */
        Node root = new Node("Root for " + name);
        targetScoreRight.attachScene(root);
    }

    /**
     * Create a full-width view port for the target score.
     */
    private static void createTargetScoreWideViewPort() {
        String name = "Target Score Wide";
        Maud application = Maud.getApplication();
        Camera cam = application.getCamera();
        Camera camera = cam.clone();
        camera.setName(name);
        camera.setParallelProjection(true);
        RenderManager renderManager = application.getRenderManager();
        targetScoreWide = renderManager.createMainView(name, camera);
        targetScoreWide.setClearFlags(true, true, true);
        targetScoreWide.setEnabled(false);
        /*
         * Attach a scene to the new view port.
         */
        Node root = new Node("Root for " + name);
        targetScoreWide.attachScene(root);
    }

    /**
     * Update the partial-width view port of the specified camera.
     *
     * @param camera the camera to update (not null)
     * @param side which side of the boundary the viewport is on (not null)
     */
    private static void updateSideCamera(Camera camera, Side side) {
        float xBoundary = Maud.getModel().getMisc().xBoundary();

        float leftEdge, rightEdge;
        switch (side) {
            case Left:
                leftEdge = 0f;
                rightEdge = xBoundary;
                break;
            case Right:
                leftEdge = xBoundary;
                rightEdge = 1f;
                break;
            default:
                throw new IllegalArgumentException(side.toString());
        }
        float bottomEdge = 0f;
        float topEdge = 1f;
        camera.setViewPort(leftEdge, rightEdge, bottomEdge, topEdge);
    }

    /**
     * Update the specified partial-width view port unless it's disabled.
     *
     * @param vp the view port to update (not null)
     * @param side which side of the boundary the viewport is on (not null)
     */
    private static void updateSideViewPort(ViewPort vp, Side side) {
        assert side != null;

        if (vp.isEnabled()) {
            Camera camera = vp.getCamera();
            updateSideCamera(camera, side);
        }
    }
}
