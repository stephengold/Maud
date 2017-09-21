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

import com.jme3.app.SimpleApplication;
import com.jme3.app.state.ScreenshotAppState;
import com.jme3.audio.openal.ALAudioRenderer;
import com.jme3.bullet.PhysicsSpace;
import com.jme3.post.FilterPostProcessor;
import com.jme3.renderer.Camera;
import com.jme3.renderer.ViewPort;
import com.jme3.scene.Node;
import com.jme3.scene.plugins.bvh.BVHLoader;
import com.jme3.shadow.DirectionalLightShadowFilter;
import com.jme3.shadow.EdgeFilteringMode;
import com.jme3.system.AppSettings;
import com.jme3.system.JmeSystem;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.prefs.BackingStoreException;
import jme3_ext_xbuf.XbufLoader;
import jme3utilities.Misc;
import jme3utilities.MyString;
import jme3utilities.StringLoader;
import jme3utilities.Validate;
import jme3utilities.debug.Dumper;
import jme3utilities.nifty.GuiApplication;
import jme3utilities.nifty.bind.BindScreen;
import jme3utilities.ui.InputMode;
import maud.dialog.QuitDialog;
import maud.model.Cgm;
import maud.model.EditorModel;
import maud.model.History;
import maud.model.option.ViewMode;
import maud.view.SceneView;
import maud.view.ScoreView;

/**
 * GUI application to edit jMonkeyEngine animated 3-D CG models. The
 * application's main entry point is in this class.
 *
 * @author Stephen Gold sgold@sonic.net
 */
public class Maud extends GuiApplication {
    // *************************************************************************
    // constants and loggers

    /**
     * width and height of rendered shadow maps (pixels per side, &gt;0)
     */
    final private static int shadowMapSize = 4_096;
    /**
     * number of shadow map splits in shadow filters (&gt;0)
     */
    final private static int shadowMapSplits = 3;
    /**
     * message logger for this class
     */
    final private static Logger logger = Logger.getLogger(Maud.class.getName());
    /**
     * path to the asset used to configure hotkey bindings
     */
    final private static String hotkeyBindingsAssetPath
            = "Interface/bindings/editor.properties";
    /**
     * application name for the window's title bar
     */
    final private static String windowTitle = "Maud";
    // *************************************************************************
    // fields

    /**
     * Nifty screen for editing hotkey bindings
     */
    final static BindScreen bindScreen = new BindScreen();
    /**
     * true once {@link #startup1()} has completed, until then false
     */
    private boolean didStartup1 = false;
    /**
     * dumper for scene dumps
     */
    final private static Dumper dumper = new Dumper();
    /**
     * controller for the editor screen, with links to menus, tools, etc.
     */
    final public static EditorScreen gui = new EditorScreen();
    /**
     * MVC model for the editor screen (live copy)
     */
    private static EditorModel editorModel = new EditorModel();
    /**
     * application instance, set by {@link #main(java.lang.String[])}
     */
    private static Maud application;
    /**
     * left half of the editor screen in scene mode
     */
    private ViewPort sourceSceneViewPort;
    /**
     * left half of the editor screen in score mode
     */
    private ViewPort sourceScoreViewPort;
    /**
     * right half of the editor screen in hybrid or scene mode
     */
    private ViewPort targetSceneRightViewPort;
    /**
     * left half of the editor screen in hybrid mode
     */
    private ViewPort targetScoreLeftViewPort;
    /**
     * right half of the editor screen in score mode
     */
    private ViewPort targetScoreRightViewPort;
    /**
     * the whole editor screen in score mode
     */
    private ViewPort targetScoreWideViewPort;
    // *************************************************************************
    // new methods exposed

    /**
     * Process a "dump physicsSpace" action.
     */
    public void dumpPhysicsSpace() {
        Cgm cgm = gui.mouseCgm();
        SceneView sceneView = cgm.getSceneView();
        PhysicsSpace space = sceneView.getPhysicsSpace();
        dumper.dump(space);
    }

    /**
     * Process a "dump renderer" action.
     */
    public void dumpRenderer() {
        dumper.setDumpBucket(true);
        dumper.setDumpCull(true);
        dumper.setDumpShadow(true);
        dumper.setDumpTransform(true);
        dumper.dump(renderManager);
    }

