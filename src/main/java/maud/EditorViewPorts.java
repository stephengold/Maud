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
package maud;

import com.jme3.asset.AssetManager;
import com.jme3.renderer.Camera;
import com.jme3.renderer.RenderManager;
import com.jme3.renderer.ViewPort;
import com.jme3.scene.Node;
import com.jme3.shadow.DirectionalLightShadowRenderer;
import java.util.logging.Level;
import java.util.logging.Logger;
import jme3utilities.Validate;
import jme3utilities.ui.Locators;
import maud.model.EditorModel;
import maud.model.option.MiscOptions;
import maud.model.option.ViewMode;
import maud.model.option.scene.SceneOptions;
import maud.view.SceneView;
import maud.view.ScoreView;

/**
 * Viewports used by Maud's "editor" screen.
 *
 * @author Stephen Gold sgold@sonic.net
 */
public class EditorViewPorts {
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
     * left half of the display in scene mode
     */
    private static ViewPort sourceSceneViewPort;
    /**
     * left half of the display in score mode
     */
    private static ViewPort sourceScoreViewPort;
    /**
     * right half of the display in hybrid mode or scene mode
     */
    private static ViewPort targetSceneRightViewPort;
    /**
     * left half of the display in hybrid mode
     */
    private static ViewPort targetScoreLeftViewPort;
    /**
     * right half of the display in score mode
     */
    private static ViewPort targetScoreRightViewPort;
    /**
     * the whole display in score mode
     */
    private static ViewPort targetScoreWideViewPort;
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
     * @param vp which view port (not null)
     * @return the new, disabled instance
     */
    public static DirectionalLightShadowRenderer addShadows(ViewPort vp) {
        Validate.nonNull(vp, "view port");

        AssetManager manager = Locators.getAssetManager();
        SceneOptions options = Maud.getModel().getScene();
        int mapSize = options.getShadowMapSize();
        int numSplits = options.getNumSplits();
        DirectionalLightShadowRenderer dlsr;
        dlsr = new DirectionalLightShadowRenderer(manager, mapSize, numSplits);
        vp.addProcessor(dlsr);

        return dlsr;
    }

    /**
     * Initialization performed during the 1st invocation of
     * {@link #simpleUpdate(float)}.
     */
    static void startup1() {
        /*
         * Configure the default view port for the target scene wide view.
         */
        Maud application = Maud.getApplication();
        Camera cam = application.getCamera();
        cam.setName("Target Scene Wide");
        ViewPort viewPort = application.getViewPort();
        /*
         * Create 2 view ports for split-display scene views.
         */
        Node sourceSceneParent = createSourceSceneViewPort();
        Node targetSceneParent = createTargetSceneViewPort();
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
        SceneView sourceSceneView = new SceneView(editorModel.getSource(),
                sourceSceneParent, null, sourceSceneViewPort);
        SceneView targetSceneView = new SceneView(editorModel.getTarget(),
                targetSceneParent, viewPort, targetSceneRightViewPort);
        /*
         * Create 2 score views.
         */
        ScoreView sourceScoreView;
        sourceScoreView = new ScoreView(null, sourceScoreViewPort, null);
        ScoreView targetScoreView = new ScoreView(targetScoreWideViewPort,
                targetScoreRightViewPort, targetScoreLeftViewPort);
        /*
         * Attach views to C-G model slots.
         */
        editorModel.getSource().setViews(sourceSceneView, sourceScoreView);
        editorModel.getTarget().setViews(targetSceneView, targetScoreView);
    }

    /**
     * Update the configuration of view ports to reflect the MVC model.
     */
    static void update() {
        ViewPort viewPort = Maud.getApplication().getViewPort();
        EditorModel editorModel = Maud.getModel();
        boolean twoModelsLoaded = editorModel.getSource().isLoaded();
        MiscOptions misc = editorModel.getMisc();

        ViewMode viewMode = misc.getViewMode();
        switch (viewMode) {
            case Hybrid: // score on left, scene on right
                sourceSceneViewPort.setEnabled(false);
                targetSceneRightViewPort.setEnabled(true);
                viewPort.setEnabled(false);
                sourceScoreViewPort.setEnabled(false);
                targetScoreLeftViewPort.setEnabled(true);
                targetScoreRightViewPort.setEnabled(false);
                targetScoreWideViewPort.setEnabled(false);
                break;

            case Scene:
                sourceSceneViewPort.setEnabled(twoModelsLoaded);
                targetSceneRightViewPort.setEnabled(twoModelsLoaded);
                viewPort.setEnabled(!twoModelsLoaded);
                sourceScoreViewPort.setEnabled(false);
                targetScoreLeftViewPort.setEnabled(false);
                targetScoreRightViewPort.setEnabled(false);
                targetScoreWideViewPort.setEnabled(false);
                break;

            case Score:
                sourceSceneViewPort.setEnabled(false);
                targetSceneRightViewPort.setEnabled(false);
                viewPort.setEnabled(false);
                sourceScoreViewPort.setEnabled(twoModelsLoaded);
                targetScoreLeftViewPort.setEnabled(false);
                targetScoreRightViewPort.setEnabled(twoModelsLoaded);
                targetScoreWideViewPort.setEnabled(!twoModelsLoaded);
                break;

            default:
                logger.log(Level.SEVERE, "view mode={0}", viewMode);
                throw new IllegalStateException("unknown view mode");
        }
    }
    // *************************************************************************
    // private methods

