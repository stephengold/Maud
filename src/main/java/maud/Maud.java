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
import com.jme3.renderer.Camera;
import com.jme3.renderer.ViewPort;
import com.jme3.scene.Node;
import com.jme3.scene.Spatial;
import com.jme3.scene.plugins.bvh.BVHLoader;
import com.jme3.system.AppSettings;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import jme3utilities.Misc;
import jme3utilities.MyString;
import jme3utilities.debug.Printer;
import jme3utilities.nifty.GuiApplication;
import jme3utilities.nifty.bind.BindScreen;
import jme3utilities.ui.InputMode;
import maud.model.EditorModel;
import maud.model.History;
import maud.model.LoadedCgm;

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
     * additional scenes (besides rootNode and guiRoot) for rendering
     */
    final private static List<Spatial> addedScenes = new ArrayList<>(3);
    /**
     * message logger for this class
     */
    final private static Logger logger = Logger.getLogger(Maud.class.getName());
    /**
     * path to hotkey bindings configuration asset
     */
    final private static String hotkeyBindingsAssetPath = "Interface/bindings/3DView.properties";
    /**
     * application name for the window's title bar
     */
    final private static String windowTitle = "Maud";
    // *************************************************************************
    // fields

    /**
     * true once {@link #startup1()} has completed, until then false
     */
    private boolean didStartup1 = false;
    /**
     * Nifty screen for editing hotkey bindings
     */
    final static BindScreen bindScreen = new BindScreen();
    /**
     * GUI portion of the editor screen, with links to tools
     */
    final public static EditorScreen gui = new EditorScreen();
    /**
     * MVC model for the editor screen
     */
    public static EditorModel model = new EditorModel();
    /**
     * application instance, set by {@link #main(java.lang.String[])}
     */
    private static Maud application;
    /**
     * printer for scene dumps TODO rename dumper
     */
    final private static Printer printer = new Printer();
    /**
     * view port for left half of split screen
     */
    private ViewPort sourceCgmViewPort;
    /**
     * view port for right half of split screen
     */
    private ViewPort targetCgmViewPort;
    // *************************************************************************
    // new methods exposed

    /**
     * Process a "print scene" action.
     */
    public void dumpScene() {
        printer.setPrintCull(true);
        printer.setPrintTransform(true);
        printer.printSubtree(rootNode);
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
        Logger.getLogger(LoadedCgm.class.getName()).setLevel(Level.INFO);
        History.logger.setLevel(Level.INFO);
        /*
        * Instantiate the application.
         */
        application = new Maud();
        /*
        * Customize the window's title bar.
         */
        AppSettings settings = new AppSettings(true);
        settings.setTitle(windowTitle);
        application.setSettings(settings);

        application.start();
        /*
        * ... and onward to Maud.guiInitializeApplication()!
         */
    }

    /**
     * Initialization performed the 1st time the editor screen is displayed.
     */
    void startup2() {
        logger.info("");
        /*
         * Attach controllers for windows in the editor screen.
         */
        gui.tools.attachAll(stateManager); // TODO attach()
        /*
         * Disable flyCam.
         */
        flyCam.setEnabled(false);
        /*
         * Capture a screenshot each time the SYSRQ hotkey is pressed.
         */
        ScreenshotAppState screenShotState = new ScreenshotAppState();
        boolean success = stateManager.attach(screenShotState);
        assert success;
    }

    /**
     * Update the configuration of the view ports.
     */
    public void updateViewPorts() {
        boolean splitScreen = Maud.model.source.isLoaded();
        sourceCgmViewPort.setEnabled(splitScreen);
        targetCgmViewPort.setEnabled(splitScreen);
        viewPort.setEnabled(!splitScreen);
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

            switch (actionString) {
                case "edit bindings":
                    InputMode im = InputMode.getActiveMode();
                    bindScreen.activate(im);
                    handled = true;
                    break;

                case "print scene":
                    dumpScene();
                    handled = true;
                    break;

                case "quit":
                    QuitDialog controller = new QuitDialog();
                    gui.showConfirmDialog("Quit Maud?", "",
                            SimpleApplication.INPUT_MAPPING_EXIT, controller);
                    handled = true;
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

        Locators.setAssetManager(assetManager);
        Locators.useDefault();

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
        /*
         * Update the logical/geometric state of all scenes other than
         * rootNode and guiNode.
         */
        for (Spatial scene : addedScenes) {
            scene.updateLogicalState(tpf);
            scene.updateGeometricState();
        }
    }
    // *************************************************************************
    // private methods

    /**
     * Initialization performed during the 1st invocation of
     * {@link #simpleUpdate(float)}.
     */
    private void startup1() {
        logger.info("");
        /*
         * register a loader for BVH files
         */
        assetManager.registerLoader(BVHLoader.class, "bvh", "BVH");
        /*
         * Create 2 view ports for split-screen editing.
         */
        createSourceCgmViewPort();
        createTargetCgmViewPort();
        /*
         * Attach screen controllers for the editor screen and bind screen.
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

    /**
     * Create view port for the source CG model's scene.
     */
    private void createSourceCgmViewPort() {
        Camera camera = cam.clone();
        float bottomEdge = 0f;
        float leftEdge = 0f;
        float rightEdge = 0.5f;
        float topEdge = 1f;
        camera.setViewPort(leftEdge, rightEdge, bottomEdge, topEdge);
        // TODO why is resize() needed?
        camera.resize(cam.getWidth(), cam.getHeight(), true);
        sourceCgmViewPort = renderManager.createMainView("Source Scene",
                camera);
        sourceCgmViewPort.setClearFlags(true, true, true);
        sourceCgmViewPort.setEnabled(false);
        /*
         * Attach a scene to the new view port.
         */
        Node scene = new Node("root for source CGM view");
        sourceCgmViewPort.attachScene(scene);
        addedScenes.add(scene);
        /*
         * Add an attachment point to the scene.
         */
        Node parent = new Node("parent for source CGM");
        scene.attachChild(parent);

        CgmView view;
        view = new CgmView(Maud.model.source, parent, null, sourceCgmViewPort);
        Maud.model.source.setView(view);
    }

    /**
     * Create a split-screen view port for target CG model's scene.
     */
    private void createTargetCgmViewPort() {
        Camera camera = cam.clone();
        float bottomEdge = 0f;
        float leftEdge = 0.5f;
        float rightEdge = 1f;
        float topEdge = 1f;
        camera.setViewPort(leftEdge, rightEdge, bottomEdge, topEdge);
        // TODO why is resize() needed?
        camera.resize(cam.getWidth(), cam.getHeight(), true);
        targetCgmViewPort = renderManager.createMainView("Target Scene",
                camera);
        targetCgmViewPort.setClearFlags(true, true, true);
        targetCgmViewPort.setEnabled(false);
        /*
         * Attach the existing scene to the new view port.
         */
        targetCgmViewPort.attachScene(rootNode);
        /*
         * Add an attachment point to the scene.
         */
        Node parent = new Node("parent for target CGM");
        rootNode.attachChild(parent);

        CgmView view = new CgmView(Maud.model.target, parent, viewPort,
                targetCgmViewPort);
        Maud.model.target.setView(view);
    }
}