    /**
     * Process a "dump scene" action.
     */
    public void dumpScene() {
        dumper.setDumpBucket(true);
        dumper.setDumpCull(true);
        dumper.setDumpShadow(true);
        dumper.setDumpTransform(true);
        dumper.dump(rootNode);
    }

    /**
     * Access the application.
     *
     * @return the pre-existing instance
     */
    public static Maud getApplication() {
        return application;
    }

    /**
     * Access the MVC model for the editor screen (live copy).
     *
     * @return the pre-existing instance
     */
    public static EditorModel getModel() {
        assert editorModel != null;
        return editorModel;
    }

    /**
     * Main entry point for Maud.
     *
     * @param arguments array of command-line arguments (not null)
     */
    public static void main(String[] arguments) {
        /*
         * Mute the chatty loggers found in some imported packages.
         */
        Misc.setLoggingLevels(Level.WARNING);
        Logger.getLogger(ALAudioRenderer.class.getName())
                .setLevel(Level.SEVERE);
        /*
         * Lower logging thresholds for classes of interest.
         */
        History.logger.setLevel(Level.INFO);
        /*
         * Load app settings from persistent storage.
         */
        boolean loadDefaults = true;
        AppSettings settings = new AppSettings(loadDefaults);
        String preferencesKey = Maud.class.getName();
        try {
            settings.load(preferencesKey);
        } catch (BackingStoreException e) {
            logger.log(Level.WARNING, "App settings were not loaded.");
        }
        /*
         * Apply overrides and dialog input.
         */
        settings.setGammaCorrection(false);
        settings.setSettingsDialogImage("Textures/icons/Maud-settings.png");
        settings.setVSync(true);
        boolean loadFromRegistry = false;
        boolean cont = JmeSystem.showSettingsDialog(settings, loadFromRegistry);
        if (!cont) {
            return;
        }
        settings.setTitle(windowTitle);
        /*
         * Save app settings to persistent storage.
         */
        try {
            settings.save(preferencesKey);
        } catch (BackingStoreException e) {
            logger.log(Level.WARNING, "App settings were not saved.");
        }
        /*
         * Instantiate and start the application.
         */
        application = new Maud();
        application.setSettings(settings);
        application.setShowSettings(false);
        application.start();
        /*
         * ... and onward to Maud.guiInitializeApplication()!
         */
    }

    /**
     * Alter the MVC model for the editor screen (live copy). Used only when
     * restoring a checkpoint.
     *
     * @param savedState saved model state to make live (not null)
     */
    public static void setModel(EditorModel savedState) {
        Validate.nonNull(savedState, "saved state");
        editorModel = savedState;
    }

    /**
     * Initialization performed the 1st time the editor screen is displayed.
     */
    void startup2() {
        logger.info("");
        /*
         * Attach controllers for windows in the editor screen.
         */
        gui.tools.attachAll(stateManager);
        /*
         * Disable flyCam.
         */
        flyCam.setEnabled(false);
        /**
         * Manage view port updating.
         */
        ViewPortAppState viewPortState = new ViewPortAppState();
        boolean success = stateManager.attach(viewPortState);
        assert success;
        /*
         * Capture a screenshot each time the SYSRQ hotkey is pressed.
         */
        ScreenshotAppState screenShotState = new ScreenshotAppState();
        success = stateManager.attach(screenShotState);
        assert success;
        /*
         * Add a locator for browsing "Written Assets". TODO read startup.txt
         * first and only do this as a fallback
         */
        String wadp = getWrittenAssetDirPath();
        editorModel.getLocations().addFilesystem(wadp);
    }