    /**
     * Instantiate a new camera with a half-width view port.
     *
     * @param onRightSide which side of the boundary the viewport is on (false
     * &rarr; left, true &rarr; right)
     * @return a new instance with perspective projection
     */
    private static Camera createHalfCamera(boolean onRightSide) {
        Camera cam = Maud.getApplication().getCamera();
        Camera camera = cam.clone();
        float xBoundary = 0.5f;
        updateSideCamera(camera, onRightSide, xBoundary);

        return camera;
    }

    /**
     * Create a left-half view port for the source scene.
     *
     * @return the attachment point (a new instance)
     */
    private static Node createSourceSceneViewPort() {
        String name = "Source Scene Left";
        Camera camera = createHalfCamera(false);
        camera.setName(name);
        RenderManager renderManager = Maud.getApplication().getRenderManager();
        sourceSceneViewPort = renderManager.createMainView(name, camera);
        sourceSceneViewPort.setClearFlags(true, true, true);
        sourceSceneViewPort.setEnabled(false);
        /*
         * Attach a scene to the new view port.
         */
        Node scene = new Node("Root for source scene");
        sourceSceneViewPort.attachScene(scene);
        /*
         * Add an attachment point to the scene.
         */
        Node parent = new Node("parent for source CGM");
        scene.attachChild(parent);

        return parent;
    }

    /**
     * Create a left-half view port for the source score.
     */
    private static void createSourceScoreViewPort() {
        String name = "Source Score Left";
        Camera camera = createHalfCamera(false);
        camera.setName(name);
        camera.setParallelProjection(true);
        RenderManager renderManager = Maud.getApplication().getRenderManager();
        sourceScoreViewPort
                = renderManager.createMainView("Source Score", camera);
        sourceScoreViewPort.setClearFlags(true, true, true);
        sourceScoreViewPort.setEnabled(false);
        /*
         * Attach a scene to the new view port.
         */
        Node root = new Node("Root for source score");
        sourceScoreViewPort.attachScene(root);
    }

    /**
     * Create a right-half view port for the target scene.
     *
     * @return the attachment point (a new instance)
     */
    private static Node createTargetSceneViewPort() {
        String name = "Target Scene Right";
        Camera camera = createHalfCamera(true);
        camera.setName(name);
        Maud application = Maud.getApplication();
        RenderManager renderManager = application.getRenderManager();
        targetSceneRightViewPort = renderManager.createMainView(name, camera);
        targetSceneRightViewPort.setClearFlags(true, true, true);
        targetSceneRightViewPort.setEnabled(false);
        /*
         * Attach the existing scene to the new view port.
         */
        Node rootNode = application.getRootNode();
        targetSceneRightViewPort.attachScene(rootNode);
        /*
         * Add an attachment point to the scene.
         */
        Node parent = new Node("parent for target CGM");
        rootNode.attachChild(parent);

        return parent;
    }

    /**
     * Create a left-half view port for the target score.
     */
    private static void createTargetScoreLeftViewPort() {
        String name = "Target Score Left";
        Camera camera = createHalfCamera(false);
        camera.setName(name);
        camera.setParallelProjection(true);
        RenderManager renderManager = Maud.getApplication().getRenderManager();
        targetScoreLeftViewPort = renderManager.createMainView(name, camera);
        targetScoreLeftViewPort.setClearFlags(true, true, true);
        targetScoreLeftViewPort.setEnabled(false);
        /*
         * Attach a scene to the new view port.
         */
        Node root = new Node("Root for " + name);
        targetScoreLeftViewPort.attachScene(root);
    }

    /**
     * Create a right-half view port for the target score.
     */
    private static void createTargetScoreRightViewPort() {
        String name = "Target Score Right";
        Camera camera = createHalfCamera(true);
        camera.setName(name);
        camera.setParallelProjection(true);
        RenderManager renderManager = Maud.getApplication().getRenderManager();
        targetScoreRightViewPort = renderManager.createMainView(name, camera);
        targetScoreRightViewPort.setClearFlags(true, true, true);
        targetScoreRightViewPort.setEnabled(false);
        /*
         * Attach a scene to the new view port.
         */
        Node root = new Node("Root for " + name);
        targetScoreRightViewPort.attachScene(root);
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
        targetScoreWideViewPort = renderManager.createMainView(name, camera);
        targetScoreWideViewPort.setClearFlags(true, true, true);
        targetScoreWideViewPort.setEnabled(false);
        /*
         * Attach a scene to the new view port.
         */
        Node root = new Node("Root for " + name);
        targetScoreWideViewPort.attachScene(root);
    }

    /**
     * Update the partial-width view port of the specified camera.
     *
     * @param camera the camera to update (not null)
     * @param onRightSide which side of the boundary the viewport is on (false
     * &rarr; left, true &rarr; right)
     * @param xBoundary the display X-coordinate of the left-right boundary
     * (&gt;0, &lt;1)
     */
    private static void updateSideCamera(Camera camera, boolean onRightSide,
            float xBoundary) {
        assert xBoundary > 0f : xBoundary;
        assert xBoundary < 1f : xBoundary;

        float leftEdge, rightEdge;
        if (onRightSide) {
            leftEdge = xBoundary;
            rightEdge = 1f;
        } else {
            leftEdge = 0f;
            rightEdge = xBoundary;
        }
        float bottomEdge = 0f;
        float topEdge = 1f;
        camera.setViewPort(leftEdge, rightEdge, bottomEdge, topEdge);
    }
}