    /**
     * Update the configuration of view ports to reflect the MVC model.
     */
    void updateViewPorts() {
        boolean splitScreen = editorModel.getSource().isLoaded();

        ViewMode viewMode = editorModel.getMisc().getViewMode();
        switch (viewMode) {
            case Hybrid:
                sourceSceneViewPort.setEnabled(false);
                targetSceneRightViewPort.setEnabled(true);
                viewPort.setEnabled(false);
                sourceScoreViewPort.setEnabled(false);
                targetScoreLeftViewPort.setEnabled(true);
                targetScoreRightViewPort.setEnabled(false);
                targetScoreWideViewPort.setEnabled(false);
                break;

            case Scene:
                sourceSceneViewPort.setEnabled(splitScreen);
                targetSceneRightViewPort.setEnabled(splitScreen);
                viewPort.setEnabled(!splitScreen);
                sourceScoreViewPort.setEnabled(false);
                targetScoreLeftViewPort.setEnabled(false);
                targetScoreRightViewPort.setEnabled(false);
                targetScoreWideViewPort.setEnabled(false);
                break;

            case Score:
                sourceSceneViewPort.setEnabled(false);
                targetSceneRightViewPort.setEnabled(false);
                viewPort.setEnabled(false);
                sourceScoreViewPort.setEnabled(splitScreen);
                targetScoreLeftViewPort.setEnabled(false);
                targetScoreRightViewPort.setEnabled(splitScreen);
                targetScoreWideViewPort.setEnabled(!splitScreen);
                break;

            default:
                logger.log(Level.SEVERE, "view mode={0}", viewMode);
                throw new IllegalStateException("unknown view mode");
        }
    }
    // *************************************************************************
    // ActionApplication methods

    /**
     * Callback invoked when an ongoing action isn't handled.
     *
     * @param actionString textual description of the action (not null)
     */
    @Override
    public void didntHandle(String actionString) {
        super.didntHandle(actionString);

        String message = String.format("unimplemented feature (action = %s)",
                MyString.quote(actionString));
        gui.setStatus(message);
    }
    // *************************************************************************
    // ActionListener methods

    /**
     * Process an action (from the GUI or keyboard) that wasn't handled by the
     * input mode.
     *
     * @param actionString textual description of the action (not null)
     * @param ongoing true if the action is ongoing, otherwise false
     * @param tpf time interval between render passes (in seconds, &ge;0)
     */
    @Override
    public void onAction(String actionString, boolean ongoing, float tpf) {
        boolean handled = false;
        if (ongoing) {
            logger.log(Level.INFO, "Got ongoing action {0}",
                    MyString.quote(actionString));

            handled = true;
            switch (actionString) {
                case "dump physicsSpace":
                    dumpPhysicsSpace();
                    break;
                case "dump renderer":
                    dumpRenderer();
                    break;
                case "dump scene":
                    dumpScene();
                    break;
                case "edit bindings":
                    InputMode im = InputMode.getActiveMode();
                    bindScreen.activate(im);
                    break;
                case "quit":
                    quit();
                    break;
                default:
                    handled = false;
            }
        }

        if (!handled) {
            /*
             * Forward unhandled action to the superclass.
             */
            super.onAction(actionString, ongoing, tpf);
        }
    }
    // *************************************************************************
    // GuiApplication methods

    /**
     * Initialize this application.
     */
    @Override
    public void guiInitializeApplication() {
        logger.info("");
        if (!Misc.areAssertionsEnabled()) {
            logger.warning("Assertions are disabled.");
        }

        StartScreen startScreen = new StartScreen();
        stateManager.attach(startScreen);
    }
    // *************************************************************************
    // SimpleApplication methods

    /**
     * Callback invoked once per render pass.
     *
     * @param tpf time interval between render passes (in seconds, &ge;0)
     */
    @Override
    public void simpleUpdate(float tpf) {
        super.simpleUpdate(tpf);

        if (!didStartup1) {
            startup1();
            didStartup1 = true;
        }
    }
    // *************************************************************************
    // private methods

    /**
     * Add shadows to the specified view port, without specifying a light.
     */
    private void addShadows(ViewPort vp) {
        DirectionalLightShadowFilter dlsf = new DirectionalLightShadowFilter(
                assetManager, shadowMapSize, shadowMapSplits);
        dlsf.setEdgeFilteringMode(EdgeFilteringMode.PCF8);
        dlsf.setEnabled(false);

        FilterPostProcessor fpp = Misc.getFpp(vp, assetManager);
        fpp.addFilter(dlsf);
    }

    /**
     * Instantiate a camera for a half-width view port.
     *
     * @param leftEdge which side (0 &rarr; left, 0.5 &rarr; right)
     * @return a new instance with perspective projection
     */
    private Camera createHalfCamera(float leftEdge) {
        Camera camera = cam.clone();
        float bottomEdge = 0f;
        float rightEdge = leftEdge + 0.5f;
        float topEdge = 1f;
        camera.setViewPort(leftEdge, rightEdge, bottomEdge, topEdge);

        return camera;
    }

    /**
     * Create a left-half view port for the source scene.
     *
     * @return the attachment point (a new instance)
     */
    private Node createSourceSceneViewPort() {
        String name = "Source Scene Left";
        Camera camera = createHalfCamera(0f);
        camera.setName(name);
        sourceSceneViewPort = renderManager.createMainView(name, camera);
        sourceSceneViewPort.setClearFlags(true, true, true);
        sourceSceneViewPort.setEnabled(false);
        addShadows(sourceSceneViewPort);
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
    private void createSourceScoreViewPort() {
        String name = "Source Score Left";
        Camera camera = createHalfCamera(0f);
        camera.setName(name);
        camera.setParallelProjection(true);
        sourceScoreViewPort = renderManager.createMainView(
                "Source Score", camera);
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
    private Node createTargetSceneViewPort() {
        String name = "Target Scene Right";
        Camera camera = createHalfCamera(0.5f);
        camera.setName(name);
        targetSceneRightViewPort = renderManager.createMainView(name, camera);
        targetSceneRightViewPort.setClearFlags(true, true, true);
        targetSceneRightViewPort.setEnabled(false);
        addShadows(targetSceneRightViewPort);
        /*
         * Attach the existing scene to the new view port.
         */
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
    private void createTargetScoreLeftViewPort() {
        String name = "Target Score Left";
        Camera camera = createHalfCamera(0f);
        camera.setName(name);
        camera.setParallelProjection(true);
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
    private void createTargetScoreRightViewPort() {
        String name = "Target Score Right";
        Camera camera = createHalfCamera(0.5f);
        camera.setName(name);
        camera.setParallelProjection(true);
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
    private void createTargetScoreWideViewPort() {
        String name = "Target Score Wide";
        Camera camera = cam.clone();
        camera.setName(name);
        camera.setParallelProjection(true);
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
     * If confirmed, terminate the application.
     */
    private void quit() {
        int cgmEdits = editorModel.getTarget().countUnsavedEdits();
        int mapEdits = editorModel.getMap().countUnsavedEdits();

        String message;
        if (cgmEdits + mapEdits == 0) {
            message = "Quit Maud?";
        } else {
            message = "You've made ";
            if (cgmEdits > 0) {
                message += String.format("%d unsaved edit%s to the model",
                        cgmEdits, cgmEdits == 1 ? "" : "s");
            }
            if (cgmEdits > 0 && mapEdits > 0) {
                message += " and ";
            }
            if (mapEdits > 0) {
                message += String.format(
                        "%d unsaved edit%s to the skeleton map", mapEdits,
                        mapEdits == 1 ? "" : "s");
            }
            message += ".\nReally quit Maud?";
        }

        QuitDialog controller = new QuitDialog();
        gui.showConfirmDialog(message, "", SimpleApplication.INPUT_MAPPING_EXIT,
                controller);
    }

    /**
     * Initialization performed during the 1st invocation of
     * {@link #simpleUpdate(float)}.
     */
    private void startup1() {
        logger.info("");
        /*
         * Register loaders for BVH, txt, and Xbuf assets.
         */
        assetManager.registerLoader(BVHLoader.class, "bvh", "BVH");
        assetManager.registerLoader(StringLoader.class, "txt");
        assetManager.registerLoader(XbufLoader.class, "xbuf");
        /*
         * Configure the default view port for the target scene wide view.
         */
        cam.setName("Target Scene Wide");
        addShadows(viewPort);
        /*
         * Create 2 view ports for split-screen scene views.
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
         * Attach views to CG model slots.
         */
        editorModel.getSource().setViews(sourceSceneView, sourceScoreView);
        editorModel.getTarget().setViews(targetSceneView, targetScoreView);
        /*
         * Attach screen controllers for the editor screen and the bind screen.
         */
        stateManager.attachAll(gui, bindScreen);
        /*
         * Configure and attach input mode for the editor screen.
         */
        gui.inputMode.setConfigPath(hotkeyBindingsAssetPath);
        stateManager.attach(gui.inputMode);
        /*
         * Disable the JME statistic displays.
         * These can be re-enabled by pressing the F5 hotkey.
         */
        setDisplayFps(false);
        setDisplayStatView(false);
    }
}
